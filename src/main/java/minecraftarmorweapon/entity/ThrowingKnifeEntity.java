package minecraftarmorweapon.entity;

import minecraftarmorweapon.init.CustomEntityInit;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages;

/**
 * 投げナイフ飛翔体。
 * 右クリックで投擲、敵にヒット時はダメージを与えて消滅、
 * ブロックにヒット時はその場にアイテムとしてドロップする（再拾得可能）。
 */
@OnlyIn(value = Dist.CLIENT, _interface = ItemSupplier.class)
public class ThrowingKnifeEntity extends ThrowableItemProjectile implements ItemSupplier {

    /** ナイフ種類 — 飛翔/着弾挙動を分岐させる */
    public enum KnifeType {
        NORMAL, GRIP, STUN, SCREW, HOMING;
        public static KnifeType byId(int i) {
            KnifeType[] v = values();
            return (i < 0 || i >= v.length) ? NORMAL : v[i];
        }
    }

    private static final float DAMAGE = 6.0f;
    private static final int STUCK_LIFETIME_TICKS = 600; // 30秒で消滅

    /**
     * SCREW ナイフのロール回転速度 (度/tick)。
     * renderer (ThrowingKnifeRenderer) もこの値を参照してパーティクルの渦方向と同期させる。
     * 120°/tick = 2400°/sec ≒ 6.7 回転/秒。
     */
    public static final float SCREW_SPIN_DEG_PER_TICK = 120f;
    private static final EntityDataAccessor<Integer> DATA_KNIFE_TYPE =
        SynchedEntityData.defineId(ThrowingKnifeEntity.class, EntityDataSerializers.INT);

    // @StickParams - 着弾時の調整値 (手動編集してビルド) — 単位: 1 = 1/100ブロック
    public static double STICK_OFFSET_NORMAL  = -5;  // 衝突面の法線方向 (壁から外向き)。+ = 手前に出る/浮く, - = めり込む
    public static double STICK_OFFSET_FORWARD = 20;  // 進行方向 (投げた方向)。0 = 刃先がブロック表面に接する, + = もっと突き刺さる, - = 手前に止まる
    // @EndStickParams
    private static final double STICK_UNIT = 0.01;   // 1単位 = 1/100ブロック
    // 0 で刃先がブロック表面に来るよう、エンティティ中心から刃先までの距離を補正で引く
    private static final double BLADE_TIP_LENGTH = 0.5;

    // 刺さり状態はクライアントにも同期が必要 (クライアント側のtickでrotation再計算を防止)
    private static final EntityDataAccessor<Boolean> DATA_STUCK =
        SynchedEntityData.defineId(ThrowingKnifeEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_STUCK_YAW =
        SynchedEntityData.defineId(ThrowingKnifeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_STUCK_PITCH =
        SynchedEntityData.defineId(ThrowingKnifeEntity.class, EntityDataSerializers.FLOAT);

    private int stuckTicks = 0;

    public boolean isStuck() { return this.entityData.get(DATA_STUCK); }
    public float getStuckYaw() { return this.entityData.get(DATA_STUCK_YAW); }
    public float getStuckPitch() { return this.entityData.get(DATA_STUCK_PITCH); }
    public KnifeType getKnifeType() { return KnifeType.byId(this.entityData.get(DATA_KNIFE_TYPE)); }
    public void setKnifeType(KnifeType t) { this.entityData.set(DATA_KNIFE_TYPE, t.ordinal()); }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_STUCK, false);
        this.entityData.define(DATA_STUCK_YAW, 0f);
        this.entityData.define(DATA_STUCK_PITCH, 0f);
        this.entityData.define(DATA_KNIFE_TYPE, 0);
    }

    public ThrowingKnifeEntity(PlayMessages.SpawnEntity packet, Level world) {
        super(CustomEntityInit.THROWING_KNIFE_ENTITY.get(), world);
    }

    public ThrowingKnifeEntity(EntityType<? extends ThrowingKnifeEntity> type, Level world) {
        super(type, world);
    }

    public ThrowingKnifeEntity(Level world, LivingEntity thrower) {
        super(CustomEntityInit.THROWING_KNIFE_ENTITY.get(), thrower, world);
    }

    @Override
    protected Item getDefaultItem() {
        return CustomEntityInit.THROWING_KNIFE.get();
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity target = result.getEntity();
        if (target == this.getOwner()) return;
        KnifeType type = getKnifeType();
        float dmg = switch (type) {
            case GRIP   -> DAMAGE + 2.0f;
            case STUN   -> DAMAGE + 4.0f;
            case SCREW  -> DAMAGE + 1.0f;
            case HOMING -> DAMAGE;
            default     -> DAMAGE;
        };
        target.hurt(this.damageSources().thrown(this, this.getOwner()), dmg);

        // STUN: 感電(雷視覚) + 移動速度低下/弱体化
        if (type == KnifeType.STUN && target instanceof net.minecraft.world.entity.LivingEntity le) {
            le.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 60, 3, false, true));
            le.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.WEAKNESS, 60, 1, false, true));
            if (!this.level().isClientSide && this.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                    le.getX(), le.getY() + le.getBbHeight() / 2.0, le.getZ(),
                    20, 0.4, 0.6, 0.4, 0.2);
            }
        }

        if (!this.level().isClientSide) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.ARROW_HIT, SoundSource.PLAYERS, 1.0f, 1.2f);
            this.discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (this.level().isClientSide) return;

        // SCREW: 木材/木製コンテナを破壊して貫通継続
        if (getKnifeType() == KnifeType.SCREW && tryScrewBreak(result)) return;

        // 衝突直前のdeltaMovementから投げた方向の姿勢を計算して同期
        // (tick処理がこの直後に deltaMovement=0 から atan2(0,0)=0 で rotation をリセットするため)
        Vec3 v = this.getDeltaMovement();
        double horiz = Math.sqrt(v.x * v.x + v.z * v.z);
        float savedYaw;
        float savedPitch;
        if (v.lengthSqr() > 1e-6) {
            savedYaw = (float)(Math.atan2(v.x, v.z) * (180.0 / Math.PI));
            savedPitch = (float)(Math.atan2(v.y, horiz) * (180.0 / Math.PI));
        } else {
            savedYaw = this.getYRot();
            savedPitch = this.getXRot();
        }
        this.entityData.set(DATA_STUCK_YAW, savedYaw);
        this.entityData.set(DATA_STUCK_PITCH, savedPitch);
        this.entityData.set(DATA_STUCK, true);

        // 衝突位置オフセット (1単位 = 1/100ブロック)
        Vec3 hitPos = result.getLocation();
        net.minecraft.core.Direction face = result.getDirection();
        double normalAmt = STICK_OFFSET_NORMAL * STICK_UNIT;   // 法線方向の実距離(ブロック)
        // 進行方向: 0で刃先がブロック表面に来るよう、エンティティ中心を刃の長さ分手前に引く
        double forwardAmt = (STICK_OFFSET_FORWARD * STICK_UNIT) - BLADE_TIP_LENGTH;
        double nx = face.getStepX() * normalAmt;
        double ny = face.getStepY() * normalAmt;
        double nz = face.getStepZ() * normalAmt;
        Vec3 fwd = v.lengthSqr() > 1e-6 ? v.normalize().scale(forwardAmt) : Vec3.ZERO;
        this.setPos(hitPos.x + nx + fwd.x, hitPos.y + ny + fwd.y, hitPos.z + nz + fwd.z);
        this.setDeltaMovement(Vec3.ZERO);
        this.setNoGravity(true);
        this.stuckTicks = 0;
        this.setYRot(savedYaw);
        this.setXRot(savedPitch);
        this.yRotO = savedYaw;
        this.xRotO = savedPitch;
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
            SoundEvents.ARROW_HIT, SoundSource.PLAYERS, 0.7f, 1.0f);
    }

    @Override
    public void tick() {
        if (isStuck()) {
            // 刺さった状態: 動かない／投げた向きで姿勢固定／プレイヤー接触で拾える／一定時間で消滅
            float yaw = getStuckYaw();
            float pitch = getStuckPitch();
            this.setDeltaMovement(Vec3.ZERO);
            this.setNoGravity(true);
            this.setYRot(yaw);
            this.setXRot(pitch);
            this.yRotO = yaw;
            this.xRotO = pitch;
            if (!this.level().isClientSide) {
                stuckTicks++;
                if (stuckTicks >= STUCK_LIFETIME_TICKS) {
                    this.discard();
                    return;
                }
                // 刺さってる間の拾得判定は毎tickだとAABBスキャンが重い。3tickに1回で十分。
                if ((stuckTicks % 3) != 0) return;
                for (Player p : this.level().getEntitiesOfClass(Player.class,
                        this.getBoundingBox().inflate(1.5))) {
                    if (p.isSpectator()) continue;
                    ItemStack knife = new ItemStack(CustomEntityInit.THROWING_KNIFE.get());
                    if (p.isCreative() || p.getInventory().add(knife)) {
                        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                            SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.4f, 1.4f);
                        this.discard();
                        return;
                    } else {
                        ItemEntity drop = new ItemEntity(this.level(),
                            this.getX(), this.getY(), this.getZ(), knife);
                        drop.setDefaultPickUpDelay();
                        this.level().addFreshEntity(drop);
                        this.discard();
                        return;
                    }
                }
            }
            return;
        }
        // HOMING: 飛翔中に最近接Mobへ徐々に旋回 (4tick毎で十分、負荷軽減)
        if (!this.level().isClientSide && getKnifeType() == KnifeType.HOMING && this.tickCount % 4 == 0) {
            applyHoming();
        }
        // SCREW: 激流トライデントのスピン時に発生するバブルパーティクルを周囲に散らす
        if (!this.level().isClientSide && getKnifeType() == KnifeType.SCREW) {
            emitScrewParticles();
        }
        super.tick();
        // super.tick() 内で onHit→stuck=true になった直後: rotation がリセットされているので復元
        if (isStuck()) {
            float yaw = getStuckYaw();
            float pitch = getStuckPitch();
            this.setYRot(yaw);
            this.setXRot(pitch);
            this.yRotO = yaw;
            this.xRotO = pitch;
        }
    }

    private void applyHoming() {
        Vec3 mv = this.getDeltaMovement();
        if (mv.lengthSqr() < 0.04) return;
        Vec3 pos = this.position();
        net.minecraft.world.entity.LivingEntity tgt = this.level().getEntitiesOfClass(
                net.minecraft.world.entity.LivingEntity.class,
                this.getBoundingBox().inflate(16.0)).stream()
            .filter(e -> e != this.getOwner() && e.isAlive() && !e.isSpectator())
            .filter(e -> {
                Vec3 to = e.getEyePosition().subtract(pos).normalize();
                return to.dot(mv.normalize()) > 0.4;
            })
            .min(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(this)))
            .orElse(null);
        if (tgt == null) return;
        Vec3 to = tgt.getEyePosition().subtract(pos).normalize();
        double power = 0.18;
        Vec3 newMv = mv.normalize().scale(1.0 - power)
            .add(to.scale(power)).normalize().scale(mv.length());
        this.setDeltaMovement(newMv);
        this.hasImpulse = true;
    }

    /**
     * SCREW飛翔中: 進行軸まわりに2本のスパイラル軌道でバブルを撒く。
     * 回転角は SCREW_SPIN_DEG_PER_TICK と同期するのでナイフの回転方向に風が
     * 回っているように見える。軽量化のため 2tick に 1 回のみ。
     */
    private void emitScrewParticles() {
        if ((this.tickCount & 1) != 0) return; // 2tick に 1 回
        if (!(this.level() instanceof net.minecraft.server.level.ServerLevel sl)) return;

        Vec3 motion = this.getDeltaMovement();
        if (motion.lengthSqr() < 1e-6) return;

        // 進行方向を軸に、それに直交する2つの単位ベクトルを作る
        Vec3 fwd = motion.normalize();
        Vec3 refUp = Math.abs(fwd.y) < 0.99 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 right = fwd.cross(refUp).normalize();
        Vec3 up    = right.cross(fwd).normalize();

        // 現在のスピン角 (renderer と同じ)
        double ang = Math.toRadians(this.tickCount * SCREW_SPIN_DEG_PER_TICK);
        double radius = 0.35;
        double tangent = 0.12; // 接線方向の速度 (風が流れる感じ)

        // 180° 離して 2 点発生 → 2 本のヘリックス (螺旋) トレイルが描かれる
        for (int i = 0; i < 2; i++) {
            double a = ang + i * Math.PI;
            double cosA = Math.cos(a);
            double sinA = Math.sin(a);
            // 発生位置: 進行軸まわり radius 距離の円周上
            double ox = right.x * cosA * radius + up.x * sinA * radius;
            double oy = right.y * cosA * radius + up.y * sinA * radius;
            double oz = right.z * cosA * radius + up.z * sinA * radius;
            // 接線速度: 回転方向 (d/da の位置) = -right*sin(a) + up*cos(a)
            double vx = (-right.x * sinA + up.x * cosA) * tangent;
            double vy = (-right.y * sinA + up.y * cosA) * tangent;
            double vz = (-right.z * sinA + up.z * cosA) * tangent;
            // count=0 で xDist/yDist/zDist が個別速度として扱われる (指向性パーティクル)
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.BUBBLE,
                this.getX() + ox, this.getY() + oy, this.getZ() + oz,
                0, vx, vy, vz, 0);
        }
    }

    /**
     * SCREWナイフ: ヒットした木材/木製コンテナを破壊し貫通継続。破壊できればtrue。
     */
    private boolean tryScrewBreak(BlockHitResult result) {
        net.minecraft.core.BlockPos pos = result.getBlockPos();
        net.minecraft.world.level.block.state.BlockState state = this.level().getBlockState(pos);
        net.minecraft.world.level.block.Block block = state.getBlock();
        boolean isWood = state.is(net.minecraft.tags.BlockTags.LOGS)
                      || state.is(net.minecraft.tags.BlockTags.PLANKS)
                      || state.is(net.minecraft.tags.BlockTags.WOODEN_DOORS)
                      || state.is(net.minecraft.tags.BlockTags.WOODEN_TRAPDOORS)
                      || state.is(net.minecraft.tags.BlockTags.WOODEN_FENCES)
                      || state.is(net.minecraft.tags.BlockTags.WOODEN_BUTTONS)
                      || state.is(net.minecraft.tags.BlockTags.WOODEN_PRESSURE_PLATES)
                      || state.is(net.minecraft.tags.BlockTags.WOODEN_SLABS)
                      || state.is(net.minecraft.tags.BlockTags.WOODEN_STAIRS)
                      || block == net.minecraft.world.level.block.Blocks.CHEST
                      || block == net.minecraft.world.level.block.Blocks.TRAPPED_CHEST
                      || block == net.minecraft.world.level.block.Blocks.BARREL
                      || block == net.minecraft.world.level.block.Blocks.CRAFTING_TABLE;
        if (!isWood) return false;
        // 軽量化: destroyBlock は levelEvent(2001) でパーティクル＋音パケットをブロードキャスト
        // するのでループ破壊時に重い。setBlock(AIR) を最小フラグで直接差し替える:
        //   UPDATE_CLIENTS(2): クライアント側のチャンク更新は必要
        //   UPDATE_KNOWN_SHAPE(16): 周辺の再レンダリング通知をスキップ
        //   UPDATE_SUPPRESS_DROPS(32): ドロップ抑止 (ItemEntity 生成させない)
        // 全ての破壊音を流すと耳障りなので、5 ブロックに 1 回だけ鳴らす。
        this.level().setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),
            2 | 16 | 32);
        if ((this.tickCount % 5) == 0) {
            this.level().playSound(null, pos, SoundEvents.WOOD_BREAK, SoundSource.PLAYERS, 0.6f, 1.3f);
        }
        return true; // 貫通継続
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Stuck", isStuck());
        tag.putInt("StuckTicks", stuckTicks);
        tag.putFloat("StuckYaw", getStuckYaw());
        tag.putFloat("StuckPitch", getStuckPitch());
        tag.putInt("KnifeType", getKnifeType().ordinal());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DATA_STUCK, tag.getBoolean("Stuck"));
        this.stuckTicks = tag.getInt("StuckTicks");
        this.entityData.set(DATA_STUCK_YAW, tag.getFloat("StuckYaw"));
        this.entityData.set(DATA_STUCK_PITCH, tag.getFloat("StuckPitch"));
        if (tag.contains("KnifeType")) {
            this.entityData.set(DATA_KNIFE_TYPE, tag.getInt("KnifeType"));
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public ItemStack getItem() {
        return new ItemStack(CustomEntityInit.THROWING_KNIFE.get());
    }
}

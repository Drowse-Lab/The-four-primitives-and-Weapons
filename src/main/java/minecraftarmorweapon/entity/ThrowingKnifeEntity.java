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
    /**
     * 刺さった後の存続時間。短くすると同時に存在するスタック数が減って
     * 全体のサーバー/クライアント負荷が下がる。300tick = 15秒。
     */
    private static final int STUCK_LIFETIME_TICKS = 300;

    /**
     * 着弾時に setPos で刺さり位置オフセットを適用するかどうか。
     * 適用すると Teleport パケットが発生しクライアントで瞬間移動して見える事がある。
     * 無効化すると衝突点にそのまま残る (見た目はブレードが壁に接する位置)。
     * 着弾時のラグを切り分けたい時にオフにして比較する。
     */
    public static boolean STICK_APPLY_OFFSET = true;

    /**
     * SCREW ナイフのロール回転速度 (度/tick)。
     * renderer (ThrowingKnifeRenderer) もこの値を参照してパーティクルの渦方向と同期させる。
     * 120°/tick = 2400°/sec ≒ 6.7 回転/秒。
     */
    public static final float SCREW_SPIN_DEG_PER_TICK = 120f;

    /**
     * SCREW のスパイラルパーティクルを有効化するかどうか。
     * false にすると sendParticles 呼び出しを完全にカット → 着弾時のカクつきが
     * 本当にパーティクル起因か切り分けたい時に使う。
     */
    public static boolean SCREW_PARTICLES_ENABLED = true;

    /** パーティクル発生間隔 (tick)。大きくするほど軽量。1 = 毎tick, 2 = 2tickに1回 */
    public static int SCREW_PARTICLE_INTERVAL = 1;
    /** 1回あたりの螺旋アーム数 (1=単発, 2=180°対向の2本で渦感, 4以上で密な渦) */
    public static int SCREW_PARTICLE_ARMS = 4;
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
        // オーナー除外: 参照比較と UUID 比較の両方。getOwner() が null を返す瞬間
        // (再ログイン直後等) でも self-hit を拾えるように UUID 保持側もチェックする。
        Entity owner = this.getOwner();
        if (target == owner) return;
        if (owner != null && target.getUUID() != null && target.getUUID().equals(owner.getUUID())) return;

        KnifeType type = getKnifeType();
        float dmg = switch (type) {
            case GRIP   -> DAMAGE + 2.0f;
            case STUN   -> DAMAGE + 4.0f;
            case SCREW  -> DAMAGE + 1.0f;
            case HOMING -> DAMAGE;
            default     -> DAMAGE;
        };
        target.hurt(this.damageSources().thrown(this, owner), dmg);

        // STUN: 感電(雷視覚) + 移動速度低下/弱体化 + 電気属性の追加ダメージ
        if (type == KnifeType.STUN && target instanceof net.minecraft.world.entity.LivingEntity le) {
            // 追加の電気属性ダメージ (導体装備ボーナス込み)。自傷しないよう target 限定で hurt。
            float electricBase = 3.0f;
            float electricDmg = minecraftarmorweapon.damage.ElectricElementDamageHandler
                .calculateDamage(le, electricBase, 1, le.damageSources().lightningBolt());
            le.hurt(le.damageSources().lightningBolt(), electricDmg);
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
        // STICK_APPLY_OFFSET が false の場合は setPos を呼ばない → Teleport パケットが
        // 発生せず、着弾時の瞬間移動によるラグが切り分けられる。
        if (STICK_APPLY_OFFSET) {
            Vec3 hitPos = result.getLocation();
            net.minecraft.core.Direction face = result.getDirection();
            double normalAmt = STICK_OFFSET_NORMAL * STICK_UNIT;
            double forwardAmt = (STICK_OFFSET_FORWARD * STICK_UNIT) - BLADE_TIP_LENGTH;
            double nx = face.getStepX() * normalAmt;
            double ny = face.getStepY() * normalAmt;
            double nz = face.getStepZ() * normalAmt;
            Vec3 fwd = v.lengthSqr() > 1e-6 ? v.normalize().scale(forwardAmt) : Vec3.ZERO;
            this.setPos(hitPos.x + nx + fwd.x, hitPos.y + ny + fwd.y, hitPos.z + nz + fwd.z);
        }
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
            // 刺さった状態: onHit* で一度 setYRot/setXRot/Deltamovement=0 を済ませている。
            // 以降は tick 毎に何もする必要がない (super.tick を呼ばないので位置/回転/物理
            // どれも変化しない)。唯一必要なのはサーバー側のライフタイム計測と拾得判定のみ。
            if (this.level().isClientSide) return;
            stuckTicks++;
            if (stuckTicks >= STUCK_LIFETIME_TICKS) {
                this.discard();
                return;
            }
            // 拾得判定は 3tick に 1 回
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
            return;
        }
        // HOMING: 飛翔中に最近接Mobへ徐々に旋回 (4tick毎で十分、負荷軽減)
        if (!this.level().isClientSide && getKnifeType() == KnifeType.HOMING && this.tickCount % 4 == 0) {
            applyHoming();
        }
        super.tick();
        // SCREW: クライアント側のみ addParticle で風トレイルを生成。
        // サーバー sendParticles は近傍全クライアントに毎tickパケットを飛ばすので
        // 着弾時の連鎖処理と重なってカクつく。クライアントローカルで描画すれば
        // ネットワーク負荷ゼロ → 着弾時のラグ軽減。
        if (this.level().isClientSide && getKnifeType() == KnifeType.SCREW && !isStuck()) {
            emitScrewParticlesClient();
        }
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
     * SCREW飛翔中: クライアントローカルで風/渦パーティクルを生成 (ネットワーク経由しない)。
     * SCREW_PARTICLE_ARMS 本の螺旋を回転と同期して配置、接線速度で回転方向の流れを表現。
     *
     * 主パーティクルに CLOUD (白い雲煙 = 風らしい)、アクセントに ENCHANTED_HIT (渦感) を少量。
     * level.addParticle はクライアント側のみ実体化 → サーバー側は no-op なので軽量。
     */
    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    private void emitScrewParticlesClient() {
        if (!SCREW_PARTICLES_ENABLED) return;
        if ((this.tickCount % SCREW_PARTICLE_INTERVAL) != 0) return;

        Vec3 motion = this.getDeltaMovement();
        if (motion.lengthSqr() < 0.04) return;

        Vec3 fwd = motion.normalize();
        Vec3 refUp = Math.abs(fwd.y) < 0.99 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 right = fwd.cross(refUp).normalize();
        Vec3 up    = right.cross(fwd).normalize();

        double ang = Math.toRadians(this.tickCount * SCREW_SPIN_DEG_PER_TICK);
        double radius = 0.4;
        // 接線速度は小さめに: 粒が遠くまで飛ばず渦らしく留まる
        double tangent = 0.08;

        int arms = Math.max(1, SCREW_PARTICLE_ARMS);
        double step = (Math.PI * 2.0) / arms;
        net.minecraft.world.level.Level lvl = this.level();
        for (int i = 0; i < arms; i++) {
            double a = ang + i * step;
            double cosA = Math.cos(a);
            double sinA = Math.sin(a);
            double ox = right.x * cosA * radius + up.x * sinA * radius;
            double oy = right.y * cosA * radius + up.y * sinA * radius;
            double oz = right.z * cosA * radius + up.z * sinA * radius;
            double vx = (-right.x * sinA + up.x * cosA) * tangent;
            double vy = (-right.y * sinA + up.y * cosA) * tangent;
            double vz = (-right.z * sinA + up.z * cosA) * tangent;
            // メイン: POOF — 短寿命の白いパフ雲。CLOUD(~30tick) より遥かに早く消えるので
            // トレイルが長く伸びず、ナイフに纏わりつく渦として見える
            lvl.addParticle(net.minecraft.core.particles.ParticleTypes.POOF,
                this.getX() + ox, this.getY() + oy, this.getZ() + oz,
                vx, vy, vz);
        }
        // アクセント: 2tick に 1 度、渦の中心に小さな光点で「マジカル回転」感を強調
        if ((this.tickCount & 1) == 0) {
            lvl.addParticle(net.minecraft.core.particles.ParticleTypes.ENCHANTED_HIT,
                this.getX(), this.getY(), this.getZ(), 0, 0, 0);
        }
    }

    /**
     * SCREWナイフ: ヒットしたブロックを破壊。破壊できればtrue。
     *
     * 対応ブロック:
     *   - 木材/木製コンテナ: ブロック自体をドロップ。ナイフは**この1ブロックで消滅**
     *     (貫通しない) — 他mod の木系ブロックも SoundType.WOOD/CHERRY_WOOD/NETHER_WOOD/
     *     BAMBOO_WOOD でフォールバック検出。
     *   - 葉っぱ: 素手で壊した時と同じ挙動 → 葉自体はドロップせず苗木/棒/リンゴが低確率。
     *     ナイフはそのまま**貫通継続**。
     *
     * ドロップは Block.dropResources + setBlock(AIR) の組み合わせで生成。
     * SUPPRESS_DROPS フラグで setBlock 側の二重ドロップを抑止。
     */
    private boolean tryScrewBreak(BlockHitResult result) {
        net.minecraft.core.BlockPos pos = result.getBlockPos();
        net.minecraft.world.level.block.state.BlockState state = this.level().getBlockState(pos);
        net.minecraft.world.level.block.Block block = state.getBlock();
        net.minecraft.world.level.block.SoundType snd = state.getSoundType();

        // 他 mod の木系ブロックも拾うため sound type でフォールバック判定。
        // WOOD / CHERRY_WOOD / NETHER_WOOD / BAMBOO_WOOD は全て "木" として扱う。
        boolean isWoodSound =
               snd == net.minecraft.world.level.block.SoundType.WOOD
            || snd == net.minecraft.world.level.block.SoundType.CHERRY_WOOD
            || snd == net.minecraft.world.level.block.SoundType.NETHER_WOOD
            || snd == net.minecraft.world.level.block.SoundType.BAMBOO_WOOD;
        boolean isWood = isWoodSound
                      || state.is(net.minecraft.tags.BlockTags.LOGS)
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
        boolean isLeaves = state.is(net.minecraft.tags.BlockTags.LEAVES);
        if (!isWood && !isLeaves) return false;

        // ドロップ生成。tool = ItemStack.EMPTY で "素手扱い"。
        //   木材: loot table が tool 条件なしでブロック自体をドロップ → 正常に落ちる
        //   葉: loot table に shears/silk_touch 条件がついているので EMPTY では葉自体は
        //       落ちず、代わりに苗木 (~5%)・棒 (~2%)・リンゴ (oak ~0.5%) が別 pool で抽選
        net.minecraft.world.entity.Entity owner = this.getOwner();
        net.minecraft.world.entity.LivingEntity le =
            owner instanceof net.minecraft.world.entity.LivingEntity lv ? lv : null;
        net.minecraft.world.level.block.Block.dropResources(
            state, this.level(), pos, null, le, net.minecraft.world.item.ItemStack.EMPTY);

        // 軽量ブロック差し替え。UPDATE_SUPPRESS_DROPS(32) で setBlock 側の二重ドロップ抑止。
        this.level().setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),
            2 | 16 | 32);

        // 破壊音は 5 ブロックに 1 回、そのブロックの素材音で
        if ((this.tickCount % 5) == 0) {
            this.level().playSound(null, pos, snd.getBreakSound(), SoundSource.BLOCKS,
                (snd.getVolume() + 1.0f) / 4.0f, snd.getPitch() * 0.9f);
        }

        // 木: 1ブロック破壊で消滅 (貫通しない)
        // 葉: 貫通継続
        if (isWood && !this.level().isClientSide) {
            this.discard();
        }
        return true;
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

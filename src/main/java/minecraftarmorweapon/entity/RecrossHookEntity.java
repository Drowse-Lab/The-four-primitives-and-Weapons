package minecraftarmorweapon.entity;

import minecraftarmorweapon.init.CustomEntityInit;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Re:Cross Hookshot のフックエンティティ.
 *
 * 元データパック {@code hookshot_remake/functions/hook.mcfunction} の挙動:
 *   - {@code armor_stand} (Marker) で表現、tag {@code ReCr_Hook}
 *   - 毎 tick {@code teleport ^ ^ ^1} (= 1 block 前進)
 *   - {@code BulletRemain=10} で kill (= 最大 10 block 飛行)
 *   - 地形ヒット → {@code area_effect_cloud} に tag {@code ReCr_HookPoint}
 *   - エンティティヒット (light) → そのエンティティを {@code ReCr_Pulled} で player へ引き寄せ
 *   - エンティティヒット (heavy = wither/ender_dragon 等) → hook_point がそのエンティティに追従
 *
 * Java 移植では:
 *   - {@code Mob} を継承して vanilla Leash で player と繋ぐ (見た目用)
 *   - 1 tick 1 block 前進、最大 12 tick (= 12 block 飛行 — 元 10 より少し長め)
 *   - 着弾 → {@link minecraftarmorweapon.event.RecrossPlayerHandler} が player を pull
 */
public class RecrossHookEntity extends Mob {

    /** どちらの手で発射されたか. */
    private static final EntityDataAccessor<Boolean> DATA_OFF_HAND =
        SynchedEntityData.defineId(RecrossHookEntity.class, EntityDataSerializers.BOOLEAN);

    public enum State {
        FLYING,    // 飛行中
        ANCHORED   // 地形/エンティティに着弾、player を pull する
    }

    /** ヒット時に player を pull する目標位置. */
    private Vec3 anchorPos;
    private State state = State.FLYING;

    private UUID ownerUuid;
    private int flyTicks = 0;
    /** 状態にかかわらず存在 tick 数. ゾンビフックの保険 (生成から 200t = 10s 経過で強制 discard). */
    private int totalTicks = 0;
    /** 安全上限 — pull が完了せず長時間 ANCHORED したまま放置された hook の永続化を防ぐ. */
    public static final int MAX_TOTAL_TICKS = 200;

    /**
     * Heavy エンティティを引っ掛けた場合に追従対象として保持 (transient, NBT 保存しない).
     * tick 中に anchorPos をこの entity の現在位置で更新 → 動く敵を追いかけて player が引っ張られる.
     */
    private Entity heavyAnchor;

    /** 1 tick あたりの前進ブロック数 (per-instance — Long/Short 用に切替可能). */
    private double flyStep = 2.0;
    /** 最大飛行 tick 数 → 最大射程 (per-instance). */
    private int maxFlyTicks = 30;

    /** 着弾エンティティ判定の line-distance しきい値. */
    public static final double LINE_HIT_RADIUS = 0.6;

    public RecrossHookEntity(EntityType<? extends Mob> type, Level level) {
        super(type, level);
        this.setNoAi(true);
        this.setSilent(true);
        this.setInvulnerable(true);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public RecrossHookEntity(Level level, Player owner, boolean offHand) {
        this(level, owner, offHand, 2.0, 30);
    }

    /** 飛行速度 / 最大射程を指定するコンストラクタ. {@code flyStep * maxFlyTicks} = 最大射程. */
    public RecrossHookEntity(Level level, Player owner, boolean offHand, double flyStep, int maxFlyTicks) {
        this(CustomEntityInit.RECROSS_HOOK_ENTITY.get(), level);
        this.ownerUuid = owner.getUUID();
        this.entityData.set(DATA_OFF_HAND, offHand);
        this.flyStep = flyStep;
        this.maxFlyTicks = maxFlyTicks;

        Vec3 eye = owner.getEyePosition();
        this.setPos(eye.x, eye.y, eye.z);
        this.setYRot(owner.getYRot());
        this.setXRot(owner.getXRot());
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 1.0)
            .add(Attributes.MOVEMENT_SPEED, 0.0)
            .add(Attributes.FOLLOW_RANGE, 0.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_OFF_HAND, false);
    }

    public boolean isOffHand() { return this.entityData.get(DATA_OFF_HAND); }
    public State getState() { return state; }
    public Vec3 getAnchorPos() { return anchorPos; }

    public Player getOwnerPlayer() {
        if (ownerUuid == null) return null;
        if (level().isClientSide) return null;
        Entity e = ((net.minecraft.server.level.ServerLevel) level()).getEntity(ownerUuid);
        return (e instanceof Player p) ? p : null;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;

        // 絶対 tick 数で上限超過 → 強制 discard (ゾンビフック保険)
        // pull が完了せず ANCHORED のまま放置 / heavyAnchor の dimension 跨ぎ等の異常系を防ぐ.
        totalTicks++;
        if (totalTicks > MAX_TOTAL_TICKS) {
            this.discard();
            return;
        }

        // owner が居なくなった/距離離れすぎ → discard
        Player owner = getOwnerPlayer();
        if (owner == null || !owner.isAlive() || owner.distanceToSqr(this) > 200 * 200) {
            this.discard();
            return;
        }

        if (state == State.FLYING) {
            spawnFlightParticles();
            tickFlying(owner);
        } else if (state == State.ANCHORED) {
            // ★ Heavy entity を引っ掛けてた場合、anchor をその entity の現在位置に更新 (追従)
            if (heavyAnchor != null) {
                if (!heavyAnchor.isAlive()) {
                    this.discard();
                    return;
                }
                anchorPos = heavyAnchor.position().add(0, heavyAnchor.getBbHeight() * 0.5, 0);
                setPos(anchorPos.x, anchorPos.y, anchorPos.z);
            }
            // 着弾後はロープ替わりの dust 鎖を hook → owner に毎 tick 描画
            spawnChainParticles(owner);
        }
    }

    /**
     * 飛行中の hook 位置に CRIT + 白 dust 粒子を spawn (元データパック {@code hook_move.mcfunction} 準拠).
     * <pre>
     * particle minecraft:crit ~ ~ ~ 0 0 0 0 1 force
     * particle minecraft:dust 1 1 1 0.75 ~ ~ ~ 0 0 0 0 1 force
     * </pre>
     */
    private void spawnFlightParticles() {
        if (!(level() instanceof ServerLevel server)) return;
        DustParticleOptions dust = new DustParticleOptions(
            new org.joml.Vector3f(1.0f, 1.0f, 1.0f), 0.75f);
        server.sendParticles(ParticleTypes.CRIT, getX(), getY(), getZ(),
            1, 0, 0, 0, 0);
        server.sendParticles(dust, getX(), getY(), getZ(),
            1, 0, 0, 0, 0);
    }

    /**
     * 着弾後の "ロープ" 描画 — hook → player を結ぶ直線上に 1 block 間隔で
     * 白 dust 粒子を spawn する (元データパック {@code chain_particle.mcfunction} 再帰呼出を一括化).
     * <pre>
     * particle minecraft:dust 1 1 1 0.5 ~ ~1 ~ 0.025 0.025 0.025 1 1
     * </pre>
     */
    private void spawnChainParticles(Player owner) {
        if (!(level() instanceof ServerLevel server)) return;
        Vec3 from = position();
        Vec3 to = owner.getEyePosition();
        Vec3 diff = to.subtract(from);
        double dist = diff.length();
        if (dist < 0.5) return;
        Vec3 dir = diff.normalize();
        int steps = (int) Math.ceil(dist);

        DustParticleOptions dust = new DustParticleOptions(
            new org.joml.Vector3f(1.0f, 1.0f, 1.0f), 0.5f);
        for (int i = 0; i < steps; i++) {
            Vec3 p = from.add(dir.scale(i));
            server.sendParticles(dust, p.x, p.y, p.z,
                1, 0.025, 0.025, 0.025, 0);
        }
    }

    private void tickFlying(Player owner) {
        // 発射時に格納した hook 自身の Rotation を使う (元データパック準拠 — ReCr_Hook0 タグ相当).
        Vec3 dir = computeForwardFromOwn();
        Vec3 from = position();

        // 高速飛行で薄いブロックや entity を素通りしないよう、サブステップで衝突判定.
        // visual の滑らかさは vanilla の client interpolation が担当 (1 tick 内で from→to を補間).
        int substeps = Math.max(1, (int) Math.ceil(flyStep));
        Vec3 stepVec = dir.scale(flyStep / substeps);
        Vec3 cur = from;

        for (int i = 0; i < substeps; i++) {
            Vec3 next = cur.add(stepVec);

            BlockHitResult bhr = level().clip(new ClipContext(
                cur, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (bhr.getType() != HitResult.Type.MISS) {
                anchorAt(bhr.getLocation());
                return;
            }

            AABB scan = new AABB(cur, next).inflate(0.5);
            Entity hit = pickHookableEntity(owner, scan, cur, dir);
            if (hit != null) {
                if (isHeavy(hit)) {
                    // 動く敵を追従 → tick 内で anchorPos を更新
                    this.heavyAnchor = hit;
                    anchorAt(hit.position().add(0, hit.getBbHeight() * 0.5, 0));
                } else {
                    pullEntityToward(hit, owner);
                    this.discard();
                }
                return;
            }

            cur = next;
        }

        // 全 substep ヒット無し → tick 末位置を確定
        setPos(cur.x, cur.y, cur.z);
        flyTicks++;
        if (flyTicks >= maxFlyTicks) {
            this.discard();
        }
    }

    /** 着弾 — anchor を設定して ANCHORED 状態に. */
    private void anchorAt(Vec3 pos) {
        this.anchorPos = pos;
        this.state = State.ANCHORED;
        setPos(pos.x, pos.y, pos.z);
        playHitSound();
    }

    /** 発射時に格納した自身の yaw/pitch から進行方向を計算 (元データパック準拠). */
    private Vec3 computeForwardFromOwn() {
        float yawRad = (float) Math.toRadians(getYRot());
        float pitchRad = (float) Math.toRadians(getXRot());
        double x = -Math.sin(yawRad) * Math.cos(pitchRad);
        double y = -Math.sin(pitchRad);
        double z =  Math.cos(yawRad) * Math.cos(pitchRad);
        return new Vec3(x, y, z);
    }

    /** 区間 from → from+dir*(substep distance) の line-distance 内にいる entity を 1 つ返す. */
    private Entity pickHookableEntity(Player owner, AABB scan, Vec3 from, Vec3 dir) {
        Entity best = null;
        double bestAlong = Double.MAX_VALUE;
        double substepDist = flyStep / Math.max(1, (int) Math.ceil(flyStep)) + 1.0;
        for (Entity e : level().getEntities(this, scan)) {
            if (e == owner || e == this) continue;
            if (e instanceof RecrossHookEntity) continue;
            if (e instanceof Player) continue;   // 他プレイヤーは引き寄せ対象外
            // ItemEntity (ドロップアイテム) と LivingEntity が pull 対象. その他は無視.
            if (!(e instanceof LivingEntity) && !(e instanceof ItemEntity)) continue;
            Vec3 toEnt = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(from);
            double along = toEnt.dot(dir);
            if (along < 0 || along > substepDist) continue;
            Vec3 closest = from.add(dir.scale(along));
            double perpSq = closest.distanceToSqr(e.position().add(0, e.getBbHeight() * 0.5, 0));
            if (perpSq < LINE_HIT_RADIUS * LINE_HIT_RADIUS && along < bestAlong) {
                bestAlong = along;
                best = e;
            }
        }
        return best;
    }

    /** light entity / ItemEntity を owner 方向に引き寄せる (元データパック ReCr_Pulled 相当). */
    private void pullEntityToward(Entity target, Player owner) {
        Vec3 from = target.position().add(0, target.getBbHeight() * 0.5, 0);
        Vec3 to = owner.position().add(0, owner.getEyeHeight() * 0.5, 0);
        Vec3 diff = to.subtract(from);
        double dist = diff.length();
        if (dist < 0.5) return;
        // 距離に応じた pull 速度: 近距離は控えめ、遠距離は強めに. 上限 3 b/tick.
        double speed = Math.min(3.0, 1.0 + dist * 0.2);
        target.setDeltaMovement(diff.normalize().scale(speed));
        target.hurtMarked = true;
        if (target instanceof LivingEntity le) {
            le.fallDistance = 0f;
        }
        target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
            net.minecraft.sounds.SoundEvents.FISHING_BOBBER_RETRIEVE,
            net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    /** ヒット効果音 (元データパック準拠の重ね). */
    private void playHitSound() {
        level().playSound(null, getX(), getY(), getZ(),
            net.minecraft.sounds.SoundEvents.BLAZE_HURT,
            net.minecraft.sounds.SoundSource.PLAYERS, 2.0f, 2.0f);
        level().playSound(null, getX(), getY(), getZ(),
            net.minecraft.sounds.SoundEvents.IRON_DOOR_OPEN,
            net.minecraft.sounds.SoundSource.PLAYERS, 1.5f, 1.0f);
    }

    /**
     * Heavy = 引き寄せられないエンティティ (= player が逆に anchor として引き寄せられる).
     * Wither, EnderDragon, Warden, IronGolem に加えて Ghast, Giant も追加.
     * 必要なら EntityType を追加してください.
     */
    public static boolean isHeavy(Entity e) {
        return e instanceof WitherBoss
            || e instanceof EnderDragon
            || e.getType() == EntityType.WARDEN
            || e.getType() == EntityType.IRON_GOLEM
            || e.getType() == EntityType.GHAST
            || e.getType() == EntityType.GIANT;
    }

    /** vanilla AI tick を完全に潰す. */
    @Override
    protected void tickDeath() {}
    @Override
    public void aiStep() {}
    @Override
    protected void tickLeash() {}

    @Override
    public boolean isAlwaysTicking() { return true; }
    @Override
    public boolean isPushable() { return false; }
    @Override
    public boolean isAttackable() { return false; }
    @Override
    public boolean isInvulnerable() { return true; }
    @Override
    public boolean shouldShowName() { return false; }
    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket() {
        return new net.minecraft.network.protocol.game.ClientboundAddEntityPacket(this);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (ownerUuid != null) tag.putUUID("OwnerUUID", ownerUuid);
        tag.putBoolean("OffHand", isOffHand());
        tag.putString("State", state.name());
        tag.putInt("FlyTicks", flyTicks);
        tag.putDouble("FlyStep", flyStep);
        tag.putInt("MaxFlyTicks", maxFlyTicks);
        if (anchorPos != null) {
            tag.putDouble("AnchorX", anchorPos.x);
            tag.putDouble("AnchorY", anchorPos.y);
            tag.putDouble("AnchorZ", anchorPos.z);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("OwnerUUID")) ownerUuid = tag.getUUID("OwnerUUID");
        this.entityData.set(DATA_OFF_HAND, tag.getBoolean("OffHand"));
        if (tag.contains("State")) {
            try { state = State.valueOf(tag.getString("State")); } catch (IllegalArgumentException ignored) {}
        }
        flyTicks = tag.getInt("FlyTicks");
        if (tag.contains("FlyStep")) flyStep = tag.getDouble("FlyStep");
        if (tag.contains("MaxFlyTicks")) maxFlyTicks = tag.getInt("MaxFlyTicks");
        if (tag.contains("AnchorX")) {
            anchorPos = new Vec3(tag.getDouble("AnchorX"), tag.getDouble("AnchorY"), tag.getDouble("AnchorZ"));
        }
    }
}

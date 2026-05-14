package the_four_primitives_and_weapons.entity;

import the_four_primitives_and_weapons.event.RecrossPlayerHandler;
import the_four_primitives_and_weapons.init.CustomEntityInit;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
 * Re:Cross Hookshot のフックエンティティ (slim 版).
 *
 * 動作:
 *   1. FLYING — owner の eye から発射、毎 tick {@code flyStep} block 前進
 *   2. ブロック衝突 → ANCHORED, anchorPos = 衝突点
 *      - 以後 {@link RecrossPlayerHandler} が player を anchor へ pull
 *   3. Light entity 衝突 → 継続 pull (entity を player の方へ毎 tick 移動)
 *   4. Heavy entity (Wither/EnderDragon/Warden/IronGolem/Ghast/Giant) 衝突 →
 *      anchor を entity 位置に追従させて player を引っ張る
 */
public class RecrossHookEntity extends Mob {

    private static final EntityDataAccessor<Boolean> DATA_OFF_HAND =
        SynchedEntityData.defineId(RecrossHookEntity.class, EntityDataSerializers.BOOLEAN);

    public enum State { FLYING, ANCHORED }

    public static final double LINE_HIT_RADIUS = 0.6;

    private UUID ownerUuid;
    private boolean offHand;
    private double flyStep = 4.0;
    private int maxFlyTicks = 20;
    private int flyTicks = 0;
    private int totalTicks = 0;

    private State state = State.FLYING;
    private Vec3 anchorPos;
    /** Anchor 確定時の owner 位置 — pull 進行度判定で使う (anchor を通り過ぎたら arrive). */
    private Vec3 pullOrigin;
    /** Heavy 対象 = anchor が entity に追従. */
    private Entity heavyAnchor;
    /** Light 対象 = entity を player へ毎 tick で引き寄せ. */
    private Entity pulledEntity;

    public RecrossHookEntity(EntityType<? extends Mob> type, Level level) {
        super(type, level);
        this.setNoAi(true);
        this.setSilent(true);
        this.setInvulnerable(true);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public RecrossHookEntity(Level level, Player owner, boolean offHand) {
        this(level, owner, offHand, 4.0, 20);
    }

    public RecrossHookEntity(Level level, Player owner, boolean offHand, double flyStep, int maxFlyTicks) {
        this(CustomEntityInit.RECROSS_HOOK_ENTITY.get(), level);
        this.ownerUuid = owner.getUUID();
        this.offHand = offHand;
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

    public boolean isOffHand() { return entityData.get(DATA_OFF_HAND); }
    public State getState() { return state; }
    public Vec3 getAnchorPos() { return anchorPos; }
    public Vec3 getPullOrigin() { return pullOrigin; }
    public boolean isPullingEntity() { return pulledEntity != null; }

    public Player getOwnerPlayer() {
        if (ownerUuid == null || level().isClientSide) return null;
        Entity e = ((ServerLevel) level()).getEntity(ownerUuid);
        return (e instanceof Player p) ? p : null;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;

        // 絶対時間上限 (ゾンビフック保険) — flight + pull を見越して max(200, maxFlyTicks*6)
        totalTicks++;
        if (totalTicks > Math.max(200, maxFlyTicks * 6)) {
            this.discard();
            return;
        }

        // 距離上限 (rarity 射程の 1.5 倍、最低 300b) を超えたら discard
        Player owner = getOwnerPlayer();
        double distLimit = Math.max(300, flyStep * maxFlyTicks * 1.5);
        if (owner == null || !owner.isAlive() || owner.distanceToSqr(this) > distLimit * distLimit) {
            this.discard();
            return;
        }

        if (state == State.FLYING) {
            if ((totalTicks & 1) == 0) spawnFlightParticles();
            tickFlying(owner);
        } else { // ANCHORED
            if (heavyAnchor != null) {
                if (!heavyAnchor.isAlive()) { this.discard(); return; }
                anchorPos = heavyAnchor.position().add(0, heavyAnchor.getBbHeight() * 0.5, 0);
                setPos(anchorPos.x, anchorPos.y, anchorPos.z);
            }
            if (pulledEntity != null) {
                tickPullEntity(owner);
                if (this.isRemoved()) return;
            }
            // 鎖の dust は 2 tick に 1 回 (帯域節約)
            if ((totalTicks & 1) == 0) spawnChainParticles(owner);
        }
    }

    private void tickFlying(Player owner) {
        Vec3 dir = computeForward();
        Vec3 cur = position();
        int substeps = Math.max(1, (int) Math.ceil(flyStep));
        Vec3 stepVec = dir.scale(flyStep / substeps);

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
                    heavyAnchor = hit;
                    anchorAt(hit.position().add(0, hit.getBbHeight() * 0.5, 0));
                } else {
                    pulledEntity = hit;
                    anchorAt(hit.position().add(0, hit.getBbHeight() * 0.5, 0));
                    level().playSound(null, hit.getX(), hit.getY(), hit.getZ(),
                        SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.PLAYERS, 1.0f, 1.0f);
                }
                return;
            }
            cur = next;
        }

        setPos(cur.x, cur.y, cur.z);
        flyTicks++;
        if (flyTicks >= maxFlyTicks) this.discard();
    }

    /** Light entity を毎 tick で player 方向に PULL_SPEED で move する。 */
    private void tickPullEntity(Player owner) {
        if (!pulledEntity.isAlive()) { this.discard(); return; }
        Vec3 from = pulledEntity.position().add(0, pulledEntity.getBbHeight() * 0.5, 0);
        Vec3 to = owner.position().add(0, owner.getEyeHeight() * 0.5, 0);
        Vec3 diff = to.subtract(from);
        double dist = diff.length();
        if (dist <= RecrossPlayerHandler.ARRIVAL_DIST) { this.discard(); return; }

        double speed = Math.min(RecrossPlayerHandler.PULL_SPEED, dist);
        pulledEntity.setDeltaMovement(diff.normalize().scale(speed));
        pulledEntity.hurtMarked = true;
        if (pulledEntity instanceof LivingEntity le) le.fallDistance = 0f;

        Vec3 ePos = pulledEntity.position().add(0, pulledEntity.getBbHeight() * 0.5, 0);
        setPos(ePos.x, ePos.y, ePos.z);
    }

    private void anchorAt(Vec3 pos) {
        anchorPos = pos;
        state = State.ANCHORED;
        setPos(pos.x, pos.y, pos.z);
        level().playSound(null, getX(), getY(), getZ(),
            SoundEvents.BLAZE_HURT, SoundSource.PLAYERS, 2.0f, 2.0f);
        level().playSound(null, getX(), getY(), getZ(),
            SoundEvents.IRON_DOOR_OPEN, SoundSource.PLAYERS, 1.5f, 1.0f);
        // light pull 中の hook は player 側 pull の対象外なので登録しない
        if (pulledEntity == null) {
            Player owner = getOwnerPlayer();
            if (owner != null) {
                // pull 開始時の owner 位置を記録 — anchor 通過判定 (= 後ろに引き戻されない) に使う
                pullOrigin = owner.position();
                RecrossPlayerHandler.registerAnchoredHook(owner, this);
            }
        }
    }

    private Vec3 computeForward() {
        float yawRad = (float) Math.toRadians(getYRot());
        float pitchRad = (float) Math.toRadians(getXRot());
        double cp = Math.cos(pitchRad);
        return new Vec3(-Math.sin(yawRad) * cp, -Math.sin(pitchRad), Math.cos(yawRad) * cp);
    }

    private Entity pickHookableEntity(Player owner, AABB scan, Vec3 from, Vec3 dir) {
        Entity best = null;
        double bestAlong = Double.MAX_VALUE;
        double substepDist = flyStep / Math.max(1, (int) Math.ceil(flyStep)) + 1.0;
        for (Entity e : level().getEntities(this, scan)) {
            if (e == owner || e == this) continue;
            if (e instanceof RecrossHookEntity || e instanceof Player) continue;
            if (!(e instanceof LivingEntity) && !(e instanceof ItemEntity)) continue;
            Vec3 mid = e.position().add(0, e.getBbHeight() * 0.5, 0);
            double along = mid.subtract(from).dot(dir);
            if (along < 0 || along > substepDist) continue;
            Vec3 closest = from.add(dir.scale(along));
            if (closest.distanceToSqr(mid) < LINE_HIT_RADIUS * LINE_HIT_RADIUS && along < bestAlong) {
                bestAlong = along;
                best = e;
            }
        }
        return best;
    }

    private void spawnFlightParticles() {
        if (!(level() instanceof ServerLevel server)) return;
        DustParticleOptions dust = new DustParticleOptions(new org.joml.Vector3f(1, 1, 1), 0.75f);
        server.sendParticles(ParticleTypes.CRIT, getX(), getY(), getZ(), 1, 0, 0, 0, 0);
        server.sendParticles(dust, getX(), getY(), getZ(), 1, 0, 0, 0, 0);
    }

    /** ロープ表現 — hook → owner を直線で結び、最大 32 セグメントに制限 (帯域節約). */
    private void spawnChainParticles(Player owner) {
        if (!(level() instanceof ServerLevel server)) return;
        Vec3 from = position();
        Vec3 diff = owner.getEyePosition().subtract(from);
        double dist = diff.length();
        if (dist < 0.5) return;
        int steps = Math.min(32, (int) Math.ceil(dist));
        Vec3 step = diff.scale(1.0 / Math.max(1, steps));
        DustParticleOptions dust = new DustParticleOptions(new org.joml.Vector3f(1, 1, 1), 0.5f);
        for (int i = 1; i < steps; i++) {
            Vec3 p = from.add(step.scale(i));
            server.sendParticles(dust, p.x, p.y, p.z, 1, 0.025, 0.025, 0.025, 0);
        }
    }

    public static boolean isHeavy(Entity e) {
        return e instanceof WitherBoss
            || e instanceof EnderDragon
            || e.getType() == EntityType.WARDEN
            || e.getType() == EntityType.IRON_GOLEM
            || e.getType() == EntityType.GHAST
            || e.getType() == EntityType.GIANT;
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide && ownerUuid != null && level() instanceof ServerLevel sl) {
            Entity e = sl.getEntity(ownerUuid);
            if (e instanceof Player p) {
                RecrossPlayerHandler.unregisterAnchoredHook(p);
                the_four_primitives_and_weapons.item.RecrossHookshotItem.unregisterFlyingHook(p);
            }
        }
        super.remove(reason);
    }

    @Override protected void tickDeath() {}
    @Override public void aiStep() {}
    @Override protected void tickLeash() {}
    @Override public boolean isAlwaysTicking() { return true; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean isAttackable() { return false; }
    @Override public boolean isInvulnerable() { return true; }
    @Override public boolean shouldShowName() { return false; }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket() {
        return new net.minecraft.network.protocol.game.ClientboundAddEntityPacket(this);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (ownerUuid != null) tag.putUUID("OwnerUUID", ownerUuid);
        tag.putBoolean("OffHand", offHand);
        tag.putString("State", state.name());
        tag.putInt("FlyTicks", flyTicks);
        tag.putDouble("FlyStep", flyStep);
        tag.putInt("MaxFlyTicks", maxFlyTicks);
        if (anchorPos != null) {
            tag.putDouble("AX", anchorPos.x);
            tag.putDouble("AY", anchorPos.y);
            tag.putDouble("AZ", anchorPos.z);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("OwnerUUID")) ownerUuid = tag.getUUID("OwnerUUID");
        offHand = tag.getBoolean("OffHand");
        entityData.set(DATA_OFF_HAND, offHand);
        if (tag.contains("State")) {
            try { state = State.valueOf(tag.getString("State")); } catch (IllegalArgumentException ignored) {}
        }
        flyTicks = tag.getInt("FlyTicks");
        if (tag.contains("FlyStep")) flyStep = tag.getDouble("FlyStep");
        if (tag.contains("MaxFlyTicks")) maxFlyTicks = tag.getInt("MaxFlyTicks");
        if (tag.contains("AX")) anchorPos = new Vec3(tag.getDouble("AX"), tag.getDouble("AY"), tag.getDouble("AZ"));
    }
}

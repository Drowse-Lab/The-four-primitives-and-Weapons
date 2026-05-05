package minecraftarmorweapon.entity;

import minecraftarmorweapon.event.MomentumPlayerHandler;
import minecraftarmorweapon.init.CustomEntityInit;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages;

import java.util.Optional;
import java.util.UUID;

/**
 * Momentum Hookshot のフック実体 (Java port of Chuzume's Momentum Hookshot).
 *
 * 元データパックは {@code summon bat} で生成し、{@code data modify entity @s Leash.UUID}
 * で player に繋ぐ — つまり **vanilla の lead/leash システム**を使ってロープを表現している。
 *
 * Java 移植もこれに合わせて **{@link Mob}** として実装し、{@link Mob#setLeashedTo} で
 * player に繋ぐ。これで vanilla の {@code MobRenderer.renderLeash} がそのままロープ描画を担当する。
 *
 *  - NoAI / Silent / Invulnerable / NoGravity / NoPhysics で完全に "操作可能な無生物" 化
 *  - {@link #tickLeash()} を no-op して vanilla の auto-break (10 block) /
 *    auto-pull (6 block) を無効化 (フックは 80+ block 飛ぶので必須)
 *  - 飛翔ロジック (14 substeps × 1 block = 14 block/tick × 6 tick = 84 block 射程) は自前
 */
public class MomentumHookEntity extends Mob {

    public enum State { FLYING, ANCHORED, DEAD }

    private static final EntityDataAccessor<Integer> DATA_STATE =
        SynchedEntityData.defineId(MomentumHookEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER_UUID =
        SynchedEntityData.defineId(MomentumHookEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Boolean> DATA_OFF_HAND =
        SynchedEntityData.defineId(MomentumHookEntity.class, EntityDataSerializers.BOOLEAN);

    public static final int FLY_SUBSTEPS = 14;
    public static final double SUBSTEP_DIST = 1.0;
    public static final int MAX_FLY_TICKS = 6;
    public static final int ANCHOR_DURATION = 6;

    private int flyTicks = 0;
    private int anchorTicks = 0;

    public MomentumHookEntity(EntityType<? extends MomentumHookEntity> type, Level level) {
        super(type, level);
        this.setNoAi(true);
        this.setSilent(true);
        this.setInvulnerable(true);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public MomentumHookEntity(PlayMessages.SpawnEntity packet, Level level) {
        this(CustomEntityInit.MOMENTUM_HOOK_ENTITY.get(), level);
    }

    public MomentumHookEntity(Level level, LivingEntity owner, boolean offHand) {
        this(CustomEntityInit.MOMENTUM_HOOK_ENTITY.get(), level);
        this.entityData.set(DATA_OWNER_UUID, Optional.of(owner.getUUID()));
        this.entityData.set(DATA_OFF_HAND, offHand);
        Vec3 eye = owner.getEyePosition();
        Vec3 dir = owner.getLookAngle();
        this.setPos(eye.x + dir.x * 0.5, eye.y - 0.2 + dir.y * 0.5, eye.z + dir.z * 0.5);
        this.setDeltaMovement(dir);
        double horiz = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
        this.setYRot((float)(Math.atan2(dir.x, dir.z) * (180.0 / Math.PI)));
        this.setXRot((float)(Math.atan2(dir.y, horiz) * (180.0 / Math.PI)));
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    /** Mob には attribute 必須. event 側で {@link CustomEntityInit#registerAttributes} に登録 */
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 1.0)
            .add(Attributes.FOLLOW_RANGE, 0.0)
            .add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    @Override
    protected void registerGoals() {
        // No AI goals — マーカー的な存在
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();   // Mob/LivingEntity の synced data も初期化必要
        this.entityData.define(DATA_STATE, State.FLYING.ordinal());
        this.entityData.define(DATA_OWNER_UUID, Optional.empty());
        this.entityData.define(DATA_OFF_HAND, false);
    }

    public State getState() {
        int v = this.entityData.get(DATA_STATE);
        State[] vals = State.values();
        return (v < 0 || v >= vals.length) ? State.FLYING : vals[v];
    }

    public void setState(State s) { this.entityData.set(DATA_STATE, s.ordinal()); }

    public boolean isOffHand() {
        return this.entityData.get(DATA_OFF_HAND);
    }

    public Player getOwnerPlayer() {
        // Mob.getLeashHolder() を優先 (vanilla 同期). 無ければ DATA_OWNER_UUID に fallback.
        Entity holder = this.getLeashHolder();
        if (holder instanceof Player p) return p;
        Optional<UUID> id = this.entityData.get(DATA_OWNER_UUID);
        if (id.isEmpty()) return null;
        return this.level().getPlayerByUUID(id.get());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override public boolean isPickable() { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean canBeCollidedWith() { return false; }
    @Override public boolean isAttackable() { return false; }
    @Override public boolean isInvulnerableTo(DamageSource source) { return true; }

    @Override
    public boolean shouldRenderAtSqrDistance(double dsq) {
        return dsq < 64 * 64 * 64;
    }

    /**
     * vanilla leash の auto-break (距離 10 block で切れる) と auto-pull (距離 6 block で
     * 引き寄せ) を完全無効化. フックは 80 block 先まで飛ぶので必須.
     */
    @Override
    public void tickLeash() {
        // No-op — vanilla の leash physics をスキップ. NBT/同期はそのまま機能する.
    }

    @Override
    public void aiStep() {
        // vanilla mob の aiStep (移動入力, 効果適用 etc) をスキップ.
        // 私の tick 側で全部制御するので不要.
    }

    @Override
    public void tick() {
        super.tick();   // Entity / LivingEntity の base tick (位置補間 etc)
        if (this.level().isClientSide) return;
        Player owner = getOwnerPlayer();
        if (owner == null || !owner.isAlive() || owner.level() != this.level()) {
            this.discard();
            return;
        }
        if (getState() == State.FLYING) {
            tickFlying(owner);
        } else if (getState() == State.ANCHORED) {
            anchorTicks++;
            if (anchorTicks >= ANCHOR_DURATION) this.discard();
        }
    }

    private void tickFlying(Player owner) {
        flyTicks++;
        if (flyTicks >= MAX_FLY_TICKS) {
            this.discard();
            return;
        }
        Vec3 dir = this.getDeltaMovement().normalize();
        for (int i = 0; i < FLY_SUBSTEPS; i++) {
            Vec3 from = this.position();
            Vec3 to = from.add(dir.scale(SUBSTEP_DIST));

            BlockHitResult bhr = this.level().clip(new ClipContext(
                from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            Entity hitEnt = pickHookableEntity(from, to);

            if (bhr.getType() != HitResult.Type.MISS && hitEnt != null) {
                double dB = bhr.getLocation().distanceToSqr(from);
                double dE = hitEnt.position().distanceToSqr(from);
                if (dB <= dE) { onBlockHit(bhr.getLocation(), owner); return; }
                else { onEntityHit(hitEnt, owner); return; }
            }
            if (bhr.getType() != HitResult.Type.MISS) { onBlockHit(bhr.getLocation(), owner); return; }
            if (hitEnt != null) { onEntityHit(hitEnt, owner); return; }

            this.setPos(to);
        }
    }

    private Entity pickHookableEntity(Vec3 from, Vec3 to) {
        AABB box = new AABB(from, to).inflate(0.3);
        Entity bestLight = null, bestHeavy = null;
        double bestLightSq = Double.MAX_VALUE, bestHeavySq = Double.MAX_VALUE;
        for (Entity e : this.level().getEntities(this, box, this::isHookable)) {
            double d = e.distanceToSqr(this);
            if (isHeavy(e)) {
                if (d < bestHeavySq) { bestHeavySq = d; bestHeavy = e; }
            } else {
                if (d < bestLightSq) { bestLightSq = d; bestLight = e; }
            }
        }
        if (bestHeavy != null) return bestHeavy;
        return bestLight;
    }

    private boolean isHookable(Entity e) {
        if (e == this) return false;
        Player owner = getOwnerPlayer();
        if (e == owner) return false;
        if (!e.isAlive()) return false;
        if (e instanceof MomentumHookEntity) return false;
        if (e instanceof Player) return false;
        if (e instanceof Projectile) return false;
        if (e instanceof ItemEntity) return false;
        if (e instanceof ExperienceOrb) return false;
        if (e instanceof AreaEffectCloud) return false;
        if (e instanceof HangingEntity) return false;
        if (e instanceof FallingBlockEntity) return false;
        if (e instanceof LightningBolt) return false;
        if (e instanceof Marker) return false;
        return e.canBeHitByProjectile();
    }

    public static boolean isHeavy(Entity e) {
        return e instanceof WitherBoss
            || e instanceof EnderDragon
            || e instanceof ElderGuardian
            || e instanceof Ravager
            || e instanceof IronGolem
            || e instanceof Ghast
            || e instanceof Shulker
            || e instanceof EndCrystal
            || e instanceof Boat
            || e instanceof AbstractMinecart;
    }

    private void onBlockHit(Vec3 hit, Player owner) {
        this.setPos(hit.x, hit.y, hit.z);
        this.setDeltaMovement(Vec3.ZERO);
        setState(State.ANCHORED);
        playHitSound();
        MomentumPlayerHandler.launchPlayer(owner, hit);
    }

    private void onEntityHit(Entity target, Player owner) {
        Vec3 anchor = new Vec3(target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ());
        this.setPos(anchor);
        this.setDeltaMovement(Vec3.ZERO);
        playHitSound();
        if (isHeavy(target)) {
            setState(State.ANCHORED);
            MomentumPlayerHandler.launchPlayer(owner, anchor);
        } else {
            Vec3 dirToOwner = owner.position().add(0, 0.6, 0).subtract(target.position()).normalize();
            double dist = owner.distanceTo(target);
            double power = Math.min(2.5, 0.5 + dist * 0.06);
            target.setDeltaMovement(dirToOwner.scale(power).add(0, 0.3, 0));
            target.hurtMarked = true;
            this.discard();
        }
    }

    private void playHitSound() {
        // 元 hit/block.mcfunction: iron_door.open + blaze.hurt + item.break
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
            SoundEvents.IRON_DOOR_OPEN, SoundSource.PLAYERS, 2.0f, 1.0f);
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
            SoundEvents.BLAZE_HURT, SoundSource.NEUTRAL, 2.0f, 2.0f);
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
            SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 2.0f, 1.5f);
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        flyTicks = tag.getInt("FlyTicks");
        anchorTicks = tag.getInt("AnchorTicks");
        if (tag.contains("State")) {
            int v = tag.getInt("State");
            State[] vals = State.values();
            if (v >= 0 && v < vals.length) setState(vals[v]);
        }
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("FlyTicks", flyTicks);
        tag.putInt("AnchorTicks", anchorTicks);
        tag.putInt("State", getState().ordinal());
    }
}

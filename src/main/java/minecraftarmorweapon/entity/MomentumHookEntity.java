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
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Marker;
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
 * Momentum Hookshot のフック飛翔体 (Java port of Chuzume's Momentum Hookshot).
 *
 * 元データパック:
 *  - 70 substeps × 0.2 block × 1 tick = 14 block/tick (相当)
 *  - 80 block 射程
 *  - block hit / heavy entity hit → プレイヤーに impulse + levitation で着弾点へ吹っ飛ばす
 *  - light entity hit → 対象を引き寄せる
 *
 * Java 版:
 *  - 12 block/tick (12 substeps × 1 block) + 12 tick max → 144 block 射程 (元パック相当 +α)
 *  - 着弾後はその場に短時間留まる → ロープが見える時間を確保 → 自爆
 *  - vehicle なし。プレイヤーへの影響は単発 impulse + 短期 boost (PD で持続) のみ
 */
public class MomentumHookEntity extends Projectile {

    public enum State { FLYING, ANCHORED, DEAD }

    private static final EntityDataAccessor<Integer> DATA_STATE =
        SynchedEntityData.defineId(MomentumHookEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER_UUID =
        SynchedEntityData.defineId(MomentumHookEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    // 元データパック: 70 substeps × 0.2 block × 1 tick = 14 block/tick
    public static final int FLY_SUBSTEPS = 14;       // 14 block/tick (元準拠)
    public static final double SUBSTEP_DIST = 1.0;
    public static final int MAX_FLY_TICKS = 6;       // 14 × 6 = 84 block 射程 (元 ≒ 80)
    public static final int ANCHOR_DURATION = 6;     // 着弾後 visual hold tick

    private int flyTicks = 0;
    private int anchorTicks = 0;

    public MomentumHookEntity(EntityType<? extends MomentumHookEntity> type, Level level) {
        super(type, level);
    }

    public MomentumHookEntity(PlayMessages.SpawnEntity packet, Level level) {
        super(CustomEntityInit.MOMENTUM_HOOK_ENTITY.get(), level);
    }

    public MomentumHookEntity(Level level, LivingEntity owner) {
        super(CustomEntityInit.MOMENTUM_HOOK_ENTITY.get(), level);
        this.setOwner(owner);
        this.entityData.set(DATA_OWNER_UUID, Optional.of(owner.getUUID()));
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

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_STATE, State.FLYING.ordinal());
        this.entityData.define(DATA_OWNER_UUID, Optional.empty());
    }

    public State getState() {
        int v = this.entityData.get(DATA_STATE);
        State[] vals = State.values();
        return (v < 0 || v >= vals.length) ? State.FLYING : vals[v];
    }

    public void setState(State s) { this.entityData.set(DATA_STATE, s.ordinal()); }

    public Player getOwnerPlayer() {
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
    @Override public boolean isNoGravity() { return true; }

    @Override
    public boolean shouldRenderAtSqrDistance(double dsq) {
        return dsq < 64 * 64 * 64;
    }

    @Override
    public void tick() {
        super.tick();
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
        if (e == this || e == this.getOwner()) return false;
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
            // light: 対象をプレイヤーへ引き寄せる
            Vec3 dirToOwner = owner.position().add(0, 0.6, 0).subtract(target.position()).normalize();
            double dist = owner.distanceTo(target);
            double power = Math.min(2.5, 0.5 + dist * 0.06);
            target.setDeltaMovement(dirToOwner.scale(power).add(0, 0.3, 0));
            target.hurtMarked = true;
            this.discard();
        }
    }

    private void playHitSound() {
        // 元データパック hit/block.mcfunction: iron_door.open + blaze.hurt + item.break
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
            SoundEvents.IRON_DOOR_OPEN, SoundSource.PLAYERS, 2.0f, 1.0f);
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
            SoundEvents.BLAZE_HURT, SoundSource.NEUTRAL, 2.0f, 2.0f);
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
            SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 2.0f, 1.5f);
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        flyTicks = tag.getInt("FlyTicks");
        anchorTicks = tag.getInt("AnchorTicks");
        if (tag.contains("State")) {
            int v = tag.getInt("State");
            State[] vals = State.values();
            if (v >= 0 && v < vals.length) setState(vals[v]);
        }
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        tag.putInt("FlyTicks", flyTicks);
        tag.putInt("AnchorTicks", anchorTicks);
        tag.putInt("State", getState().ordinal());
    }
}

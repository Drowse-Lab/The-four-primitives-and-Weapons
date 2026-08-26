package the_four_primitives_and_weapons.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages;
import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModEntities;
import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModItems;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.UUID;

/** Qで投げたLunaの軽量な護衛形態。 */
public class LunaCompanionEntity extends PathfinderMob {
    private static final EntityDataAccessor<Boolean> FIRING =
            SynchedEntityData.defineId(LunaCompanionEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> ENGAGING =
            SynchedEntityData.defineId(LunaCompanionEntity.class, EntityDataSerializers.BOOLEAN);
    private UUID ownerId;
    private ItemStack storedItem = ItemStack.EMPTY;
    private LivingEntity guardTarget;
    private int attackCooldown;
    private int firingTicks;
    private boolean itemReturned;

    public LunaCompanionEntity(PlayMessages.SpawnEntity packet, Level level) {
        this(TheFourPrimitivesAndWeaponsModEntities.LUNA_COMPANION.get(), level);
    }

    public LunaCompanionEntity(EntityType<LunaCompanionEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setNoGravity(true);
        xpReward = 0;
    }

    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
    @Override protected void registerGoals() { }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(FIRING, false);
        entityData.define(ENGAGING, false);
    }

    public boolean isFiringLaser() {
        return entityData.get(FIRING);
    }

    public boolean isEngagingTarget() {
        return entityData.get(ENGAGING);
    }

    public void bind(Player owner, ItemStack item) {
        ownerId = owner.getUUID();
        storedItem = item.copy();
        storedItem.setCount(1);
    }

    @Nullable
    private ServerPlayer owner() {
        if (ownerId == null || !(level() instanceof ServerLevel serverLevel)) return null;
        return serverLevel.getServer().getPlayerList().getPlayer(ownerId);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        setNoGravity(true);
        if (level().isClientSide) return;
        ServerPlayer owner = owner();
        if (owner == null) return;
        if (attackCooldown > 0) attackCooldown--;
        if (firingTicks > 0 && --firingTicks == 0) entityData.set(FIRING, false);

        // 索敵は毎tickではなく0.5秒ごと。
        if (tickCount % 10 == 0) {
            // 召喚者が攻撃した相手を最優先し、次に召喚者を攻撃した相手を守備対象にする。
            LivingEntity attackedByOwner = owner.getLastHurtMob();
            LivingEntity attackedOwner = owner.getLastHurtByMob();
            if (validTarget(attackedByOwner, owner)) guardTarget = attackedByOwner;
            else if (validTarget(attackedOwner, owner)) guardTarget = attackedOwner;
            else guardTarget = level().getEntitiesOfClass(Monster.class, owner.getBoundingBox().inflate(12.0),
                            mob -> mob.isAlive() && mob.getTarget() == owner).stream()
                    .min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        }

        if (validTarget(guardTarget, owner)) {
            entityData.set(ENGAGING, true);
            faceBladeToward(guardTarget);
            double targetDistance = distanceToSqr(guardTarget);
            if (targetDistance > 100.0) {
                moveToward(guardTarget.getX(), guardTarget.getY() + guardTarget.getBbHeight() * 0.5, guardTarget.getZ(), 0.32);
            } else {
                setDeltaMovement(getDeltaMovement().scale(0.65));
            }
            if (targetDistance <= 144.0 && attackCooldown == 0) {
                fireLaser(guardTarget);
                attackCooldown = 20;
            }
        } else {
            guardTarget = null;
            entityData.set(ENGAGING, false);
            double angle = tickCount * 0.05;
            moveToward(owner.getX() + Math.cos(angle) * 1.5, owner.getY() + 1.4,
                    owner.getZ() + Math.sin(angle) * 1.5, 0.22);
        }
        if (distanceToSqr(owner) > 1024.0) teleportTo(owner.getX(), owner.getY() + 1.0, owner.getZ());
    }

    private void faceBladeToward(LivingEntity target) {
        double dx = target.getX() - getX();
        double dz = target.getZ() - getZ();
        // 待機モデルをZ軸で90度倒すと切先はローカル+Xを向くため、
        // その+Xを敵への水平ベクトルへ合わせる。
        setYRot((float)Math.toDegrees(Math.atan2(dz, dx)));
        yRotO = getYRot();
    }

    private void fireLaser(LivingEntity target) {
        if (!(level() instanceof ServerLevel serverLevel)) return;
        entityData.set(FIRING, true);
        firingTicks = 7;
        Vec3 start = position().add(0, 0.65, 0);
        Vec3 end = target.position().add(0, target.getBbHeight() * 0.5, 0);
        Vec3 line = end.subtract(start);
        faceBladeToward(target);
        int points = Math.max(12, (int)(line.length() * 8.0));
        for (int i = 0; i <= points; i++) {
            Vec3 pos = start.add(line.scale(i / (double)points));
            serverLevel.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
            if (i % 4 == 0)
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, 1, 0.01, 0.01, 0.01, 0);
        }
        target.hurt(damageSources().mobAttack(this), 8.0F);
    }

    private void moveToward(double x, double y, double z, double speed) {
        Vec3 delta = new Vec3(x - getX(), y - getY(), z - getZ());
        setDeltaMovement(delta.lengthSqr() > 0.04 ? delta.normalize().scale(speed) : getDeltaMovement().scale(0.5));
    }

    private boolean validTarget(@Nullable LivingEntity target, Player owner) {
        return target != null && target.isAlive() && target != owner && target != this
                && target.distanceToSqr(owner) <= 576.0;
    }

    @Override public boolean isPushable() { return false; }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (ownerId == null || !ownerId.equals(player.getUUID())) return InteractionResult.PASS;
        if (!level().isClientSide) {
            returnItem();
            discard();
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public void die(DamageSource source) {
        returnItem();
        super.die(source);
    }

    private void returnItem() {
        if (itemReturned || level().isClientSide) return;
        itemReturned = true;
        ItemStack item = storedItem.isEmpty() ? new ItemStack(TheFourPrimitivesAndWeaponsModItems.LUNA.get()) : storedItem.copy();
        ServerPlayer owner = owner();
        if (owner != null) {
            if (!owner.getInventory().add(item)) owner.drop(item, false);
        } else spawnAtLocation(item);
        storedItem = ItemStack.EMPTY;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (ownerId != null) tag.putUUID("Owner", ownerId);
        if (!storedItem.isEmpty()) tag.put("Luna", storedItem.save(new CompoundTag()));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        storedItem = tag.contains("Luna") ? ItemStack.of(tag.getCompound("Luna")) : ItemStack.EMPTY;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.ARMOR, 6.0)
                .add(Attributes.ATTACK_DAMAGE, 8.0).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FOLLOW_RANGE, 24.0);
    }
}

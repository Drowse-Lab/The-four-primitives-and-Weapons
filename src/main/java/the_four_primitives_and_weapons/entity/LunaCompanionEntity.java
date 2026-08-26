package the_four_primitives_and_weapons.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.particles.ParticleTypes;
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
    private boolean standbyAnchorInitialized;
    private double standbyYaw;
    private double lastOwnerX;
    private double lastOwnerY;
    private double lastOwnerZ;

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

    public boolean isOwnedBy(UUID playerId) {
        return ownerId != null && ownerId.equals(playerId);
    }

    /** 所有者の死亡直前または右クリック時に、アイテムへ戻して消える。 */
    public void recallToOwner() {
        if (level().isClientSide) return;
        returnItem();
        discard();
    }

    public void bind(Player owner, ItemStack item) {
        ownerId = owner.getUUID();
        storedItem = item.copy();
        storedItem.setCount(1);
        initializeStandbyAnchor(owner);
    }

    private void initializeStandbyAnchor(Player owner) {
        standbyAnchorInitialized = true;
        standbyYaw = owner.getYRot();
        lastOwnerX = owner.getX();
        lastOwnerY = owner.getY();
        lastOwnerZ = owner.getZ();
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
        if (!standbyAnchorInitialized) initializeStandbyAnchor(owner);

        double ownerDx = owner.getX() - lastOwnerX;
        double ownerDy = owner.getY() - lastOwnerY;
        double ownerDz = owner.getZ() - lastOwnerZ;
        // 視点だけでは待機方向を更新しない。実際に位置が変化した時だけ更新する。
        if (ownerDx * ownerDx + ownerDy * ownerDy + ownerDz * ownerDz > 0.0004) {
            standbyYaw = owner.getYRot();
        }
        lastOwnerX = owner.getX();
        lastOwnerY = owner.getY();
        lastOwnerZ = owner.getZ();
        if (attackCooldown > 0) attackCooldown--;
        if (firingTicks > 0 && --firingTicks == 0) entityData.set(FIRING, false);

        // 召喚中だけ有効な、粒子・HUDアイコンを表示しない暗視。
        if (tickCount % 20 == 0) {
            // 200tick未満だとバニラの暗視が明滅するため、余裕を持って更新する。
            owner.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 220, 0, true, false, false));
        }
        // 常時演出は5tickに1個だけにして負荷を抑える。
        if (tickCount % 5 == 0 && level() instanceof ServerLevel serverLevel) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double radius = 0.35 + random.nextDouble() * 0.55;
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    getX() + Math.cos(angle) * radius,
                    getY() + 0.25 + random.nextDouble() * 1.0,
                    getZ() + Math.sin(angle) * radius,
                    1, 0.02, 0.02, 0.02, 0.0);
        }

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
                // プレイヤーの攻撃速度ゲージ回復時間より5tickだけ長くする。
                attackCooldown = Math.max(10,
                        (int)Math.ceil(owner.getCurrentItemAttackStrengthDelay()) + 5);
            }
        } else {
            guardTarget = null;
            entityData.set(ENGAGING, false);
            // 周回させず、プレイヤーの右横に固定する。プレイヤーの移動または
            // 視点変更で固定位置が変わった時だけ追従し、到着後は完全停止する。
            double yaw = Math.toRadians(standbyYaw);
            double standbyX = owner.getX() + Math.cos(yaw) * 1.5;
            double standbyY = owner.getY() + 1.4;
            double standbyZ = owner.getZ() + Math.sin(yaw) * 1.5;
            moveToward(standbyX, standbyY, standbyZ, 0.22);
        }
        if (distanceToSqr(owner) > 1024.0) teleportTo(owner.getX(), owner.getY() + 1.0, owner.getZ());
    }

    private void faceBladeToward(LivingEntity target) {
        double dx = target.getX() - getX();
        double dy = target.getY() + target.getBbHeight() * 0.5 - (getY() + getBbHeight() * 0.55);
        double dz = target.getZ() - getZ();
        // 待機モデルをZ軸で90度倒すと切先はローカル+Xを向くため、
        // その+Xを敵への水平ベクトルへ合わせる。
        setYRot((float)Math.toDegrees(Math.atan2(dz, dx)));
        setXRot((float)Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));
        yRotO = getYRot();
        xRotO = getXRot();
    }

    private void fireLaser(LivingEntity target) {
        if (!(level() instanceof ServerLevel serverLevel)) return;
        entityData.set(FIRING, true);
        firingTicks = 7;
        faceBladeToward(target);
        the_four_primitives_and_weapons.procedures.LunaenteiteigaaitemuwoZhentutaShiProcedure
                .fireSummonedStraightLaser(serverLevel, this, target, owner());
        target.hurt(damageSources().mobAttack(this), 8.0F);
    }

    private void moveToward(double x, double y, double z, double speed) {
        Vec3 delta = new Vec3(x - getX(), y - getY(), z - getZ());
        double distance = delta.length();
        if (distance <= 0.08) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        // 目的地直前では減速し、通り過ぎて往復しないようにする。
        setDeltaMovement(delta.scale(Math.min(speed, distance) / distance));
    }

    private boolean validTarget(@Nullable LivingEntity target, Player owner) {
        return target != null && target.isAlive() && target != owner && target != this
                && target.distanceToSqr(owner) <= 576.0;
    }

    @Override public boolean isPushable() { return false; }

    /** 物理衝突は持たず、プレイヤーや他エンティティが通り抜けられる。 */
    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    /** Mobのターゲット選択や通常攻撃の対象にしない。 */
    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (ownerId == null || !ownerId.equals(player.getUUID())) return InteractionResult.PASS;
        if (!level().isClientSide) {
            recallToOwner();
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

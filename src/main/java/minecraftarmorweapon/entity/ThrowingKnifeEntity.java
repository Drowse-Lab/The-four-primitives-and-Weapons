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

    private static final float DAMAGE = 6.0f;
    private static final int STUCK_LIFETIME_TICKS = 600; // 30秒で消滅

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

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_STUCK, false);
        this.entityData.define(DATA_STUCK_YAW, 0f);
        this.entityData.define(DATA_STUCK_PITCH, 0f);
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
        target.hurt(this.damageSources().thrown(this, this.getOwner()), DAMAGE);
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

        Vec3 hitPos = result.getLocation();
        this.setPos(hitPos.x, hitPos.y, hitPos.z);
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

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Stuck", isStuck());
        tag.putInt("StuckTicks", stuckTicks);
        tag.putFloat("StuckYaw", getStuckYaw());
        tag.putFloat("StuckPitch", getStuckPitch());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DATA_STUCK, tag.getBoolean("Stuck"));
        this.stuckTicks = tag.getInt("StuckTicks");
        this.entityData.set(DATA_STUCK_YAW, tag.getFloat("StuckYaw"));
        this.entityData.set(DATA_STUCK_PITCH, tag.getFloat("StuckPitch"));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public ItemStack getItem() {
        return new ItemStack(CustomEntityInit.THROWING_KNIFE.get());
    }
}

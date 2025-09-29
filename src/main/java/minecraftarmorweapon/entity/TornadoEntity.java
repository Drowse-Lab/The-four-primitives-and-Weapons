package minecraftarmorweapon.entity;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages;
import minecraftarmorweapon.util.DamageCalculator;
import minecraftarmorweapon.init.MinecraftArmorWeaponModCustomEntities;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.ArrayList;

public class TornadoEntity extends Entity {

    private static final EntityDataAccessor<Boolean> WITH_ELECTRICITY = SynchedEntityData.defineId(TornadoEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(TornadoEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SPEED = SynchedEntityData.defineId(TornadoEntity.class, EntityDataSerializers.FLOAT);

    private Player owner;
    private Vec3 moveDirection;
    private int lifespan = 200; // 10秒間存在
    private float maxHeight = 15.0f;
    private float radius = 4.0f;
    private List<LivingEntity> affectedEntities = new ArrayList<>();
    private int tickCount = 0;
    private ItemStack weaponStack = ItemStack.EMPTY;

    public TornadoEntity(EntityType<? extends TornadoEntity> type, Level world) {
        super(type, world);
        this.noPhysics = true;
        this.noCulling = true;
    }

    // Client-side spawn constructor
    public TornadoEntity(PlayMessages.SpawnEntity packet, Level world) {
        this(MinecraftArmorWeaponModCustomEntities.TORNADO.get(), world);
    }

    public TornadoEntity(Level world, Player owner, Vec3 position, Vec3 direction, boolean withElectricity, float damage, ItemStack weapon) {
        this(MinecraftArmorWeaponModCustomEntities.TORNADO.get(), world);
        this.owner = owner;
        this.moveDirection = direction.normalize();
        this.weaponStack = weapon.copy();
        setPos(position.x, position.y, position.z);
        entityData.set(WITH_ELECTRICITY, withElectricity);
        entityData.set(DAMAGE, damage);
        entityData.set(SPEED, 0.5f);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(WITH_ELECTRICITY, false);
        entityData.define(DAMAGE, 10.0f);
        entityData.define(SPEED, 0.5f);
    }

    @Override
    public void tick() {
        super.tick();

        if (!level.isClientSide) {
            tickCount++;

            // 寿命チェック
            if (tickCount >= lifespan) {
                discard();
                return;
            }

            // 移動
            moveAlongPath();

            // エフェクトと攻撃処理
            if (tickCount % 2 == 0) {
                applyTornadoEffects();
            }

            // ダメージ処理（5tickごと）
            if (tickCount % 5 == 0) {
                damageNearbyEntities();
            }
        } else {
            // クライアント側のエフェクト
            createVisualEffects();
        }
    }

    private void moveAlongPath() {
        Vec3 currentPos = position();
        Vec3 newPos = currentPos.add(moveDirection.scale(entityData.get(SPEED)));
        setPos(newPos.x, newPos.y, newPos.z);
    }

    private void applyTornadoEffects() {
        if (!(level instanceof ServerLevel serverLevel)) return;

        Vec3 pos = position();
        boolean withElectricity = entityData.get(WITH_ELECTRICITY);

        // 竜巻の物理効果
        AABB searchArea = new AABB(
            pos.x - radius * 1.5, pos.y - 1, pos.z - radius * 1.5,
            pos.x + radius * 1.5, pos.y + maxHeight, pos.z + radius * 1.5
        );

        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, searchArea,
            entity -> entity != owner && entity.isAlive());

        for (LivingEntity target : targets) {
            Vec3 targetPos = target.position();
            Vec3 toCenter = pos.subtract(targetPos);
            double horizontalDistance = Math.sqrt(toCenter.x * toCenter.x + toCenter.z * toCenter.z);

            if (horizontalDistance <= radius * 1.5) {
                // 竜巻に巻き込む
                liftAndRotateEntity(target, pos);

                // 感電効果（StormItemの場合のみ）
                if (withElectricity && !affectedEntities.contains(target)) {
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2));

                    // 感電パーティクル
                    serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        target.getX(), target.getY() + 1, target.getZ(),
                        20, 0.5, 0.5, 0.5, 0.1);
                }

                // 影響を受けたエンティティのリストに追加
                if (!affectedEntities.contains(target)) {
                    affectedEntities.add(target);
                }
            }
        }
    }

    private void damageNearbyEntities() {
        if (!(level instanceof ServerLevel)) return;

        Vec3 pos = position();
        float damage = entityData.get(DAMAGE);

        // ダメージ範囲内のエンティティを検索
        AABB damageArea = new AABB(
            pos.x - radius, pos.y - 1, pos.z - radius,
            pos.x + radius, pos.y + maxHeight * 0.7, pos.z + radius
        );

        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, damageArea,
            entity -> entity != owner && affectedEntities.contains(entity));

        for (LivingEntity target : targets) {
            // ダメージを与える
            if (owner != null) {
                DamageCalculator.dealDamage(owner, target, damage, weaponStack);
            } else {
                target.hurt(DamageSource.MAGIC, damage);
            }
        }
    }

    private void liftAndRotateEntity(LivingEntity entity, Vec3 tornadoCenter) {
        Vec3 toCenter = tornadoCenter.subtract(entity.position());
        double horizontalDistance = Math.sqrt(toCenter.x * toCenter.x + toCenter.z * toCenter.z);

        if (horizontalDistance > 0.1) {
            // 中心に向かって引き寄せ
            double pullStrength = Math.max(0, 1.0 - horizontalDistance / (radius * 1.5)) * 0.6;
            Vec3 pullVec = new Vec3(toCenter.x, 0, toCenter.z).normalize().scale(pullStrength);

            // 回転方向のベクトル
            Vec3 rotationVec = new Vec3(-toCenter.z, 0, toCenter.x).normalize().scale(0.8);

            // 高さに応じた上昇力
            double currentHeight = entity.getY() - tornadoCenter.y;
            double liftForce;
            if (currentHeight < maxHeight * 0.3) {
                liftForce = 1.2;
            } else if (currentHeight < maxHeight * 0.7) {
                liftForce = 0.7;
            } else {
                liftForce = 0.3;
            }

            // 螺旋状の動き
            double spiralAngle = tickCount * 0.2;
            double spiralRadius = 0.3;
            double spiralX = Math.cos(spiralAngle) * spiralRadius;
            double spiralZ = Math.sin(spiralAngle) * spiralRadius;

            // 運動量を設定
            entity.setDeltaMovement(
                pullVec.x + rotationVec.x + spiralX,
                liftForce,
                pullVec.z + rotationVec.z + spiralZ
            );

            // エンティティを回転
            entity.setYRot(entity.getYRot() + 35);
            entity.setXRot(entity.getXRot() + 10);

            // 落下ダメージを無効化
            entity.fallDistance = 0;
        }
    }

    private void createVisualEffects() {
        Vec3 pos = position();
        boolean withElectricity = entityData.get(WITH_ELECTRICITY);
        double timeOffset = tickCount * 0.1;

        // 竜巻の視覚効果
        for (double h = 0; h <= maxHeight; h += 0.5) {
            double heightRatio = h / maxHeight;
            double currentRadius;

            if (heightRatio < 0.1) {
                currentRadius = radius * (1.0 + (0.1 - heightRatio) * 2);
            } else if (heightRatio < 0.8) {
                currentRadius = radius * (1.0 - heightRatio * 0.5);
            } else {
                currentRadius = radius * 0.6 * (1.0 + (heightRatio - 0.8) * 0.5);
            }

            // 螺旋パーティクル
            int particleCount = Math.max(8, (int)(currentRadius * 10));
            for (int i = 0; i < particleCount; i++) {
                double angle = (i / (double)particleCount) * Math.PI * 2 + h * 1.2 + timeOffset;
                double xOffset = Math.cos(angle) * currentRadius;
                double zOffset = Math.sin(angle) * currentRadius;

                // 煙パーティクル
                level.addParticle(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,
                    pos.x + xOffset, pos.y + h, pos.z + zOffset,
                    xOffset * 0.05, 0.1, zOffset * 0.05);

                // 感電エフェクト（StormItemの場合）
                if (withElectricity && i % 3 == 0 && Math.random() < 0.3) {
                    level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                        pos.x + xOffset, pos.y + h, pos.z + zOffset,
                        0, 0, 0);
                }
            }
        }

        // 地面の巻き上げ効果
        for (int i = 0; i < 10; i++) {
            double angle = (i / 10.0) * Math.PI * 2 + timeOffset;
            double groundRadius = radius * 1.2;
            level.addParticle(ParticleTypes.POOF,
                pos.x + Math.cos(angle) * groundRadius,
                pos.y + 0.1,
                pos.z + Math.sin(angle) * groundRadius,
                0, 0.05, 0);
        }
    }

    @Override
    public void onRemovedFromWorld() {
        super.onRemovedFromWorld();

        // 竜巻が消える時のエフェクト
        if (level instanceof ServerLevel serverLevel) {
            Vec3 pos = position();
            serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                pos.x, pos.y + maxHeight / 2, pos.z,
                10, radius / 2, maxHeight / 4, radius / 2, 0.1);

            level.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 0.5f, 1.2f);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        lifespan = compound.getInt("Lifespan");
        tickCount = compound.getInt("TickCount");
        radius = compound.getFloat("Radius");
        maxHeight = compound.getFloat("MaxHeight");
        if (compound.hasUUID("Owner")) {
            if (level instanceof ServerLevel serverLevel) {
                Entity entity = serverLevel.getEntity(compound.getUUID("Owner"));
                if (entity instanceof Player) {
                    owner = (Player) entity;
                }
            }
        }
        if (compound.contains("Weapon")) {
            weaponStack = ItemStack.of(compound.getCompound("Weapon"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        compound.putInt("Lifespan", lifespan);
        compound.putInt("TickCount", tickCount);
        compound.putFloat("Radius", radius);
        compound.putFloat("MaxHeight", maxHeight);
        if (owner != null) {
            compound.putUUID("Owner", owner.getUUID());
        }
        if (!weaponStack.isEmpty()) {
            compound.put("Weapon", weaponStack.save(new CompoundTag()));
        }
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    public void setSpeed(float speed) {
        entityData.set(SPEED, speed);
    }

    public void setDamage(float damage) {
        entityData.set(DAMAGE, damage);
    }

    public void setLifespan(int ticks) {
        this.lifespan = ticks;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    public void setMaxHeight(float height) {
        this.maxHeight = height;
    }

    public void setOwner(Player player) {
        this.owner = player;
    }

    public void setDirection(Vec3 direction) {
        this.moveDirection = direction.normalize();
    }

    public void setWithElectricity(boolean withElectricity) {
        entityData.set(WITH_ELECTRICITY, withElectricity);
    }

    public void setWeapon(ItemStack weapon) {
        this.weaponStack = weapon.copy();
    }
}
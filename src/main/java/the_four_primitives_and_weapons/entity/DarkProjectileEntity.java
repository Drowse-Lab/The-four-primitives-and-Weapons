package the_four_primitives_and_weapons.entity;

import the_four_primitives_and_weapons.util.VersionHelper;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages;

import the_four_primitives_and_weapons.util.DamageCalculator;
import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModCustomEntities;

public class DarkProjectileEntity extends AbstractHurtingProjectile {
    private LivingEntity target;
    private LivingEntity shooter;
    private float damage = 15.0f;
    private int ticksAlive = 0;
    private static final int MAX_LIFE = 200; // 10秒

    public DarkProjectileEntity(EntityType<? extends DarkProjectileEntity> type, Level world) {
        super(type, world);
    }

    public DarkProjectileEntity(Level world, LivingEntity shooter, LivingEntity target, float damage) {
        super(TheFourPrimitivesAndWeaponsModCustomEntities.DARK_PROJECTILE.get(), shooter, 0, 0, 0, world);
        this.shooter = shooter;
        this.target = target;
        this.damage = damage;
        this.setNoGravity(true);
    }

    public DarkProjectileEntity(PlayMessages.SpawnEntity packet, Level world) {
        super(TheFourPrimitivesAndWeaponsModCustomEntities.DARK_PROJECTILE.get(), world);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void tick() {
        super.tick();

        ticksAlive++;
        if (ticksAlive > MAX_LIFE) {
            this.discard();
            return;
        }

        // パーティクルエフェクト
        if (VersionHelper.getLevel(this) instanceof ServerLevel serverLevel) {
            // 闇のパーティクル
            serverLevel.sendParticles(ParticleTypes.SMOKE,
                this.getX(), this.getY(), this.getZ(),
                2, 0.1, 0.1, 0.1, 0.01);

            serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
                this.getX(), this.getY(), this.getZ(),
                2, 0.08, 0.08, 0.08, 0.01);

            // 軌跡エフェクト
            serverLevel.sendParticles(ParticleTypes.SQUID_INK,
                this.getX(), this.getY(), this.getZ(),
                1, 0, 0, 0, 0);
        }

        // ターゲットへの追尾
        if (target != null && target.isAlive() && !target.isRemoved()) {
            Vec3 toTarget = target.position().add(0, target.getBbHeight() / 2, 0).subtract(this.position());
            double distance = toTarget.length();

            if (distance > 0.1) {
                // 追尾の強さ（徐々に強くなる）
                double homingStrength = Math.min(0.5 + ticksAlive * 0.02, 2.0);
                Vec3 currentMotion = this.getDeltaMovement();
                Vec3 targetDirection = toTarget.normalize().scale(0.4);

                // 現在の運動量と目標方向を補間
                Vec3 newMotion = currentMotion.lerp(targetDirection, homingStrength * 0.1);

                // 速度制限
                double speed = Math.min(newMotion.length(), 1.0);
                if (newMotion.length() > 0.01) {
                    newMotion = newMotion.normalize().scale(speed);
                }

                this.setDeltaMovement(newMotion);

                // 回転を更新
                this.setYRot((float)(Math.atan2(newMotion.x, newMotion.z) * 180.0D / Math.PI));
                this.setXRot((float)(Math.atan2(newMotion.y, newMotion.horizontalDistance()) * 180.0D / Math.PI));
            }
        } else if (target == null) {
            // ターゲットがいない場合は直進
            if (this.getDeltaMovement().length() < 0.01) {
                Vec3 shooterLook = shooter.getLookAngle();
                this.setDeltaMovement(shooterLook.scale(0.5));
            }
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity entity = result.getEntity();

        if (entity instanceof LivingEntity livingEntity && entity != this.getOwner()) {
            // ダメージを与える
            if (shooter != null && shooter instanceof net.minecraft.world.entity.player.Player player) {
                // 実際の攻撃イベントを発火（武器攻撃力 + エンチャント + エフェクト）
                player.attack(livingEntity);

                // 無敵時間をリセットしてボーナスダメージ（+7）を追加
                livingEntity.invulnerableTime = 0;
                livingEntity.hurt(player.damageSources().playerAttack(player), damage);
            } else if (shooter != null) {
                livingEntity.hurt(shooter.damageSources().mobAttack(shooter), damage);
            } else {
                livingEntity.hurt(this.damageSources().magic(), damage);
            }

            // ヒットエフェクト
            if (VersionHelper.getLevel(this) instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SQUID_INK,
                    livingEntity.getX(), livingEntity.getY() + livingEntity.getBbHeight() / 2, livingEntity.getZ(),
                    16, 0.35, 0.45, 0.35, 0.04);

                serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                    livingEntity.getX(), livingEntity.getY() + 1, livingEntity.getZ(),
                    30, 0.3, 0.3, 0.3, 0.05);
            }

            // ヒットサウンド
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.WITHER_HURT, SoundSource.HOSTILE, 1.0f, 0.5f);

            this.discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        // ブロックに当たった時のエフェクト
        if (VersionHelper.getLevel(this) instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SMOKE,
                result.getLocation().x, result.getLocation().y, result.getLocation().z,
                10, 0.3, 0.3, 0.3, 0.05);
        }

        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
            SoundEvents.WITHER_BREAK_BLOCK, SoundSource.HOSTILE, 0.5f, 1.0f);

        this.discard();
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }

    @Override
    protected float getInertia() {
        return 1.0F;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putFloat("Damage", this.damage);
        compound.putInt("TicksAlive", this.ticksAlive);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.damage = compound.getFloat("Damage");
        this.ticksAlive = compound.getInt("TicksAlive");
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return ParticleTypes.SMOKE;
    }
}

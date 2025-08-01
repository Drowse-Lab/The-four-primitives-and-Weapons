

package minecraftarmorweapon.entity;

import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.network.PlayMessages;
import net.minecraftforge.network.NetworkHooks;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
//import net.minecraft.damagesource.DamageSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.Entity;
import minecraftarmorweapon.init.MinecraftArmorWeaponModEntities;
//package minecraftarmorweapon.entity.ai;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import minecraftarmorweapon.entity.ai.CustomMeleeAttackGoal;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

public class FlyingAttackerEntity extends Monster {
    private LivingEntity owner;
    private LivingEntity designatedTarget;

    public FlyingAttackerEntity(PlayMessages.SpawnEntity packet, Level world) {
        this(MinecraftArmorWeaponModEntities.FLYING_ATTACKER.get(), world);
    }

    public FlyingAttackerEntity(EntityType<FlyingAttackerEntity> type, Level world) {
        super(type, world);
        maxUpStep = 0.6f;
        xpReward = 0;
        setNoAi(false);
        setPersistenceRequired();
    }

    public LivingEntity getOwner() {
        return this.owner;
    }

    public void setOwner(LivingEntity owner) {
        this.owner = owner;
    }

    public LivingEntity getDesignatedTarget() {
        return this.designatedTarget;
    }

    public void setDesignatedTarget(LivingEntity target) {
        this.designatedTarget = target;
        if (target != null) {
            this.setTarget(target);
        }
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();

        // 指定されたターゲットを優先的に攻撃
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(
            this,
            LivingEntity.class,
            true,
            entity -> {
                // 指定されたターゲットがいる場合は、そのターゲットのみを攻撃
                if (this.designatedTarget != null && this.designatedTarget.isAlive()) {
                    return entity == this.designatedTarget;
                }
                // 指定されたターゲットがいない場合は、召喚者以外を攻撃
                return entity != this.getOwner() && entity.isAlive();
            }
        ));

        // 攻撃行動を追加（近接攻撃）
        this.goalSelector.addGoal(1, new CustomMeleeAttackGoal(this, 1.2, false));

        // その他の行動
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(3, new FloatGoal(this));
        this.targetSelector.addGoal(4, new HurtByTargetGoal(this));
    }

    @Override
    public MobType getMobType() {
        return MobType.UNDEFINED;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public double getMyRidingOffset() {
        return -0.35D;
    }

    @Override
    public SoundEvent getHurtSound(DamageSource ds) {
        return ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.generic.hurt"));
    }

    @Override
    public SoundEvent getDeathSound() {
        return ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.generic.death"));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        this.setNoGravity(true);

        // 指定されたターゲットが死んだり、離れすぎた場合は消滅
        if (this.designatedTarget != null) {
            if (!this.designatedTarget.isAlive() || this.distanceToSqr(this.designatedTarget) > 256.0D) {
                this.discard();
                return;
            }
        }

        if (this.tickCount % 20 < 10) {
            this.setDeltaMovement(this.getDeltaMovement().add(0, 0.01, 0));
        } else {
            this.setDeltaMovement(this.getDeltaMovement().add(0, -0.01, 0));
        }
    }

    public static void init() {}

    public static AttributeSupplier.Builder createAttributes() {
        AttributeSupplier.Builder builder = Mob.createMobAttributes();
        builder = builder.add(Attributes.MOVEMENT_SPEED, 0.3);
        builder = builder.add(Attributes.MAX_HEALTH, 10);
        builder = builder.add(Attributes.ARMOR, 0);
        builder = builder.add(Attributes.ATTACK_DAMAGE, 3);
        builder = builder.add(Attributes.FOLLOW_RANGE, 16);
        return builder;
    }
}

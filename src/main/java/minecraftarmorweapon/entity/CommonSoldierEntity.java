package minecraftarmorweapon.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

import minecraftarmorweapon.ai.PlayerLikeAIGoal;
import minecraftarmorweapon.init.MinecraftArmorWeaponModItems;

/**
 * 一般兵（CommonSoldier）エンティティ
 *
 * ティア1のAIを持つ、プレイヤーのような動作をする敵対Mob
 *
 * 特徴:
 * - 基本的な回避能力（成功率30%）
 * - チャージ攻撃を使用
 * - 鉄の刀を装備
 * - プレイヤーと同じような戦闘スタイル
 */
public class CommonSoldierEntity extends PathfinderMob {

    private static final int AI_TIER = 1;
    private PlayerLikeAIGoal playerLikeAI;

    public CommonSoldierEntity(EntityType<? extends PathfinderMob> type, Level world) {
        super(type, world);

        this.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(MinecraftArmorWeaponModItems.IRON_KATANA.get()));
        this.setDropChance(net.minecraft.world.entity.EquipmentSlot.MAINHAND, 0.0f);
        this.xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 40.0)
            .add(Attributes.MOVEMENT_SPEED, 0.28)
            .add(Attributes.ATTACK_DAMAGE, 5.0)
            .add(Attributes.ARMOR, 4.0)
            .add(Attributes.FOLLOW_RANGE, 32.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.2);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        
        this.playerLikeAI = new PlayerLikeAIGoal(this, AI_TIER);
        this.goalSelector.addGoal(1, this.playerLikeAI);
        
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source == DamageSource.FALL && playerLikeAI != null && playerLikeAI.isFallDamageImmune()) {
            if (!this.level.isClientSide) {
                this.level.broadcastEntityEvent(this, (byte) 2);
            }
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource cause) {
        super.die(cause);
        if (!this.level.isClientSide) {
            if (cause.getEntity() instanceof Player player) {
                player.displayClientMessage(Component.literal("§7一般兵を倒した"), true);
            }
        }
    }

    @Override
    public Component getName() {
        return Component.literal("§f一般兵");
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SKELETON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PLAYER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PLAYER_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 0.8f;
    }

    @Override
    public boolean canAttack(net.minecraft.world.entity.LivingEntity target) {
        return target instanceof Player && super.canAttack(target);
    }

    public int getAITier() {
        return AI_TIER;
    }

    @Override
    protected boolean isSunBurnTick() {
        return false;
    }

    public static void init() {
    }
}
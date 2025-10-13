package minecraftarmorweapon.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
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
public class CommonSoldierEntity extends Monster {

    private static final int AI_TIER = 1; // 一般兵 = ティア1
    private PlayerLikeAIGoal playerLikeAI;

    public CommonSoldierEntity(EntityType<? extends Monster> type, Level world) {
        super(type, world);

        // 鉄の刀を装備
        this.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(MinecraftArmorWeaponModItems.IRON_KATANA.get()));

        // 装備を落とさないように設定
        this.setDropChance(net.minecraft.world.entity.EquipmentSlot.MAINHAND, 0.0f);

        // 経験値
        this.xpReward = 10;
    }

    /**
     * エンティティの属性を設定
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 40.0) // HP: 40（プレイヤーの2倍）
            .add(Attributes.MOVEMENT_SPEED, 0.28) // 移動速度: やや遅い
            .add(Attributes.ATTACK_DAMAGE, 5.0) // 攻撃力: 5
            .add(Attributes.ARMOR, 4.0) // 防御力: 4（鉄防具相当）
            .add(Attributes.FOLLOW_RANGE, 32.0) // 索敵範囲: 32ブロック
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.2); // ノックバック耐性: 20%
    }

    /**
     * AI Goalの登録
     */
    @Override
    protected void registerGoals() {
        // 水に浮く
        this.goalSelector.addGoal(0, new FloatGoal(this));

        // ★ プレイヤーのような動作をするAI（最優先）
        this.playerLikeAI = new PlayerLikeAIGoal(this, AI_TIER);
        this.goalSelector.addGoal(1, this.playerLikeAI);

        // 通常の近接攻撃（PlayerLikeAIGoalが処理しない場合のバックアップ）
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, false));

        // 周囲を移動
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));

        // プレイヤーを見る
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));

        // ランダムに周囲を見る
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        // ターゲット選択
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /**
     * ダメージ処理
     * PlayerLikeAIGoalの回避による落下ダメージ無効を処理
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        // 落下ダメージで、かつ無効時間中なら無効化
        if (source == DamageSource.FALL && playerLikeAI != null && playerLikeAI.isFallDamageImmune()) {
            // 落下ダメージ無効のエフェクト
            if (!this.level.isClientSide) {
                this.level.broadcastEntityEvent(this, (byte) 2); // ハートのパーティクル
            }
            return false;
        }

        return super.hurt(source, amount);
    }

    /**
     * 死亡時の処理
     */
    @Override
    public void die(DamageSource cause) {
        super.die(cause);

        // 死亡メッセージ（デバッグ用）
        if (!this.level.isClientSide) {
            if (cause.getEntity() instanceof Player player) {
                player.displayClientMessage(
                    Component.literal("§7一般兵を倒した"),
                    true
                );
            }
        }
    }

    /**
     * エンティティの表示名
     */
    @Override
    public Component getName() {
        return Component.literal("§f一般兵");
    }

    /**
     * 環境音（足音など）
     */
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SKELETON_AMBIENT;
    }

    /**
     * ダメージを受けた時の音
     */
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PLAYER_HURT;
    }

    /**
     * 死亡時の音
     */
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PLAYER_DEATH;
    }

    /**
     * 足音の音量
     */
    @Override
    protected float getSoundVolume() {
        return 0.8f;
    }

    /**
     * 常に敵対的
     */
    @Override
    public boolean canAttack(net.minecraft.world.entity.LivingEntity target) {
        return target instanceof Player && super.canAttack(target);
    }

    /**
     * AIティアを取得
     */
    public int getAITier() {
        return AI_TIER;
    }

    /**
     * 日光で燃えない
     */
    @Override
    public boolean isSunBurnTick() {
        return false;
    }

    /**
     * エンティティの初期化
     */
    public static void init() {
        // スポーン設定などを行う場合はここに記述
    }
}

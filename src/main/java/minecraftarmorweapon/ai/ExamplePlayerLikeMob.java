package minecraftarmorweapon.ai;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level().Level;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.network.chat.Component;

import minecraftarmorweapon.init.MinecraftArmorWeaponModItems;

/**
 * プレイヤーのような動作をするMobの実装例
 *
 * このクラスは、PlayerLikeAIGoalの使い方を示すサンプルです。
 * 実際のMobエンティティに適用する際の参考にしてください。
 */
public class ExamplePlayerLikeMob extends Monster {

    private final int aiTier;
    private PlayerLikeAIGoal playerLikeAI;

    /**
     * コンストラクタ
     *
     * @param type エンティティタイプ
     * @param world ワールド
     * @param aiTier AIのティア（1～7）
     */
    public ExamplePlayerLikeMob(EntityType<? extends Monster> type, Level world, int aiTier) {
        super(type, world);
        this.aiTier = aiTier;

        // ティアに応じて武器を持たせる（オプション）
        this.setItemInHand(InteractionHand.MAIN_HAND, getWeaponForTier(aiTier));
    }

    /**
     * AI Goalの登録
     */
    @Override
    protected void registerGoals() {
        // プレイヤーのような動作をするAIを最優先で追加
        this.playerLikeAI = new PlayerLikeAIGoal(this, aiTier);
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, playerLikeAI);  // ★ これが重要！

        // 通常の近接攻撃AI（PlayerLikeAIGoalが通常攻撃も処理するため、優先度を下げる）
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, false));

        // 周囲をうろつくAI
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        // ターゲット選択AI
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /**
     * ティアに応じた武器を取得
     */
    private ItemStack getWeaponForTier(int tier) {
        switch (tier) {
            case 1:
                return new ItemStack(MinecraftArmorWeaponModItems.IRON_KATANA.get());
            case 2:
                return new ItemStack(MinecraftArmorWeaponModItems.GOLD_KATANA.get());
            case 3:
                return new ItemStack(MinecraftArmorWeaponModItems.WITHER_KATANA.get());
            case 4:
                return new ItemStack(MinecraftArmorWeaponModItems.REPLICA_SWORD_OF_LIGHT.get());
            case 5:
                return new ItemStack(MinecraftArmorWeaponModItems.MAGICAL_KATANA.get());
            case 6:
                return new ItemStack(MinecraftArmorWeaponModItems.MAGISCHES_FEEN_KATANA.get());
            case 7:
                return new ItemStack(MinecraftArmorWeaponModItems.SWORD_OF_NIGHT.get());
            default:
                return ItemStack.EMPTY;
        }
    }

    /**
     * 落下ダメージの処理
     * PlayerLikeAIGoalが回避時に落下ダメージ無効を設定するため、それをチェック
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        // 落下ダメージで、かつ無効時間中なら無効化
        if (source == DamageSource.FALL && playerLikeAI != null && playerLikeAI.isFallDamageImmune()) {
            return false;
        }

        return super.hurt(source, amount);
    }

    /**
     * AIティアを取得
     */
    public int getAITier() {
        return aiTier;
    }

    /**
     * エンティティの表示名を設定（ティアに応じて変更）
     */
    @Override
    public Component getName() {
        String tierName = getTierName(aiTier);
        return Component.literal(tierName + " ").append(super.getName());
    }

    /**
     * ティア名を取得
     */
    private String getTierName(int tier) {
        switch (tier) {
            case 1: return "一般兵";
            case 2: return "エリート兵";
            case 3: return "特異点";
            case 4: return "英雄級";
            case 5: return "神話級";
            case 6: return "天使級";
            case 7: return "神聖級";
            default: return "不明";
        }
    }
}

/**
 * 使用例：
 *
 * // ティア1の一般兵を召喚
 * ExamplePlayerLikeMob mob1 = new ExamplePlayerLikeMob(EntityType.ZOMBIE, level, 1);
 * level.addFreshEntity(mob1);
 *
 * // ティア7の神聖級を召喚
 * ExamplePlayerLikeMob mob7 = new ExamplePlayerLikeMob(EntityType.ZOMBIE, level, 7);
 * level.addFreshEntity(mob7);
 *
 * // 既存のエンティティにPlayerLikeAIGoalを追加
 * public class MyCustomMob extends Monster {
 *     @Override
 *     protected void registerGoals() {
 *         this.goalSelector.addGoal(1, new PlayerLikeAIGoal(this, 3)); // ティア3
 *         // ... その他のGoal
 *     }
 * }
 */

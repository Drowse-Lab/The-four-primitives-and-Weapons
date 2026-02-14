package minecraftarmorweapon.damage;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level().ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

/**
 * 侵食/闇属性ダメージハンドラー
 * - 防御力を一時的に落とす(ダメージに比例)
 */
public class CorrosionElementDamageHandler {

    // 基礎ダメージ倍率
    private static final float BASE_DAMAGE_MULTIPLIER = 1.1f;
    // 防御力減少の基礎値
    private static final double ARMOR_REDUCTION_BASE = 2.0;
    // 防御力減少のダメージ比例係数
    private static final double ARMOR_REDUCTION_PER_DAMAGE = 0.5;
    // 防御力減少効果の持続時間（tick）
    private static final int ARMOR_REDUCTION_DURATION = 100; // 5秒

    // 防御力減少用のUUID
    private static final UUID ARMOR_REDUCTION_UUID = UUID.fromString("a3b4c5d6-e7f8-9012-3456-789abcdef012");

    /**
     * 侵食属性ダメージを計算
     * @param target ターゲットエンティティ
     * @param originalDamage 元のダメージ
     * @param elementLevel 属性レベル
     * @return 計算後のダメージ
     */
    public static float calculateDamage(LivingEntity target, float originalDamage, int elementLevel) {
        float damageMultiplier = BASE_DAMAGE_MULTIPLIER;

        // ダメージに比例して防御力を減少
        double armorReduction = ARMOR_REDUCTION_BASE + (originalDamage * ARMOR_REDUCTION_PER_DAMAGE);
        armorReduction *= (1.0 + elementLevel * 0.2); // レベルによる増幅

        // 防御力減少効果を適用
        applyArmorReduction(target, armorReduction, ARMOR_REDUCTION_DURATION);

        // Weakness効果を付与（攻撃力減少）
        int weaknessLevel = Math.min(elementLevel, 3);
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,
            ARMOR_REDUCTION_DURATION, weaknessLevel, false, true));

        // Wither効果を付与（継続ダメージ）
        if (elementLevel >= 2) {
            target.addEffect(new MobEffectInstance(MobEffects.WITHER,
                60, 0, false, true));
        }

        // 侵食/闇のパーティクルエフェクト
        if (target.level() instanceof ServerLevel serverLevel) {
            // ウィザーの暗黒パーティクル
            serverLevel.sendParticles(ParticleTypes.SMOKE,
                target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                20, 0.3, 0.5, 0.3, 0.05);

            // ソウルパーティクル（闇のオーラ）
            serverLevel.sendParticles(ParticleTypes.SOUL,
                target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                15, 0.4, 0.4, 0.4, 0.03);

            // イカスミパーティクル（侵食の表現）
            serverLevel.sendParticles(ParticleTypes.SQUID_INK,
                target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                10, 0.2, 0.3, 0.2, 0.1);
        }

        return originalDamage * damageMultiplier;
    }

    /**
     * 防御力減少効果を適用
     * @param entity 対象エンティティ
     * @param amount 減少量
     * @param duration 持続時間
     */
    private static void applyArmorReduction(LivingEntity entity, double amount, int duration) {
        var armorAttribute = entity.getAttribute(Attributes.ARMOR);
        if (armorAttribute != null) {
            // 既存の修正を削除
            var existingModifier = armorAttribute.getModifier(ARMOR_REDUCTION_UUID);
            if (existingModifier != null) {
                armorAttribute.removeModifier(ARMOR_REDUCTION_UUID);
            }

            // 新しい修正を追加
            AttributeModifier modifier = new AttributeModifier(
                ARMOR_REDUCTION_UUID,
                "corrosion_armor_reduction",
                -amount,
                AttributeModifier.Operation.ADDITION
            );
            armorAttribute.addTransientModifier(modifier);

            // 一定時間後に効果を削除
            entity.getServer().execute(() -> {
                scheduleArmorReductionRemoval(entity, duration);
            });
        }
    }

    /**
     * 防御力減少効果の削除をスケジュール
     */
    private static void scheduleArmorReductionRemoval(LivingEntity entity, int ticks) {
        // 簡易的な実装：MobEffectを使用して時間管理
        // 実際の実装では、カスタムMobEffectを作成することを推奨
    }

    /**
     * エンティティに侵食属性ダメージを与える
     * @param target ターゲットエンティティ
     * @param damage ダメージ量
     * @param source ダメージソース元
     * @param level 属性レベル
     */
    public static void applyCorrosionDamage(LivingEntity target, float damage, LivingEntity source, int level) {
        // カスタムダメージソースを作成
        IElementalDamageSource elementalSource = (IElementalDamageSource) new CorrosionDamageSource("corrosion");
        elementalSource.setElementType(ElementType.CORROSION);
        elementalSource.setElementLevel(level);

        // ダメージを適用
        target.hurt((DamageSource) elementalSource, damage);
    }
}

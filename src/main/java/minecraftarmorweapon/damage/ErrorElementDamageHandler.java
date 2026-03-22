package minecraftarmorweapon.damage;

import minecraftarmorweapon.util.VersionHelper;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * エラー属性ダメージハンドラー
 * - 防御値（アーマー）と防御エフェクト（耐性）を貫通する
 * - レベル1ごとに10%防御貫通
 * - レベル10以上で100%貫通 + 追加攻撃力ボーナス
 */
public class ErrorElementDamageHandler {

    // レベルあたりの防御貫通率（10%）
    private static final float PENETRATION_PER_LEVEL = 0.1f;
    // レベル10超過時の追加ダメージ倍率（レベルごと）
    private static final float BONUS_DAMAGE_PER_LEVEL = 0.15f;

    /**
     * エラー属性ダメージを計算
     * 防御貫通分を追加ダメージとして上乗せし、レベル10以上で攻撃力ボーナス
     *
     * @param target        ターゲットエンティティ
     * @param originalDamage 元のダメージ
     * @param elementLevel  属性レベル
     * @return 計算後のダメージ
     */
    public static float calculateDamage(LivingEntity target, float originalDamage, int elementLevel) {
        // 貫通率: レベル * 10%、最大100%
        float penetrationPercent = Math.min(elementLevel * PENETRATION_PER_LEVEL, 1.0f);

        // ターゲットの防御値を取得
        float armorValue = (float) target.getArmorValue();
        float armorToughness = (float) target.getAttributeValue(Attributes.ARMOR_TOUGHNESS);

        // アーマーによる軽減量を概算し、貫通分だけダメージに加算
        float armorReduction = estimateArmorReduction(originalDamage, armorValue, armorToughness);
        float penetrationBonus = armorReduction * penetrationPercent;

        // 耐性エフェクトの貫通: Resistanceがある場合、その軽減分も貫通率に応じて戻す
        MobEffectInstance resistance = target.getEffect(MobEffects.DAMAGE_RESISTANCE);
        float resistanceBonus = 0;
        if (resistance != null) {
            // Resistance: レベル1 = 20%軽減、レベル2 = 40%軽減...
            int resistLevel = resistance.getAmplifier() + 1;
            float resistReduction = Math.min(resistLevel * 0.2f, 1.0f);
            resistanceBonus = originalDamage * resistReduction * penetrationPercent;
        }

        float totalDamage = originalDamage + penetrationBonus + resistanceBonus;

        // レベル10以上: 追加攻撃力ボーナス
        if (elementLevel >= 10) {
            int bonusLevels = elementLevel - 9;
            totalDamage += originalDamage * (bonusLevels * BONUS_DAMAGE_PER_LEVEL);
        }

        // パーティクルエフェクト
        if (VersionHelper.getLevel(target) instanceof ServerLevel serverLevel) {
            double x = target.getX();
            double y = target.getY() + target.getBbHeight() / 2;
            double z = target.getZ();

            // グリッチ風クリティカル
            serverLevel.sendParticles(ParticleTypes.CRIT,
                x, y, z, 15, 0.4, 0.5, 0.4, 0.2);

            // ポータル粒子（エラー感）
            serverLevel.sendParticles(ParticleTypes.PORTAL,
                x, y, z, 10, 0.3, 0.4, 0.3, 0.1);

            // レベル10以上で追加の強いエフェクト
            if (elementLevel >= 10) {
                serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                    x, y, z, 5, 0.3, 0.3, 0.3, 0.1);
            }
        }

        return totalDamage;
    }

    /**
     * バニラのアーマー軽減量を概算
     * 実際にアーマーで吸収されるダメージ量を返す
     */
    private static float estimateArmorReduction(float damage, float armor, float toughness) {
        // バニラの計算式に基づく概算
        float defensePoints = armor - damage / (2.0f + toughness / 4.0f);
        defensePoints = Math.max(armor * 0.2f, Math.min(defensePoints, 20.0f));
        float reductionPercent = defensePoints / 25.0f;
        return damage * reductionPercent;
    }
}

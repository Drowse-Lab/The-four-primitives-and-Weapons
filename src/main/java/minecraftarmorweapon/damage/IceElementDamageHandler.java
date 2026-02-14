package minecraftarmorweapon.damage;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level().ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * 氷属性ダメージハンドラー
 * - 氷で固まっている時(Slowness効果)に攻撃をすると多くダメージが入る
 * - 時間経過でダメージが大きくなる(レベルによってダメージが変わる)
 */
public class IceElementDamageHandler {

    // 氷属性の基礎ダメージ倍率
    private static final float BASE_DAMAGE_MULTIPLIER = 1.5f;
    // レベルごとの追加ダメージ倍率
    private static final float LEVEL_DAMAGE_MULTIPLIER = 0.25f;
    // 時間経過ボーナスの最大倍率
    private static final float MAX_TIME_BONUS = 0.5f;

    /**
     * 氷属性ダメージを計算
     * @param target ターゲットエンティティ
     * @param originalDamage 元のダメージ
     * @param elementLevel 属性レベル
     * @return 計算後のダメージ
     */
    public static float calculateDamage(LivingEntity target, float originalDamage, int elementLevel) {
        float damageMultiplier = 1.0f;

        // ターゲットが凍結状態(Slowness効果)を持っているか確認
        if (target.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
            MobEffectInstance slownessEffect = target.getEffect(MobEffects.MOVEMENT_SLOWDOWN);

            if (slownessEffect != null) {
                // 効果レベルを取得
                int slownessLevel = slownessEffect.getAmplifier() + 1; // 0始まりなので+1

                // ダメージ倍率を計算
                // 基礎倍率 + (レベル × レベル倍率)
                damageMultiplier = BASE_DAMAGE_MULTIPLIER + (slownessLevel * LEVEL_DAMAGE_MULTIPLIER);

                // 時間経過でダメージが増加
                int duration = slownessEffect.getDuration();
                if (duration > 0) {
                    // 残り時間が長いほどダメージが増加
                    float timeBonus = Math.min(duration / 60.0f, 1.0f) * MAX_TIME_BONUS;
                    damageMultiplier += timeBonus;
                }

                // 凍結効果を延長 (1秒追加)
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                    duration + 20, slownessLevel - 1, false, false));
            }
        } else {
            // Slowness効果がない場合は付与 (5秒間、レベルは属性レベルに応じて)
            int slownessLevel = Math.min(elementLevel, 3);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                100, slownessLevel, false, true));
        }

        // 凍結状態の視覚効果: Frozen状態を設定（パウダースノーで凍った見た目）
        target.setTicksFrozen(Math.min(target.getTicksFrozen() + 140, target.getTicksRequiredToFreeze() + 100));

        // 氷のパーティクルエフェクト
        if (target.level() instanceof ServerLevel serverLevel) {
            // 氷の結晶パーティクル
            serverLevel.sendParticles(ParticleTypes.SNOWFLAKE,
                target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                20, 0.3, 0.5, 0.3, 0.05);

            // 冷気のパーティクル
            serverLevel.sendParticles(ParticleTypes.ITEM_SNOWBALL,
                target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                15, 0.4, 0.4, 0.4, 0.1);
        }

        return originalDamage * damageMultiplier;
    }

    /**
     * エンティティに氷属性ダメージを与える
     * @param target ターゲットエンティティ
     * @param damage ダメージ量
     * @param source ダメージソース元
     * @param level 属性レベル
     */
    public static void applyIceDamage(LivingEntity target, float damage, LivingEntity source, int level) {
        // カスタムダメージソースを作成
        IElementalDamageSource elementalSource = (IElementalDamageSource) new IceDamageSource("ice");
        elementalSource.setElementType(ElementType.ICE);
        elementalSource.setElementLevel(level);

        // ダメージを適用
        target.hurt((net.minecraft.world.damagesource.DamageSource) elementalSource, damage);
    }
}

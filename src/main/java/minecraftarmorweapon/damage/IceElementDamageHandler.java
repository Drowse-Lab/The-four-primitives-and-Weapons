package minecraftarmorweapon.damage;

import minecraftarmorweapon.util.VersionHelper;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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

    // 同一tickでの二重適用を防ぐ（MixinとEventの両方から呼ばれるため）
    private static final Map<UUID, Long> lastAppliedTick = new ConcurrentHashMap<>();

    /**
     * 氷属性ダメージを計算
     * @param target ターゲットエンティティ
     * @param originalDamage 元のダメージ
     * @param elementLevel 属性レベル
     * @return 計算後のダメージ
     */
    public static float calculateDamage(LivingEntity target, float originalDamage, int elementLevel) {
        float damageMultiplier = 1.0f;

        // ダメージ倍率は常に計算する（既存のSlow効果がある場合のボーナス）
        if (target.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
            MobEffectInstance slownessEffect = target.getEffect(MobEffects.MOVEMENT_SLOWDOWN);

            if (slownessEffect != null) {
                int slownessLevel = slownessEffect.getAmplifier() + 1;
                damageMultiplier = BASE_DAMAGE_MULTIPLIER + (slownessLevel * LEVEL_DAMAGE_MULTIPLIER);

                int duration = slownessEffect.getDuration();
                if (duration > 0) {
                    float timeBonus = Math.min(duration / 60.0f, 1.0f) * MAX_TIME_BONUS;
                    damageMultiplier += timeBonus;
                }
            }
        }

        // 副作用（Slowness付与・frozen ticks）は同一tickで1回だけ適用
        long currentTick = target.level().getGameTime();
        Long lastTick = lastAppliedTick.get(target.getUUID());
        if (lastTick != null && lastTick == currentTick) {
            // 同一tickの2回目以降 → ダメージ倍率だけ返す（効果は重複させない）
            return originalDamage * damageMultiplier;
        }
        lastAppliedTick.put(target.getUUID(), currentTick);

        // Slowness効果を適用
        if (target.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
            MobEffectInstance slownessEffect = target.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
            if (slownessEffect != null) {
                int duration = slownessEffect.getDuration();
                // 凍結効果を延長（最大60tickにキャップ、frozen tickの減衰と同期）
                int newDuration = Math.min(duration + 20, 60);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                    newDuration, slownessEffect.getAmplifier(), false, false));
            }
        } else {
            // 初回: Slowness付与（3秒間、amplifier最大2）
            int slownessLevel = Math.min(elementLevel, 2);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                60, slownessLevel, false, true));
        }

        // 凍結状態の視覚効果: Frozen状態を設定（100%を超えないようにキャップ）
        int maxFrozen = target.getTicksRequiredToFreeze();
        target.setTicksFrozen(Math.min(target.getTicksFrozen() + 40, maxFrozen));

        // 氷のパーティクルエフェクト
        if (VersionHelper.getLevel(target) instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SNOWFLAKE,
                target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                20, 0.3, 0.5, 0.3, 0.05);

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
        // カスタムダメージソースを作成（1.20.1: factory method使用）
        net.minecraft.world.damagesource.DamageSource ds = target.damageSources().magic();
        IElementalDamageSource elementalSource = (IElementalDamageSource) ds;
        elementalSource.setElementType(ElementType.ICE);
        elementalSource.setElementLevel(level);

        // ダメージを適用
        target.hurt(ds, damage);
    }
}

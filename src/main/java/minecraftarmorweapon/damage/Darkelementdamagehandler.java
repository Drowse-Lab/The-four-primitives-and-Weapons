package minecraftarmorweapon.damage;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class DarkElementDamageHandler {

    // 基礎倍率
    private static final float BASE_MULTIPLIER         = 1.1f;
    // 暗所ボーナス（光レベル7以下）
    private static final float DARKNESS_MULTIPLIER     = 1.4f;
    private static final int   DARKNESS_LIGHT_THRESHOLD = 7;
    // Blindness持続時間 (tick)
    private static final int   BASE_BLINDNESS_DURATION  = 60;   // 3秒
    private static final int   BLINDNESS_DURATION_PER_LV = 40;  // +2秒/Lv
    // Wither付与の最低レベル
    private static final int   WITHER_MIN_LEVEL         = 3;

    /**
     * 闇属性ダメージを計算して返す。
     * 対象に Blindness を付与し、暗所では追加ダメージ。
     * 高レベルでは Wither 効果も付与する。
     *
     * @param attacker 攻撃者
     * @param target   攻撃対象
     * @param weapon   使用武器
     * @param baseDmg  基礎ダメージ
     * @return 属性込みの最終ダメージ
     */
    public static float handleDarkDamage(LivingEntity attacker,
                                         LivingEntity target,
                                         ItemStack weapon,
                                         float baseDmg) {

        int level      = ElementalDamageUtils.getElementLevel(weapon);
        float multiplier = BASE_MULTIPLIER;

        // 暗所ボーナス（攻撃者の足元の光レベルが低い場合）
        int lightLevel = attacker.level().getBrightness(
                net.minecraft.world.level.LightLayer.BLOCK,
                attacker.blockPosition()
        );
        if (lightLevel <= DARKNESS_LIGHT_THRESHOLD) {
            multiplier = DARKNESS_MULTIPLIER;
        }

        // Blindness付与
        int blindDuration = BASE_BLINDNESS_DURATION + BLINDNESS_DURATION_PER_LV * (level - 1);
        target.addEffect(new MobEffectInstance(
                MobEffects.BLINDNESS,
                blindDuration,
                0,
                false,
                true
        ));

        // 高レベルでWither付与
        if (level >= WITHER_MIN_LEVEL) {
            target.addEffect(new MobEffectInstance(
                    MobEffects.WITHER,
                    60,                  // 3秒
                    level - WITHER_MIN_LEVEL,
                    false,
                    true
            ));
        }

        // 相手の既存デバフを強化する
        amplifyDebuffs(target, level);

        return baseDmg * multiplier;
    }

    /**
     * レベル指定で闇属性ダメージ計算（魔導書経由用）
     */
    public static float calculateDamage(LivingEntity attacker, LivingEntity target, float baseDmg, int level) {
        float multiplier = BASE_MULTIPLIER;

        int lightLevel = attacker.level().getBrightness(
                net.minecraft.world.level.LightLayer.BLOCK, attacker.blockPosition());
        if (lightLevel <= DARKNESS_LIGHT_THRESHOLD) {
            multiplier = DARKNESS_MULTIPLIER;
        }

        int blindDuration = BASE_BLINDNESS_DURATION + BLINDNESS_DURATION_PER_LV * Math.max(level - 1, 0);
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, blindDuration, 0, false, true));

        if (level >= WITHER_MIN_LEVEL) {
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, level - WITHER_MIN_LEVEL, false, true));
        }

        // 相手の既存デバフを強化する
        amplifyDebuffs(target, level);

        return baseDmg * multiplier;
    }

    /**
     * ターゲットの既存デバフ（有害なエフェクト）を強化する。
     * 持続時間を延長し、レベル3以上ではアンプリファイアも+1する。
     *
     * @param target 攻撃対象
     * @param level  闇属性レベル
     */
    private static void amplifyDebuffs(LivingEntity target, int level) {
        // 現在の有害エフェクトを収集（イテレーション中の変更を避けるためコピー）
        List<MobEffectInstance> debuffs = new ArrayList<>();
        for (MobEffectInstance effect : target.getActiveEffects()) {
            if (effect.getEffect().getCategory() == MobEffectCategory.HARMFUL) {
                debuffs.add(effect);
            }
        }

        // 各デバフを強化して再付与
        for (MobEffectInstance debuff : debuffs) {
            // 持続時間延長: +40tick(2秒) × レベル
            int extendedDuration = debuff.getDuration() + 40 * level;
            // レベル3以上ではアンプリファイアも+1
            int newAmplifier = debuff.getAmplifier() + (level >= 3 ? 1 : 0);

            target.addEffect(new MobEffectInstance(
                    debuff.getEffect(),
                    extendedDuration,
                    newAmplifier,
                    debuff.isAmbient(),
                    debuff.isVisible()
            ));
        }
    }
}

package minecraftarmorweapon.damage;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

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

        return baseDmg * multiplier;
    }
}

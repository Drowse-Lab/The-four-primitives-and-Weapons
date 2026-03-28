package minecraftarmorweapon.damage;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class WaterElementDamageHandler {

    // 基礎倍率
    private static final float BASE_MULTIPLIER       = 1.0f;
    // Slowness持続時間 (tick)
    private static final int BASE_SLOWNESS_DURATION  = 80;   // 4秒
    private static final int SLOWNESS_DURATION_PER_LV = 40;  // +2秒/Lv
    // Slownessの増幅レベル (0 = Slowness I)
    private static final int SLOWNESS_AMPLIFIER      = 1;    // Slowness II

    /**
     * 水属性ダメージを計算して返す。
     * 対象に Slowness を付与して移動を妨害する。
     *
     * @param attacker 攻撃者
     * @param target   攻撃対象
     * @param weapon   使用武器
     * @param baseDmg  基礎ダメージ
     * @return 属性込みの最終ダメージ
     */
    public static float handleWaterDamage(LivingEntity attacker,
                                          LivingEntity target,
                                          ItemStack weapon,
                                          float baseDmg) {

        int level    = ElementalDamageUtils.getElementLevel(weapon);
        int duration = BASE_SLOWNESS_DURATION + SLOWNESS_DURATION_PER_LV * (level - 1);

        // Slowness付与
        target.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN,
                duration,
                SLOWNESS_AMPLIFIER,
                false,
                true
        ));

        return baseDmg * BASE_MULTIPLIER;
    }

    /**
     * レベル指定で水属性ダメージ計算（魔導書経由用）
     */
    public static float calculateDamage(LivingEntity target, float baseDmg, int level) {
        int duration = BASE_SLOWNESS_DURATION + SLOWNESS_DURATION_PER_LV * Math.max(level - 1, 0);
        target.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN, duration, SLOWNESS_AMPLIFIER, false, true));
        return baseDmg * BASE_MULTIPLIER;
    }
}

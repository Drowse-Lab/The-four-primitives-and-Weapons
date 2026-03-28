package minecraftarmorweapon.damage;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class WindElementDamageHandler {

    // 基礎倍率
    private static final float BASE_MULTIPLIER         = 1.0f;
    // 攻撃力上昇ボーナス (倍率加算)
    private static final float ATTACK_BOOST_BASE       = 0.2f;  // +20%
    private static final float ATTACK_BOOST_PER_LEVEL  = 0.1f;  // +10%/Lv
    // 攻撃力上昇バフ持続時間 (tick)
    private static final int BUFF_DURATION             = 100;   // 5秒
    private static final int BUFF_DURATION_PER_LV      = 40;    // +2秒/Lv

    /**
     * 風属性ダメージを計算して返す。
     * 攻撃者に Strength 効果を付与し、攻撃力を上昇させる。
     *
     * @param attacker 攻撃者
     * @param target   攻撃対象
     * @param weapon   使用武器
     * @param baseDmg  基礎ダメージ
     * @return 属性込みの最終ダメージ
     */
    public static float handleWindDamage(LivingEntity attacker,
                                         LivingEntity target,
                                         ItemStack weapon,
                                         float baseDmg) {

        int level    = ElementalDamageUtils.getElementLevel(weapon);
        int duration = BUFF_DURATION + BUFF_DURATION_PER_LV * (level - 1);

        // 攻撃者に Strength バフを付与
        // amplifier 0 = Strength I (+3 attack), 1 = Strength II (+6 attack)
        int amplifier = Math.min(level - 1, 1);
        attacker.addEffect(new MobEffectInstance(
                MobEffects.DAMAGE_BOOST,
                duration,
                amplifier,
                false,
                true
        ));

        // ダメージ自体も攻撃力上昇分を加算
        float attackBoost = ATTACK_BOOST_BASE + ATTACK_BOOST_PER_LEVEL * (level - 1);
        return baseDmg * (BASE_MULTIPLIER + attackBoost);
    }

    /**
     * レベル指定で風属性ダメージ計算（魔導書経由用）
     */
    public static float calculateDamage(LivingEntity attacker, LivingEntity target, float baseDmg, int level) {
        int duration = BUFF_DURATION + BUFF_DURATION_PER_LV * Math.max(level - 1, 0);
        int amplifier = Math.min(level - 1, 1);
        attacker.addEffect(new MobEffectInstance(
                MobEffects.DAMAGE_BOOST, duration, amplifier, false, true));
        float attackBoost = ATTACK_BOOST_BASE + ATTACK_BOOST_PER_LEVEL * Math.max(level - 1, 0);
        return baseDmg * (BASE_MULTIPLIER + attackBoost);
    }
}

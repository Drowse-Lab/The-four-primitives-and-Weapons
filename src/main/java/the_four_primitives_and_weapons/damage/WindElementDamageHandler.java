package the_four_primitives_and_weapons.damage;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * 風属性ダメージハンドラー
 *
 * 仕様変更:
 *   旧: MobEffects.DAMAGE_BOOST (Strength) を攻撃者に付与 + 倍率加算。
 *       → 牛乳で消える & 間接的なので、 ユーザー要望で廃止。
 *   新: 武器または魔導書のレベルに応じて、 命中ダメージに直接加算する。
 *       Lv1 = +1.0 / Lv5 = +5.0 / Lv10 = +10.0
 */
public class WindElementDamageHandler {

    /** 基礎倍率 (素のダメージはそのまま、 加算のみ) */
    private static final float BASE_MULTIPLIER       = 1.0f;
    /** レベル毎の直接追加ダメージ */
    private static final float BONUS_DAMAGE_PER_LV   = 1.0f;
    /** Lv0 ガード時 (= NONE element の旧 calculateDamage) でも最低限の加算 */
    private static final float BONUS_DAMAGE_MIN      = 0.5f;

    /**
     * 風属性ダメージを計算して返す。 武器のレベルに応じた直接追加ダメージを加算。
     */
    public static float handleWindDamage(LivingEntity attacker,
                                         LivingEntity target,
                                         ItemStack weapon,
                                         float baseDmg) {
        int level = ElementalDamageUtils.getElementLevel(weapon);
        return baseDmg * BASE_MULTIPLIER + bonusFor(level);
    }

    /** レベル指定で風属性ダメージ計算 (魔導書経由用) */
    public static float calculateDamage(LivingEntity attacker, LivingEntity target,
                                        float baseDmg, int level) {
        return baseDmg * BASE_MULTIPLIER + bonusFor(level);
    }

    /** レベル → 直接追加ダメージ */
    private static float bonusFor(int level) {
        if (level <= 0) return BONUS_DAMAGE_MIN;
        return BONUS_DAMAGE_PER_LV * level;
    }
}

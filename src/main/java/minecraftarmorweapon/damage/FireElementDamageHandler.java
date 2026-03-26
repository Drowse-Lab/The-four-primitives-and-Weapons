package minecraftarmorweapon.damage;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class FireElementDamageHandler {

    // 基礎倍率
    private static final float BASE_MULTIPLIER = 1.0f;
    // 炎上時間 (tick) レベルごとに延長
    private static final int BASE_FIRE_TICKS   = 60;   // 3秒
    private static final int FIRE_TICKS_PER_LV = 40;   // +2秒/Lv

    /**
     * 火属性ダメージを計算して返す。
     * バニラの炎と同じく対象を炎上させる。
     *
     * @param attacker 攻撃者
     * @param target   攻撃対象
     * @param weapon   使用武器
     * @param baseDmg  基礎ダメージ
     * @return 属性込みの最終ダメージ
     */
    public static float handleFireDamage(LivingEntity attacker,
                                         LivingEntity target,
                                         ItemStack weapon,
                                         float baseDmg) {

        int level = ElementalDamageUtils.getElementLevel(weapon);

        // 炎上付与（バニラと同挙動）
        int fireTicks = BASE_FIRE_TICKS + FIRE_TICKS_PER_LV * (level - 1);
        target.setSecondsOnFire(fireTicks / 20);

        return baseDmg * BASE_MULTIPLIER;
    }
}

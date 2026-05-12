package the_four_primitives_and_weapons.api;

import net.minecraft.world.entity.player.Player;

/**
 * 外部modがスキルを実装するためのインターフェース。
 *
 * <pre>
 * // 使用例:
 * SkillRegistry.register("my_mod:fire_slash", "炎斬り", "炎を纏った斬撃",
 *     MotionCategory.SPECIAL, EnumSet.allOf(AttackSlot.class), "MyWeaponItem",
 *     (player, chargePercent) -> {
 *         // chargePercent: 0.0 = 通常攻撃、0.0~1.0 = チャージ攻撃
 *         float damage = ChargeHelper.scaleDamage(10.0f, chargePercent, 2.0f);
 *         // ... 攻撃処理
 *     });
 * </pre>
 */
@FunctionalInterface
public interface ISkillAction {

    /**
     * スキルを実行する。
     *
     * @param player       実行するプレイヤー
     * @param chargePercent チャージ率 (0.0 = 通常攻撃、0.0~1.0 = チャージ攻撃)
     */
    void execute(Player player, float chargePercent);
}

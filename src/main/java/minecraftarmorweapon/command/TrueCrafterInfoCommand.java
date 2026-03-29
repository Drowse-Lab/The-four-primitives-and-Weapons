package minecraftarmorweapon.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class TrueCrafterInfoCommand {

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("difficultyinfo")
            .requires(source -> source.hasPermission(0))
            .executes(context -> {
                CustomDifficultyCommand.CustomDifficulty diff = CustomDifficultyCommand.getCurrentDifficulty();
                String c = diff.getColorCode();

                // ヘッダー
                context.getSource().sendSuccess(() ->
                    Component.literal(c + "§l=== 難易度情報: " + diff.getName().toUpperCase() + " ==="), false);

                // 基本情報
                context.getSource().sendSuccess(() ->
                    Component.literal(String.format(
                        "§7Mobダメージ倍率: §e%.1fx §7| MobHP倍率: §e%.2fx §7| AI Lv: §e%d",
                        diff.getDamageMultiplier(), diff.getHealthMultiplier(), diff.getAiLevel()
                    )), false);

                // 装備品質
                String eqLabel;
                switch (diff.getEquipmentTier()) {
                    case 0: eqLabel = "§7なし"; break;
                    case 1: eqLabel = "§a革/木"; break;
                    case 2: eqLabel = "§e鉄"; break;
                    case 3: eqLabel = "§bダイヤ"; break;
                    case 4: eqLabel = "§dネザライト/MOD"; break;
                    default: eqLabel = "§7不明"; break;
                }
                final String equipLabel = eqLabel;
                context.getSource().sendSuccess(() ->
                    Component.literal("§7装備品質: " + equipLabel + " §7| 盾確率: §e" +
                        String.format("%.0f%%", diff.getShieldChance() * 100)), false);

                // 確率パラメータ
                if (diff.getAiLevel() > 0) {
                    context.getSource().sendSuccess(() ->
                        Component.literal(String.format(
                            "§7エリート出現: §e%.0f%% §7| 援軍: §e%.0f%% §7| バフ: §e%.0f%%",
                            diff.getEliteSpawnChance() * 100,
                            diff.getReinforceChance() * 100,
                            diff.getBuffEffectChance() * 100
                        )), false);
                }

                // 有効機能リスト
                context.getSource().sendSuccess(() ->
                    Component.literal("§6--- 有効な機能 ---"), false);

                if (diff.getAiLevel() >= 1) {
                    context.getSource().sendSuccess(() ->
                        Component.literal("  §a✓ 武器切り替え" + getSpeedLabel(diff.getAiLevel())), false);
                    context.getSource().sendSuccess(() ->
                        Component.literal("  §a✓ 矢回避行動" + getSpeedLabel(diff.getAiLevel())), false);
                }
                if (diff.getShieldChance() > 0) {
                    context.getSource().sendSuccess(() ->
                        Component.literal("  §a✓ 盾防御 (" + String.format("%.0f%%", diff.getShieldChance() * 100) + "確率)"), false);
                }
                if (diff.getEliteSpawnChance() > 0) {
                    context.getSource().sendSuccess(() ->
                        Component.literal("  §b✓ エリートMob出現 (" + String.format("%.0f%%", diff.getEliteSpawnChance() * 100) + ")"), false);
                }
                if (diff.getReinforceChance() > 0) {
                    context.getSource().sendSuccess(() ->
                        Component.literal("  §b✓ 援軍スポーン (" + String.format("%.0f%%", diff.getReinforceChance() * 100) + ")"), false);
                }
                if (diff.getBuffEffectChance() > 0) {
                    context.getSource().sendSuccess(() ->
                        Component.literal("  §d✓ バフ効果付与 (" + String.format("%.0f%%", diff.getBuffEffectChance() * 100) + ")"), false);
                }
                if (diff.isBlockPlaceEnabled()) {
                    context.getSource().sendSuccess(() ->
                        Component.literal("  §e✓ ブロック設置 (ピラーアップ・橋建設)"), false);
                }
                if (diff.isBlockBreakEnabled()) {
                    context.getSource().sendSuccess(() ->
                        Component.literal("  §e✓ ブロック破壊 (壁破壊進軍)"), false);
                }
                if (diff.isWallSenseEnabled()) {
                    context.getSource().sendSuccess(() ->
                        Component.literal("  §c✓ 壁越しプレイヤー感知"), false);
                }
                if (diff.isFallDmgImmune()) {
                    context.getSource().sendSuccess(() ->
                        Component.literal("  §c✓ 追跡中の落下ダメージ無効"), false);
                }
                if (!diff.isBedSleepEnabled()) {
                    context.getSource().sendSuccess(() ->
                        Component.literal("  §4✗ ベッド睡眠無効"), false);
                }
                if (diff.getHealthMultiplier() > 1.0f) {
                    context.getSource().sendSuccess(() ->
                        Component.literal("  §c✓ Mob HP " + String.format("%.0f%%", diff.getHealthMultiplier() * 100) + " に増加"), false);
                }

                // 特性システム
                if (diff.getTraitChance() > 0) {
                    context.getSource().sendSuccess(() ->
                        Component.literal("§6--- Mob特性システム ---"), false);
                    context.getSource().sendSuccess(() ->
                        Component.literal("  §e特性付与確率: " + String.format("%.0f%%", diff.getTraitChance() * 100)), false);
                    context.getSource().sendSuccess(() ->
                        Component.literal("  §9[鉄壁] §7防御+20, KB耐性, HP×1.5"), false);
                    context.getSource().sendSuccess(() ->
                        Component.literal("  §c[狂戦士] §7攻撃×2.5, HP30%以下で怒モード"), false);
                    context.getSource().sendSuccess(() ->
                        Component.literal("  §b[迅速] §7移動速度×2, 追跡範囲+16"), false);
                    context.getSource().sendSuccess(() ->
                        Component.literal("  §a[再生者] §7永続リジェネII, HP×1.3"), false);
                    context.getSource().sendSuccess(() ->
                        Component.literal("  §8[蜘蛛糸] §7足元に蜘蛛の巣設置, Lv3+で範囲拡大"), false);
                    context.getSource().sendSuccess(() ->
                        Component.literal("  §3[反射] §7近接ダメージの20-60%を攻撃者に反射"), false);
                    context.getSource().sendSuccess(() ->
                        Component.literal("  §d[矢盾] §7飛び道具ダメージ50-100%カット, Lv5で完全無効"), false);
                    context.getSource().sendSuccess(() ->
                        Component.literal("  §2[猛毒] §7毒+衰弱, Lv3+で衰弱追加, Lv5+で空腹"), false);
                    context.getSource().sendSuccess(() ->
                        Component.literal("  §8[盲目] §7盲目+暗闇コンボ, 透明化, Lv4+で鈍化追加"), false);
                    context.getSource().sendSuccess(() ->
                        Component.literal("  §e[吐き気] §7吐き気+空腹, Lv3+で採掘疲労, Lv5+で鈍化"), false);
                    context.getSource().sendSuccess(() ->
                        Component.literal("  §7[弱体] §7弱体化+鈍化, Lv3+で採掘疲労, Lv4+で更に弱体"), false);
                    context.getSource().sendSuccess(() ->
                        Component.literal("  §6[爆裂] §7攻撃時小爆発, 死亡時大爆発"), false);
                    context.getSource().sendSuccess(() ->
                        Component.literal("  §4§l[不死] §7§l最上位 §7— トーテム所持、使用後に再補充"), false);
                }

                // True Crafterモード判定
                if (CustomDifficultyCommand.isTrueCrafterEnabled()) {
                    context.getSource().sendSuccess(() ->
                        Component.literal(c + "§l[True Crafter Mode: AI Lv" + diff.getAiLevel() + " 有効]"), false);
                } else {
                    context.getSource().sendSuccess(() ->
                        Component.literal("§7[True Crafter Mode: 無効] (hard以上で有効化)"), false);
                }

                return 1;
            })
        );
    }

    private static String getSpeedLabel(int aiLevel) {
        switch (aiLevel) {
            case 1: return " (遅い)";
            case 2: return " (普通)";
            case 3: return " (速い)";
            case 4: return " (とても速い)";
            case 5: return " (超高速)";
            default: return "";
        }
    }
}

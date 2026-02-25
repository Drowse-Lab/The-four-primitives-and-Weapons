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
        
        // /difficultyinfo コマンドを登録
        dispatcher.register(Commands.literal("difficultyinfo")
            .requires(source -> source.hasPermission(0)) // 誰でも実行可能
            .executes(context -> {
                CustomDifficultyCommand.CustomDifficulty difficulty = CustomDifficultyCommand.getCurrentDifficulty();
                
                context.getSource().sendSuccess(() ->
                    Component.literal("§6=== 現在の難易度情報 ==="),
                    false
                );
                
                context.getSource().sendSuccess(() ->
                    Component.literal("§7難易度: §f" + difficulty.getName()),
                    false
                );
                
                context.getSource().sendSuccess(() ->
                    Component.literal("§7ダメージ倍率: §e" + String.format("%.1fx", difficulty.getDamageMultiplier())),
                    false
                );
                
                if (CustomDifficultyCommand.isTrueCrafterEnabled()) {
                    context.getSource().sendSuccess(() ->
                        Component.literal("§4§l[True Crafter Mode: 強制有効]"),
                        false
                    );
                    context.getSource().sendSuccess(() ->
                        Component.literal("§7※ nightmare以上で自動的に有効化されます"),
                        false
                    );
                    
                    // 難易度ごとの能力表示
                    switch (difficulty.getName()) {
                        case "nightmare":
                            context.getSource().sendSuccess(() -> Component.literal("§c§l=== True Crafter Level 1 ==="), false);
                            context.getSource().sendSuccess(() -> Component.literal("§7敵の能力:"), false);
                            context.getSource().sendSuccess(() -> Component.literal("  §a✓ 武器切り替え (遅い)"), false);
                            context.getSource().sendSuccess(() -> Component.literal("  §a✓ アクロバティック回避 (基本)"), false);
                            context.getSource().sendSuccess(() -> Component.literal("  §a✓ 盾防御 (30%確率)"), false);
                            context.getSource().sendSuccess(() -> Component.literal("  §b✓ 日光耐性ヘルメット"), false);
                            break;
                            
                        case "realistic":
                            context.getSource().sendSuccess(() -> Component.literal("§c§l=== True Crafter Level 2 ==="), false);
                            context.getSource().sendSuccess(() -> Component.literal("§7敵の能力:"), false);
                            context.getSource().sendSuccess(() -> Component.literal("  §a✓ 武器切り替え (普通)"), false);
                            context.getSource().sendSuccess(() -> Component.literal("  §a✓ アクロバティック回避 (改善)"), false);
                            context.getSource().sendSuccess(() -> Component.literal("  §a✓ 盾防御 (45%確率)"), false);
                            context.getSource().sendSuccess(() -> Component.literal("  §e✓ ブロック設置 (遅い)"), false);
                            context.getSource().sendSuccess(() -> Component.literal("  §e✓ 橋建設 (遅い)"), false);
                            context.getSource().sendSuccess(() -> Component.literal("  §b✓ 日光耐性ヘルメット"), false);
                            context.getSource().sendSuccess(() -> Component.literal("  §d✓ ゾンビ重装備+飛びつき"), false);
                            context.getSource().sendSuccess(() -> Component.literal("  §9✓ 水中でガーディアン騎乗"), false);
                            break;
                            
                        case "lunatic":
                            context.getSource().sendSuccess(() -> Component.literal("§5§l=== True Crafter Level 3 ==="), false);
                            context.getSource().sendSuccess(() -> Component.literal("§7敵の能力:"), false);
                            context.getSource().sendSuccess(() -> Component.literal("  §a✓ 武器切り替え (速い)"), false);
                            context.getSource().sendSuccess(() -> Component.literal("  §a✓ アクロバティック回避 (高性能)"), false);
                            context.getSource().sendSuccess(() -> Component.literal("  §a✓ 盾防御 (60%確率)"), false);
                            context.getSource().sendSuccess(() -> Component.literal("  §e✓ ブロック設置 (普通)"), false);
                            context.getSource().sendSuccess(() -> Component.literal("  §e✓ 橋建設 (普通)"), false);
                            context.getSource().sendSuccess(() -> Component.literal("  §b✓ 鉄ヘルメット (日光完全耐性)"), false);
                            context.getSource().sendSuccess(() -> Component.literal("  §d✓ ゾンビ重装備+飛びつき"), false);
                            context.getSource().sendSuccess(() -> Component.literal("  §9✓ 水中でガーディアン騎乗"), false);
                            break;
                            
                        case "lunatic+":
                            context.getSource().sendSuccess(() -> Component.literal("§5§l=== True Crafter Level 4 ==="), false);
                            context.getSource().sendSuccess(() -> Component.literal("§7敵の能力:"), false);
                            context.getSource().sendSuccess(() -> Component.literal("  §a✓ 武器切り替え (とても速い)"), false);
                            context.getSource().sendSuccess(() -> Component.literal("  §a✓ アクロバティック回避 (超高性能)"), false);
                            context.getSource().sendSuccess(() -> Component.literal("  §a✓ 盾防御 (75%確率)"), false);
                            context.getSource().sendSuccess(() -> Component.literal("  §e✓ ブロック設置 (速い)"), false);
                            context.getSource().sendSuccess(() -> Component.literal("  §e✓ 橋建設 (速い)"), false);
                            context.getSource().sendSuccess(() -> Component.literal("  §b✓ 鉄ヘルメット (日光完全耐性)"), false);
                            context.getSource().sendSuccess(() -> Component.literal("  §d✓ ゾンビ重装備+飛びつき"), false);
                            context.getSource().sendSuccess(() -> Component.literal("  §9✓ 水中でガーディアン召喚+騎乗"), false);
                            context.getSource().sendSuccess(() -> Component.literal("  §c✓ エリトラ (15%確率)"), false);
                            break;
                            
                        case "lunatic_extreme":
                            context.getSource().sendSuccess(() -> Component.literal("§4§l=== True Crafter Level 5 (MAX) ==="), false);
                            context.getSource().sendSuccess(() -> Component.literal("§7敵の能力:"), false);
                            context.getSource().sendSuccess(() -> Component.literal("  §a✓ 武器切り替え (超高速)"), false);
                            context.getSource().sendSuccess(() -> Component.literal("  §a✓ アクロバティック回避 (最高性能)"), false);
                            context.getSource().sendSuccess(() -> Component.literal("  §a✓ 盾防御 (90%確率)"), false);
                            context.getSource().sendSuccess(() -> Component.literal("  §e✓ ブロック設置 (超高速)"), false);
                            context.getSource().sendSuccess(() -> Component.literal("  §e✓ 橋建設 (超高速)"), false);
                            context.getSource().sendSuccess(() -> Component.literal("  §b✓ 鉄ヘルメット (日光完全耐性)"), false);
                            context.getSource().sendSuccess(() -> Component.literal("  §d✓ ゾンビ重装備+飛びつき"), false);
                            context.getSource().sendSuccess(() -> Component.literal("  §9✓ 水中でガーディアン召喚+騎乗"), false);
                            context.getSource().sendSuccess(() -> Component.literal("  §c✓ エリトラ滑空 (15%確率)"), false);
                            break;
                    }
                } else {
                    context.getSource().sendSuccess(() ->
                        Component.literal("§7[True Crafter Mode: 無効]"),
                        false
                    );
                    context.getSource().sendSuccess(() ->
                        Component.literal("§7nightmare以上の難易度で有効化されます"),
                        false
                    );
                }
                
                return 1;
            })
        );
    }
}
package minecraftarmorweapon.mixin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import minecraftarmorweapon.difficulty.CustomDifficulty;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.DifficultyCommand;
import net.minecraft.world.Difficulty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DifficultyCommand.class)
public class DifficultyCommandMixin {
    
    @Inject(method = "register", at = @At("TAIL"))
    private static void registerCustomDifficulties(CommandDispatcher<CommandSourceStack> dispatcher, CallbackInfo ci) {
        // カスタム難易度コマンドの追加
        LiteralArgumentBuilder<CommandSourceStack> customDifficulty = Commands.literal("customdifficulty")
            .requires((source) -> source.hasPermission(2))
            .then(Commands.literal("nightmare")
                .executes((context) -> {
                    return setCustomDifficulty(context.getSource(), CustomDifficulty.NIGHTMARE);
                }))
            .then(Commands.literal("realistic")
                .executes((context) -> {
                    return setCustomDifficulty(context.getSource(), CustomDifficulty.REALISTIC);
                }))
            .then(Commands.literal("creative_plus")
                .executes((context) -> {
                    return setCustomDifficulty(context.getSource(), CustomDifficulty.CREATIVE_PLUS);
                }))
            .then(Commands.literal("lunatic")
                .executes((context) -> {
                    return setCustomDifficulty(context.getSource(), CustomDifficulty.LUNATIC);
                }))
            .then(Commands.literal("lunatic_plus")
                .executes((context) -> {
                    return setCustomDifficulty(context.getSource(), CustomDifficulty.LUNATIC_PLUS);
                }))
            .then(Commands.literal("lunatic_extreme")
                .executes((context) -> {
                    return setCustomDifficulty(context.getSource(), CustomDifficulty.LUNATIC_EXTREME);
                }))
            .then(Commands.literal("clear")
                .executes((context) -> {
                    return clearCustomDifficulty(context.getSource());
                }))
            .then(Commands.literal("query")
                .executes((context) -> {
                    return queryCustomDifficulty(context.getSource());
                }));
        
        dispatcher.register(customDifficulty);
    }
    
    private static int setCustomDifficulty(CommandSourceStack source, String difficulty) {
        CustomDifficulty.setCustomDifficulty(difficulty);
        Component message = Component.literal("Custom difficulty set to: ")
            .append(CustomDifficulty.getDisplayName(difficulty));
        source.sendSuccess(message, true);
        
        // 通常の難易度をハードに設定（カスタム難易度のベース）
        source.getServer().setDifficulty(Difficulty.HARD, true);
        
        return Command.SINGLE_SUCCESS;
    }
    
    private static int clearCustomDifficulty(CommandSourceStack source) {
        CustomDifficulty.clearCustomDifficulty();
        source.sendSuccess(Component.literal("Custom difficulty cleared. Using vanilla difficulty."), true);
        return Command.SINGLE_SUCCESS;
    }
    
    private static int queryCustomDifficulty(CommandSourceStack source) {
        if (CustomDifficulty.isCustomDifficultyActive()) {
            String current = CustomDifficulty.getCurrentCustomDifficulty();
            Component message = Component.literal("Current custom difficulty: ")
                .append(CustomDifficulty.getDisplayName(current));
            source.sendSuccess(message, false);
        } else {
            source.sendSuccess(Component.literal("No custom difficulty active. Using vanilla difficulty."), false);
        }
        return Command.SINGLE_SUCCESS;
    }
}
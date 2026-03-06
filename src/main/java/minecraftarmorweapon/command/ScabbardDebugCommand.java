package minecraftarmorweapon.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import minecraftarmorweapon.client.renderer.ScabbardCurioRenderer;

import java.util.Arrays;
import java.util.List;

/**
 * /scabbard コマンド — 鞘の位置・回転・スケールをリアルタイム調整
 *
 * 使い方:
 *   /scabbard back rotX 30      ← 背中の X回転を30度に
 *   /scabbard belt x -0.4       ← ベルトの X位置を-0.4に
 *   /scabbard back scaleY 1.5   ← 背中の Y縮尺を1.5に
 *   /scabbard show               ← 現在の全パラメータを表示
 */
@Mod.EventBusSubscriber
public class ScabbardDebugCommand {

    private static final List<String> SLOTS = Arrays.asList("back", "belt");
    private static final List<String> PARAMS = Arrays.asList(
            "x", "y", "z", "rotX", "rotY", "rotZ", "scaleX", "scaleY", "scaleZ");

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        LiteralArgumentBuilder<CommandSourceStack> cmd = Commands.literal("scabbard")
                .requires(s -> s.hasPermission(2))

                // /scabbard show — 現在値を全部表示
                .then(Commands.literal("show")
                        .executes(ctx -> {
                            showAll(ctx.getSource());
                            return 1;
                        }))

                // /scabbard <slot> <param> <value>
                .then(Commands.argument("slot", StringArgumentType.word())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(SLOTS, builder))
                        .then(Commands.argument("param", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(PARAMS, builder))
                                .then(Commands.argument("value", DoubleArgumentType.doubleArg(-360, 360))
                                        .executes(ctx -> {
                                            String slot = StringArgumentType.getString(ctx, "slot");
                                            String param = StringArgumentType.getString(ctx, "param");
                                            double value = DoubleArgumentType.getDouble(ctx, "value");
                                            return setParam(ctx.getSource(), slot, param, value);
                                        }))));

        dispatcher.register(cmd);
    }

    private static int setParam(CommandSourceStack source, String slot, String param, double value) {
        float f = (float) value;

        if ("back".equals(slot)) {
            switch (param) {
                case "x" -> ScabbardCurioRenderer.backX = value;
                case "y" -> ScabbardCurioRenderer.backY = value;
                case "z" -> ScabbardCurioRenderer.backZ = value;
                case "rotX" -> ScabbardCurioRenderer.backRotX = f;
                case "rotY" -> ScabbardCurioRenderer.backRotY = f;
                case "rotZ" -> ScabbardCurioRenderer.backRotZ = f;
                case "scaleX" -> ScabbardCurioRenderer.backScaleX = f;
                case "scaleY" -> ScabbardCurioRenderer.backScaleY = f;
                case "scaleZ" -> ScabbardCurioRenderer.backScaleZ = f;
                default -> {
                    source.sendFailure(Component.literal("§c不明なパラメータ: " + param));
                    return 0;
                }
            }
        } else if ("belt".equals(slot)) {
            switch (param) {
                case "x" -> ScabbardCurioRenderer.beltX = value;
                case "y" -> ScabbardCurioRenderer.beltY = value;
                case "z" -> ScabbardCurioRenderer.beltZ = value;
                case "rotX" -> ScabbardCurioRenderer.beltRotX = f;
                case "rotY" -> ScabbardCurioRenderer.beltRotY = f;
                case "rotZ" -> ScabbardCurioRenderer.beltRotZ = f;
                case "scaleX" -> ScabbardCurioRenderer.beltScaleX = f;
                case "scaleY" -> ScabbardCurioRenderer.beltScaleY = f;
                case "scaleZ" -> ScabbardCurioRenderer.beltScaleZ = f;
                default -> {
                    source.sendFailure(Component.literal("§c不明なパラメータ: " + param));
                    return 0;
                }
            }
        } else {
            source.sendFailure(Component.literal("§c不明なスロット: " + slot + " (back / belt)"));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(
                "§a[鞘] " + slot + "." + param + " = " + value), false);
        return 1;
    }

    private static void showAll(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("§e=== 背中 (back) ==="), false);
        source.sendSuccess(() -> Component.literal(String.format(
                "  §7位置: §fx=%.3f  y=%.3f  z=%.3f",
                ScabbardCurioRenderer.backX, ScabbardCurioRenderer.backY, ScabbardCurioRenderer.backZ)), false);
        source.sendSuccess(() -> Component.literal(String.format(
                "  §7回転: §frotX=%.1f  rotY=%.1f  rotZ=%.1f",
                ScabbardCurioRenderer.backRotX, ScabbardCurioRenderer.backRotY, ScabbardCurioRenderer.backRotZ)), false);
        source.sendSuccess(() -> Component.literal(String.format(
                "  §7縮尺: §fscaleX=%.2f  scaleY=%.2f  scaleZ=%.2f",
                ScabbardCurioRenderer.backScaleX, ScabbardCurioRenderer.backScaleY, ScabbardCurioRenderer.backScaleZ)), false);

        source.sendSuccess(() -> Component.literal("§e=== ベルト (belt) ==="), false);
        source.sendSuccess(() -> Component.literal(String.format(
                "  §7位置: §fx=%.3f  y=%.3f  z=%.3f",
                ScabbardCurioRenderer.beltX, ScabbardCurioRenderer.beltY, ScabbardCurioRenderer.beltZ)), false);
        source.sendSuccess(() -> Component.literal(String.format(
                "  §7回転: §frotX=%.1f  rotY=%.1f  rotZ=%.1f",
                ScabbardCurioRenderer.beltRotX, ScabbardCurioRenderer.beltRotY, ScabbardCurioRenderer.beltRotZ)), false);
        source.sendSuccess(() -> Component.literal(String.format(
                "  §7縮尺: §fscaleX=%.2f  scaleY=%.2f  scaleZ=%.2f",
                ScabbardCurioRenderer.beltScaleX, ScabbardCurioRenderer.beltScaleY, ScabbardCurioRenderer.beltScaleZ)), false);
    }
}

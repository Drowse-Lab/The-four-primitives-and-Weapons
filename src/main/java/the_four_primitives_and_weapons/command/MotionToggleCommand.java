package the_four_primitives_and_weapons.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import the_four_primitives_and_weapons.skill.PlayerSkillData;
import the_four_primitives_and_weapons.skill.SkillRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * /motion toggle <motion_id>   — その技 (motion) の ON/OFF を切替
 * /motion enable <motion_id>   — 強制 ON
 * /motion disable <motion_id>  — 強制 OFF
 * /motion list                  — 現在 OFF の技一覧
 *
 * 技を OFF にすると、 該当 motion を発動する handler が「無効化」されているのを検知して
 * 通常攻撃などにフォールバックする。
 */
@Mod.EventBusSubscriber
public class MotionToggleCommand {

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("motion")
            .requires(s -> s.hasPermission(0)) // 全プレイヤー可

            .then(Commands.literal("toggle")
                .then(Commands.argument("motion_id", StringArgumentType.word())
                    .suggests((ctx, b) -> SharedSuggestionProvider.suggest(allMotionIds(), b))
                    .executes(ctx -> setMotion(ctx.getSource(),
                            StringArgumentType.getString(ctx, "motion_id"), null))))

            .then(Commands.literal("enable")
                .then(Commands.argument("motion_id", StringArgumentType.word())
                    .suggests((ctx, b) -> SharedSuggestionProvider.suggest(allMotionIds(), b))
                    .executes(ctx -> setMotion(ctx.getSource(),
                            StringArgumentType.getString(ctx, "motion_id"), Boolean.TRUE))))

            .then(Commands.literal("disable")
                .then(Commands.argument("motion_id", StringArgumentType.word())
                    .suggests((ctx, b) -> SharedSuggestionProvider.suggest(allMotionIds(), b))
                    .executes(ctx -> setMotion(ctx.getSource(),
                            StringArgumentType.getString(ctx, "motion_id"), Boolean.FALSE))))

            .then(Commands.literal("list")
                .executes(ctx -> listDisabled(ctx.getSource())))
        );
    }

    private static List<String> allMotionIds() {
        List<String> ids = new ArrayList<>();
        try {
            SkillRegistry.getAllMotions().forEach(m -> ids.add(m.getId()));
        } catch (Throwable ignored) {}
        return ids;
    }

    /** force = null → トグル / true → ON / false → OFF */
    private static int setMotion(CommandSourceStack source, String motionId, Boolean force) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("§cこのコマンドはプレイヤー専用"));
            return 0;
        }
        boolean currentlyEnabled = PlayerSkillData.isMotionEnabled(player, motionId);
        boolean target = (force != null) ? force : !currentlyEnabled;
        PlayerSkillData.setMotionEnabled(player, motionId, target);

        final String label = target ? "§a有効" : "§c無効";
        source.sendSuccess(() -> Component.literal(
                "§7技 §e" + motionId + "§7 を " + label + " §7にしました"), false);
        return 1;
    }

    private static int listDisabled(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("§cこのコマンドはプレイヤー専用"));
            return 0;
        }
        List<String> disabled = new ArrayList<>();
        for (String id : allMotionIds()) {
            if (!PlayerSkillData.isMotionEnabled(player, id)) disabled.add(id);
        }
        if (disabled.isEmpty()) {
            source.sendSuccess(() -> Component.literal("§7無効化されている技はありません"), false);
        } else {
            source.sendSuccess(() -> Component.literal(
                    "§7無効化中 (" + disabled.size() + "): §c" + String.join(", ", disabled)), false);
        }
        return 1;
    }
}

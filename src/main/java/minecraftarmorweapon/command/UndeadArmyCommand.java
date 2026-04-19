package minecraftarmorweapon.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import minecraftarmorweapon.event.UndeadArmyEvent;

/**
 * /undeadarmy start [player]   — プレイヤーにアンデットアーミー侵攻を発動
 * /undeadarmy cancel [player]  — 進行中の侵攻をキャンセル
 */
@Mod.EventBusSubscriber
public class UndeadArmyCommand {

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("undeadarmy")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("start")
                .executes(ctx -> startRaid(ctx.getSource(), selfOrThrow(ctx.getSource())))
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(ctx -> startRaid(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))
                )
            )
            .then(Commands.literal("cancel")
                .executes(ctx -> cancelRaid(ctx.getSource(), selfOrThrow(ctx.getSource())))
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(ctx -> cancelRaid(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))
                )
            )
        );
    }

    private static ServerPlayer selfOrThrow(CommandSourceStack source) throws CommandSyntaxException {
        return source.getPlayerOrException();
    }

    private static int startRaid(CommandSourceStack source, ServerPlayer target) {
        boolean triggered = UndeadArmyEvent.triggerRaid(target);
        if (triggered) {
            source.sendSuccess(() -> Component.literal(
                "§a" + target.getName().getString() + " にアンデットアーミーを発動"
            ), true);
            return 1;
        } else {
            source.sendFailure(Component.literal(
                "§c" + target.getName().getString() + " は既に侵攻中、またはワールドが見つかりません"
            ));
            return 0;
        }
    }

    private static int cancelRaid(CommandSourceStack source, ServerPlayer target) {
        boolean canceled = UndeadArmyEvent.cancelRaid(target);
        if (canceled) {
            source.sendSuccess(() -> Component.literal(
                "§e" + target.getName().getString() + " の侵攻をキャンセル"
            ), true);
            return 1;
        } else {
            source.sendFailure(Component.literal(
                "§c" + target.getName().getString() + " は侵攻中ではありません"
            ));
            return 0;
        }
    }
}

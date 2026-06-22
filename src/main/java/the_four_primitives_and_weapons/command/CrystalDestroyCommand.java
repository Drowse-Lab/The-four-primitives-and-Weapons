package the_four_primitives_and_weapons.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import the_four_primitives_and_weapons.events.MagicalKatanaCrystalHandler;

/**
 * /crystal destroy_mine
 *   サーバ上の全プレイヤーのインベントリを走査し、 自分の UUID が刻まれている具現化
 *   Magical Katana を全部破壊する。 ( = 「他人のスロットの自分の武器も破壊」 仕様 )
 */
@Mod.EventBusSubscriber
public class CrystalDestroyCommand {

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("crystal")
            .requires(s -> s.hasPermission(0)) // 全プレイヤー可
            .then(Commands.literal("destroy_mine")
                .executes(ctx -> destroyMine(ctx.getSource())))
        );
    }

    private static int destroyMine(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("§cこのコマンドはプレイヤー専用"));
            return 0;
        }
        int n = MagicalKatanaCrystalHandler.destroyAllOwnedMaterialized(
                source.getServer(), player.getUUID());
        final int total = n;
        source.sendSuccess(() -> Component.literal(
                "§7自分の具現化武器を §c" + total + " §7本破壊しました"), false);
        return n;
    }
}

// package minecraftarmorweapon.item;

// import com.mojang.brigadier.Command;
// import com.mojang.brigadier.CommandDispatcher;
// import com.mojang.brigadier.arguments.StringArgumentType;
// import net.minecraft.commands.CommandSourceStack;
// import net.minecraft.commands.Commands;
// import net.minecraft.network.chat.Component;
// import net.minecraft.ChatFormatting;
// import net.minecraft.world.entity.player.Player;
// import net.minecraft.world.item.ItemStack;
// import net.minecraftforge.event.server.ServerStartingEvent;
// import net.minecraftforge.eventbus.api.SubscribeEvent;
// import net.minecraftforge.fml.common.Mod;

// @Mod.EventBusSubscriber
// public class SetFeynCommand {
//     @SubscribeEvent
//     public static void onServerStarting(ServerStartingEvent event) {
//         CommandDispatcher<CommandSourceStack> dispatcher = event.getServer().getCommands().getDispatcher();
//         register(dispatcher);
//     }

//     public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
//         dispatcher.register(Commands.literal("setFeyn")
//             .requires(source -> source.hasPermission(2))
//             .then(Commands.argument("value", StringArgumentType.word())
//                 .executes(context -> {
//                     String value = StringArgumentType.getString(context, "value");
//                     CommandSourceStack source = context.getSource();
//                     Player player = source.getPlayerOrException();
//                     ItemStack itemStack = player.getMainHandItem();

//                     if (!itemStack.isEmpty()) {
//                         itemStack.getOrCreateTag().putString("Feyn", value);
//                         player.sendSystemMessage(Component.literal("Feynタグが " + value + " に設定されました。").withStyle(ChatFormatting.GREEN));
//                         return Command.SINGLE_SUCCESS;
//                     } else {
//                         player.sendSystemMessage(Component.literal("手に持っているアイテムがありません。").withStyle(ChatFormatting.RED));
//                         return Command.SINGLE_SUCCESS;
//                     }
//                 })));
//     }
// }

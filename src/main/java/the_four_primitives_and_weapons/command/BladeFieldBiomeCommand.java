package the_four_primitives_and_weapons.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;

/** /bladefield locate <name> を標準の /locate biome へ接続する短縮コマンド。 */
@Mod.EventBusSubscriber(modid = TheFourPrimitivesAndWeaponsMod.MODID)
public final class BladeFieldBiomeCommand {
    private static final String[] NAMES = {"normal", "fire", "ice", "thunder", "water", "blood", "wind", "corrosion"};

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> locate = Commands.literal("locate");
        for (String name : NAMES) locate.then(Commands.literal(name).executes(ctx -> locate(ctx.getSource(), name)));
        event.getDispatcher().register(Commands.literal("bladefield")
                .requires(source -> source.hasPermission(2))
                .then(locate)
                .then(Commands.literal("list").executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> Component.literal(String.join(", ", NAMES)), false);
                    return NAMES.length;
                })));
    }

    private static int locate(CommandSourceStack source, String name) {
        String path = name.equals("normal") ? "blade_field" : "blade_field_" + name;
        return source.getServer().getCommands().performPrefixedCommand(source,
                "locate biome " + TheFourPrimitivesAndWeaponsMod.MODID + ":" + path);
    }

    private BladeFieldBiomeCommand() {}
}

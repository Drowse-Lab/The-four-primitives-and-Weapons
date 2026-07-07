package the_four_primitives_and_weapons.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import the_four_primitives_and_weapons.damage.ElementType;
import the_four_primitives_and_weapons.damage.IElementalDamageSource;
import the_four_primitives_and_weapons.damage.ModDamageSources;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * /edamage <target> <amount> <element> [level]
 * 属性付きダメージをエンティティに与えるコマンド
 *
 * 例:
 *   /edamage @e[type=zombie,limit=1] 20 holy 5
 *   /edamage @p 10 ice 3
 *   /edamage @e[type=the_four_primitives_and_weapons:debug_mob_spawn_egg] 100 electric
 */
@Mod.EventBusSubscriber
public class ElementDamageCommand {

    private static final java.util.List<String> ELEMENT_NAMES = Arrays.stream(ElementType.values())
        .filter(e -> e != ElementType.NONE && e != ElementType.ERASURE)
        .map(e -> e.getName())
        .collect(Collectors.toList());

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("damage")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("targets", EntityArgument.entities())
                .then(Commands.argument("amount", FloatArgumentType.floatArg(0.0f))
                    .then(Commands.argument("element", StringArgumentType.word())
                        .suggests((ctx, builder) ->
                            SharedSuggestionProvider.suggest(ELEMENT_NAMES, builder))
                        // /edamage <target> <amount> <element>  (level=1)
                        .executes(ctx -> execute(ctx.getSource(),
                            EntityArgument.getEntities(ctx, "targets"),
                            FloatArgumentType.getFloat(ctx, "amount"),
                            StringArgumentType.getString(ctx, "element"),
                            1))
                        // /edamage <target> <amount> <element> <level>
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 100))
                            .executes(ctx -> execute(ctx.getSource(),
                                EntityArgument.getEntities(ctx, "targets"),
                                FloatArgumentType.getFloat(ctx, "amount"),
                                StringArgumentType.getString(ctx, "element"),
                                IntegerArgumentType.getInteger(ctx, "level")))
                        )
                    )
                )
            )
        );
    }

    private static int execute(CommandSourceStack source,
                               java.util.Collection<? extends Entity> targets,
                               float amount, String elementName, int level) {
        ElementType element = ElementType.fromString(elementName);
        if (element == ElementType.NONE) {
            source.sendFailure(Component.literal("§c不明な属性: " + elementName));
            return 0;
        }

        int count = 0;
        Entity attacker = source.getEntity();
        for (Entity entity : targets) {
            if (entity instanceof LivingEntity living) {
                net.minecraft.world.damagesource.DamageSource ds =
                    ModDamageSources.ofElement(living.level(), element, attacker);
                if (ds instanceof IElementalDamageSource elemSource) {
                    elemSource.setElementType(element);
                    elemSource.setElementLevel(level);
                }
                living.hurt(ds, amount);
                count++;
            }
        }

        final int total = count;
        source.sendSuccess(() -> Component.literal(
            String.format("§a%d体に §6%s§a Lv.%d で §c%.1f§a ダメージを与えました",
                total, element.getName().toUpperCase(), level, amount)
        ), true);

        return count;
    }
}

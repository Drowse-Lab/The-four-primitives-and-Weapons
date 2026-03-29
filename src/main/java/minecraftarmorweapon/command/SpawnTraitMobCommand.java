package minecraftarmorweapon.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import minecraftarmorweapon.trait.MobTrait;
import minecraftarmorweapon.trait.MobTraitHandler;

import java.util.Arrays;
import java.util.List;

/**
 * /spawntrait <entity> <trait> — 特定の特性を持つMobをスポーンするコマンド
 *
 * 例:
 *   /spawntrait minecraft:zombie undying
 *   /spawntrait minecraft:skeleton explosive
 *   /spawntrait minecraft:spider iron_wall
 */
@Mod.EventBusSubscriber
public class SpawnTraitMobCommand {

    private static final List<String> TRAIT_NAMES = Arrays.stream(MobTrait.values())
        .map(t -> t.name().toLowerCase())
        .toList();

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("spawntrait")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("entity", ResourceLocationArgument.id())
                .suggests(SuggestionProviders.SUMMONABLE_ENTITIES)
                .then(Commands.argument("trait", StringArgumentType.word())
                    .suggests((context, builder) -> SharedSuggestionProvider.suggest(TRAIT_NAMES, builder))
                    .executes(context -> {
                        ResourceLocation entityId = ResourceLocationArgument.getId(context, "entity");
                        String traitName = StringArgumentType.getString(context, "trait");
                        return spawnTraitMob(context.getSource(), entityId, traitName);
                    })
                )
            )
        );
    }

    private static int spawnTraitMob(CommandSourceStack source, ResourceLocation entityId, String traitName) {
        // 特性を検索
        MobTrait foundTrait = null;
        for (MobTrait t : MobTrait.values()) {
            if (t.name().equalsIgnoreCase(traitName) || t.getDisplayName().equals(traitName)) {
                foundTrait = t;
                break;
            }
        }

        if (foundTrait == null) {
            source.sendFailure(Component.literal("§c不明な特性: " + traitName));
            source.sendFailure(Component.literal("§7使用可能: " + String.join(", ", TRAIT_NAMES)));
            return 0;
        }

        final MobTrait trait = foundTrait;

        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());

        // エンティティを作成
        EntityType<?> type = EntityType.byString(entityId.toString()).orElse(null);
        if (type == null) {
            source.sendFailure(Component.literal("§c不明なエンティティ: " + entityId));
            return 0;
        }

        Entity entity = type.spawn(level, pos, MobSpawnType.COMMAND);
        if (entity == null) {
            source.sendFailure(Component.literal("§cエンティティのスポーンに失敗しました"));
            return 0;
        }

        if (!(entity instanceof Monster monster)) {
            source.sendFailure(Component.literal("§c" + entityId + " はモンスターではありません"));
            entity.discard();
            return 0;
        }

        // 特性を適用
        MobTraitHandler.applyTraitToMob(monster, trait);

        source.sendSuccess(() -> Component.literal(
            trait.getColorCode() + "§l" + trait.getFormattedName() + " §f" +
            type.getDescription().getString() + " §aをスポーンしました"
        ), true);

        return 1;
    }
}

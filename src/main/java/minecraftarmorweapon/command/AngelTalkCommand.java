package minecraftarmorweapon.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import minecraftarmorweapon.entity.AngelTrioEntity;
import minecraftarmorweapon.ai.lisp.AngelChatAI;
import net.minecraft.network.chat.Component;

import java.util.UUID;

/**
 * /angel_talk yes|no <entity_uuid>  — チャット内の[Yes][No]ボタンから呼ばれる
 * /angel_talk apikey <key>          — Claude APIキーを設定
 * /angel_talk end                   — 会話モードを終了
 */
@Mod.EventBusSubscriber
public class AngelTalkCommand {

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("angel_talk")
            // /angel_talk yes|no <uuid>
            .then(Commands.argument("response", StringArgumentType.word())
                .then(Commands.argument("uuid", StringArgumentType.string())
                    .executes(context -> {
                        String response = StringArgumentType.getString(context, "response");
                        String uuidStr = StringArgumentType.getString(context, "uuid");

                        if (context.getSource().getEntity() instanceof ServerPlayer player) {
                            try {
                                UUID entityUUID = UUID.fromString(uuidStr);
                                AngelTrioEntity.handleTalkCommand(player, response, entityUUID);
                            } catch (Exception e) {
                                // UUID無効
                            }
                        }
                        return 1;
                    }))
                // /angel_talk end（uuidなし）
                .executes(context -> {
                    String response = StringArgumentType.getString(context, "response");
                    if ("end".equals(response) || "bye".equals(response)) {
                        if (context.getSource().getEntity() instanceof ServerPlayer player) {
                            AngelChatAI.endSession(player);
                        }
                    }
                    return 1;
                }))
            // /angel_talk apikey <key>
            .then(Commands.literal("apikey")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("key", StringArgumentType.string())
                    .executes(context -> {
                        String key = StringArgumentType.getString(context, "key");
                        AngelChatAI.setApiKey(key);
                        context.getSource().sendSuccess(
                            () -> Component.literal("§aAPIキーを設定しました"), true);
                        return 1;
                    }))));
    }
}

package minecraftarmorweapon.ai.lisp;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 会話モード中のプレイヤーのチャットを横取りしてAIに渡す。
 * 他のプレイヤーにはメッセージが見えない（キャンセルする）。
 */
@Mod.EventBusSubscriber(modid = "minecraft_armor_weapon")
public class AngelChatEventHandler {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onServerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        if (player == null) return;

        // 会話モード中でなければスルー
        if (!AngelChatAI.isInSession(player.getUUID())) return;

        // チャットをキャンセル（他プレイヤーには見せない）
        event.setCanceled(true);

        // AIに渡す
        String message = event.getMessage().getString();
        AngelChatAI.handlePlayerMessage(player, message);
    }
}

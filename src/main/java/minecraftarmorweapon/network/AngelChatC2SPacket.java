package minecraftarmorweapon.network;

import minecraftarmorweapon.MinecraftArmorWeaponMod;
import minecraftarmorweapon.ai.lisp.AngelChatAI;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/**
 * C2S: プレイヤーがGUIからメッセージを送信する。
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class AngelChatC2SPacket {
    private final String message;
    private final String entityUUID;

    public AngelChatC2SPacket(String message, String entityUUID) {
        this.message = message;
        this.entityUUID = entityUUID;
    }

    public AngelChatC2SPacket(FriendlyByteBuf buf) {
        this.message = buf.readUtf(256);
        this.entityUUID = buf.readUtf(64);
    }

    public static void encode(AngelChatC2SPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.message, 256);
        buf.writeUtf(msg.entityUUID, 64);
    }

    public static void handle(AngelChatC2SPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            // "bye" で終了
            if ("bye".equalsIgnoreCase(msg.message.trim())) {
                AngelChatAI.endSession(player);
                // GUI閉じる指示をクライアントに送る
                MinecraftArmorWeaponMod.PACKET_HANDLER.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new AngelChatS2CPacket("", "", true));
                return;
            }

            // AIに処理を委譲（非同期）
            AngelChatAI.handlePlayerMessageWithCallback(player, msg.message, (response, entityName) -> {
                // S2Cパケットで返答を返す
                MinecraftArmorWeaponMod.PACKET_HANDLER.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new AngelChatS2CPacket(response, entityName, false));
            });
        });
        ctx.get().setPacketHandled(true);
    }

    @SubscribeEvent
    public static void register(FMLCommonSetupEvent event) {
        MinecraftArmorWeaponMod.addNetworkMessage(AngelChatC2SPacket.class,
            AngelChatC2SPacket::encode, AngelChatC2SPacket::new, AngelChatC2SPacket::handle);
    }
}

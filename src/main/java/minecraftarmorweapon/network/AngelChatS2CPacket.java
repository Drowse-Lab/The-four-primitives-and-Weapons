package minecraftarmorweapon.network;

import minecraftarmorweapon.MinecraftArmorWeaponMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S2C: サーバーからクライアントにAI応答を送る / GUI開閉指示。
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class AngelChatS2CPacket {
    public final String message;
    public final String entityName;
    public final boolean closeGui;
    // GUI開く用
    public final boolean openGui;
    public final int personality;
    public final String entityUUID;

    /** 応答メッセージ or GUI閉じる */
    public AngelChatS2CPacket(String message, String entityName, boolean closeGui) {
        this.message = message;
        this.entityName = entityName;
        this.closeGui = closeGui;
        this.openGui = false;
        this.personality = 0;
        this.entityUUID = "";
    }

    /** GUI開く */
    public AngelChatS2CPacket(String entityName, int personality, String entityUUID, String firstMessage) {
        this.message = firstMessage;
        this.entityName = entityName;
        this.closeGui = false;
        this.openGui = true;
        this.personality = personality;
        this.entityUUID = entityUUID;
    }

    public AngelChatS2CPacket(FriendlyByteBuf buf) {
        this.message = buf.readUtf(1024);
        this.entityName = buf.readUtf(64);
        this.closeGui = buf.readBoolean();
        this.openGui = buf.readBoolean();
        this.personality = buf.readInt();
        this.entityUUID = buf.readUtf(64);
    }

    public static void encode(AngelChatS2CPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.message, 1024);
        buf.writeUtf(msg.entityName, 64);
        buf.writeBoolean(msg.closeGui);
        buf.writeBoolean(msg.openGui);
        buf.writeInt(msg.personality);
        buf.writeUtf(msg.entityUUID, 64);
    }

    public static void handle(AngelChatS2CPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // クライアント側で処理
            minecraftarmorweapon.client.screen.AngelChatScreen.handlePacket(msg);
        });
        ctx.get().setPacketHandled(true);
    }

    @SubscribeEvent
    public static void register(FMLCommonSetupEvent event) {
        MinecraftArmorWeaponMod.addNetworkMessage(AngelChatS2CPacket.class,
            AngelChatS2CPacket::encode, AngelChatS2CPacket::new, AngelChatS2CPacket::handle);
    }
}

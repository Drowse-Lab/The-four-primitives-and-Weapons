package the_four_primitives_and_weapons.network;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * S2C: サーバーからクライアントに会話ノードを送る（選択肢付き）。
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class AngelChatS2CPacket {
    public final String entityName;
    public final int personality;
    public final String entityUUID;
    public final String npcText;
    public final List<String> choiceTexts;
    public final List<String> choiceNexts;
    public final boolean isEnd;

    public AngelChatS2CPacket(String entityName, int personality, String entityUUID, String npcText,
                              List<String> choiceTexts, List<String> choiceNexts, boolean isEnd) {
        this.entityName = entityName;
        this.personality = personality;
        this.entityUUID = entityUUID;
        this.npcText = npcText;
        this.choiceTexts = choiceTexts;
        this.choiceNexts = choiceNexts;
        this.isEnd = isEnd;
    }

    public AngelChatS2CPacket(FriendlyByteBuf buf) {
        this.entityName = buf.readUtf(64);
        this.personality = buf.readInt();
        this.entityUUID = buf.readUtf(64);
        this.npcText = buf.readUtf(1024);
        int count = buf.readInt();
        this.choiceTexts = new ArrayList<>();
        this.choiceNexts = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            this.choiceTexts.add(buf.readUtf(256));
            this.choiceNexts.add(buf.readUtf(64));
        }
        this.isEnd = buf.readBoolean();
    }

    public static void encode(AngelChatS2CPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.entityName, 64);
        buf.writeInt(msg.personality);
        buf.writeUtf(msg.entityUUID, 64);
        buf.writeUtf(msg.npcText, 1024);
        buf.writeInt(msg.choiceTexts.size());
        for (int i = 0; i < msg.choiceTexts.size(); i++) {
            buf.writeUtf(msg.choiceTexts.get(i), 256);
            buf.writeUtf(msg.choiceNexts.get(i), 64);
        }
        buf.writeBoolean(msg.isEnd);
    }

    public static void handle(AngelChatS2CPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            the_four_primitives_and_weapons.client.screen.AngelChatScreen.handlePacket(msg);
        });
        ctx.get().setPacketHandled(true);
    }

    @SubscribeEvent
    public static void register(FMLCommonSetupEvent event) {
        TheFourPrimitivesAndWeaponsMod.addNetworkMessage(AngelChatS2CPacket.class,
            AngelChatS2CPacket::encode, AngelChatS2CPacket::new, AngelChatS2CPacket::handle);
    }
}

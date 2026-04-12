package minecraftarmorweapon.network;

import minecraftarmorweapon.MinecraftArmorWeaponMod;
import minecraftarmorweapon.ai.lisp.DialogueManager;
import minecraftarmorweapon.entity.AngelTrioEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * C2S: プレイヤーが選択肢を選んだ時の通知。
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class AngelChatC2SPacket {
    private final String nextNodeId;
    private final String entityUUID;

    public AngelChatC2SPacket(String nextNodeId, String entityUUID) {
        this.nextNodeId = nextNodeId;
        this.entityUUID = entityUUID;
    }

    public AngelChatC2SPacket(FriendlyByteBuf buf) {
        this.nextNodeId = buf.readUtf(64);
        this.entityUUID = buf.readUtf(64);
    }

    public static void encode(AngelChatC2SPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.nextNodeId, 64);
        buf.writeUtf(msg.entityUUID, 64);
    }

    public static void handle(AngelChatC2SPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            // エンティティを取得して人格を確認
            int personality = 0;
            String entityName = "???";
            try {
                UUID uuid = UUID.fromString(msg.entityUUID);
                if (player.level() instanceof ServerLevel level) {
                    Entity e = level.getEntity(uuid);
                    if (e instanceof AngelTrioEntity angel) {
                        personality = angel.getPersonality();
                        entityName = "???";
                    }
                }
            } catch (Exception e) {
                // 無視
            }

            // 次のノードを取得
            DialogueManager.Node node = DialogueManager.getNode(personality, msg.nextNodeId);
            if (node == null) {
                node = DialogueManager.getStartNode(personality);
            }
            if (node == null) return;

            // 選択肢を配列にする
            List<String> choiceTexts = new ArrayList<>();
            List<String> choiceNexts = new ArrayList<>();
            for (DialogueManager.Choice c : node.choices) {
                choiceTexts.add(c.text);
                choiceNexts.add(c.next != null ? c.next : "start");
            }

            // S2Cで返す
            MinecraftArmorWeaponMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new AngelChatS2CPacket(entityName, personality, msg.entityUUID, node.text,
                    choiceTexts, choiceNexts, node.isEnd));
        });
        ctx.get().setPacketHandled(true);
    }

    @SubscribeEvent
    public static void register(FMLCommonSetupEvent event) {
        MinecraftArmorWeaponMod.addNetworkMessage(AngelChatC2SPacket.class,
            AngelChatC2SPacket::encode, AngelChatC2SPacket::new, AngelChatC2SPacket::handle);
    }
}

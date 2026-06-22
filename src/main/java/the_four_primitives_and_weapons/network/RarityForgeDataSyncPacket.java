package the_four_primitives_and_weapons.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.damage.ElementType;
import the_four_primitives_and_weapons.item.rarity.RarityForgeCenterLogic;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * RarityForgeCenterLogic の data table を server → client に同期するパケット。
 *   - catalyst_levels  : Map&lt;String, Integer&gt;
 *   - book_elements    : Map&lt;String, ElementType&gt;
 *   - unbreakable_pairs: List&lt;UnbreakablePair&gt;
 *
 * datapack の reload 完了時 ( OnDatapackSyncEvent ) に全 player へ送信される。
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class RarityForgeDataSyncPacket {

    private final Map<String, Integer> catalystLevels;
    private final Map<String, ElementType> bookElements;
    private final List<RarityForgeCenterLogic.UnbreakablePair> unbreakablePairs;

    public RarityForgeDataSyncPacket(Map<String, Integer> catalystLevels,
                                     Map<String, ElementType> bookElements,
                                     List<RarityForgeCenterLogic.UnbreakablePair> unbreakablePairs) {
        this.catalystLevels = catalystLevels;
        this.bookElements = bookElements;
        this.unbreakablePairs = unbreakablePairs;
    }

    public RarityForgeDataSyncPacket(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        this.catalystLevels = new LinkedHashMap<>(n);
        for (int i = 0; i < n; i++) {
            String key = buf.readUtf();
            int level = buf.readVarInt();
            this.catalystLevels.put(key, level);
        }
        int m = buf.readVarInt();
        this.bookElements = new LinkedHashMap<>(m);
        for (int i = 0; i < m; i++) {
            String key = buf.readUtf();
            String elem = buf.readUtf();
            try {
                this.bookElements.put(key, ElementType.valueOf(elem));
            } catch (IllegalArgumentException ignored) {
                // unknown element name — skip
            }
        }
        int p = buf.readVarInt();
        this.unbreakablePairs = new ArrayList<>(p);
        for (int i = 0; i < p; i++) {
            String a = buf.readUtf();
            String b = buf.readUtf();
            this.unbreakablePairs.add(new RarityForgeCenterLogic.UnbreakablePair(a, b));
        }
    }

    public static void encode(RarityForgeDataSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.catalystLevels.size());
        for (Map.Entry<String, Integer> e : msg.catalystLevels.entrySet()) {
            buf.writeUtf(e.getKey());
            buf.writeVarInt(e.getValue());
        }
        buf.writeVarInt(msg.bookElements.size());
        for (Map.Entry<String, ElementType> e : msg.bookElements.entrySet()) {
            buf.writeUtf(e.getKey());
            buf.writeUtf(e.getValue().name());
        }
        buf.writeVarInt(msg.unbreakablePairs.size());
        for (RarityForgeCenterLogic.UnbreakablePair p : msg.unbreakablePairs) {
            buf.writeUtf(p.catA());
            buf.writeUtf(p.catB());
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // client side で table を更新
            if (ctx.get().getDirection() == NetworkDirection.PLAY_TO_CLIENT
                    && Minecraft.getInstance() != null) {
                RarityForgeCenterLogic.setCatalystLevels(catalystLevels);
                RarityForgeCenterLogic.setBookElements(bookElements);
                RarityForgeCenterLogic.setUnbreakablePairs(unbreakablePairs);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    @SubscribeEvent
    public static void registerMessage(FMLCommonSetupEvent event) {
        TheFourPrimitivesAndWeaponsMod.addNetworkMessage(RarityForgeDataSyncPacket.class,
                RarityForgeDataSyncPacket::encode,
                RarityForgeDataSyncPacket::new,
                (msg, ctx) -> msg.handle(ctx));
    }
}

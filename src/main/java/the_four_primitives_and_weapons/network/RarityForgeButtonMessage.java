package the_four_primitives_and_weapons.network;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * ( 旧 ) クラフト候補クリック通知。 シンプル化版テーブルでは未使用 ( no-op )。
 * ネットワーク channel は登録残置 ( 古いクライアントからの受信を黙って捨てるため )。
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class RarityForgeButtonMessage {

    private final int x, y, z;
    private final int recipeIndex;

    public RarityForgeButtonMessage(int x, int y, int z, int recipeIndex) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.recipeIndex = recipeIndex;
    }

    public RarityForgeButtonMessage(FriendlyByteBuf buffer) {
        this.x = buffer.readInt();
        this.y = buffer.readInt();
        this.z = buffer.readInt();
        this.recipeIndex = buffer.readInt();
    }

    public static void buffer(RarityForgeButtonMessage message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.x);
        buffer.writeInt(message.y);
        buffer.writeInt(message.z);
        buffer.writeInt(message.recipeIndex);
    }

    public static void handler(RarityForgeButtonMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        // シンプル化版テーブルでは候補クリック方式を撤去。 黙って消費するだけ。
        contextSupplier.get().setPacketHandled(true);
    }

    @SubscribeEvent
    public static void registerMessage(FMLCommonSetupEvent event) {
        TheFourPrimitivesAndWeaponsMod.addNetworkMessage(
                RarityForgeButtonMessage.class,
                RarityForgeButtonMessage::buffer,
                RarityForgeButtonMessage::new,
                RarityForgeButtonMessage::handler
        );
    }
}

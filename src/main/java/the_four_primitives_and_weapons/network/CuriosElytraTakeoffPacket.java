package the_four_primitives_and_weapons.network;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.events.CuriosElytraFlightHandler;

import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

/**
 * クライアント→サーバー: Curios の elytra スロットのエリトラで離陸 (滑空開始) するパケット。
 * 空中でジャンプキーが押されたときに {@code CuriosElytraClientHandler} から送られる。
 * 条件判定はサーバー側 {@link CuriosElytraFlightHandler#tryTakeoff(ServerPlayer)} で行う。
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class CuriosElytraTakeoffPacket {

    public CuriosElytraTakeoffPacket() {
    }

    public CuriosElytraTakeoffPacket(FriendlyByteBuf buffer) {
    }

    public static void buffer(CuriosElytraTakeoffPacket message, FriendlyByteBuf buffer) {
    }

    public static void handler(CuriosElytraTakeoffPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            if (!player.level().hasChunkAt(player.blockPosition())) return;

            CuriosElytraFlightHandler.tryTakeoff(player);
        });
        context.setPacketHandled(true);
    }

    @SubscribeEvent
    public static void registerMessage(FMLCommonSetupEvent event) {
        TheFourPrimitivesAndWeaponsMod.addNetworkMessage(
            CuriosElytraTakeoffPacket.class,
            CuriosElytraTakeoffPacket::buffer,
            CuriosElytraTakeoffPacket::new,
            CuriosElytraTakeoffPacket::handler
        );
    }
}

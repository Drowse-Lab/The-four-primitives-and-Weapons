package the_four_primitives_and_weapons.network;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.event.MultiJumpHandler;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * クライアント側で「空中での keyJump 新規押下」を検知したことをサーバーへ通知。
 * サーバー側 {@link MultiJumpHandler#tryAirJump(ServerPlayer)} で残ジャンプ確認＋実行する。
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class MultiJumpPacket {

    @SubscribeEvent
    public static void registerMessage(FMLCommonSetupEvent event) {
        TheFourPrimitivesAndWeaponsMod.addNetworkMessage(MultiJumpPacket.class,
            MultiJumpPacket::encode, MultiJumpPacket::new, MultiJumpPacket::handle);
    }

    public MultiJumpPacket() {}
    public MultiJumpPacket(FriendlyByteBuf buf) {}
    public static void encode(MultiJumpPacket msg, FriendlyByteBuf buf) {}

    public static void handle(MultiJumpPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                MultiJumpHandler.tryAirJump(player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}

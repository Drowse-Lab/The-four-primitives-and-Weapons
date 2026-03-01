package minecraftarmorweapon.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import minecraftarmorweapon.MinecraftArmorWeaponMod;
import minecraftarmorweapon.events.DodgeAndBattouHandler;

import java.util.function.Supplier;

/**
 * RightClickEmpty（クライアント専用イベント）から回避をサーバーに通知するパケット。
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class DodgeRequestPacket {

    @SubscribeEvent
    public static void registerMessage(FMLCommonSetupEvent event) {
        MinecraftArmorWeaponMod.addNetworkMessage(DodgeRequestPacket.class,
                DodgeRequestPacket::encode, DodgeRequestPacket::new, DodgeRequestPacket::handle);
    }

    public DodgeRequestPacket() {
    }

    public DodgeRequestPacket(FriendlyByteBuf buf) {
        // データなし
    }

    public static void encode(DodgeRequestPacket msg, FriendlyByteBuf buf) {
        // データなし
    }

    public static void handle(DodgeRequestPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                DodgeAndBattouHandler.performDodge(player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}

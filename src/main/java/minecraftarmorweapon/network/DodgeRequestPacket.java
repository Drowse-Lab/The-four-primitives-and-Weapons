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
 * クライアントのWASD入力をサーバーに送信して回避を実行するパケット。
 * player.xxa / player.zza はサーバーに同期されないため、
 * クライアント側の値をパケットで送る必要がある。
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class DodgeRequestPacket {

    private final float forward; // player.zza (W/S)
    private final float strafe;  // player.xxa (A/D)

    @SubscribeEvent
    public static void registerMessage(FMLCommonSetupEvent event) {
        MinecraftArmorWeaponMod.addNetworkMessage(DodgeRequestPacket.class,
                DodgeRequestPacket::encode, DodgeRequestPacket::new, DodgeRequestPacket::handle);
    }

    public DodgeRequestPacket(float forward, float strafe) {
        this.forward = forward;
        this.strafe = strafe;
    }

    public DodgeRequestPacket(FriendlyByteBuf buf) {
        this.forward = buf.readFloat();
        this.strafe = buf.readFloat();
    }

    public static void encode(DodgeRequestPacket msg, FriendlyByteBuf buf) {
        buf.writeFloat(msg.forward);
        buf.writeFloat(msg.strafe);
    }

    public static void handle(DodgeRequestPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                // クライアントのWASD入力をサーバープレイヤーに反映
                player.zza = msg.forward;
                player.xxa = msg.strafe;
                DodgeAndBattouHandler.performDodge(player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}

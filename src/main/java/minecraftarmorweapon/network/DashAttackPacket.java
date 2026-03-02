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
 * 回避中に攻撃ボタンを押した時にサーバーへ送信するパケット。
 * サーバー側でダッシュ攻撃（突進斬り等）を実行する。
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class DashAttackPacket {

    public DashAttackPacket() {
    }

    public DashAttackPacket(FriendlyByteBuf buf) {
    }

    public static void encode(DashAttackPacket msg, FriendlyByteBuf buf) {
    }

    public static void handle(DashAttackPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                DodgeAndBattouHandler.performDashAttack(player);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    @SubscribeEvent
    public static void registerMessage(FMLCommonSetupEvent event) {
        MinecraftArmorWeaponMod.addNetworkMessage(DashAttackPacket.class,
                DashAttackPacket::encode, DashAttackPacket::new, DashAttackPacket::handle);
    }
}

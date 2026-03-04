package minecraftarmorweapon.network;

import minecraftarmorweapon.util.CuriosScabbardHelper;
import minecraftarmorweapon.MinecraftArmorWeaponMod;

import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;

import java.util.function.Supplier;

/**
 * クライアント→サーバー: Curiosスロットの鞘から抜刀するパケット
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class BattouFromCurioPacket {

    public BattouFromCurioPacket() {
    }

    public BattouFromCurioPacket(FriendlyByteBuf buffer) {
    }

    public static void buffer(BattouFromCurioPacket message, FriendlyByteBuf buffer) {
    }

    public static void handler(BattouFromCurioPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            Player player = context.getSender();
            if (player == null) return;
            if (!player.level().hasChunkAt(player.blockPosition())) return;

            // メインハンドが空なら抜刀
            if (player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) {
                CuriosScabbardHelper.battouFromCurioSlot(player, InteractionHand.MAIN_HAND);
            }
        });
        context.setPacketHandled(true);
    }

    @SubscribeEvent
    public static void registerMessage(FMLCommonSetupEvent event) {
        MinecraftArmorWeaponMod.addNetworkMessage(
            BattouFromCurioPacket.class,
            BattouFromCurioPacket::buffer,
            BattouFromCurioPacket::new,
            BattouFromCurioPacket::handler
        );
    }
}

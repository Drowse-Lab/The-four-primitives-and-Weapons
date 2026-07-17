package the_four_primitives_and_weapons.network;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.util.CuriosHandsHelper;

import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

/**
 * クライアント→サーバー: インベントリ画面の防具横に表示している手袋スロット
 * ( Curios hands ) をクリックしたときの装備/取り外し/入れ替え要求。
 *
 * <p>カーソルの持ち物 ( carried ) と hands スロットの中身を入れ替える。
 * カーソルが空ならスロットから取り出し、 hands 装備可能アイテムを持っていれば装備する。
 * carried の同期はバニラの {@code broadcastChanges}、 スロットの同期は Curios が行う。</p>
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class HandsSlotClickPacket {

    public HandsSlotClickPacket() {
    }

    public HandsSlotClickPacket(FriendlyByteBuf buffer) {
    }

    public static void buffer(HandsSlotClickPacket message, FriendlyByteBuf buffer) {
    }

    public static void handler(HandsSlotClickPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            // 自分のインベントリ画面を開いているときのみ ( チェスト等では carried の意味が変わる )
            if (player.containerMenu != player.inventoryMenu) return;

            ItemStack carried = player.containerMenu.getCarried();
            ItemStack inSlot = CuriosHandsHelper.getSlotStack(player);

            if (carried.isEmpty()) {
                if (inSlot.isEmpty()) return;
                // 取り外し: スロット → カーソル
                CuriosHandsHelper.setSlotStack(player, ItemStack.EMPTY);
                player.containerMenu.setCarried(inSlot);
            } else {
                if (!CuriosHandsHelper.isHandsEquippable(carried)) return;
                // 装備 / 入れ替え: カーソル → スロット、 元の中身 → カーソル
                CuriosHandsHelper.setSlotStack(player, carried);
                player.containerMenu.setCarried(inSlot);
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.PLAYERS, 1.0f, 1.0f);
            }
            player.containerMenu.broadcastChanges();
        });
        context.setPacketHandled(true);
    }

    @SubscribeEvent
    public static void registerMessage(FMLCommonSetupEvent event) {
        TheFourPrimitivesAndWeaponsMod.addNetworkMessage(
            HandsSlotClickPacket.class,
            HandsSlotClickPacket::buffer,
            HandsSlotClickPacket::new,
            HandsSlotClickPacket::handler
        );
    }
}

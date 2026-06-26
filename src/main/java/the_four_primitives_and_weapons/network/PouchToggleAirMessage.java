package the_four_primitives_and_weapons.network;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.item.MaterializedPouchItem;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.function.Supplier;

/**
 * 結晶ポーチの空きスロットを ( 空カーソルで ) クリックした時、 そのスロットの
 * 「空気置換」 フラグ ( {@link MaterializedPouchItem#toggleReplaceAir} ) を切り替える。
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class PouchToggleAirMessage {

    public final int pouchInvSlot;
    public final int slotIndex;

    public PouchToggleAirMessage(int pouchInvSlot, int slotIndex) {
        this.pouchInvSlot = pouchInvSlot;
        this.slotIndex = slotIndex;
    }

    public PouchToggleAirMessage(FriendlyByteBuf buf) {
        this.pouchInvSlot = buf.readInt();
        this.slotIndex = buf.readInt();
    }

    public static void buffer(PouchToggleAirMessage m, FriendlyByteBuf buf) {
        buf.writeInt(m.pouchInvSlot);
        buf.writeInt(m.slotIndex);
    }

    public static void handler(PouchToggleAirMessage m, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            if (m.slotIndex < 0 || m.slotIndex >= MaterializedPouchItem.SLOTS) return;

            ItemStack pouch = ItemStack.EMPTY;
            if (m.pouchInvSlot >= 0 && m.pouchInvSlot < player.getInventory().getContainerSize()
                    && player.getInventory().getItem(m.pouchInvSlot).getItem() instanceof MaterializedPouchItem) {
                pouch = player.getInventory().getItem(m.pouchInvSlot);
            } else {
                pouch = MaterializedPouchItem.findFirst(player);
            }
            if (pouch.isEmpty()) return;

            MaterializedPouchItem.toggleReplaceAir(pouch, m.slotIndex);
            // ポーチ NBT 変更をクライアントへ同期 ( 白ハイライト更新用 )
            player.inventoryMenu.broadcastChanges();
        });
        ctx.setPacketHandled(true);
    }

    @SubscribeEvent
    public static void registerMessage(FMLCommonSetupEvent event) {
        TheFourPrimitivesAndWeaponsMod.addNetworkMessage(
            PouchToggleAirMessage.class, PouchToggleAirMessage::buffer, PouchToggleAirMessage::new, PouchToggleAirMessage::handler);
    }
}

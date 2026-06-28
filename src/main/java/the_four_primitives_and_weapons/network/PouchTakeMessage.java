package the_four_primitives_and_weapons.network;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.item.MaterializedPouchItem;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 結晶ポーチのスロットから 1 個取り出して「プレイヤーのインベントリへ直接追加」 する。
 * カーソル ( carried ) を経由しないので、 クリエイティブでの複製を防ぐ ( サーバー権威 )。
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class PouchTakeMessage {

	public final int pouchInvSlot;
	public final int slotIndex;

	public PouchTakeMessage(int pouchInvSlot, int slotIndex) {
		this.pouchInvSlot = pouchInvSlot;
		this.slotIndex = slotIndex;
	}

	public PouchTakeMessage(FriendlyByteBuf buf) {
		this.pouchInvSlot = buf.readInt();
		this.slotIndex = buf.readInt();
	}

	public static void buffer(PouchTakeMessage m, FriendlyByteBuf buf) {
		buf.writeInt(m.pouchInvSlot);
		buf.writeInt(m.slotIndex);
	}

	public static void handler(PouchTakeMessage m, Supplier<NetworkEvent.Context> ctxSupplier) {
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

			ItemStack[] lo = MaterializedPouchItem.getLoadout(pouch);
			ItemStack item = lo[m.slotIndex];
			if (item.isEmpty()) return;

			lo[m.slotIndex] = ItemStack.EMPTY;
			MaterializedPouchItem.setLoadout(pouch, lo);

			ItemStack give = item.copy();
			if (!player.getInventory().add(give)) player.drop(give, false);
			player.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8f, 1.1f);
		});
		ctx.setPacketHandled(true);
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		TheFourPrimitivesAndWeaponsMod.addNetworkMessage(
				PouchTakeMessage.class, PouchTakeMessage::buffer, PouchTakeMessage::new, PouchTakeMessage::handler);
	}
}

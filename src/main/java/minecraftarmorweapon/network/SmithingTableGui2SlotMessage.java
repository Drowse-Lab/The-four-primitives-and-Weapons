
package minecraftarmorweapon.network;

import minecraftarmorweapon.util.VersionHelper;

import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;

import minecraftarmorweapon.world.inventory.SmithingTableGui2Menu;

import minecraftarmorweapon.procedures.Smithingtablegui2naiyoukawattaProcedure;

import minecraftarmorweapon.MinecraftArmorWeaponMod;

import java.util.function.Supplier;
import java.util.HashMap;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class SmithingTableGui2SlotMessage {
	private final int slotID, x, y, z, changeType, meta;

	public SmithingTableGui2SlotMessage(int slotID, int x, int y, int z, int changeType, int meta) {
		this.slotID = slotID;
		this.x = x;
		this.y = y;
		this.z = z;
		this.changeType = changeType;
		this.meta = meta;
	}

	public SmithingTableGui2SlotMessage(FriendlyByteBuf buffer) {
		this.slotID = buffer.readInt();
		this.x = buffer.readInt();
		this.y = buffer.readInt();
		this.z = buffer.readInt();
		this.changeType = buffer.readInt();
		this.meta = buffer.readInt();
	}

	public static void buffer(SmithingTableGui2SlotMessage message, FriendlyByteBuf buffer) {
		buffer.writeInt(message.slotID);
		buffer.writeInt(message.x);
		buffer.writeInt(message.y);
		buffer.writeInt(message.z);
		buffer.writeInt(message.changeType);
		buffer.writeInt(message.meta);
	}

	public static void handler(SmithingTableGui2SlotMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			Player entity = context.getSender();
			int slotID = message.slotID;
			int changeType = message.changeType;
			int meta = message.meta;
			int x = message.x;
			int y = message.y;
			int z = message.z;
			handleSlotAction(entity, slotID, changeType, meta, x, y, z);
		});
		context.setPacketHandled(true);
	}

	public static void handleSlotAction(Player entity, int slotID, int changeType, int meta, int x, int y, int z) {
		Level world = VersionHelper.getLevel(entity);
		HashMap guistate = SmithingTableGui2Menu.guistate;
		// security measure to prevent arbitrary chunk generation
		if (!world.hasChunkAt(new BlockPos((int) (x), (int) (y), (int) (z))))
			return;
		if (slotID == 0 && changeType == 1) {

			Smithingtablegui2naiyoukawattaProcedure.execute(entity);
		}
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		MinecraftArmorWeaponMod.addNetworkMessage(SmithingTableGui2SlotMessage.class, SmithingTableGui2SlotMessage::buffer, SmithingTableGui2SlotMessage::new, SmithingTableGui2SlotMessage::handler);
	}
}

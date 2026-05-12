
package the_four_primitives_and_weapons.network;

import the_four_primitives_and_weapons.util.VersionHelper;

import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;

import the_four_primitives_and_weapons.world.inventory.RpgBookGuiMenu;

import the_four_primitives_and_weapons.procedures.RpgBookGuiVampireProcedure;
import the_four_primitives_and_weapons.procedures.RpgBookGuiToziruProcedure;
import the_four_primitives_and_weapons.procedures.RpgBookGuiNinjaTapProcedure;
import the_four_primitives_and_weapons.procedures.RpgBookGuiNiguTapProcedure;
import the_four_primitives_and_weapons.procedures.RpgBookGuiMagicSwordsmanTapProcedure;
import the_four_primitives_and_weapons.procedures.RpgBookGuiChuzumeProcedure;
import the_four_primitives_and_weapons.procedures.RpgBookGuiBoggedOuterTapProcedure;
import the_four_primitives_and_weapons.procedures.RogBookGuiKakusiTapProcedure;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;

import java.util.function.Supplier;
import java.util.HashMap;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class RpgBookGuiButtonMessage {
	private final int buttonID, x, y, z;

	public RpgBookGuiButtonMessage(FriendlyByteBuf buffer) {
		this.buttonID = buffer.readInt();
		this.x = buffer.readInt();
		this.y = buffer.readInt();
		this.z = buffer.readInt();
	}

	public RpgBookGuiButtonMessage(int buttonID, int x, int y, int z) {
		this.buttonID = buttonID;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public static void buffer(RpgBookGuiButtonMessage message, FriendlyByteBuf buffer) {
		buffer.writeInt(message.buttonID);
		buffer.writeInt(message.x);
		buffer.writeInt(message.y);
		buffer.writeInt(message.z);
	}

	public static void handler(RpgBookGuiButtonMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			Player entity = context.getSender();
			int buttonID = message.buttonID;
			int x = message.x;
			int y = message.y;
			int z = message.z;
			handleButtonAction(entity, buttonID, x, y, z);
		});
		context.setPacketHandled(true);
	}

	public static void handleButtonAction(Player entity, int buttonID, int x, int y, int z) {
		Level world = VersionHelper.getLevel(entity);
		HashMap guistate = RpgBookGuiMenu.guistate;
		// security measure to prevent arbitrary chunk generation
		if (!world.hasChunkAt(new BlockPos((int) (x), (int) (y), (int) (z))))
			return;
		if (buttonID == 0) {

			RpgBookGuiBoggedOuterTapProcedure.execute(entity);
		}
		if (buttonID == 1) {

			RpgBookGuiMagicSwordsmanTapProcedure.execute(entity);
		}
		if (buttonID == 2) {

			RpgBookGuiNinjaTapProcedure.execute(entity);
		}
		if (buttonID == 3) {

			RpgBookGuiVampireProcedure.execute(entity);
		}
		if (buttonID == 4) {

			RpgBookGuiNiguTapProcedure.execute(entity);
		}
		if (buttonID == 5) {

			RpgBookGuiChuzumeProcedure.execute(entity);
		}
		if (buttonID == 6) {

			RpgBookGuiToziruProcedure.execute(entity);
		}
		if (buttonID == 7) {

			RogBookGuiKakusiTapProcedure.execute(entity);
		}
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		TheFourPrimitivesAndWeaponsMod.addNetworkMessage(RpgBookGuiButtonMessage.class, RpgBookGuiButtonMessage::buffer, RpgBookGuiButtonMessage::new, RpgBookGuiButtonMessage::handler);
	}
}

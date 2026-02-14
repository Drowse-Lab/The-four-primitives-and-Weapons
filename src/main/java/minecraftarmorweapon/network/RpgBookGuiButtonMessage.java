
package minecraftarmorweapon.network;

import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import net.minecraft.world.level().Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;

import minecraftarmorweapon.world.inventory.RpgBookGuiMenu;

import minecraftarmorweapon.procedures.RpgBookGuiVampireProcedure;
import minecraftarmorweapon.procedures.RpgBookGuiToziruProcedure;
import minecraftarmorweapon.procedures.RpgBookGuiNinjaTapProcedure;
import minecraftarmorweapon.procedures.RpgBookGuiNiguTapProcedure;
import minecraftarmorweapon.procedures.RpgBookGuiMagicSwordsmanTapProcedure;
import minecraftarmorweapon.procedures.RpgBookGuiChuzumeProcedure;
import minecraftarmorweapon.procedures.RpgBookGuiBoggedOuterTapProcedure;
import minecraftarmorweapon.procedures.RogBookGuiKakusiTapProcedure;

import minecraftarmorweapon.MinecraftArmorWeaponMod;

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
		Level world = entity.level();
		HashMap guistate = RpgBookGuiMenu.guistate;
		// security measure to prevent arbitrary chunk generation
		if (!world.hasChunkAt(new BlockPos(x, y, z)))
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
		MinecraftArmorWeaponMod.addNetworkMessage(RpgBookGuiButtonMessage.class, RpgBookGuiButtonMessage::buffer, RpgBookGuiButtonMessage::new, RpgBookGuiButtonMessage::handler);
	}
}


package minecraftarmorweapon.network;

import minecraftarmorweapon.util.VersionHelper;

import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.network.FriendlyByteBuf;

import minecraftarmorweapon.procedures.RkigaYasaretatokiProcedure;
import minecraftarmorweapon.events.DodgeAndBattouHandler;

import minecraftarmorweapon.MinecraftArmorWeaponMod;

import java.util.function.Supplier;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class RMessage {
	int type, pressedms;

	public RMessage(int type, int pressedms) {
		this.type = type;
		this.pressedms = pressedms;
	}

	public RMessage(FriendlyByteBuf buffer) {
		this.type = buffer.readInt();
		this.pressedms = buffer.readInt();
	}

	public static void buffer(RMessage message, FriendlyByteBuf buffer) {
		buffer.writeInt(message.type);
		buffer.writeInt(message.pressedms);
	}

	public static void handler(RMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			pressAction(context.getSender(), message.type, message.pressedms);
		});
		context.setPacketHandled(true);
	}

	public static void pressAction(Player entity, int type, int pressedms) {
		Level world = VersionHelper.getLevel(entity);
		double x = entity.getX();
		double y = entity.getY();
		double z = entity.getZ();
		// security measure to prevent arbitrary chunk generation
		if (!world.hasChunkAt(entity.blockPosition()))
			return;
		if (type == 0) {
			// 納刀チェック: 武器+鞘を持っていればRキーで納刀
			ItemStack mainHand = entity.getItemInHand(InteractionHand.MAIN_HAND);
			ItemStack offHand = entity.getItemInHand(InteractionHand.OFF_HAND);
			if (DodgeAndBattouHandler.isWeapon(mainHand) && DodgeAndBattouHandler.isSaya(offHand)) {
				DodgeAndBattouHandler.performSheathing(entity, mainHand, offHand,
						InteractionHand.MAIN_HAND, InteractionHand.OFF_HAND);
				return;
			} else if (DodgeAndBattouHandler.isWeapon(offHand) && DodgeAndBattouHandler.isSaya(mainHand)) {
				DodgeAndBattouHandler.performSheathing(entity, offHand, mainHand,
						InteractionHand.OFF_HAND, InteractionHand.MAIN_HAND);
				return;
			}

			RkigaYasaretatokiProcedure.execute(entity);
		}
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		MinecraftArmorWeaponMod.addNetworkMessage(RMessage.class, RMessage::buffer, RMessage::new, RMessage::handler);
	}
}


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
import minecraftarmorweapon.util.CuriosScabbardHelper;
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
			ItemStack mainHand = entity.getItemInHand(InteractionHand.MAIN_HAND);
			ItemStack offHand = entity.getItemInHand(InteractionHand.OFF_HAND);

			// Bluepurgeアイテムは納刀せずCoverUp処理へ直行
			boolean mainIsBluepurge = isBluepurge(mainHand);
			boolean offIsBluepurge = isBluepurge(offHand);

			// 納刀チェック: 武器+空の鞘を持っていればRキーで納刀（Bluepurgeは納刀しない、満杯鞘はスキップ）
			if (!mainIsBluepurge && DodgeAndBattouHandler.isWeapon(mainHand) && DodgeAndBattouHandler.isSaya(offHand)
					&& !CuriosScabbardHelper.hasStoredWeapon(offHand)) {
				DodgeAndBattouHandler.performSheathing(entity, mainHand, offHand,
						InteractionHand.MAIN_HAND, InteractionHand.OFF_HAND);
				return;
			} else if (!offIsBluepurge && DodgeAndBattouHandler.isWeapon(offHand) && DodgeAndBattouHandler.isSaya(mainHand)
					&& !CuriosScabbardHelper.hasStoredWeapon(mainHand)) {
				DodgeAndBattouHandler.performSheathing(entity, offHand, mainHand,
						InteractionHand.OFF_HAND, InteractionHand.MAIN_HAND);
				return;
			}

			// 手に鞘がない場合、Curiosスロットの鞘に納刀を試みる（Bluepurgeは納刀しない）
			if (!mainIsBluepurge && DodgeAndBattouHandler.isWeapon(mainHand)) {
				if (CuriosScabbardHelper.sheathIntoCurioSlot(entity, mainHand, InteractionHand.MAIN_HAND)) {
					return;
				}
			} else if (!offIsBluepurge && DodgeAndBattouHandler.isWeapon(offHand)) {
				if (CuriosScabbardHelper.sheathIntoCurioSlot(entity, offHand, InteractionHand.OFF_HAND)) {
					return;
				}
			}

			RkigaYasaretatokiProcedure.execute(entity);
		}
	}

	private static boolean isBluepurge(ItemStack stack) {
		if (stack.isEmpty()) return false;
		String name = stack.getItem().getClass().getSimpleName();
		return name.contains("Bluepurge");
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		MinecraftArmorWeaponMod.addNetworkMessage(RMessage.class, RMessage::buffer, RMessage::new, RMessage::handler);
	}
}

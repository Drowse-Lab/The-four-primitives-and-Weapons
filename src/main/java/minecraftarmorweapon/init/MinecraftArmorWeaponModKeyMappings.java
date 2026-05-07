
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package minecraftarmorweapon.init;

import org.lwjgl.glfw.GLFW;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;

import minecraftarmorweapon.network.RMessage;
import minecraftarmorweapon.events.DodgeAndBattouHandler;
import minecraftarmorweapon.client.WeaponWheelState;
import minecraftarmorweapon.client.BowSkillWheelState;

import minecraftarmorweapon.MinecraftArmorWeaponMod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = {Dist.CLIENT})
public class MinecraftArmorWeaponModKeyMappings {
	public static final KeyMapping R = new KeyMapping("key.minecraft_armor_weapon.r", GLFW.GLFW_KEY_R, "key.categories.misc") {
		private boolean isDownOld = false;

		@Override
		public void setDown(boolean isDown) {
			super.setDown(isDown);
			if (isDownOld != isDown) {
				net.minecraft.world.entity.player.Player player = Minecraft.getInstance().player;
				if (player == null) { isDownOld = isDown; return; }

				if (isDown) {
					// 押下時: 手に武器があれば従来の納刀処理
					ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
					ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);

					// 弓/クロスボウを持っている場合は弓スキル選択ホイールへ
					if (mainHand.getItem() instanceof net.minecraft.world.item.BowItem
						|| mainHand.getItem() instanceof net.minecraft.world.item.CrossbowItem) {
						if (BowSkillWheelState.openOnRKey()) {
							isDownOld = isDown;
							return;
						}
					}

					// 鞘（納刀済み）やBluepurgeはホイール対象なので「武器」として扱わない
					boolean mainIsRealWeapon = DodgeAndBattouHandler.isWeapon(mainHand)
						&& !DodgeAndBattouHandler.isSaya(mainHand)
						&& !isBluepurge(mainHand);
					boolean offIsRealWeapon = DodgeAndBattouHandler.isWeapon(offHand)
						&& !DodgeAndBattouHandler.isSaya(offHand)
						&& !isBluepurge(offHand);
					boolean hasWeaponInHand = mainIsRealWeapon || offIsRealWeapon;

					if (hasWeaponInHand) {
						// 納刀ホイールを試みる（2個以上の空鞘があればホイール表示）
						if (!WeaponWheelState.onRKeyPressedForSheathing(player)) {
							// 0-1個の空鞘 → 従来の納刀処理
							MinecraftArmorWeaponMod.PACKET_HANDLER.sendToServer(new RMessage(0, 0));
							RMessage.pressAction(player, 0, 0);
						}
					} else {
						// 手が空/鞘/Bluepurge → 抜刀/ホイールモード
						if (!WeaponWheelState.onRKeyPressed()) {
							// WeaponWheelStateが処理しなかった場合（CoverUp等）→ 従来処理
							MinecraftArmorWeaponMod.PACKET_HANDLER.sendToServer(new RMessage(0, 0));
							RMessage.pressAction(player, 0, 0);
						}
					}
				} else {
					// リリース時
					BowSkillWheelState.releaseOnRKey();
					WeaponWheelState.onRKeyReleased();
				}
			}
			isDownOld = isDown;
		}
	};

	private static boolean isBluepurge(ItemStack stack) {
		if (stack.isEmpty()) return false;
		String name = stack.getItem().getClass().getSimpleName();
		return name.contains("Bluepurge");
	}

	@SubscribeEvent
	public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
		event.register(R);
	}

	@Mod.EventBusSubscriber({Dist.CLIENT})
	public static class KeyEventListener {
		@SubscribeEvent
		public static void onClientTick(TickEvent.ClientTickEvent event) {
			if (Minecraft.getInstance().screen == null) {
				R.consumeClick();
			}
		}
	}
}

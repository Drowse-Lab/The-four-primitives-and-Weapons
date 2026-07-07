
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package the_four_primitives_and_weapons.init;

import org.lwjgl.glfw.GLFW;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;

import the_four_primitives_and_weapons.network.RMessage;
import the_four_primitives_and_weapons.events.DodgeAndBattouHandler;
import the_four_primitives_and_weapons.client.WeaponWheelState;
import the_four_primitives_and_weapons.client.BowSkillWheelState;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = {Dist.CLIENT})
public class TheFourPrimitivesAndWeaponsModKeyMappings {
	// デフォルトは R キー。 R 単押しでは何もせず ( Iron's Spellbooks 等にキーを譲る )、
	// 「R 押下中に左クリック」で 抜刀/納刀/各ホイール/具現化破壊 を発動する ( triggerRAction )。
	// R 離しで開いているホイールを確定。 ユーザーは Controls 設定で自由に変更可能。
	public static final KeyMapping R = new KeyMapping("key.the_four_primitives_and_weapons.r", GLFW.GLFW_KEY_R, "key.categories.misc") {
		private boolean isDownOld = false;

		@Override
		public void setDown(boolean isDown) {
			super.setDown(isDown);
			if (isDownOld != isDown) {
				if (isDown) {
					// R 単押しでは何もしない。実際の発動は R+左クリック ( triggerRAction )。
					comboTriggered = false;
				} else {
					// リリース時: 開いているホイールを確定/クローズ
					BowSkillWheelState.releaseOnRKey();
					WeaponWheelState.onRKeyReleased();
					comboTriggered = false;
				}
			}
			isDownOld = isDown;
		}
	};

	/** R 保持中に既に左クリックで発動済みか ( 1 回の保持で 1 度だけ発動させる )。 */
	private static boolean comboTriggered = false;

	/** R キーが ( 物理的に ) 押されているか。 リバインドにも追従。 */
	public static boolean isRHeld() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.getWindow() == null) return false;
		try {
			return InputConstants.isKeyDown(mc.getWindow().getWindow(), R.getKey().getValue());
		} catch (Throwable t) {
			return R.isDown();
		}
	}

	/**
	 * R 押下中に左クリックされた時に呼ぶ。 旧「R 単押し」の動作を文脈で振り分ける:
	 * 弓スキルホイール / 納刀ホイール(or 納刀) / 抜刀ホイール(or 抜刀・具現化破壊)。
	 */
	public static void triggerRAction() {
		net.minecraft.world.entity.player.Player player = Minecraft.getInstance().player;
		if (player == null) return;
		if (comboTriggered) return; // R 保持中は 1 度だけ
		comboTriggered = true;

		ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
		ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);

		// 弓/クロスボウ → 弓スキル選択ホイール
		if (mainHand.getItem() instanceof net.minecraft.world.item.BowItem
			|| mainHand.getItem() instanceof net.minecraft.world.item.CrossbowItem) {
			if (BowSkillWheelState.openOnRKey()) return;
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
			// 納刀ホイールを試みる (2個以上の空鞘があればホイール表示)
			if (!WeaponWheelState.onRKeyPressedForSheathing(player)) {
				// 0-1個の空鞘 → 従来の納刀処理 (サーバーのみで実行)
				TheFourPrimitivesAndWeaponsMod.PACKET_HANDLER.sendToServer(new RMessage(0, 0));
			}
		} else {
			// 手が空/鞘/Bluepurge → 抜刀/ホイールモード
			if (!WeaponWheelState.onRKeyPressed()) {
				// WeaponWheelState が処理しなかった場合 (CoverUp / 具現化破壊 等) → サーバーのみで実行
				TheFourPrimitivesAndWeaponsMod.PACKET_HANDLER.sendToServer(new RMessage(0, 0));
			}
		}
	}

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
			if (event.phase != TickEvent.Phase.END) return;
			if (Minecraft.getInstance().screen == null) {
				R.consumeClick();
			}
			// ホイール状態を毎 tick 更新 (R 物理状態のチェック・選択更新)
			WeaponWheelState.tick();
		}

		/**
		 * R 押下中の左クリック ( 攻撃 ) を横取りして 抜刀/納刀/ホイール を発動する。
		 * R を押していない通常時の攻撃には一切干渉しない。
		 */
		@SubscribeEvent
		public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
			if (!event.isAttack()) return;
			if (!isRHeld()) return;
			event.setSwingHand(false);
			event.setCanceled(true);
			triggerRAction();
		}
	}
}

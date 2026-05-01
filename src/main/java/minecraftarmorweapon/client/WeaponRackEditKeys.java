package minecraftarmorweapon.client;

import minecraftarmorweapon.MinecraftArmorWeaponMod;
import minecraftarmorweapon.entity.WeaponRackEntity;
import minecraftarmorweapon.network.WeaponRackEditPacket;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.lwjgl.glfw.GLFW;

/**
 * WeaponRackEntity をホットキー編集するためのクライアント側キーバインド。
 * シフトキーを押しながらラックを見ている時のみ動作:
 *   Q/E: X軸回転 ±5° (前後の傾き)
 *   A/D: Y軸回転 ±5° (左右の振り)
 *   Z/C: Z軸回転 ±5° (時計回り)
 *   X:   リセット (全ての追加回転を0に戻す)
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class WeaponRackEditKeys {

	private static final String CATEGORY = "key.categories.minecraft_armor_weapon.rack";
	private static final float STEP = 5f;

	public static final KeyMapping KEY_X_PLUS = new KeyMapping("key.minecraft_armor_weapon.rack_x_plus", GLFW.GLFW_KEY_E, CATEGORY);
	public static final KeyMapping KEY_X_MINUS = new KeyMapping("key.minecraft_armor_weapon.rack_x_minus", GLFW.GLFW_KEY_Q, CATEGORY);
	public static final KeyMapping KEY_Y_PLUS = new KeyMapping("key.minecraft_armor_weapon.rack_y_plus", GLFW.GLFW_KEY_D, CATEGORY);
	public static final KeyMapping KEY_Y_MINUS = new KeyMapping("key.minecraft_armor_weapon.rack_y_minus", GLFW.GLFW_KEY_A, CATEGORY);
	public static final KeyMapping KEY_Z_PLUS = new KeyMapping("key.minecraft_armor_weapon.rack_z_plus", GLFW.GLFW_KEY_C, CATEGORY);
	public static final KeyMapping KEY_Z_MINUS = new KeyMapping("key.minecraft_armor_weapon.rack_z_minus", GLFW.GLFW_KEY_Z, CATEGORY);
	public static final KeyMapping KEY_RESET = new KeyMapping("key.minecraft_armor_weapon.rack_reset", GLFW.GLFW_KEY_X, CATEGORY);

	@SubscribeEvent
	public static void register(RegisterKeyMappingsEvent event) {
		event.register(KEY_X_PLUS);
		event.register(KEY_X_MINUS);
		event.register(KEY_Y_PLUS);
		event.register(KEY_Y_MINUS);
		event.register(KEY_Z_PLUS);
		event.register(KEY_Z_MINUS);
		event.register(KEY_RESET);
	}

	@Mod.EventBusSubscriber(value = Dist.CLIENT)
	public static class TickHandler {
		@SubscribeEvent
		public static void onClientTick(TickEvent.ClientTickEvent event) {
			if (event.phase != TickEvent.Phase.END) return;
			Minecraft mc = Minecraft.getInstance();
			if (mc.screen != null || mc.player == null) return;
			Player player = mc.player;
			// シフト押下中のみ反応 (通常の Q/E/A/D 移動と衝突しないように)
			if (!player.isShiftKeyDown()) return;

			WeaponRackEntity rack = lookingAtRack(mc);
			if (rack == null) return;

			int id = rack.getId();
			while (KEY_X_PLUS.consumeClick())  send(id, 0, +STEP);
			while (KEY_X_MINUS.consumeClick()) send(id, 0, -STEP);
			while (KEY_Y_PLUS.consumeClick())  send(id, 1, +STEP);
			while (KEY_Y_MINUS.consumeClick()) send(id, 1, -STEP);
			while (KEY_Z_PLUS.consumeClick())  send(id, 2, +STEP);
			while (KEY_Z_MINUS.consumeClick()) send(id, 2, -STEP);
			while (KEY_RESET.consumeClick())   send(id, 3, 0f);
		}

		private static WeaponRackEntity lookingAtRack(Minecraft mc) {
			HitResult hit = mc.hitResult;
			if (hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof WeaponRackEntity rack) {
				return rack;
			}
			return null;
		}

		private static void send(int entityId, int axis, float delta) {
			MinecraftArmorWeaponMod.PACKET_HANDLER.sendToServer(new WeaponRackEditPacket(entityId, axis, delta));
		}
	}
}

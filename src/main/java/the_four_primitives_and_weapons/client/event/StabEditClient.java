package the_four_primitives_and_weapons.client.event;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.entity.StabbedWeaponEntity;
import the_four_primitives_and_weapons.init.KnifeExtrasRegistrar;
import the_four_primitives_and_weapons.network.StabEditMessage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 突き刺さった武器/杭を、 杭 ( 編集ツール ) を持って見ながらスクロールで微調整する。
 *   スクロール          → 向き ( 15° )
 *   Shift + スクロール   → 傾き ( 5° )
 *   Ctrl + スクロール    → 高さ ( 0.1 )
 */
@Mod.EventBusSubscriber(modid = "the_four_primitives_and_weapons", value = Dist.CLIENT)
public class StabEditClient {

	@SubscribeEvent
	public static void onScroll(InputEvent.MouseScrollingEvent event) {
		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		if (player == null) return;

		// 杭 ( 編集ツール ) を持っている時だけ
		Object stake = KnifeExtrasRegistrar.BATTLE_STAKE.get();
		if (player.getMainHandItem().getItem() != stake && player.getOffhandItem().getItem() != stake) return;

		// 視線先の武器を OBB ( カプセル ) 精密判定で取得 ( 見た目通りに当たる )
		double reach = 6.0;
		Vec3 eye = player.getEyePosition(1.0f);
		Vec3 look = player.getViewVector(1.0f);
		AABB search = player.getBoundingBox().expandTowards(look.scale(reach)).inflate(2.0);
		StabbedWeaponEntity tgt = null;
		double bestT = Double.MAX_VALUE;
		for (StabbedWeaponEntity cand : player.level().getEntitiesOfClass(StabbedWeaponEntity.class, search)) {
			double t = cand.clipWeapon(eye, look, reach);
			if (t >= 0 && t < bestT) { bestT = t; tgt = cand; }
		}
		if (tgt == null) return;

		double scroll = event.getScrollDelta();
		if (scroll == 0) return;
		int dir = scroll > 0 ? 1 : -1;

		// ギズモのリング色に対応: 無印=Y緑(向き) / Shift=X赤(傾き) / Ctrl=Z青(ロール) / Alt=黄(高さ)
		//   Shift+Ctrl = 大きさ ( スケール ) — 単独修飾より先に判定する
		int mode;
		float delta;
		String label;
		if (Screen.hasShiftDown() && Screen.hasControlDown()) {
			mode = 5; delta = dir * 0.1f; label = "§b 大きさ " + (dir > 0 ? "+" : "-");
		} else if (Screen.hasShiftDown()) {
			mode = 1; delta = dir * 5f;   label = "§c X軸 傾き " + (dir > 0 ? "+" : "-");
		} else if (Screen.hasControlDown()) {
			mode = 4; delta = dir * 5f;   label = "§9 Z軸 ロール " + (dir > 0 ? "+" : "-");
		} else if (Screen.hasAltDown()) {
			mode = 2; delta = dir * 0.1f; label = "§e 高さ " + (dir > 0 ? "↑" : "↓");
		} else {
			mode = 0; delta = dir * 15f;  label = "§a Y軸 向き " + (dir > 0 ? "→" : "←");
		}

		TheFourPrimitivesAndWeaponsMod.PACKET_HANDLER.sendToServer(new StabEditMessage(tgt.getId(), mode, delta));
		player.displayClientMessage(Component.literal(label
				+ " §7( 緑Y:向き / Shift赤X:傾き / Ctrl青Z:ロール / Alt:高さ / Shift+Ctrl:大きさ )"), true);
		event.setCanceled(true); // ホットバー切替を止める
	}
}

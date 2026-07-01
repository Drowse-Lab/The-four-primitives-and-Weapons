package the_four_primitives_and_weapons.client;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModItems;
import the_four_primitives_and_weapons.util.KatanaFittings;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 刀の 柄 ( tintindex 1 ) と 鍔 ( tintindex 2 ) を、 {@link KatanaFittings} に保存した色で着色する。
 * 未設定 ( 染色前 ) は白 ( = 無着色 ) を返してテクスチャそのまま。 まずは IRON_KATANA で試験。
 */
@Mod.EventBusSubscriber(modid = TheFourPrimitivesAndWeaponsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class KatanaColorClient {

	@SubscribeEvent
	public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
		event.register((stack, tintIndex) -> {
			// 色を設定した部位は モデル側でグレー版テクスチャに差し替わる ( iron_katana )。
			// そこへ 16進色を乗算tintするので、 任意の色 ( 黒含む ) が綺麗に乗る ( 軍服の染色と同じ )。
			switch (tintIndex) {
				case 1: return tint(KatanaFittings.tsukaRgb(stack));
				case 2: return tint(KatanaFittings.tsubaRgb(stack));
				case 3: return tint(KatanaFittings.kashiraRgb(stack));
				// 縁(fuchi=4)は無し
				default: return 0xFFFFFFFF;
			}
		},
				TheFourPrimitivesAndWeaponsModItems.IRON_KATANA.get(),
				TheFourPrimitivesAndWeaponsModItems.IRON_TYOKUTO.get(),
				TheFourPrimitivesAndWeaponsModItems.IRON_RAPIER.get());
	}

	/** 色を tint 値(0xFFRRGGBB)へ。 未設定は白 ( = 無着色 )。 */
	private static int tint(int rgb) {
		return (rgb < 0) ? 0xFFFFFFFF : (0xFF000000 | rgb);
	}
}

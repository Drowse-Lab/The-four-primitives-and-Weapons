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
			if (tintIndex == 1) {
				int c = KatanaFittings.tsukaRgb(stack);
				return (c < 0) ? 0xFFFFFFFF : (0xFF000000 | c);
			}
			if (tintIndex == 2) {
				int c = KatanaFittings.tsubaRgb(stack);
				return (c < 0) ? 0xFFFFFFFF : (0xFF000000 | c);
			}
			if (tintIndex == 3) {
				int c = KatanaFittings.kashiraRgb(stack);
				return (c < 0) ? 0xFFFFFFFF : (0xFF000000 | c);
			}
			if (tintIndex == 4) {
				int c = KatanaFittings.fuchiRgb(stack);
				return (c < 0) ? 0xFFFFFFFF : (0xFF000000 | c);
			}
			return 0xFFFFFFFF;
		}, TheFourPrimitivesAndWeaponsModItems.IRON_KATANA.get());
	}
}

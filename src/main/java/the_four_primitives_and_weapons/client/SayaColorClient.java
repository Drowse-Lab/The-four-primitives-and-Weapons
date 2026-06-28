package the_four_primitives_and_weapons.client;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModItems;
import the_four_primitives_and_weapons.util.SayaDesign;

import net.minecraft.world.item.ItemStack;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 鞘 ( saya ) の地色 ( 染色 ) を、 鞘本体 ( wrap ) の面の tintindex 0 に反映する。
 * 染色は作業台 ( {@link the_four_primitives_and_weapons.item.SayaDyeRecipe} ) で設定する。
 *
 * <p>鞘本体テクスチャは乗算tintで色が乗るよう明るいグレーに調整済み。 未染色のときは
 * 暗いグレーを返して元の色合い ( ほぼ黒 ) に戻し、 染色されたら染料色を鮮やかに乗せる。</p>
 */
@Mod.EventBusSubscriber(modid = TheFourPrimitivesAndWeaponsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class SayaColorClient {

	/** 未染色時の地色 ( 明るくしたテクスチャを元の暗さに戻すための乗算グレー )。 */
	private static final int UNDYED = 0xFF4A4A4A;

	@SubscribeEvent
	public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
		event.register((stack, tintIndex) -> tintIndex == 0 ? baseRgb(stack) : 0xFFFFFFFF,
				TheFourPrimitivesAndWeaponsModItems.SAYA.get(),
				TheFourPrimitivesAndWeaponsModItems.TYOKUTO_SAYA.get(),
				TheFourPrimitivesAndWeaponsModItems.SWORD_SAYA.get(),
				TheFourPrimitivesAndWeaponsModItems.RAPIER_SAYA.get());
	}

	/** 鞘本体の地色を 0xFFRRGGBB で返す。 未染色は暗いグレー。 */
	private static int baseRgb(ItemStack stack) {
		int rgb = SayaDesign.getBaseRgb(stack);
		return (rgb < 0) ? UNDYED : (0xFF000000 | rgb);
	}
}

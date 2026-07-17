package the_four_primitives_and_weapons.client;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.init.MeijiUniformRegistrar;
import the_four_primitives_and_weapons.item.GloveItem;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 手袋 ( GloveItem ) のインベントリアイコンに染色色 ( GloveColor ) を乗算 tint する。
 * 未染色時は白 ( = テクスチャそのまま )。
 */
@Mod.EventBusSubscriber(modid = TheFourPrimitivesAndWeaponsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class GloveColorClient {

	@SubscribeEvent
	public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
		event.register((stack, tintIndex) -> {
			if (tintIndex != 0) return 0xFFFFFFFF;
			int rgb = ((GloveItem) stack.getItem()).getColor(stack);
			return 0xFF000000 | rgb;
		},
				MeijiUniformRegistrar.GLOVES.get(),
				MeijiUniformRegistrar.LEATHER_GLOVES.get(),
				MeijiUniformRegistrar.ARCHER_GLOVE.get());
	}
}

package the_four_primitives_and_weapons;

import net.minecraft.world.item.Item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.item.ItemProperties;

import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModItems;

import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class TheFourPrimitivesAndWeaponsItemProrties {
	public static void addCustomItemProperties() {
		makeShield(TheFourPrimitivesAndWeaponsModItems.ACHROMATIC_SHIELD.get());
	}

	private static void makeShield(Item item) {
		ItemProperties.register(item, new ResourceLocation("blocking"), (p_174590_, p_174591_, p_174592_, p_174593_) -> {
			return p_174592_ != null && p_174592_.isUsingItem() && p_174592_.getUseItem() == p_174590_ ? 1.0F : 0.0F;
		});
	}
}

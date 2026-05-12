
package the_four_primitives_and_weapons.item;

import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.food.FoodProperties;

import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModTabs;

public class HarvestMoon2023929Item extends Item {
	public HarvestMoon2023929Item() {
		super(new Item.Properties().stacksTo(64).rarity(Rarity.COMMON).food((new FoodProperties.Builder()).nutrition(3).saturationMod(1f)

				.build()));
	}

	@Override
	public int getUseDuration(ItemStack itemstack) {
		return 5;
	}
}

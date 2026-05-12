
package the_four_primitives_and_weapons.item;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.Entity;

import the_four_primitives_and_weapons.procedures.AaaYoukuritukusitatokiProcedure;

import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModTabs;

public class AaaItem extends Item {
	public AaaItem() {
		super(new Item.Properties().stacksTo(64).rarity(Rarity.COMMON));
	}

	@Override
	public void inventoryTick(ItemStack itemstack, Level world, Entity entity, int slot, boolean selected) {
		super.inventoryTick(itemstack, world, entity, slot, selected);
		if (selected)
			AaaYoukuritukusitatokiProcedure.execute(entity);
	}
}


package the_four_primitives_and_weapons.item;

import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item;

import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModTabs;

public class ImitationItem extends Item {
	public ImitationItem() {
		super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
	}
}

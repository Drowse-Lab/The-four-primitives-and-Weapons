
package the_four_primitives_and_weapons.item;

import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTab;

public class SkinOfDragonItem extends Item {
	public SkinOfDragonItem() {
		super(new Item.Properties().stacksTo(64).rarity(Rarity.EPIC));
	}
}

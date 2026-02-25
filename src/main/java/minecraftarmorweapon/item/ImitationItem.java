
package minecraftarmorweapon.item;

import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item;

import minecraftarmorweapon.init.MinecraftArmorWeaponModTabs;

public class ImitationItem extends Item {
	public ImitationItem() {
		super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
	}
}

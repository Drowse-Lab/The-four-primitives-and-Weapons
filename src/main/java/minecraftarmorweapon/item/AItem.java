
package minecraftarmorweapon.item;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.Entity;

import minecraftarmorweapon.procedures.AaitemuwoShoudeChituteiruJiannoteitukuProcedure;

import minecraftarmorweapon.init.MinecraftArmorWeaponModTabs;

public class AItem extends Item {
	public AItem() {
		super(new Item.Properties().tab(MinecraftArmorWeaponModTabs.TAB_YOPKEINAMONO).stacksTo(1).rarity(Rarity.COMMON));
	}

	@Override
	public void inventoryTick(ItemStack itemstack, Level world, Entity entity, int slot, boolean selected) {
		super.inventoryTick(itemstack, world, entity, slot, selected);
		if (selected)
			AaitemuwoShoudeChituteiruJiannoteitukuProcedure.execute(world, entity.getX(), entity.getY(), entity.getZ(), entity);
	}
}

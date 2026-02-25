
package minecraftarmorweapon.item;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;

import minecraftarmorweapon.procedures.KabuseruturuwoShoudeChituteiruJiannoteitukuProcedure;
import minecraftarmorweapon.procedures.KabuserumobugaturudeGongJisaretatokiProcedure;

import minecraftarmorweapon.init.MinecraftArmorWeaponModTabs;

public class KabuseruItem extends SwordItem {
	public KabuseruItem() {
		super(new Tier() {
			public int getUses() {
				return 0;
			}

			public float getSpeed() {
				return 0f;
			}

			public float getAttackDamageBonus() {
				return -2f;
			}

			public int getLevel() {
				return 0;
			}

			public int getEnchantmentValue() {
				return 0;
			}

			public Ingredient getRepairIngredient() {
				return Ingredient.of();
			}
		}, 3, 96f, new Item.Properties());
	}

	@Override
	public boolean hurtEnemy(ItemStack itemstack, LivingEntity entity, LivingEntity sourceentity) {
		boolean retval = super.hurtEnemy(itemstack, entity, sourceentity);
		KabuserumobugaturudeGongJisaretatokiProcedure.execute(entity, sourceentity);
		return retval;
	}

	@Override
	public void inventoryTick(ItemStack itemstack, Level world, Entity entity, int slot, boolean selected) {
		super.inventoryTick(itemstack, world, entity, slot, selected);
		if (selected)
			KabuseruturuwoShoudeChituteiruJiannoteitukuProcedure.execute(entity);
	}
}

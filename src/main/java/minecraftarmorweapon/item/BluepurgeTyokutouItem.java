
package minecraftarmorweapon.item;

import net.minecraft.world.level().Level;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionHand;
import net.minecraft.nbt.CompoundTag;

import minecraftarmorweapon.init.MinecraftArmorWeaponModTabs;

public class BluepurgeTyokutouItem extends SwordItem {
	public BluepurgeTyokutouItem() {
		super(new Tier() {
			public int getUses() {
				return 0;
			}

			public float getSpeed() {
				return 4f;
			}

			public float getAttackDamageBonus() {
				return 6f;
			}

			public int getLevel() {
				return 1;
			}

			public int getEnchantmentValue() {
				return 2;
			}

			public Ingredient getRepairIngredient() {
				return Ingredient.of();
			}
		}, 3, -1.4f, new Item.Properties().tab(MinecraftArmorWeaponModTabs.TAB_WEAPON));
	}

	@Override
	public void inventoryTick(ItemStack itemstack, Level world, Entity entity, int slot, boolean selected) {
		super.inventoryTick(itemstack, world, entity, slot, selected);

		// State management for bluepurge animation
		CompoundTag tag = itemstack.getOrCreateTag();
		int state = tag.getInt("BluepurgeState"); // 0=normal, 1=disappeared, 2=disappearing, 3=reappearing
		int timer = tag.getInt("BluepurgeTimer");

		if (state == 2) { // Disappearing animation
			if (timer > 0) {
				tag.putInt("BluepurgeTimer", timer - 1);
			} else {
				// Remove the item from player's hand when animation completes
				if (entity instanceof Player player) {
					player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
				}
			}
		} else if (state == 3) { // Reappearing animation
			if (timer > 0) {
				tag.putInt("BluepurgeTimer", timer - 1);
			} else {
				// Clear all bluepurge NBT tags to return to normal state
				tag.remove("BluepurgeState");
				tag.remove("BluepurgeTimer");
			}
		}
	}

	@Override
	public boolean isFoil(ItemStack itemstack) {
		// Make item enchanted glint appear when glowing
		CompoundTag tag = itemstack.getOrCreateTag();

		// If no BluepurgeState tag exists, no glint effect
		if (!tag.contains("BluepurgeState")) {
			return false;
		}

		int state = tag.getInt("BluepurgeState");
		int timer = tag.getInt("BluepurgeTimer");
		// Show glint during light phase
		// State 2 (disappearing): glint when timer > 40 (bluepurgelight)
		// State 3 (reappearing): glint when timer <= 40 (bluepurgelight)
		return (state == 2 && timer > 40) || (state == 3 && timer <= 40 && timer > 0);
	}
}

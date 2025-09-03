
package minecraftarmorweapon.item;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;

import minecraftarmorweapon.procedures.PrototypeKatanaYoukuritukusitatokiProcedure;
import minecraftarmorweapon.procedures.IronKatanaturuwoShoudeChituteiruJiannoteitukuProcedure;

import minecraftarmorweapon.init.MinecraftArmorWeaponModTabs;

import java.util.List;

public class KatanaNiguHumerusItem extends SwordItem {
	public KatanaNiguHumerusItem() {
		super(new Tier() {
			public int getUses() {
				return 0;
			}

			public float getSpeed() {
				return 4f;
			}

			public float getAttackDamageBonus() {
				return 7f;
			}

			public int getLevel() {
				return 1;
			}

			public int getEnchantmentValue() {
				return 9;
			}

			public Ingredient getRepairIngredient() {
				return Ingredient.of();
			}
		}, 3, -1.4f, new Item.Properties().tab(MinecraftArmorWeaponModTabs.TAB_NIGU));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player entity, InteractionHand hand) {
		ItemStack itemstack = entity.getItemInHand(hand);
		
		// Initialize NBT if not present
		if (!itemstack.hasTag()) {
			itemstack.setTag(new CompoundTag());
		}
		
		// Toggle sheath state on right-click (if needed for sheath/unsheath action)
		// This is commented out as the procedure might handle the action instead
		// boolean currentSheathed = isSheathed(itemstack);
		// setSheathed(itemstack, !currentSheathed);
		
		InteractionResultHolder<ItemStack> ar = super.use(world, entity, hand);
		PrototypeKatanaYoukuritukusitatokiProcedure.execute(world, entity.getX(), entity.getY(), entity.getZ(), entity);
		return ar;
	}

	@Override
	public void appendHoverText(ItemStack itemstack, Level world, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(itemstack, world, list, flag);
		list.add(Component.literal("\u3053\u306E\u5200\u5B9F\u306F\u2026\u2026"));
		list.add(Component.literal("\u67C4\u306E\u90E8\u5206\u304C\u4E8C\u30B0\u69D8\u306E\u4E0A\u8155\u9AA8\u3067\u3067\u304D\u3066\u308B\u3093\u3060"));
	}

	@Override
	public void inventoryTick(ItemStack itemstack, Level world, Entity entity, int slot, boolean selected) {
		super.inventoryTick(itemstack, world, entity, slot, selected);
		if (selected) {
			// Initialize NBT if not present
			if (!itemstack.hasTag()) {
				itemstack.setTag(new CompoundTag());
			}
			CompoundTag nbt = itemstack.getTag();
			
			// Initialize sheath state
			if (!nbt.contains("IsSheathed")) {
				nbt.putBoolean("IsSheathed", false);
			}
			
			// Initialize and preserve the selected ability mode
			if (!nbt.contains("SelectedAbility")) {
				// Don't use default value 2, preserve current selection or use a neutral value
				nbt.putInt("SelectedAbility", -1);
			}
			
			IronKatanaturuwoShoudeChituteiruJiannoteitukuProcedure.execute(world, entity.getX(), entity.getY(), entity.getZ(), entity);
		}
	}
	
	// Method to check if the katana is sheathed
	public static boolean isSheathed(ItemStack itemstack) {
		if (itemstack.hasTag()) {
			CompoundTag nbt = itemstack.getTag();
			return nbt.getBoolean("IsSheathed");
		}
		return false;
	}
	
	// Method to set the sheathed state
	public static void setSheathed(ItemStack itemstack, boolean sheathed) {
		if (!itemstack.hasTag()) {
			itemstack.setTag(new CompoundTag());
		}
		CompoundTag nbt = itemstack.getTag();
		nbt.putBoolean("IsSheathed", sheathed);
	}
	
	// Method to get the selected ability
	public static int getSelectedAbility(ItemStack itemstack) {
		if (itemstack.hasTag()) {
			CompoundTag nbt = itemstack.getTag();
			return nbt.getInt("SelectedAbility");
		}
		return -1;
	}
	
	// Method to set the selected ability
	public static void setSelectedAbility(ItemStack itemstack, int ability) {
		if (!itemstack.hasTag()) {
			itemstack.setTag(new CompoundTag());
		}
		CompoundTag nbt = itemstack.getTag();
		nbt.putInt("SelectedAbility", ability);
	}
}

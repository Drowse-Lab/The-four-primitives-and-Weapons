
package the_four_primitives_and_weapons.item;

import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.api.distmarker.Dist;

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

import the_four_primitives_and_weapons.procedures.TyokutouProcedure;
import the_four_primitives_and_weapons.procedures.IronKatanaturuwoShoudeChituteiruJiannoteitukuProcedure;

import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModTabs;

import java.util.List;

public class KurikarakenItem extends SwordItem {
	public static final String WEAPON_TYPE_TAG = "WeaponType";
	public static final String MODEL_TAG = "KurikarakenModel";

	public KurikarakenItem() {
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
				return 9;
			}

			public Ingredient getRepairIngredient() {
				return Ingredient.of();
			}
		}, 3, -2.4f, new Item.Properties().fireResistant());
	}

//	@Override
//	public InteractionResultHolder<ItemStack> use(Level world, Player entity, InteractionHand hand) {
//		InteractionResultHolder<ItemStack> ar = super.use(world, entity, hand);
//		TyokutouProcedure.execute(world, entity.getX(), entity.getY(), entity.getZ(), entity);
//		return ar;
//	}

	@Override
	public void appendHoverText(ItemStack itemstack, Level world, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(itemstack, world, list, flag);
		list.add(Component.literal("\u00A78Sharp sword to cut off demons"));
		list.add(Component.literal("\u00A77Model: " + getModel(itemstack)
				+ " / Type: " + getWeaponType(itemstack)));
	}

	public static String getWeaponType(ItemStack stack) {
		if (stack.hasTag() && stack.getTag().contains(WEAPON_TYPE_TAG)) {
			String type = stack.getTag().getString(WEAPON_TYPE_TAG);
			if ("katana".equals(type) || "sword".equals(type) || "straight_sword".equals(type)) {
				return type;
			}
		}
		return "straight_sword";
	}

	public static String getModel(ItemStack stack) {
		if (stack.hasTag() && stack.getTag().contains(MODEL_TAG)) {
			String model = stack.getTag().getString(MODEL_TAG);
			if ("katana".equals(model) || "sword".equals(model) || "tyokuto".equals(model)) {
				return model;
			}
		}
		return "tyokuto";
	}

	@Override
	public void inventoryTick(ItemStack itemstack, Level world, Entity entity, int slot, boolean selected) {
		super.inventoryTick(itemstack, world, entity, slot, selected);
		if (selected)
			IronKatanaturuwoShoudeChituteiruJiannoteitukuProcedure.execute(world, entity.getX(), entity.getY(), entity.getZ(), entity);
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public boolean isFoil(ItemStack itemstack) {
		return true;
	}
	}


package the_four_primitives_and_weapons.item;

import top.theillusivec4.curios.api.type.capability.ICurioItem;
import top.theillusivec4.curios.api.SlotContext;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;

import the_four_primitives_and_weapons.procedures.WindStepYoukuritukusitatokiProcedure;

import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModTabs;

public class WindStepItem extends Item implements ICurioItem {
	public WindStepItem() {
		super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player entity, InteractionHand hand) {
		the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod.LOGGER.info("WindStepItem.use() called! World: {}, Hand: {}", world.isClientSide ? "CLIENT" : "SERVER", hand);

		InteractionResultHolder<ItemStack> ar = super.use(world, entity, hand);
		ItemStack itemstack = ar.getObject();
		double x = entity.getX();
		double y = entity.getY();
		double z = entity.getZ();

		the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod.LOGGER.info("WindStepItem: Calling WindStepYoukuritukusitatokiProcedure.execute()");
		WindStepYoukuritukusitatokiProcedure.execute(entity);

		return ar;
	}

	@Override
	public boolean canEquip(SlotContext slotContext, ItemStack stack) {
		return true;
	}

	@Override
	public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
		return true;
	}
}

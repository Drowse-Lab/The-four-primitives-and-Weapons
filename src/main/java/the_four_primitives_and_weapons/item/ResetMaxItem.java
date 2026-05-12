
package the_four_primitives_and_weapons.item;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;

import the_four_primitives_and_weapons.procedures.ResetMaxYoukuritukusitatokiProcedure;

import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModTabs;

public class ResetMaxItem extends Item {
	public ResetMaxItem() {
		super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
	}

	@Override
	public UseAnim getUseAnimation(ItemStack itemstack) {
		return UseAnim.SPEAR;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player entity, InteractionHand hand) {
		InteractionResultHolder<ItemStack> ar = super.use(world, entity, hand);
		ItemStack itemstack = ar.getObject();
		double x = entity.getX();
		double y = entity.getY();
		double z = entity.getZ();

		ResetMaxYoukuritukusitatokiProcedure.execute(world, x, y, z, entity, itemstack);
		return ar;
	}
}

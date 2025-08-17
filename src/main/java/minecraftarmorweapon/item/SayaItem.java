
package minecraftarmorweapon.item;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;

import minecraftarmorweapon.procedures.SayaRightclickedProcedure;

import minecraftarmorweapon.init.MinecraftArmorWeaponModTabs;

public class SayaItem extends Item {
	public SayaItem() {
		super(new Item.Properties().tab(MinecraftArmorWeaponModTabs.TAB_WEAPON).stacksTo(1).rarity(Rarity.UNCOMMON));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player entity, InteractionHand hand) {
		InteractionResultHolder<ItemStack> ar = super.use(world, entity, hand);
		ItemStack itemstack = ar.getObject();
		double x = entity.getX();
		double y = entity.getY();
		double z = entity.getZ();

		SayaRightclickedProcedure.execute(world, entity);
		return ar;
	}
}

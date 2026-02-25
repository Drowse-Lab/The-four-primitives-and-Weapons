
package minecraftarmorweapon.item;

import top.theillusivec4.curios.api.type.capability.ICurioItem;
import top.theillusivec4.curios.api.SlotContext;

import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;

import minecraftarmorweapon.procedures.NaihazunoRingWhileBaubleIsEquippedTickProcedure;

public class NaihazunoRingItem extends Item implements ICurioItem {
	public NaihazunoRingItem() {
		super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
	}

	@Override
	public UseAnim getUseAnimation(ItemStack itemstack) {
		return UseAnim.BLOCK;
	}

	@Override
	public void curioTick(SlotContext slotContext, ItemStack stack) {
		NaihazunoRingWhileBaubleIsEquippedTickProcedure.execute(slotContext.entity());
	}
}

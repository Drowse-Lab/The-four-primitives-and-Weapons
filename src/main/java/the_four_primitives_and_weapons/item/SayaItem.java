
package the_four_primitives_and_weapons.item;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;

import top.theillusivec4.curios.api.type.capability.ICurioItem;
import top.theillusivec4.curios.api.SlotContext;

import the_four_primitives_and_weapons.procedures.SayaRightclickedProcedure;

import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModTabs;

import java.util.List;

public class SayaItem extends Item implements ICurioItem {
	public SayaItem() {
		super(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player entity, InteractionHand hand) {
		InteractionResultHolder<ItemStack> ar = super.use(world, entity, hand);
		ItemStack itemstack = ar.getObject();
		double x = entity.getX();
		double y = entity.getY();
		double z = entity.getZ();

		SayaRightclickedProcedure.execute(world, entity, itemstack, hand);
		return ar;
	}

	@Override
	public void appendHoverText(ItemStack stack, Level world, List<Component> list, net.minecraft.world.item.TooltipFlag flag) {
		super.appendHoverText(stack, world, list, flag);

		if (stack.hasTag() && stack.getTag().contains("StoredKatana")) {
			ItemStack storedKatana = ItemStack.of(stack.getTag().getCompound("StoredKatana"));
			list.add(Component.literal("§7納刀中: §f" + storedKatana.getHoverName().getString()));
		} else {
			list.add(Component.literal("§7空の鞘"));
			list.add(Component.literal("§8Rキーで納刀"));
		}
	}

	@Override
	public boolean canEquip(SlotContext slotContext, ItemStack stack) {
		String id = slotContext.identifier();
		return id.equals("belt") || id.equals("back");
	}

	@Override
	public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
		return false;
	}
}


package the_four_primitives_and_weapons.item;

import top.theillusivec4.curios.api.type.capability.ICurioItem;
import top.theillusivec4.curios.api.SlotContext;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;

import the_four_primitives_and_weapons.procedures.DarknessaitemuwoShoudeChituteiruJiannoteitukuProcedure;
import the_four_primitives_and_weapons.procedures.DarknessYoukuritukusitatokiProcedure;

import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModTabs;

public class DarknessItem extends Item implements ICurioItem {
	public DarknessItem() {
		super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player entity, InteractionHand hand) {
		InteractionResultHolder<ItemStack> ar = super.use(world, entity, hand);
		ItemStack itemstack = ar.getObject();
		double x = entity.getX();
		double y = entity.getY();
		double z = entity.getZ();

		DarknessYoukuritukusitatokiProcedure.execute(world, entity, itemstack);
		return ar;
	}

	@Override
	public void inventoryTick(ItemStack itemstack, Level world, Entity entity, int slot, boolean selected) {
		super.inventoryTick(itemstack, world, entity, slot, selected);
		// MCreator が同じ呼び出しを 2 行生成していた (copy-paste バグ) → 1 回に集約。
		// curio で身に着けている場合と main hand で持っている場合の両方に対応するため、
		// selected フラグに依存せず常に呼び出す (procedure 内で持ち主判定済み)。
		DarknessaitemuwoShoudeChituteiruJiannoteitukuProcedure.execute(entity);
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

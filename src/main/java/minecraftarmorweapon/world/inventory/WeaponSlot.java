package minecraftarmorweapon.world.inventory;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 * 武器装備スロット。SwordItem系の武器（直刀・刀・剣）を配置可能、スタック数1。
 */
public class WeaponSlot extends SlotItemHandler {

    public WeaponSlot(IItemHandler handler, int index, int x, int y) {
        super(handler, index, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        if (stack.isEmpty()) return false;

        // SwordItem（直刀・刀・剣含む全武器）のみ許可
        if (!(stack.getItem() instanceof SwordItem)) return false;

        // 他のスロットに同じ武器クラスが既にある場合は不許可
        String weaponClass = stack.getItem().getClass().getSimpleName();
        IItemHandler handler = this.getItemHandler();
        for (int i = 0; i < handler.getSlots(); i++) {
            if (i == this.getSlotIndex()) continue;
            ItemStack existing = handler.getStackInSlot(i);
            if (!existing.isEmpty() && existing.getItem().getClass().getSimpleName().equals(weaponClass)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return 1;
    }
}

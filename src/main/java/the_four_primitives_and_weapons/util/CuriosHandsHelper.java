package the_four_primitives_and_weapons.util;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.function.Predicate;

/**
 * Curios「hands」スロット ( 手袋類 ) へのアクセスを提供するユーティリティ。
 */
public class CuriosHandsHelper {

    public static final String SLOT_ID = "hands";

    /**
     * hands スロットから条件に合うアイテムを探す。 無ければ {@link ItemStack#EMPTY}。
     */
    public static ItemStack find(LivingEntity entity, Predicate<ItemStack> predicate) {
        var ref = new Object() { ItemStack result = ItemStack.EMPTY; };
        CuriosApi.getCuriosHelper().getCuriosHandler(entity).ifPresent(handler -> {
            handler.getStacksHandler(SLOT_ID).ifPresent(stacksHandler -> {
                for (int i = 0; i < stacksHandler.getStacks().getSlots(); i++) {
                    ItemStack stack = stacksHandler.getStacks().getStackInSlot(i);
                    if (!stack.isEmpty() && predicate.test(stack)) {
                        ref.result = stack;
                        return;
                    }
                }
            });
        });
        return ref.result;
    }

    /** hands スロットに条件に合うアイテムを装備しているか。 */
    public static boolean isWearing(LivingEntity entity, Predicate<ItemStack> predicate) {
        return !find(entity, predicate).isEmpty();
    }
}

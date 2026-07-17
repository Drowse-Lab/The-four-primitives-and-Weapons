package the_four_primitives_and_weapons.util;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

import the_four_primitives_and_weapons.init.ElytraCuriosSlot;

/**
 * Curios「elytra」スロット内のエリトラへのアクセスを提供するユーティリティ。
 * (ElytraSlot mod 相当機能)
 */
public class CuriosElytraHelper {

    /**
     * Curios の elytra スロットから「飛行可能な」エリトラを探す。
     * 見つからなければ {@link ItemStack#EMPTY}。
     *
     * <p>判定は {@code ItemStack#canElytraFly} (Forge 拡張) なので、
     * バニラエリトラ以外でも canElytraFly を実装した MOD アイテムなら飛べる。</p>
     */
    public static ItemStack findFlyableElytra(LivingEntity entity) {
        var ref = new Object() { ItemStack result = ItemStack.EMPTY; };
        CuriosApi.getCuriosHelper().getCuriosHandler(entity).ifPresent(handler -> {
            handler.getStacksHandler(ElytraCuriosSlot.SLOT_ID).ifPresent(stacksHandler -> {
                for (int i = 0; i < stacksHandler.getStacks().getSlots(); i++) {
                    ItemStack stack = stacksHandler.getStacks().getStackInSlot(i);
                    if (!stack.isEmpty() && stack.canElytraFly(entity)) {
                        ref.result = stack;
                        return;
                    }
                }
            });
        });
        return ref.result;
    }

    /**
     * Curios の elytra スロットに入っているアイテム（飛行可否問わず、描画用）。
     * 見つからなければ {@link ItemStack#EMPTY}。
     */
    public static ItemStack findEquippedElytra(LivingEntity entity) {
        var ref = new Object() { ItemStack result = ItemStack.EMPTY; };
        CuriosApi.getCuriosHelper().getCuriosHandler(entity).ifPresent(handler -> {
            handler.getStacksHandler(ElytraCuriosSlot.SLOT_ID).ifPresent(stacksHandler -> {
                for (int i = 0; i < stacksHandler.getStacks().getSlots(); i++) {
                    ItemStack stack = stacksHandler.getStacks().getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        ref.result = stack;
                        return;
                    }
                }
            });
        });
        return ref.result;
    }
}

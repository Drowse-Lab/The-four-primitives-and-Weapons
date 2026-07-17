package the_four_primitives_and_weapons.util;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.function.Predicate;

/**
 * Curios「hands」スロット ( 手袋類 ) へのアクセスを提供するユーティリティ。
 */
public class CuriosHandsHelper {

    public static final String SLOT_ID = "hands";

    /** hands スロットに装備できるアイテムのタグ ( data/curios/tags/items/hands.json )。 */
    public static final TagKey<Item> HANDS_TAG =
            TagKey.create(Registries.ITEM, new ResourceLocation("curios", "hands"));

    /** hands スロットに装備できるアイテムか。 */
    public static boolean isHandsEquippable(ItemStack stack) {
        return !stack.isEmpty() && stack.is(HANDS_TAG);
    }

    /** hands スロット ( index 0 ) の中身。 無ければ {@link ItemStack#EMPTY}。 */
    public static ItemStack getSlotStack(LivingEntity entity) {
        var ref = new Object() { ItemStack result = ItemStack.EMPTY; };
        CuriosApi.getCuriosHelper().getCuriosHandler(entity).ifPresent(handler ->
                handler.getStacksHandler(SLOT_ID).ifPresent(stacksHandler -> {
                    if (stacksHandler.getStacks().getSlots() > 0) {
                        ref.result = stacksHandler.getStacks().getStackInSlot(0);
                    }
                }));
        return ref.result;
    }

    /** hands スロット ( index 0 ) に stack を入れる ( サーバー側で呼ぶ。 Curios が同期する )。 */
    public static void setSlotStack(LivingEntity entity, ItemStack stack) {
        CuriosApi.getCuriosHelper().getCuriosHandler(entity).ifPresent(handler ->
                handler.getStacksHandler(SLOT_ID).ifPresent(stacksHandler -> {
                    if (stacksHandler.getStacks().getSlots() > 0) {
                        stacksHandler.getStacks().setStackInSlot(0, stack);
                    }
                }));
    }

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

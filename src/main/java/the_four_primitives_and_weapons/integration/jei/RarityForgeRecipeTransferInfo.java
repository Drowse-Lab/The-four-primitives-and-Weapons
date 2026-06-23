package the_four_primitives_and_weapons.integration.jei;

import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferInfo;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.CraftingRecipe;

import the_four_primitives_and_weapons.init.RarityForgeRegistration;
import the_four_primitives_and_weapons.world.inventory.RarityForgeMenu;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI から RarityForgeMenu の 3×3 グリッドに crafting recipe を転送する。
 * これで JEI レシピ画面の「+」 ボタンが機能する。
 */
public class RarityForgeRecipeTransferInfo implements IRecipeTransferInfo<RarityForgeMenu, CraftingRecipe> {

    @Override
    public Class<? extends RarityForgeMenu> getContainerClass() {
        return RarityForgeMenu.class;
    }

    @Override
    public java.util.Optional<net.minecraft.world.inventory.MenuType<RarityForgeMenu>> getMenuType() {
        return java.util.Optional.of(RarityForgeRegistration.getMenuType());
    }

    @Override
    public RecipeType<CraftingRecipe> getRecipeType() {
        return RecipeTypes.CRAFTING;
    }

    @Override
    public boolean canHandle(RarityForgeMenu menu, CraftingRecipe recipe) {
        return true;
    }

    /** グリッドスロット ( 2-10 ) を recipe 入力スロットとして公開。 */
    @Override
    public List<Slot> getRecipeSlots(RarityForgeMenu menu, CraftingRecipe recipe) {
        List<Slot> slots = new ArrayList<>(RarityForgeMenu.GRID_SIZE);
        for (int i = 0; i < RarityForgeMenu.GRID_SIZE; i++) {
            slots.add(menu.getSlot(RarityForgeMenu.GRID_START + i));
        }
        return slots;
    }

    /** 結果スロットの後ろ ( = プレイヤーインベントリ + ホットバー ) を inventory として公開。 */
    @Override
    public List<Slot> getInventorySlots(RarityForgeMenu menu, CraftingRecipe recipe) {
        List<Slot> slots = new ArrayList<>();
        int start = RarityForgeMenu.CONTAINER_SLOTS; // = 12
        for (int i = start; i < menu.slots.size(); i++) {
            slots.add(menu.getSlot(i));
        }
        return slots;
    }
}

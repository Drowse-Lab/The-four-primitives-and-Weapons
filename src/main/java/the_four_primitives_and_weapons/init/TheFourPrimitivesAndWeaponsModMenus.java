
/*
 *	MCreator note: This file will be REGENERATED on each build.
 */
package the_four_primitives_and_weapons.init;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.common.extensions.IForgeMenuType;

import net.minecraft.world.inventory.MenuType;

import the_four_primitives_and_weapons.world.inventory.SmithingTableGui2Menu;
import the_four_primitives_and_weapons.world.inventory.QuestscreenMenu;
import the_four_primitives_and_weapons.world.inventory.ItemStandGuiMenu;
import the_four_primitives_and_weapons.world.inventory.CustomCrafterCraftingguiMenu;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;

public class TheFourPrimitivesAndWeaponsModMenus {
	public static final DeferredRegister<MenuType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.MENU_TYPES, TheFourPrimitivesAndWeaponsMod.MODID);
	public static final RegistryObject<MenuType<SmithingTableGui2Menu>> SMITHING_TABLE_GUI_2 = REGISTRY.register("smithing_table_gui_2", () -> IForgeMenuType.create(SmithingTableGui2Menu::new));
	public static final RegistryObject<MenuType<CustomCrafterCraftingguiMenu>> CUSTOM_CRAFTER_CRAFTINGGUI = REGISTRY.register("custom_crafter_craftinggui", () -> IForgeMenuType.create(CustomCrafterCraftingguiMenu::new));
	public static final RegistryObject<MenuType<QuestscreenMenu>> QUESTSCREEN = REGISTRY.register("questscreen", () -> IForgeMenuType.create(QuestscreenMenu::new));
	public static final RegistryObject<MenuType<ItemStandGuiMenu>> ITEM_STAND_GUI = REGISTRY.register("item_stand_gui", () -> IForgeMenuType.create(ItemStandGuiMenu::new));
}

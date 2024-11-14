
/*
 *	MCreator note: This file will be REGENERATED on each build.
 */
package minecraftarmorweapon.init;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.common.extensions.IForgeMenuType;

import net.minecraft.world.inventory.MenuType;

import minecraftarmorweapon.world.inventory.SmithingTableGui2Menu;
import minecraftarmorweapon.world.inventory.RpgBookGuiMenu;
import minecraftarmorweapon.world.inventory.QuestscreenMenu;
import minecraftarmorweapon.world.inventory.ItemStandGuiMenu;
import minecraftarmorweapon.world.inventory.CustomCrafterCraftingguiMenu;

import minecraftarmorweapon.MinecraftArmorWeaponMod;

public class MinecraftArmorWeaponModMenus {
	public static final DeferredRegister<MenuType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MinecraftArmorWeaponMod.MODID);
	public static final RegistryObject<MenuType<RpgBookGuiMenu>> RPG_BOOK_GUI = REGISTRY.register("rpg_book_gui", () -> IForgeMenuType.create(RpgBookGuiMenu::new));
	public static final RegistryObject<MenuType<SmithingTableGui2Menu>> SMITHING_TABLE_GUI_2 = REGISTRY.register("smithing_table_gui_2", () -> IForgeMenuType.create(SmithingTableGui2Menu::new));
	public static final RegistryObject<MenuType<CustomCrafterCraftingguiMenu>> CUSTOM_CRAFTER_CRAFTINGGUI = REGISTRY.register("custom_crafter_craftinggui", () -> IForgeMenuType.create(CustomCrafterCraftingguiMenu::new));
	public static final RegistryObject<MenuType<QuestscreenMenu>> QUESTSCREEN = REGISTRY.register("questscreen", () -> IForgeMenuType.create(QuestscreenMenu::new));
	public static final RegistryObject<MenuType<ItemStandGuiMenu>> ITEM_STAND_GUI = REGISTRY.register("item_stand_gui", () -> IForgeMenuType.create(ItemStandGuiMenu::new));
}

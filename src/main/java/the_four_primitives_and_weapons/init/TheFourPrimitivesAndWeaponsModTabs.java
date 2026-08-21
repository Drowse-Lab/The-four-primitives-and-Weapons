
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package the_four_primitives_and_weapons.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;

public class TheFourPrimitivesAndWeaponsModTabs {
	public static final DeferredRegister<CreativeModeTab> REGISTRY =
			DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TheFourPrimitivesAndWeaponsMod.MODID);

	public static final RegistryObject<CreativeModeTab> TAB_WEAPON = REGISTRY.register("weapon",
			() -> CreativeModeTab.builder()
					.title(Component.translatable("itemGroup.tabweapon"))
					.icon(() -> new ItemStack(TheFourPrimitivesAndWeaponsModItems.IRON_KATANA.get()))
					.displayItems((params, output) -> {})
					.build());

	public static final RegistryObject<CreativeModeTab> TAB_MAGIC_BOOKS = REGISTRY.register("magic_books",
			() -> CreativeModeTab.builder()
					.title(Component.translatable("itemGroup.tabmagic_books"))
					.icon(() -> new ItemStack(Items.WRITABLE_BOOK))
					.displayItems((params, output) -> {})
					.build());

	public static final RegistryObject<CreativeModeTab> TAB_ARMOR = REGISTRY.register("armor",
			() -> CreativeModeTab.builder()
					.title(Component.translatable("itemGroup.tabarmor"))
					.icon(() -> new ItemStack(Items.TURTLE_HELMET))
					.displayItems((params, output) -> {})
					.build());

	public static final RegistryObject<CreativeModeTab> TAB_YOPKEINAMONO = REGISTRY.register("yopkeinamono",
			() -> CreativeModeTab.builder()
					.title(Component.translatable("itemGroup.tabyopkeinamono"))
					.icon(() -> new ItemStack(TheFourPrimitivesAndWeaponsModBlocks.CROSS.get()))
					.displayItems((params, output) -> {})
					.build());

	public static final RegistryObject<CreativeModeTab> TAB_EVENT = REGISTRY.register("event",
			() -> CreativeModeTab.builder()
					.title(Component.translatable("itemGroup.tabevent"))
					.icon(() -> new ItemStack(TheFourPrimitivesAndWeaponsModItems.HARVEST_MOON_2023929.get()))
					.displayItems((params, output) -> {})
					.build());

	public static final RegistryObject<CreativeModeTab> TAB_DRAGON_ARMOR_TAB = REGISTRY.register("dragon_armor_tab",
			() -> CreativeModeTab.builder()
					.title(Component.translatable("itemGroup.tabdragon_armor_tab"))
					.icon(() -> new ItemStack(TheFourPrimitivesAndWeaponsModItems.DRAGON_ARMOR_HELMET.get()))
					.displayItems((params, output) -> {})
					.build());

	public static final RegistryObject<CreativeModeTab> TAB_NIGU = REGISTRY.register("nigu",
			() -> CreativeModeTab.builder()
					.title(Component.translatable("itemGroup.tabnigu"))
					.icon(() -> new ItemStack(TheFourPrimitivesAndWeaponsModItems.KATANA_NIGU_HUMERUS.get()))
					.displayItems((params, output) -> {})
					.build());

	// 鞘 ( saya ) 専用タブ: 各鞘・漆・仕立て済みの鞘をまとめる。 中身は CreativeTabPopulator で追加。
	public static final RegistryObject<CreativeModeTab> TAB_SAYA = REGISTRY.register("saya",
			() -> CreativeModeTab.builder()
					.title(Component.translatable("itemGroup.tabsaya"))
					.icon(() -> new ItemStack(TheFourPrimitivesAndWeaponsModItems.SAYA.get()))
					.displayItems((params, output) -> {})
					.build());
}


/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package minecraftarmorweapon.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import minecraftarmorweapon.MinecraftArmorWeaponMod;

public class MinecraftArmorWeaponModTabs {
	public static final DeferredRegister<CreativeModeTab> REGISTRY =
			DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MinecraftArmorWeaponMod.MODID);

	public static final RegistryObject<CreativeModeTab> TAB_WEAPON = REGISTRY.register("weapon",
			() -> CreativeModeTab.builder()
					.title(Component.translatable("itemGroup.tabweapon"))
					.icon(() -> new ItemStack(MinecraftArmorWeaponModItems.IRON_KATANA.get()))
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
					.icon(() -> new ItemStack(MinecraftArmorWeaponModBlocks.CROSS.get()))
					.displayItems((params, output) -> {})
					.build());

	public static final RegistryObject<CreativeModeTab> TAB_EVENT = REGISTRY.register("event",
			() -> CreativeModeTab.builder()
					.title(Component.translatable("itemGroup.tabevent"))
					.icon(() -> new ItemStack(MinecraftArmorWeaponModItems.HARVEST_MOON_2023929.get()))
					.displayItems((params, output) -> {})
					.build());

	public static final RegistryObject<CreativeModeTab> TAB_DRAGON_ARMOR_TAB = REGISTRY.register("dragon_armor_tab",
			() -> CreativeModeTab.builder()
					.title(Component.translatable("itemGroup.tabdragon_armor_tab"))
					.icon(() -> new ItemStack(MinecraftArmorWeaponModItems.DRAGON_ARMOR_HELMET.get()))
					.displayItems((params, output) -> {})
					.build());

	public static final RegistryObject<CreativeModeTab> TAB_NIGU = REGISTRY.register("nigu",
			() -> CreativeModeTab.builder()
					.title(Component.translatable("itemGroup.tabnigu"))
					.icon(() -> new ItemStack(MinecraftArmorWeaponModItems.KATANA_NIGU_HUMERUS.get()))
					.displayItems((params, output) -> {})
					.build());

	public static final RegistryObject<CreativeModeTab> TAB_CHUZUME_TAB = REGISTRY.register("chuzume_tab",
			() -> CreativeModeTab.builder()
					.title(Component.translatable("itemGroup.tabchuzume_tab"))
					.icon(() -> new ItemStack(MinecraftArmorWeaponModItems.SWORD_OF_NIGHT.get()))
					.displayItems((params, output) -> {})
					.build());
}

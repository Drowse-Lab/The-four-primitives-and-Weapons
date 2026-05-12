
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package the_four_primitives_and_weapons.init;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;

import net.minecraft.world.item.enchantment.Enchantment;

import the_four_primitives_and_weapons.enchantment.KillEnchantment;
import the_four_primitives_and_weapons.enchantment.DemonizedEnchantment;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;

public class TheFourPrimitivesAndWeaponsModEnchantments {
	public static final DeferredRegister<Enchantment> REGISTRY = DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, TheFourPrimitivesAndWeaponsMod.MODID);
	public static final RegistryObject<Enchantment> DEMONIZED = REGISTRY.register("demonized", () -> new DemonizedEnchantment());
	public static final RegistryObject<Enchantment> KILL = REGISTRY.register("kill", () -> new KillEnchantment());
}

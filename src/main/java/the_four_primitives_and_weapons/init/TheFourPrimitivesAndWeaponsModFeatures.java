
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package the_four_primitives_and_weapons.init;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.world.level.levelgen.feature.Feature;

import the_four_primitives_and_weapons.world.features.plants.RoseFeature;
import the_four_primitives_and_weapons.world.features.OutpostSiteFeature;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;

@Mod.EventBusSubscriber
public class TheFourPrimitivesAndWeaponsModFeatures {
	public static final DeferredRegister<Feature<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.FEATURES, TheFourPrimitivesAndWeaponsMod.MODID);
	public static final RegistryObject<Feature<?>> ROSE = REGISTRY.register("rose", RoseFeature::feature);
	public static final RegistryObject<Feature<?>> OUTPOST_SITE = REGISTRY.register("outpost_site", OutpostSiteFeature::feature);
}

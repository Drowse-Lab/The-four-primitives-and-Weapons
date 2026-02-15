
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package minecraftarmorweapon.init;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.world.level.levelgen.feature.Feature;

import minecraftarmorweapon.world.features.plants.RoseFeature;
import minecraftarmorweapon.world.features.OutpostSiteFeature;

import minecraftarmorweapon.MinecraftArmorWeaponMod;

@Mod.EventBusSubscriber
public class MinecraftArmorWeaponModFeatures {
	public static final DeferredRegister<Feature<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.FEATURES, MinecraftArmorWeaponMod.MODID);
	public static final RegistryObject<Feature<?>> ROSE = REGISTRY.register("rose", RoseFeature::feature);
	public static final RegistryObject<Feature<?>> OUTPOST_SITE = REGISTRY.register("outpost_site", OutpostSiteFeature::feature);
}

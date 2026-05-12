
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package the_four_primitives_and_weapons.init;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.Block;

import the_four_primitives_and_weapons.block.entity.ItemStanBlockEntity;
import the_four_primitives_and_weapons.block.entity.CustomCrafterCraftingBlockEntity;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;

public class TheFourPrimitivesAndWeaponsModBlockEntities {
	public static final DeferredRegister<BlockEntityType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, TheFourPrimitivesAndWeaponsMod.MODID);
	public static final RegistryObject<BlockEntityType<?>> CUSTOM_CRAFTER_CRAFTING = register("custom_crafter_crafting", TheFourPrimitivesAndWeaponsModBlocks.CUSTOM_CRAFTER_CRAFTING, CustomCrafterCraftingBlockEntity::new);
	public static final RegistryObject<BlockEntityType<?>> ITEM_STAN = register("item_stan", TheFourPrimitivesAndWeaponsModBlocks.ITEM_STAN, ItemStanBlockEntity::new);

	private static RegistryObject<BlockEntityType<?>> register(String registryname, RegistryObject<Block> block, BlockEntityType.BlockEntitySupplier<?> supplier) {
		return REGISTRY.register(registryname, () -> BlockEntityType.Builder.of(supplier, block.get()).build(null));
	}
}


/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package minecraftarmorweapon.init;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;

import net.minecraft.world.level.block.Block;

import minecraftarmorweapon.block.WitherSkeletonSpawnerBlock;
import minecraftarmorweapon.block.StoneKatanaBlockBlock;
import minecraftarmorweapon.block.StoneKatanaBlock1Block;
import minecraftarmorweapon.block.StoneBricksTrapDoorBlock;
import minecraftarmorweapon.block.RoseFlowerPotBlock;
import minecraftarmorweapon.block.RoseBlock;
import minecraftarmorweapon.block.MotoWitherKatanaBlockBlock;
import minecraftarmorweapon.block.MotoWitherKatanaBlock1Block;
import minecraftarmorweapon.block.MakiwaridaiBlock;
import minecraftarmorweapon.block.KurikarakenBlockBlock;
import minecraftarmorweapon.block.ItemStanBlock;
import minecraftarmorweapon.block.CustomSmithingTableBlock;
import minecraftarmorweapon.block.CustomCrafterCraftingBlock;
import minecraftarmorweapon.block.CrossBlock;
import minecraftarmorweapon.block.AlchemyCraftBlockBlock;

import minecraftarmorweapon.MinecraftArmorWeaponMod;

public class MinecraftArmorWeaponModBlocks {
	public static final DeferredRegister<Block> REGISTRY = DeferredRegister.create(ForgeRegistries.BLOCKS, MinecraftArmorWeaponMod.MODID);
	public static final RegistryObject<Block> ROSE = REGISTRY.register("rose", () -> new RoseBlock());
	public static final RegistryObject<Block> CROSS = REGISTRY.register("cross", () -> new CrossBlock());
	public static final RegistryObject<Block> STONE_BRICKS_TRAP_DOOR = REGISTRY.register("stone_bricks_trap_door", () -> new StoneBricksTrapDoorBlock());
	public static final RegistryObject<Block> ROSE_FLOWER_POT = REGISTRY.register("rose_flower_pot", () -> new RoseFlowerPotBlock());
	public static final RegistryObject<Block> KURIKARAKEN_BLOCK = REGISTRY.register("kurikaraken_block", () -> new KurikarakenBlockBlock());
	public static final RegistryObject<Block> STONE_KATANA_BLOCK = REGISTRY.register("stone_katana_block", () -> new StoneKatanaBlockBlock());
	public static final RegistryObject<Block> STONE_KATANA_BLOCK_1 = REGISTRY.register("stone_katana_block_1", () -> new StoneKatanaBlock1Block());
	public static final RegistryObject<Block> MAKIWARIDAI = REGISTRY.register("makiwaridai", () -> new MakiwaridaiBlock());
	public static final RegistryObject<Block> CUSTOM_SMITHING_TABLE = REGISTRY.register("custom_smithing_table", () -> new CustomSmithingTableBlock());
	public static final RegistryObject<Block> MOTO_WITHER_KATANA_BLOCK_1 = REGISTRY.register("moto_wither_katana_block_1", () -> new MotoWitherKatanaBlock1Block());
	public static final RegistryObject<Block> MOTO_WITHER_KATANA_BLOCK = REGISTRY.register("moto_wither_katana_block", () -> new MotoWitherKatanaBlockBlock());
	public static final RegistryObject<Block> WITHER_SKELETON_SPAWNER = REGISTRY.register("wither_skeleton_spawner", () -> new WitherSkeletonSpawnerBlock());
	public static final RegistryObject<Block> CUSTOM_CRAFTER_CRAFTING = REGISTRY.register("custom_crafter_crafting", () -> new CustomCrafterCraftingBlock());
	public static final RegistryObject<Block> ITEM_STAN = REGISTRY.register("item_stan", () -> new ItemStanBlock());
	public static final RegistryObject<Block> ALCHEMY_CRAFT_BLOCK = REGISTRY.register("alchemy_craft_block", () -> new AlchemyCraftBlockBlock());
}

package the_four_primitives_and_weapons.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.AbstractTreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;

/**
 * 漆の木 ( ブロック一式 )。 原木/皮剥ぎ/木/板材/葉/苗木。 自然生成はしないが 苗木は育つ
 * ( {@link #URUSHI_TREE} の configured_feature を参照 )。 生漆採取は どの原木でも可なので、
 * この木専用の採取ボーナス等は付けていない ( = 普通の原木と同じく火打石/ビンで採れる )。
 */
public final class UrushiWoodInit {

	private UrushiWoodInit() {}

	public static final DeferredRegister<Block> BLOCKS =
			DeferredRegister.create(ForgeRegistries.BLOCKS, TheFourPrimitivesAndWeaponsMod.MODID);
	public static final DeferredRegister<Item> ITEMS =
			DeferredRegister.create(ForgeRegistries.ITEMS, TheFourPrimitivesAndWeaponsMod.MODID);

	/** 苗木が育てる木 ( configured_feature: data/.../worldgen/configured_feature/urushi_tree.json )。 */
	public static final ResourceKey<ConfiguredFeature<?, ?>> URUSHI_TREE =
			ResourceKey.create(Registries.CONFIGURED_FEATURE,
					new ResourceLocation(TheFourPrimitivesAndWeaponsMod.MODID, "urushi_tree"));

	private static final class UrushiTreeGrower extends AbstractTreeGrower {
		@Override
		protected ResourceKey<ConfiguredFeature<?, ?>> getConfiguredFeature(RandomSource rand, boolean hasFlowers) {
			return URUSHI_TREE;
		}
	}

	// ===== ブロック =====
	public static final RegistryObject<Block> URUSHI_LOG = BLOCKS.register("urushi_log",
			() -> new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.OAK_LOG)));
	public static final RegistryObject<Block> STRIPPED_URUSHI_LOG = BLOCKS.register("stripped_urushi_log",
			() -> new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.STRIPPED_OAK_LOG)));
	public static final RegistryObject<Block> URUSHI_WOOD = BLOCKS.register("urushi_wood",
			() -> new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.OAK_WOOD)));
	public static final RegistryObject<Block> STRIPPED_URUSHI_WOOD = BLOCKS.register("stripped_urushi_wood",
			() -> new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.STRIPPED_OAK_WOOD)));
	public static final RegistryObject<Block> URUSHI_PLANKS = BLOCKS.register("urushi_planks",
			() -> new Block(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS)));
	public static final RegistryObject<Block> URUSHI_LEAVES = BLOCKS.register("urushi_leaves",
			() -> new LeavesBlock(BlockBehaviour.Properties.copy(Blocks.OAK_LEAVES)));
	public static final RegistryObject<Block> URUSHI_SAPLING = BLOCKS.register("urushi_sapling",
			() -> new SaplingBlock(new UrushiTreeGrower(), BlockBehaviour.Properties.copy(Blocks.OAK_SAPLING)));

	// ===== ブロックアイテム =====
	public static final RegistryObject<Item> URUSHI_LOG_ITEM = item("urushi_log", URUSHI_LOG);
	public static final RegistryObject<Item> STRIPPED_URUSHI_LOG_ITEM = item("stripped_urushi_log", STRIPPED_URUSHI_LOG);
	public static final RegistryObject<Item> URUSHI_WOOD_ITEM = item("urushi_wood", URUSHI_WOOD);
	public static final RegistryObject<Item> STRIPPED_URUSHI_WOOD_ITEM = item("stripped_urushi_wood", STRIPPED_URUSHI_WOOD);
	public static final RegistryObject<Item> URUSHI_PLANKS_ITEM = item("urushi_planks", URUSHI_PLANKS);
	public static final RegistryObject<Item> URUSHI_LEAVES_ITEM = item("urushi_leaves", URUSHI_LEAVES);
	public static final RegistryObject<Item> URUSHI_SAPLING_ITEM = item("urushi_sapling", URUSHI_SAPLING);

	private static RegistryObject<Item> item(String name, RegistryObject<Block> block) {
		return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
	}

	/** mod 本体から呼ぶ: ブロックとアイテムのレジストリをバスに登録。 */
	public static void register(net.minecraftforge.eventbus.api.IEventBus bus) {
		BLOCKS.register(bus);
		ITEMS.register(bus);
	}
}

package the_four_primitives_and_weapons.init;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.block.CorrosionCrystalBlock;

public final class BladeCrystalInit {
    /** 色追加・変更はこの16進RGB値だけを編集すればよい。 */
    public static final int VIOLET_HEX = 0xA855F7;
    public static final int CYAN_HEX = 0x22D3EE;
    public static final int AMBER_HEX = 0xF59E0B;
    public static final int CRIMSON_HEX = 0xDC264C;
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, TheFourPrimitivesAndWeaponsMod.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, TheFourPrimitivesAndWeaponsMod.MODID);
    public static final RegistryObject<Block> VIOLET = crystal("violet_corrosion_crystal");
    public static final RegistryObject<Block> CYAN = crystal("cyan_corrosion_crystal");
    public static final RegistryObject<Block> AMBER = crystal("amber_corrosion_crystal");
    public static final RegistryObject<Block> CRIMSON = crystal("crimson_corrosion_crystal");
    public static final RegistryObject<Block> CORRODED_EARTH = ground("crystal_corroded_earth", Blocks.COARSE_DIRT);
    public static final RegistryObject<Block> BLOOD_SOAKED_EARTH = ground("blood_soaked_earth", Blocks.MUD);
    public static final RegistryObject<Block> COAGULATED_BLOOD = ground("coagulated_blood", Blocks.HONEY_BLOCK);
    public static final RegistryObject<Item> VIOLET_SHARD = shard("violet_corrosion_crystal_shard");
    public static final RegistryObject<Item> CYAN_SHARD = shard("cyan_corrosion_crystal_shard");
    public static final RegistryObject<Item> AMBER_SHARD = shard("amber_corrosion_crystal_shard");
    public static final RegistryObject<Item> CRIMSON_SHARD = shard("crimson_corrosion_crystal_shard");

    private static RegistryObject<Block> crystal(String name) {
        RegistryObject<Block> block = BLOCKS.register(name, () -> new CorrosionCrystalBlock(
                BlockBehaviour.Properties.copy(Blocks.WHITE_STAINED_GLASS)
                        .strength(0.3F).noOcclusion().lightLevel(state -> 10)));
        ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static RegistryObject<Item> shard(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties()));
    }

    private static RegistryObject<Block> ground(String name, Block source) {
        RegistryObject<Block> block = BLOCKS.register(name,
                () -> new Block(BlockBehaviour.Properties.copy(source).strength(0.6F)));
        ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }
    public static void register(net.minecraftforge.eventbus.api.IEventBus bus) { BLOCKS.register(bus); ITEMS.register(bus); }

    @Mod.EventBusSubscriber(modid = TheFourPrimitivesAndWeaponsMod.MODID,
            bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Colors {
        @SubscribeEvent
        public static void blocks(RegisterColorHandlersEvent.Block event) {
            event.register((state, level, pos, tint) -> tint == 0 ? color(state.getBlock()) : 0xFFFFFF,
                    VIOLET.get(), CYAN.get(), AMBER.get(), CRIMSON.get());
        }

        @SubscribeEvent
        public static void items(RegisterColorHandlersEvent.Item event) {
            event.register((stack, tint) -> tint == 0 ? color(Block.byItem(stack.getItem())) : 0xFFFFFF,
                    VIOLET.get().asItem(), CYAN.get().asItem(), AMBER.get().asItem(), CRIMSON.get().asItem());
            event.register((stack, tint) -> tint == 0 ? shardColor(stack.getItem()) : 0xFFFFFF,
                    VIOLET_SHARD.get(), CYAN_SHARD.get(), AMBER_SHARD.get(), CRIMSON_SHARD.get());
        }

        private static int color(Block block) {
            if (block == CYAN.get()) return CYAN_HEX;
            if (block == AMBER.get()) return AMBER_HEX;
            if (block == CRIMSON.get()) return CRIMSON_HEX;
            return VIOLET_HEX;
        }

        private static int shardColor(Item item) {
            if (item == CYAN_SHARD.get()) return CYAN_HEX;
            if (item == AMBER_SHARD.get()) return AMBER_HEX;
            if (item == CRIMSON_SHARD.get()) return CRIMSON_HEX;
            return VIOLET_HEX;
        }
    }
    private BladeCrystalInit() {}
}

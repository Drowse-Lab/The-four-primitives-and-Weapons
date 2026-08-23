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

    private static RegistryObject<Block> crystal(String name) {
        RegistryObject<Block> block = BLOCKS.register(name, () -> new CorrosionCrystalBlock(
                BlockBehaviour.Properties.copy(Blocks.GLASS).strength(0.3F).noOcclusion()));
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
        }

        private static int color(Block block) {
            if (block == CYAN.get()) return CYAN_HEX;
            if (block == AMBER.get()) return AMBER_HEX;
            if (block == CRIMSON.get()) return CRIMSON_HEX;
            return VIOLET_HEX;
        }
    }
    private BladeCrystalInit() {}
}

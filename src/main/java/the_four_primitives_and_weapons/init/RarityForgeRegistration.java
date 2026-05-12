package the_four_primitives_and_weapons.init;

import the_four_primitives_and_weapons.block.RarityForgeBlock;
import the_four_primitives_and_weapons.block.entity.RarityForgeBlockEntity;
import the_four_primitives_and_weapons.client.gui.RarityForgeScreen;
import the_four_primitives_and_weapons.client.gui.SkillSelectionScreen;
import the_four_primitives_and_weapons.world.inventory.RarityForgeMenu;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

/**
 * MCreatorのinit再生成に影響されない独自の登録クラス。
 * RegisterEventで直接登録するため、initファイルを編集する必要がない。
 */
@Mod.EventBusSubscriber(modid = "the_four_primitives_and_weapons", bus = Mod.EventBusSubscriber.Bus.MOD)
public class RarityForgeRegistration {

    private static Block block;
    private static Item item;
    private static BlockEntityType<?> blockEntityType;
    private static MenuType<RarityForgeMenu> menuType;

    public static Block getBlock() { return block; }
    public static Item getItem() { return item; }
    public static BlockEntityType<?> getBlockEntityType() { return blockEntityType; }
    public static MenuType<RarityForgeMenu> getMenuType() { return menuType; }

    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        event.register(ForgeRegistries.Keys.BLOCKS, helper -> {
            block = new RarityForgeBlock();
            helper.register(new ResourceLocation("the_four_primitives_and_weapons", "rarity_forge"), block);
        });
        event.register(ForgeRegistries.Keys.ITEMS, helper -> {
            item = new BlockItem(block, new Item.Properties());
            helper.register(new ResourceLocation("the_four_primitives_and_weapons", "rarity_forge"), item);
        });
        event.register(ForgeRegistries.Keys.BLOCK_ENTITY_TYPES, helper -> {
            blockEntityType = BlockEntityType.Builder.of(RarityForgeBlockEntity::new, block).build(null);
            helper.register(new ResourceLocation("the_four_primitives_and_weapons", "rarity_forge"), blockEntityType);
        });
        event.register(ForgeRegistries.Keys.MENU_TYPES, helper -> {
            menuType = IForgeMenuType.create(RarityForgeMenu::new);
            helper.register(new ResourceLocation("the_four_primitives_and_weapons", "rarity_forge_menu"), menuType);
        });
    }

    @Mod.EventBusSubscriber(modid = "the_four_primitives_and_weapons", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class Client {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                MenuScreens.register(menuType, RarityForgeScreen::new);
                MenuScreens.register(SkillSelectionRegistration.getMenuType(), SkillSelectionScreen::new);
            });
        }
    }
}

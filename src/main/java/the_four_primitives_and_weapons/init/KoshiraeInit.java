package the_four_primitives_and_weapons.init;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.inventory.MenuType;

import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.block.KoshiraeBlock;
import the_four_primitives_and_weapons.menu.KoshiraeMenu;

/** 拵え台 ( ブロック + アイテム + メニュー ) の登録。 */
public final class KoshiraeInit {

	private KoshiraeInit() {}

	public static final DeferredRegister<Block> BLOCKS =
			DeferredRegister.create(ForgeRegistries.BLOCKS, TheFourPrimitivesAndWeaponsMod.MODID);
	public static final DeferredRegister<Item> ITEMS =
			DeferredRegister.create(ForgeRegistries.ITEMS, TheFourPrimitivesAndWeaponsMod.MODID);
	public static final DeferredRegister<MenuType<?>> MENUS =
			DeferredRegister.create(ForgeRegistries.MENU_TYPES, TheFourPrimitivesAndWeaponsMod.MODID);

	public static final RegistryObject<Block> BLOCK = BLOCKS.register("koshirae_bench",
			() -> new KoshiraeBlock(BlockBehaviour.Properties.of()
					.mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD).noOcclusion()));

	public static final RegistryObject<Item> ITEM = ITEMS.register("koshirae_bench",
			() -> new BlockItem(BLOCK.get(), new Item.Properties()));

	public static final RegistryObject<MenuType<KoshiraeMenu>> MENU = MENUS.register("koshirae",
			() -> IForgeMenuType.create((id, inv, buf) -> new KoshiraeMenu(id, inv)));

	public static void register(IEventBus bus) {
		BLOCKS.register(bus);
		ITEMS.register(bus);
		MENUS.register(bus);
	}
}

package the_four_primitives_and_weapons.init;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.item.ImperialArmyUniformItem;
import the_four_primitives_and_weapons.item.BattoutaiUniformItem;
import the_four_primitives_and_weapons.item.MeijiPoliceUniformItem;
import the_four_primitives_and_weapons.item.KeishiPatrolUniformItem;
import the_four_primitives_and_weapons.item.KeishiOfficerUniformItem;

import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 明治期の3制服（大日本軍 軍服 / 抜刀隊 制服 / 明治警官 制服）の防具一式を登録する。
 * MCreator が {@code TheFourPrimitivesAndWeaponsModItems} を再生成しても残るよう
 * 専用 DeferredRegister で登録し、 クリエイティブタブにも自前で追加する。
 */
@Mod.EventBusSubscriber(modid = TheFourPrimitivesAndWeaponsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class MeijiUniformRegistrar {

	public static final DeferredRegister<Item> ITEMS =
			DeferredRegister.create(ForgeRegistries.ITEMS, TheFourPrimitivesAndWeaponsMod.MODID);

	// === 大日本軍 軍服 ===
	public static final RegistryObject<Item> IMPERIAL_ARMY_CAP =
			ITEMS.register("imperial_army_cap", ImperialArmyUniformItem.Helmet::new);
	public static final RegistryObject<Item> IMPERIAL_ARMY_TUNIC =
			ITEMS.register("imperial_army_tunic", ImperialArmyUniformItem.Chestplate::new);
	public static final RegistryObject<Item> IMPERIAL_ARMY_TROUSERS =
			ITEMS.register("imperial_army_trousers", ImperialArmyUniformItem.Leggings::new);
	public static final RegistryObject<Item> IMPERIAL_ARMY_BOOTS =
			ITEMS.register("imperial_army_boots", ImperialArmyUniformItem.Boots::new);

	// === 抜刀隊 制服 ===
	public static final RegistryObject<Item> BATTOUTAI_CAP =
			ITEMS.register("battoutai_cap", BattoutaiUniformItem.Helmet::new);
	public static final RegistryObject<Item> BATTOUTAI_TUNIC =
			ITEMS.register("battoutai_tunic", BattoutaiUniformItem.Chestplate::new);
	public static final RegistryObject<Item> BATTOUTAI_TROUSERS =
			ITEMS.register("battoutai_trousers", BattoutaiUniformItem.Leggings::new);
	public static final RegistryObject<Item> BATTOUTAI_BOOTS =
			ITEMS.register("battoutai_boots", BattoutaiUniformItem.Boots::new);

	// === 明治警官 制服 ===
	public static final RegistryObject<Item> MEIJI_POLICE_CAP =
			ITEMS.register("meiji_police_cap", MeijiPoliceUniformItem.Helmet::new);
	public static final RegistryObject<Item> MEIJI_POLICE_TUNIC =
			ITEMS.register("meiji_police_tunic", MeijiPoliceUniformItem.Chestplate::new);
	public static final RegistryObject<Item> MEIJI_POLICE_TROUSERS =
			ITEMS.register("meiji_police_trousers", MeijiPoliceUniformItem.Leggings::new);
	public static final RegistryObject<Item> MEIJI_POLICE_BOOTS =
			ITEMS.register("meiji_police_boots", MeijiPoliceUniformItem.Boots::new);

	// === 警視抜刀隊 巡査 ( ダブルボタン / 細線 / 草鞋 ) ===
	public static final RegistryObject<Item> KEISHI_PATROL_CAP =
			ITEMS.register("keishi_patrol_cap", KeishiPatrolUniformItem.Helmet::new);
	public static final RegistryObject<Item> KEISHI_PATROL_TUNIC =
			ITEMS.register("keishi_patrol_tunic", KeishiPatrolUniformItem.Chestplate::new);
	public static final RegistryObject<Item> KEISHI_PATROL_TROUSERS =
			ITEMS.register("keishi_patrol_trousers", KeishiPatrolUniformItem.Leggings::new);
	public static final RegistryObject<Item> KEISHI_PATROL_BOOTS =
			ITEMS.register("keishi_patrol_boots", KeishiPatrolUniformItem.Boots::new);

	// === 警視抜刀隊 警部 ( シングルボタン / 太線 / 革靴 ) ===
	public static final RegistryObject<Item> KEISHI_OFFICER_CAP =
			ITEMS.register("keishi_officer_cap", KeishiOfficerUniformItem.Helmet::new);
	public static final RegistryObject<Item> KEISHI_OFFICER_TUNIC =
			ITEMS.register("keishi_officer_tunic", KeishiOfficerUniformItem.Chestplate::new);
	public static final RegistryObject<Item> KEISHI_OFFICER_TROUSERS =
			ITEMS.register("keishi_officer_trousers", KeishiOfficerUniformItem.Leggings::new);
	public static final RegistryObject<Item> KEISHI_OFFICER_BOOTS =
			ITEMS.register("keishi_officer_boots", KeishiOfficerUniformItem.Boots::new);

	private MeijiUniformRegistrar() {}

	/** TAB_ARMOR に12アイテムを追加 */
	@SubscribeEvent
	public static void onBuildTabContents(BuildCreativeModeTabContentsEvent event) {
		if (event.getTab() != TheFourPrimitivesAndWeaponsModTabs.TAB_ARMOR.get()) return;
		event.accept(IMPERIAL_ARMY_CAP);
		event.accept(IMPERIAL_ARMY_TUNIC);
		event.accept(IMPERIAL_ARMY_TROUSERS);
		event.accept(IMPERIAL_ARMY_BOOTS);
		event.accept(BATTOUTAI_CAP);
		event.accept(BATTOUTAI_TUNIC);
		event.accept(BATTOUTAI_TROUSERS);
		event.accept(BATTOUTAI_BOOTS);
		event.accept(MEIJI_POLICE_CAP);
		event.accept(MEIJI_POLICE_TUNIC);
		event.accept(MEIJI_POLICE_TROUSERS);
		event.accept(MEIJI_POLICE_BOOTS);
		event.accept(KEISHI_PATROL_CAP);
		event.accept(KEISHI_PATROL_TUNIC);
		event.accept(KEISHI_PATROL_TROUSERS);
		event.accept(KEISHI_PATROL_BOOTS);
		event.accept(KEISHI_OFFICER_CAP);
		event.accept(KEISHI_OFFICER_TUNIC);
		event.accept(KEISHI_OFFICER_TROUSERS);
		event.accept(KEISHI_OFFICER_BOOTS);
	}
}

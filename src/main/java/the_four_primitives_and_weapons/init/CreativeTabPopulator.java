package the_four_primitives_and_weapons.init;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.init.RarityForgeRegistration;

@Mod.EventBusSubscriber(modid = TheFourPrimitivesAndWeaponsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CreativeTabPopulator {

	@SubscribeEvent
	public static void onBuildTabContents(BuildCreativeModeTabContentsEvent event) {
		// === TAB_WEAPON ===
		if (event.getTab() == TheFourPrimitivesAndWeaponsModTabs.TAB_WEAPON.get()) {
			event.accept(TheFourPrimitivesAndWeaponsModItems.WOODEN_KATANA);
			event.accept(TheFourPrimitivesAndWeaponsModItems.IRON_KATANA);
			event.accept(TheFourPrimitivesAndWeaponsModItems.STONE_KATANA);
			event.accept(TheFourPrimitivesAndWeaponsModItems.GOLD_KATANA);
			event.accept(TheFourPrimitivesAndWeaponsModItems.DIAMOND_KATANA);
			event.accept(TheFourPrimitivesAndWeaponsModItems.NETHERITE_KATANA);
			event.accept(TheFourPrimitivesAndWeaponsModItems.WITHER_KATANA);
			event.accept(TheFourPrimitivesAndWeaponsModItems.MY_TEST_IRON_KATANA);
			event.accept(TheFourPrimitivesAndWeaponsModItems.MAGICAL_KATANA);
			event.accept(TheFourPrimitivesAndWeaponsModItems.MAGISCHES_FEEN_KATANA);
			event.accept(TheFourPrimitivesAndWeaponsModItems.DARKNESS_KATANA);
			event.accept(TheFourPrimitivesAndWeaponsModItems.RIVERS_OF_BLOOD);
			event.accept(TheFourPrimitivesAndWeaponsModItems.KURIKARAKEN);
			event.accept(TheFourPrimitivesAndWeaponsModItems.KURIKARAKENSWORD);
			event.accept(TheFourPrimitivesAndWeaponsModItems.KURIKARAKENUTIGATANA);
			event.accept(TheFourPrimitivesAndWeaponsModItems.LUNA);
			event.accept(TheFourPrimitivesAndWeaponsModItems.NINJATOU);
			event.accept(TheFourPrimitivesAndWeaponsModItems.HAMMER);
			event.accept(TheFourPrimitivesAndWeaponsModItems.MACHETE);
			event.accept(TheFourPrimitivesAndWeaponsModItems.SCYTHE);
			event.accept(TheFourPrimitivesAndWeaponsModItems.SMALL_SWORD);
			event.accept(TheFourPrimitivesAndWeaponsModItems.WARABITETOU);
			event.accept(TheFourPrimitivesAndWeaponsModItems.HALLOWEEN_2023_10_31_SICKLE);
			event.accept(TheFourPrimitivesAndWeaponsModItems.ACHROMATIC_SHIELD);
			event.accept(TheFourPrimitivesAndWeaponsModItems.NIGU_SHIELD);
			event.accept(TheFourPrimitivesAndWeaponsModItems.SAYA);
			event.accept(TheFourPrimitivesAndWeaponsModItems.TYOKUTO_SAYA);
			event.accept(TheFourPrimitivesAndWeaponsModItems.SWORD_SAYA);
			event.accept(CustomEntityInit.WEAPON_RACK_ITEM);
			event.accept(CustomEntityInit.WEAPON_RACK_SPRUCE);
			event.accept(CustomEntityInit.WEAPON_RACK_BIRCH);
			event.accept(CustomEntityInit.WEAPON_RACK_JUNGLE);
			event.accept(CustomEntityInit.WEAPON_RACK_ACACIA);
			event.accept(CustomEntityInit.WEAPON_RACK_DARK_OAK);
			event.accept(CustomEntityInit.WEAPON_RACK_MANGROVE);
			event.accept(CustomEntityInit.WEAPON_RACK_CHERRY);
			event.accept(CustomEntityInit.WEAPON_RACK_BAMBOO);
			event.accept(CustomEntityInit.WEAPON_RACK_CRIMSON);
			event.accept(CustomEntityInit.WEAPON_RACK_WARPED);
			event.accept(TheFourPrimitivesAndWeaponsModItems.WOODEN_TYOKUTO);
			event.accept(TheFourPrimitivesAndWeaponsModItems.IRON_TYOKUTO);
			event.accept(TheFourPrimitivesAndWeaponsModItems.GOLD_TYOKUTO);
			event.accept(TheFourPrimitivesAndWeaponsModItems.STONE_TYOKUTO);
			event.accept(TheFourPrimitivesAndWeaponsModItems.DIAMOND_TYOKUTO);
			event.accept(TheFourPrimitivesAndWeaponsModItems.NETHERITE_TYOKUTO);
			event.accept(TheFourPrimitivesAndWeaponsModItems.WOODEN_RAPIER);
			event.accept(TheFourPrimitivesAndWeaponsModItems.STONE_RAPIER);
			event.accept(TheFourPrimitivesAndWeaponsModItems.IRON_RAPIER);
			event.accept(TheFourPrimitivesAndWeaponsModItems.GOLD_RAPIER);
			event.accept(TheFourPrimitivesAndWeaponsModItems.DIAMOND_RAPIER);
			event.accept(TheFourPrimitivesAndWeaponsModItems.NETHERITE_RAPIER);
			event.accept(TheFourPrimitivesAndWeaponsModItems.RAPIER_SAYA);
			event.accept(TheFourPrimitivesAndWeaponsModItems.RAW_URUSHI);
			event.accept(TheFourPrimitivesAndWeaponsModItems.URUSHI_BLACK);
			event.accept(TheFourPrimitivesAndWeaponsModItems.URUSHI_RED);
			// 鮫鞘 ( 素材mod未確定でクラフト不可なので、 完成品をクリエイティブで配布 )
			{
				net.minecraft.world.item.ItemStack same =
						new net.minecraft.world.item.ItemStack(TheFourPrimitivesAndWeaponsModItems.SAYA.get());
				the_four_primitives_and_weapons.util.SayaDesign.setStyle(same, "same");
				event.accept(same);
			}
			event.accept(TheFourPrimitivesAndWeaponsModItems.GATE);
			event.accept(TheFourPrimitivesAndWeaponsModItems.CONVERGENT_GATE);
			event.accept(TheFourPrimitivesAndWeaponsModItems.IMMORTAL_CORE);
			event.accept(TheFourPrimitivesAndWeaponsModItems.RARITY_CHARM);
			event.accept(TheFourPrimitivesAndWeaponsModItems.OFUDA);
			event.accept(CustomEntityInit.THROWING_KNIFE);
			// ナイフ系は通常投げナイフ + ホルダーのみプレイヤーに提供。
			// スタン/スクリュー/ホーミング/グリップ の個別アイテムは入手不可
			// (KnifeLauncher の mode 切替で全機能にアクセス可能)。
			// - event.accept(CustomEntityInit.GRIP_KNIFE);
			// - event.accept(CustomEntityInit.STUN_KNIFE);
			// - event.accept(CustomEntityInit.SCREW_KNIFE);
			// - event.accept(CustomEntityInit.HOMING_KNIFE);
			event.accept(CustomEntityInit.KNIFE_LAUNCHER);
			event.accept(KnifeExtrasRegistrar.EXPLOSIVE_THROWING_KNIFE);
			event.accept(KnifeExtrasRegistrar.ANTI_GRAVITY_BRACELET);
			event.accept(KnifeExtrasRegistrar.MATERIALIZED_POUCH);
			event.accept(KnifeExtrasRegistrar.BATTLE_STAKE);
			event.accept(CustomEntityInit.GUIDE_BOOK);
			event.accept(CustomEntityInit.MANA_POTION);
			event.accept(CustomEntityInit.UNDEAD_ARMY_BANISH);
			event.accept(CustomEntityInit.RECROSS_HOOKSHOT_LONG);
			event.accept(CustomEntityInit.RECROSS_HOOKSHOT_SHORT);
		}

		// === TAB_MAGIC_BOOKS ===
		if (event.getTab() == TheFourPrimitivesAndWeaponsModTabs.TAB_MAGIC_BOOKS.get()) {
			event.accept(TheFourPrimitivesAndWeaponsModItems.FIREBALL);
			event.accept(TheFourPrimitivesAndWeaponsModItems.THUNDERBOLT);
			event.accept(TheFourPrimitivesAndWeaponsModItems.BUBBLESHOT);
			event.accept(TheFourPrimitivesAndWeaponsModItems.STORM);
			event.accept(TheFourPrimitivesAndWeaponsModItems.WIND_STEP);
			event.accept(TheFourPrimitivesAndWeaponsModItems.DARKNESS);
			event.accept(TheFourPrimitivesAndWeaponsModItems.ICE_BOOK);
			event.accept(TheFourPrimitivesAndWeaponsModItems.ELECTRIC_BOOK);
			event.accept(TheFourPrimitivesAndWeaponsModItems.CORROSION_BOOK);
			event.accept(TheFourPrimitivesAndWeaponsModItems.HOLY_BOOK);
			event.accept(TheFourPrimitivesAndWeaponsModItems.MIASMA_BOOK);
			event.accept(TheFourPrimitivesAndWeaponsModItems.ELEMENT_CLEANSE_POTION);
			event.accept(TheFourPrimitivesAndWeaponsModItems.MAGICWAND);
			event.accept(TheFourPrimitivesAndWeaponsModItems.IMITATION);
			event.accept(TheFourPrimitivesAndWeaponsModItems.TUKAENA);
			event.accept(TheFourPrimitivesAndWeaponsModItems.QUESTBOOK);
			event.accept(TheFourPrimitivesAndWeaponsModItems.RPG_BOOK);
			event.accept(TheFourPrimitivesAndWeaponsModItems.DECORATION_POT_WITH_ARROWS);
			event.accept(TheFourPrimitivesAndWeaponsModItems.ROSE_FLOWER_POT);
			event.accept(TheFourPrimitivesAndWeaponsModItems.ITEM_STAN);
			event.accept(TheFourPrimitivesAndWeaponsModItems.MAGIC_POT);
		}

		// === TAB_ARMOR ===
		if (event.getTab() == TheFourPrimitivesAndWeaponsModTabs.TAB_ARMOR.get()) {
			event.accept(TheFourPrimitivesAndWeaponsModItems.PILLAGER_ARMOR_HELMET);
			event.accept(TheFourPrimitivesAndWeaponsModItems.PILLAGER_ARMOR_CHESTPLATE);
			event.accept(TheFourPrimitivesAndWeaponsModItems.PILLAGER_ARMOR_LEGGINGS);
			event.accept(TheFourPrimitivesAndWeaponsModItems.PILLAGER_ARMOR_BOOTS);
			event.accept(TheFourPrimitivesAndWeaponsModItems.ILLUSIONER_ARMOR_HELMET);
			event.accept(TheFourPrimitivesAndWeaponsModItems.ILLUSIONER_ARMOR_CHESTPLATE);
			event.accept(TheFourPrimitivesAndWeaponsModItems.ILLUSIONER_ARMOR_LEGGINGS);
			event.accept(TheFourPrimitivesAndWeaponsModItems.ILLUSIONER_ARMOR_BOOTS);
			event.accept(TheFourPrimitivesAndWeaponsModItems.DAS_HERZ_EINER_FEE_ARMOR_HELMET);
			event.accept(TheFourPrimitivesAndWeaponsModItems.DAS_HERZ_EINER_FEE_ARMOR_CHESTPLATE);
			event.accept(TheFourPrimitivesAndWeaponsModItems.DAS_HERZ_EINER_FEE_ARMOR_LEGGINGS);
			event.accept(TheFourPrimitivesAndWeaponsModItems.DAS_HERZ_EINER_FEE_ARMOR_BOOTS);
			event.accept(TheFourPrimitivesAndWeaponsModItems.STRAY_OUTER_ARMOR_HELMET);
			event.accept(TheFourPrimitivesAndWeaponsModItems.STRAY_OUTER_ARMOR_CHESTPLATE);
			event.accept(TheFourPrimitivesAndWeaponsModItems.STRAY_OUTER_ARMOR_LEGGINGS);
			event.accept(TheFourPrimitivesAndWeaponsModItems.STRAYOUTERARMORHAT_HELMET);
			event.accept(TheFourPrimitivesAndWeaponsModItems.BOGGED_OUTER_HELMET);
			event.accept(TheFourPrimitivesAndWeaponsModItems.BOGGED_OUTER_CHESTPLATE);
			event.accept(TheFourPrimitivesAndWeaponsModItems.BOGGED_OUTER_LEGGINGS);
			event.accept(TheFourPrimitivesAndWeaponsModItems.BOGGED_OUTER_BOOTS);
			event.accept(TheFourPrimitivesAndWeaponsModItems.ONINOMEN_HELMET);
		}

		// === TAB_YOPKEINAMONO ===
		if (event.getTab() == TheFourPrimitivesAndWeaponsModTabs.TAB_YOPKEINAMONO.get()) {
			event.accept(TheFourPrimitivesAndWeaponsModItems.CROSS);
			event.accept(TheFourPrimitivesAndWeaponsModItems.STONE_BRICKS_TRAP_DOOR);
			event.accept(TheFourPrimitivesAndWeaponsModItems.KURIKARAKEN_BLOCK);
			event.accept(TheFourPrimitivesAndWeaponsModItems.STONE_KATANA_BLOCK);
			event.accept(TheFourPrimitivesAndWeaponsModItems.STONE_KATANA_BLOCK_1);
			event.accept(TheFourPrimitivesAndWeaponsModItems.MAKIWARIDAI);
			event.accept(TheFourPrimitivesAndWeaponsModItems.WITHER_SKELETON_SPAWNER);
			event.accept(TheFourPrimitivesAndWeaponsModItems.A);
			event.accept(TheFourPrimitivesAndWeaponsModItems.A_2);
			event.accept(TheFourPrimitivesAndWeaponsModItems.AAA);
			event.accept(TheFourPrimitivesAndWeaponsModItems.ARROW_HEAD);
			event.accept(TheFourPrimitivesAndWeaponsModItems.DESPORN_KENTI);
			event.accept(TheFourPrimitivesAndWeaponsModItems.KABUSERU);
			event.accept(TheFourPrimitivesAndWeaponsModItems.KENTI);
			event.accept(TheFourPrimitivesAndWeaponsModItems.MOTASERU);
			event.accept(TheFourPrimitivesAndWeaponsModItems.RESET_1);
			event.accept(TheFourPrimitivesAndWeaponsModItems.RESET_MAX);
		}

		// === TAB_EVENT ===
		if (event.getTab() == TheFourPrimitivesAndWeaponsModTabs.TAB_EVENT.get()) {
			event.accept(TheFourPrimitivesAndWeaponsModItems.HARVEST_MOON_2023929);
			event.accept(TheFourPrimitivesAndWeaponsModItems.PUMPKIN_HEAD_HELMET);
		}

		// === TAB_DRAGON_ARMOR_TAB ===
		if (event.getTab() == TheFourPrimitivesAndWeaponsModTabs.TAB_DRAGON_ARMOR_TAB.get()) {
			event.accept(TheFourPrimitivesAndWeaponsModItems.DRAGON_ARMOR_HELMET);
			event.accept(TheFourPrimitivesAndWeaponsModItems.DRAGON_ARMOR_CHESTPLATE);
			event.accept(TheFourPrimitivesAndWeaponsModItems.DRAGON_ARMOR_LEGGINGS);
			event.accept(TheFourPrimitivesAndWeaponsModItems.DRAGON_ARMOR_BOOTS);
			event.accept(TheFourPrimitivesAndWeaponsModItems.DRAGON_GREEN_ARMOR_HELMET);
			event.accept(TheFourPrimitivesAndWeaponsModItems.DRAGON_GREEN_ARMOR_CHESTPLATE);
			event.accept(TheFourPrimitivesAndWeaponsModItems.DRAGON_GREEN_ARMOR_LEGGINGS);
			event.accept(TheFourPrimitivesAndWeaponsModItems.DRAGON_GREEN_ARMOR_BOOTS);
			event.accept(TheFourPrimitivesAndWeaponsModItems.DRAGON_BLACK_ARMOR_HELMET);
			event.accept(TheFourPrimitivesAndWeaponsModItems.DRAGON_BLACK_ARMOR_CHESTPLATE);
			event.accept(TheFourPrimitivesAndWeaponsModItems.DRAGON_BLACK_ARMOR_LEGGINGS);
			event.accept(TheFourPrimitivesAndWeaponsModItems.DRAGON_BLACK_ARMOR_BOOTS);
			event.accept(TheFourPrimitivesAndWeaponsModItems.DRAGON_RED_ARMOR_HELMET);
			event.accept(TheFourPrimitivesAndWeaponsModItems.DRAGON_RED_ARMOR_CHESTPLATE);
			event.accept(TheFourPrimitivesAndWeaponsModItems.DRAGON_RED_ARMOR_LEGGINGS);
			event.accept(TheFourPrimitivesAndWeaponsModItems.DRAGON_RED_ARMOR_BOOTS);
			event.accept(TheFourPrimitivesAndWeaponsModItems.DRAGON_BLUE_ARMOR_HELMET);
			event.accept(TheFourPrimitivesAndWeaponsModItems.DRAGON_BLUE_ARMOR_CHESTPLATE);
			event.accept(TheFourPrimitivesAndWeaponsModItems.DRAGON_BLUE_ARMOR_LEGGINGS);
			event.accept(TheFourPrimitivesAndWeaponsModItems.DRAGON_BLUE_ARMOR_BOOTS);
		}

		// === TAB_NIGU ===
		if (event.getTab() == TheFourPrimitivesAndWeaponsModTabs.TAB_NIGU.get()) {
			event.accept(TheFourPrimitivesAndWeaponsModItems.KATANA_NIGU_HUMERUS);
		}

		// === TAB_CHUZUME_TAB ===
		if (event.getTab() == TheFourPrimitivesAndWeaponsModTabs.TAB_CHUZUME_TAB.get()) {
			event.accept(TheFourPrimitivesAndWeaponsModItems.SWORD_OF_NIGHT);
			event.accept(TheFourPrimitivesAndWeaponsModItems.REPLICA_SWORD_OF_LIGHT);
		}

		// === Vanilla tabs ===
		if (event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
			event.accept(TheFourPrimitivesAndWeaponsModItems.BLOOD_BOTTLE);
		}

		if (event.getTabKey() == CreativeModeTabs.COMBAT) {
			event.accept(TheFourPrimitivesAndWeaponsModItems.CHUZUME_HUSK_ARMOR_HELMET);
			event.accept(TheFourPrimitivesAndWeaponsModItems.CHUZUME_HUSK_ARMOR_CHESTPLATE);
			event.accept(TheFourPrimitivesAndWeaponsModItems.CHUZUME_HUSK_ARMOR_LEGGINGS);
			event.accept(TheFourPrimitivesAndWeaponsModItems.CHUZUME_HUSK_ARMOR_BOOTS);
			event.accept(TheFourPrimitivesAndWeaponsModItems.WARDEN_ARMOR_HELMET);
			event.accept(TheFourPrimitivesAndWeaponsModItems.WARDEN_ARMOR_CHESTPLATE);
			event.accept(TheFourPrimitivesAndWeaponsModItems.WARDEN_ARMOR_LEGGINGS);
			event.accept(TheFourPrimitivesAndWeaponsModItems.KATANA_TOBU);
			event.accept(TheFourPrimitivesAndWeaponsModItems.COPPER_ARMOR_HELMET);
			event.accept(TheFourPrimitivesAndWeaponsModItems.COPPER_ARMOR_CHESTPLATE);
			event.accept(TheFourPrimitivesAndWeaponsModItems.COPPER_ARMOR_LEGGINGS);
			event.accept(TheFourPrimitivesAndWeaponsModItems.COPPER_ARMOR_BOOTS);
		}

		if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
			event.accept(TheFourPrimitivesAndWeaponsModItems.MAGIC_MCRYSTAL);
			event.accept(TheFourPrimitivesAndWeaponsModItems.SKIN_OF_DRAGON);
			event.accept(TheFourPrimitivesAndWeaponsModItems.STONE_SLAB);
			event.accept(TheFourPrimitivesAndWeaponsModItems.STRAY_BONE);
			event.accept(TheFourPrimitivesAndWeaponsModItems.WITHER_BONE);
		}

		if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
			event.accept(RarityForgeRegistration.getItem());
		}

		if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
			event.accept(TheFourPrimitivesAndWeaponsModItems.SKELTON_MOB_SPAWN_EGG);
			event.accept(TheFourPrimitivesAndWeaponsModItems.OTIRUYO_SPAWN_EGG);
			event.accept(TheFourPrimitivesAndWeaponsModItems.KILLOTIRU_SPAWN_EGG);
			event.accept(TheFourPrimitivesAndWeaponsModItems.BLACKHOLE_SPAWN_EGG);
			event.accept(TheFourPrimitivesAndWeaponsModItems.METEOR_ARROW_SPAWN_EGG);
			event.accept(TheFourPrimitivesAndWeaponsModItems.FLYING_ATTACKER_SPAWN_EGG);
			// CustomEntityInitで登録した非MCreatorスポーンエッグ
			event.accept(CustomEntityInit.COMMON_SOLDIER_SPAWN_EGG);
			event.accept(CustomEntityInit.ELITE_SOLDIER_SPAWN_EGG);
			event.accept(CustomEntityInit.SINGULARITY_SPAWN_EGG);
			event.accept(CustomEntityInit.HEROIC_TIER_SPAWN_EGG);
			event.accept(CustomEntityInit.DEBUG_MOB_SPAWN_EGG);
			event.accept(CustomEntityInit.ANGEL_SERIOUS_SPAWN_EGG);
			event.accept(CustomEntityInit.ANGEL_MOCKER1_SPAWN_EGG);
			event.accept(CustomEntityInit.ANGEL_MOCKER2_SPAWN_EGG);
		}

		// === TAB_SAYA ( 鞘・漆・仕立て済みの鞘 ) ===
		if (event.getTab() == TheFourPrimitivesAndWeaponsModTabs.TAB_SAYA.get()) {
			// 素の鞘
			event.accept(TheFourPrimitivesAndWeaponsModItems.SAYA);
			event.accept(TheFourPrimitivesAndWeaponsModItems.TYOKUTO_SAYA);
			event.accept(TheFourPrimitivesAndWeaponsModItems.SWORD_SAYA);
			event.accept(TheFourPrimitivesAndWeaponsModItems.RAPIER_SAYA);
			// 漆素材
			event.accept(TheFourPrimitivesAndWeaponsModItems.RAW_URUSHI);
			event.accept(TheFourPrimitivesAndWeaponsModItems.URUSHI_BLACK);
			event.accept(TheFourPrimitivesAndWeaponsModItems.URUSHI_RED);
			// 漆の木 ( ブロック一式 )
			event.accept(UrushiWoodInit.URUSHI_LOG_ITEM);
			event.accept(UrushiWoodInit.STRIPPED_URUSHI_LOG_ITEM);
			event.accept(UrushiWoodInit.URUSHI_WOOD_ITEM);
			event.accept(UrushiWoodInit.STRIPPED_URUSHI_WOOD_ITEM);
			event.accept(UrushiWoodInit.URUSHI_PLANKS_ITEM);
			event.accept(UrushiWoodInit.URUSHI_LEAVES_ITEM);
			event.accept(UrushiWoodInit.URUSHI_SAPLING_ITEM);
			// 仕立て済みの鞘 ( 刀の鞘ベースで見本を1つずつ )
			for (String style : new String[]{
					"kise", "kizami", "ishime", "same",
					"kuroro", "roiro", "shunuri", "tame"}) {
				event.accept(styledSaya(style));
			}
			// 木目鞘 ( 木材ごと )
			for (String wood : the_four_primitives_and_weapons.util.SayaStyles.WOODS) {
				event.accept(styledSaya("wood_" + wood));
			}
		}
	}

	/** 指定スタイルを付けた刀の鞘の ItemStack を作る ( クリエイティブ見本用 )。 */
	private static net.minecraft.world.item.ItemStack styledSaya(String style) {
		net.minecraft.world.item.ItemStack s =
				new net.minecraft.world.item.ItemStack(TheFourPrimitivesAndWeaponsModItems.SAYA.get());
		the_four_primitives_and_weapons.util.SayaDesign.setStyle(s, style);
		return s;
	}
}

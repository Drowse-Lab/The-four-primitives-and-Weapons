package minecraftarmorweapon.init;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import minecraftarmorweapon.MinecraftArmorWeaponMod;
import minecraftarmorweapon.init.RarityForgeRegistration;

@Mod.EventBusSubscriber(modid = MinecraftArmorWeaponMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CreativeTabPopulator {

	@SubscribeEvent
	public static void onBuildTabContents(BuildCreativeModeTabContentsEvent event) {
		// === TAB_WEAPON ===
		if (event.getTab() == MinecraftArmorWeaponModTabs.TAB_WEAPON.get()) {
			event.accept(MinecraftArmorWeaponModItems.IRON_KATANA);
			event.accept(MinecraftArmorWeaponModItems.STONE_KATANA);
			event.accept(MinecraftArmorWeaponModItems.GOLD_KATANA);
			event.accept(MinecraftArmorWeaponModItems.NETHERITE_KATANA);
			event.accept(MinecraftArmorWeaponModItems.WITHER_KATANA);
			event.accept(MinecraftArmorWeaponModItems.MOTO_WITHER_KATANA);
			event.accept(MinecraftArmorWeaponModItems.MY_TEST_IRON_KATANA);
			event.accept(MinecraftArmorWeaponModItems.MAGICAL_KATANA);
			event.accept(MinecraftArmorWeaponModItems.MAGISCHES_FEEN_KATANA);
			event.accept(MinecraftArmorWeaponModItems.DARKNESS_KATANA);
			event.accept(MinecraftArmorWeaponModItems.RIVERS_OF_BLOOD);
			event.accept(MinecraftArmorWeaponModItems.KURIKARAKEN);
			event.accept(MinecraftArmorWeaponModItems.KURIKARAKENSWORD);
			event.accept(MinecraftArmorWeaponModItems.KURIKARAKENUTIGATANA);
			event.accept(MinecraftArmorWeaponModItems.LUNA);
			event.accept(MinecraftArmorWeaponModItems.NINJATOU);
			event.accept(MinecraftArmorWeaponModItems.HAMMER);
			event.accept(MinecraftArmorWeaponModItems.MACHETE);
			event.accept(MinecraftArmorWeaponModItems.SCYTHE);
			event.accept(MinecraftArmorWeaponModItems.SMALL_SWORD);
			event.accept(MinecraftArmorWeaponModItems.WARABITETOU);
			event.accept(MinecraftArmorWeaponModItems.HALLOWEEN_2023_10_31_SICKLE);
			event.accept(MinecraftArmorWeaponModItems.ACHROMATIC_SHIELD);
			event.accept(MinecraftArmorWeaponModItems.NIGU_SHIELD);
			event.accept(MinecraftArmorWeaponModItems.SAYA);
			event.accept(MinecraftArmorWeaponModItems.TYOKUTO_SAYA);
			event.accept(MinecraftArmorWeaponModItems.TEST_BOW);
			event.accept(MinecraftArmorWeaponModItems.BLUEPURGE);
			event.accept(MinecraftArmorWeaponModItems.BLUEPURGE_UTIGATANA);
			event.accept(MinecraftArmorWeaponModItems.BLUEPURGE_TYOKUTOU);
		}

		// === TAB_MAGIC_BOOKS ===
		if (event.getTab() == MinecraftArmorWeaponModTabs.TAB_MAGIC_BOOKS.get()) {
			event.accept(MinecraftArmorWeaponModItems.FIREBALL);
			event.accept(MinecraftArmorWeaponModItems.THUNDERBOLT);
			event.accept(MinecraftArmorWeaponModItems.BUBBLESHOT);
			event.accept(MinecraftArmorWeaponModItems.STORM);
			event.accept(MinecraftArmorWeaponModItems.WIND_STEP);
			event.accept(MinecraftArmorWeaponModItems.DARKNESS);
			event.accept(MinecraftArmorWeaponModItems.MAGICWAND);
			event.accept(MinecraftArmorWeaponModItems.IMITATION);
			event.accept(MinecraftArmorWeaponModItems.TUKAENA);
			event.accept(MinecraftArmorWeaponModItems.QUESTBOOK);
			event.accept(MinecraftArmorWeaponModItems.RPG_BOOK);
			event.accept(MinecraftArmorWeaponModItems.DECORATION_POT_WITH_ARROWS);
			event.accept(MinecraftArmorWeaponModItems.ROSE_FLOWER_POT);
			event.accept(MinecraftArmorWeaponModItems.ITEM_STAN);
			event.accept(MinecraftArmorWeaponModItems.MAGIC_POT);
		}

		// === TAB_ARMOR ===
		if (event.getTab() == MinecraftArmorWeaponModTabs.TAB_ARMOR.get()) {
			event.accept(MinecraftArmorWeaponModItems.PILLAGER_ARMOR_HELMET);
			event.accept(MinecraftArmorWeaponModItems.PILLAGER_ARMOR_CHESTPLATE);
			event.accept(MinecraftArmorWeaponModItems.PILLAGER_ARMOR_LEGGINGS);
			event.accept(MinecraftArmorWeaponModItems.PILLAGER_ARMOR_BOOTS);
			event.accept(MinecraftArmorWeaponModItems.ILLUSIONER_ARMOR_HELMET);
			event.accept(MinecraftArmorWeaponModItems.ILLUSIONER_ARMOR_CHESTPLATE);
			event.accept(MinecraftArmorWeaponModItems.ILLUSIONER_ARMOR_LEGGINGS);
			event.accept(MinecraftArmorWeaponModItems.ILLUSIONER_ARMOR_BOOTS);
			event.accept(MinecraftArmorWeaponModItems.DAS_HERZ_EINER_FEE_ARMOR_HELMET);
			event.accept(MinecraftArmorWeaponModItems.DAS_HERZ_EINER_FEE_ARMOR_CHESTPLATE);
			event.accept(MinecraftArmorWeaponModItems.DAS_HERZ_EINER_FEE_ARMOR_LEGGINGS);
			event.accept(MinecraftArmorWeaponModItems.DAS_HERZ_EINER_FEE_ARMOR_BOOTS);
			event.accept(MinecraftArmorWeaponModItems.STRAY_OUTER_ARMOR_HELMET);
			event.accept(MinecraftArmorWeaponModItems.STRAY_OUTER_ARMOR_CHESTPLATE);
			event.accept(MinecraftArmorWeaponModItems.STRAY_OUTER_ARMOR_LEGGINGS);
			event.accept(MinecraftArmorWeaponModItems.STRAYOUTERARMORHAT_HELMET);
			event.accept(MinecraftArmorWeaponModItems.BOGGED_OUTER_HELMET);
			event.accept(MinecraftArmorWeaponModItems.BOGGED_OUTER_CHESTPLATE);
			event.accept(MinecraftArmorWeaponModItems.BOGGED_OUTER_LEGGINGS);
			event.accept(MinecraftArmorWeaponModItems.BOGGED_OUTER_BOOTS);
			event.accept(MinecraftArmorWeaponModItems.ONINOMEN_HELMET);
		}

		// === TAB_YOPKEINAMONO ===
		if (event.getTab() == MinecraftArmorWeaponModTabs.TAB_YOPKEINAMONO.get()) {
			event.accept(MinecraftArmorWeaponModItems.CROSS);
			event.accept(MinecraftArmorWeaponModItems.STONE_BRICKS_TRAP_DOOR);
			event.accept(MinecraftArmorWeaponModItems.KURIKARAKEN_BLOCK);
			event.accept(MinecraftArmorWeaponModItems.STONE_KATANA_BLOCK);
			event.accept(MinecraftArmorWeaponModItems.STONE_KATANA_BLOCK_1);
			event.accept(MinecraftArmorWeaponModItems.MAKIWARIDAI);
			event.accept(MinecraftArmorWeaponModItems.MOTO_WITHER_KATANA_BLOCK);
			event.accept(MinecraftArmorWeaponModItems.MOTO_WITHER_KATANA_BLOCK_1);
			event.accept(MinecraftArmorWeaponModItems.WITHER_SKELETON_SPAWNER);
			event.accept(MinecraftArmorWeaponModItems.A);
			event.accept(MinecraftArmorWeaponModItems.A_2);
			event.accept(MinecraftArmorWeaponModItems.AAA);
			event.accept(MinecraftArmorWeaponModItems.ARROW_HEAD);
			event.accept(MinecraftArmorWeaponModItems.DESPORN_KENTI);
			event.accept(MinecraftArmorWeaponModItems.KABUSERU);
			event.accept(MinecraftArmorWeaponModItems.KENTI);
			event.accept(MinecraftArmorWeaponModItems.MOTASERU);
			event.accept(MinecraftArmorWeaponModItems.RESET_1);
			event.accept(MinecraftArmorWeaponModItems.RESET_MAX);
		}

		// === TAB_EVENT ===
		if (event.getTab() == MinecraftArmorWeaponModTabs.TAB_EVENT.get()) {
			event.accept(MinecraftArmorWeaponModItems.HARVEST_MOON_2023929);
			event.accept(MinecraftArmorWeaponModItems.PUMPKIN_HEAD_HELMET);
		}

		// === TAB_DRAGON_ARMOR_TAB ===
		if (event.getTab() == MinecraftArmorWeaponModTabs.TAB_DRAGON_ARMOR_TAB.get()) {
			event.accept(MinecraftArmorWeaponModItems.DRAGON_ARMOR_HELMET);
			event.accept(MinecraftArmorWeaponModItems.DRAGON_ARMOR_CHESTPLATE);
			event.accept(MinecraftArmorWeaponModItems.DRAGON_ARMOR_LEGGINGS);
			event.accept(MinecraftArmorWeaponModItems.DRAGON_ARMOR_BOOTS);
			event.accept(MinecraftArmorWeaponModItems.DRAGON_GREEN_ARMOR_HELMET);
			event.accept(MinecraftArmorWeaponModItems.DRAGON_GREEN_ARMOR_CHESTPLATE);
			event.accept(MinecraftArmorWeaponModItems.DRAGON_GREEN_ARMOR_LEGGINGS);
			event.accept(MinecraftArmorWeaponModItems.DRAGON_GREEN_ARMOR_BOOTS);
			event.accept(MinecraftArmorWeaponModItems.DRAGON_BLACK_ARMOR_HELMET);
			event.accept(MinecraftArmorWeaponModItems.DRAGON_BLACK_ARMOR_CHESTPLATE);
			event.accept(MinecraftArmorWeaponModItems.DRAGON_BLACK_ARMOR_LEGGINGS);
			event.accept(MinecraftArmorWeaponModItems.DRAGON_BLACK_ARMOR_BOOTS);
			event.accept(MinecraftArmorWeaponModItems.DRAGON_RED_ARMOR_HELMET);
			event.accept(MinecraftArmorWeaponModItems.DRAGON_RED_ARMOR_CHESTPLATE);
			event.accept(MinecraftArmorWeaponModItems.DRAGON_RED_ARMOR_LEGGINGS);
			event.accept(MinecraftArmorWeaponModItems.DRAGON_RED_ARMOR_BOOTS);
			event.accept(MinecraftArmorWeaponModItems.DRAGON_BLUE_ARMOR_HELMET);
			event.accept(MinecraftArmorWeaponModItems.DRAGON_BLUE_ARMOR_CHESTPLATE);
			event.accept(MinecraftArmorWeaponModItems.DRAGON_BLUE_ARMOR_LEGGINGS);
			event.accept(MinecraftArmorWeaponModItems.DRAGON_BLUE_ARMOR_BOOTS);
		}

		// === TAB_NIGU ===
		if (event.getTab() == MinecraftArmorWeaponModTabs.TAB_NIGU.get()) {
			event.accept(MinecraftArmorWeaponModItems.KATANA_NIGU_HUMERUS);
		}

		// === TAB_CHUZUME_TAB ===
		if (event.getTab() == MinecraftArmorWeaponModTabs.TAB_CHUZUME_TAB.get()) {
			event.accept(MinecraftArmorWeaponModItems.SWORD_OF_NIGHT);
			event.accept(MinecraftArmorWeaponModItems.LOKI_THE_TRICKSTER);
			event.accept(MinecraftArmorWeaponModItems.REPLICA_SWORD_OF_LIGHT);
		}

		// === Vanilla tabs ===
		if (event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
			event.accept(MinecraftArmorWeaponModItems.BLOOD_BOTTLE);
		}

		if (event.getTabKey() == CreativeModeTabs.COMBAT) {
			event.accept(MinecraftArmorWeaponModItems.CHUZUME_HUSK_ARMOR_HELMET);
			event.accept(MinecraftArmorWeaponModItems.CHUZUME_HUSK_ARMOR_CHESTPLATE);
			event.accept(MinecraftArmorWeaponModItems.CHUZUME_HUSK_ARMOR_LEGGINGS);
			event.accept(MinecraftArmorWeaponModItems.CHUZUME_HUSK_ARMOR_BOOTS);
			event.accept(MinecraftArmorWeaponModItems.WARDEN_ARMOR_HELMET);
			event.accept(MinecraftArmorWeaponModItems.WARDEN_ARMOR_CHESTPLATE);
			event.accept(MinecraftArmorWeaponModItems.WARDEN_ARMOR_LEGGINGS);
			event.accept(MinecraftArmorWeaponModItems.KATANA_TOBU);
			event.accept(MinecraftArmorWeaponModItems.LOKI_DECOYDASU);
		}

		if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
			event.accept(MinecraftArmorWeaponModItems.MAGIC_MCRYSTAL);
			event.accept(MinecraftArmorWeaponModItems.SKIN_OF_DRAGON);
			event.accept(MinecraftArmorWeaponModItems.STONE_SLAB);
			event.accept(MinecraftArmorWeaponModItems.STRAY_BONE);
			event.accept(MinecraftArmorWeaponModItems.WITHER_BONE);
		}

		if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
			event.accept(RarityForgeRegistration.getItem());
		}

		if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
			event.accept(MinecraftArmorWeaponModItems.SKELTON_MOB_SPAWN_EGG);
			event.accept(MinecraftArmorWeaponModItems.OTIRUYO_SPAWN_EGG);
			event.accept(MinecraftArmorWeaponModItems.KILLOTIRU_SPAWN_EGG);
			event.accept(MinecraftArmorWeaponModItems.BLACKHOLE_SPAWN_EGG);
			event.accept(MinecraftArmorWeaponModItems.METEOR_ARROW_SPAWN_EGG);
			event.accept(MinecraftArmorWeaponModItems.FLYING_ATTACKER_SPAWN_EGG);
		}
	}
}

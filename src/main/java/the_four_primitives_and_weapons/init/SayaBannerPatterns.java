package the_four_primitives_and_weapons.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BannerPattern;

import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;

/**
 * 鞘専用の機織り模様 ( カスタム BannerPattern )。
 *
 * <p>ここで登録した模様は data/minecraft/tags/banner_pattern/no_item_required.json に入れてあるので、
 * 機織り機の模様一覧に ( 模様アイテム無しで ) 出る。 機織りで鞘に付けると 旗フォーマットNBTで保存され、
 * 鞘の表示は {@code textures/saya_pattern/<type>/<id>.png} ( SayaPatternModel ) が使われる。</p>
 *
 * <p>新しい鞘専用模様を足したいとき:
 *   1. ここに register("xxx") を追加
 *   2. tags/banner_pattern/no_item_required.json に "the_four_primitives_and_weapons:xxx" を追加
 *   3. textures/entity/banner/xxx.png ( 機織りプレビュー/旗用 ) と
 *      textures/saya_pattern/&lt;type&gt;/xxx.png ( 鞘表示用 ) を用意</p>
 */
public final class SayaBannerPatterns {

	private SayaBannerPatterns() {}

	public static final DeferredRegister<BannerPattern> REGISTRY =
			DeferredRegister.create(Registries.BANNER_PATTERN, TheFourPrimitivesAndWeaponsMod.MODID);

	// hashname は NBT に保存される一意の文字列。 衝突しないよう modid 接頭辞付き。
	public static final RegistryObject<BannerPattern> SAYA_MON =
			REGISTRY.register("saya_mon", () -> new BannerPattern("tfpw_saya_mon"));
	public static final RegistryObject<BannerPattern> SAYA_WAVE =
			REGISTRY.register("saya_wave", () -> new BannerPattern("tfpw_saya_wave"));
	public static final RegistryObject<BannerPattern> SAYA_SCALE =
			REGISTRY.register("saya_scale", () -> new BannerPattern("tfpw_saya_scale"));
	// 大きく面を占める模様: 侵食 / 雷 / サビ
	public static final RegistryObject<BannerPattern> SAYA_EROSION =
			REGISTRY.register("saya_erosion", () -> new BannerPattern("tfpw_saya_erosion"));
	public static final RegistryObject<BannerPattern> SAYA_THUNDER =
			REGISTRY.register("saya_thunder", () -> new BannerPattern("tfpw_saya_thunder"));
	public static final RegistryObject<BannerPattern> SAYA_RUST =
			REGISTRY.register("saya_rust", () -> new BannerPattern("tfpw_saya_rust"));
}

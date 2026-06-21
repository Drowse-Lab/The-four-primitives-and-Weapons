package the_four_primitives_and_weapons.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;

/**
 * Mod 独自の DamageType ResourceKey 一覧。
 * 実体 (JSON) は data/the_four_primitives_and_weapons/damage_type/<name>.json に置く。
 * 各属性の振る舞いはタグでバニラ挙動に乗せる:
 *   - bypasses_armor: 全 element ( thunder/fire 含むほぼ全部 )
 *   - is_lightning  : thunder のみ
 *   - is_fire       : fire のみ
 *   - witch_resistant_to: 全 element ( 魔法系扱い )
 */
public final class TheFourPrimitivesAndWeaponsModDamageTypes {
	private TheFourPrimitivesAndWeaponsModDamageTypes() {}

	public static final ResourceKey<DamageType> HOLY      = key("holy");
	public static final ResourceKey<DamageType> ICE       = key("ice");
	public static final ResourceKey<DamageType> ELECTRIC  = key("electric");
	public static final ResourceKey<DamageType> THUNDER   = key("thunder");
	public static final ResourceKey<DamageType> CORROSION = key("corrosion");
	public static final ResourceKey<DamageType> DARK      = key("dark");
	public static final ResourceKey<DamageType> MIASMA    = key("miasma");
	public static final ResourceKey<DamageType> FIRE      = key("fire");
	public static final ResourceKey<DamageType> WATER     = key("water");
	public static final ResourceKey<DamageType> WIND      = key("wind");
	public static final ResourceKey<DamageType> ERROR     = key("error");

	private static ResourceKey<DamageType> key(String name) {
		return ResourceKey.create(Registries.DAMAGE_TYPE,
				new ResourceLocation(TheFourPrimitivesAndWeaponsMod.MODID, name));
	}
}

package the_four_primitives_and_weapons.damage;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModDamageTypes;

/**
 * バニラの {@code damageSources().magic()} 等を使わずに、 mod 独自の
 * data-driven な DamageType ( data/the_four_primitives_and_weapons/damage_type/* ) から
 * DamageSource を作るためのヘルパー。
 *
 * 取得失敗時のフォールバックは {@code damageSources().magic()}。
 */
public final class ModDamageSources {
	private ModDamageSources() {}

	public static DamageSource of(Level level, ResourceKey<DamageType> key) {
		return of(level, key, null, null);
	}

	public static DamageSource of(Level level, ResourceKey<DamageType> key, Entity attacker) {
		return of(level, key, attacker, attacker);
	}

	public static DamageSource of(Level level, ResourceKey<DamageType> key, Entity directEntity, Entity causingEntity) {
		try {
			Holder.Reference<DamageType> holder = level.registryAccess()
					.registryOrThrow(Registries.DAMAGE_TYPE)
					.getHolderOrThrow(key);
			return new DamageSource(holder, directEntity, causingEntity);
		} catch (Throwable t) {
			// data pack が未投入 / registry 解決失敗時は magic にフォールバック (見た目だけ崩れる)
			return level.damageSources().magic();
		}
	}

	/** ElementType → mod の DamageType key */
	public static ResourceKey<DamageType> keyFor(ElementType element) {
		if (element == null) return TheFourPrimitivesAndWeaponsModDamageTypes.ERROR;
		switch (element) {
			case HOLY:      return TheFourPrimitivesAndWeaponsModDamageTypes.HOLY;
			case ICE:       return TheFourPrimitivesAndWeaponsModDamageTypes.ICE;
			case ELECTRIC:  return TheFourPrimitivesAndWeaponsModDamageTypes.ELECTRIC;
			case THUNDER:   return TheFourPrimitivesAndWeaponsModDamageTypes.THUNDER;
			case CORROSION: return TheFourPrimitivesAndWeaponsModDamageTypes.CORROSION;
			case DARK:      return TheFourPrimitivesAndWeaponsModDamageTypes.DARK;
			case MIASMA:    return TheFourPrimitivesAndWeaponsModDamageTypes.MIASMA;
			case FIRE:      return TheFourPrimitivesAndWeaponsModDamageTypes.FIRE;
			case WATER:     return TheFourPrimitivesAndWeaponsModDamageTypes.WATER;
			case WIND:      return TheFourPrimitivesAndWeaponsModDamageTypes.WIND;
			default:        return TheFourPrimitivesAndWeaponsModDamageTypes.ERROR;
		}
	}

	/** ElementType + attacker から DamageSource を生成 (mixin で elementType / level も付与する想定) */
	public static DamageSource ofElement(Level level, ElementType element, Entity attacker) {
		return of(level, keyFor(element), attacker);
	}
}

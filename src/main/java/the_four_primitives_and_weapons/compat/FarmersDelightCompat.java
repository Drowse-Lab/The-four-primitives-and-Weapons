package the_four_primitives_and_weapons.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Farmer's Delight (farmersdelight) 連携。
 *
 * ロードされていれば「満腹 (Nourishment)」エフェクトの有無を判定できる。
 * ロードされていなければ常に false を返すだけで、本 MOD は通常どおり動作する。
 *
 * 結合はレジストリ参照のみ (compile 依存不要 / 未導入でもロード失敗しない)。
 */
public final class FarmersDelightCompat {
    public static final String MOD_ID = "farmersdelight";

    /** Farmer's Delight の「満腹」エフェクト ID */
    private static final ResourceLocation NOURISHMENT_ID = new ResourceLocation(MOD_ID, "nourishment");

    private static Boolean loaded;
    private static boolean resolved;
    private static MobEffect nourishment;

    private FarmersDelightCompat() {}

    public static boolean isLoaded() {
        if (loaded == null) loaded = ModList.get().isLoaded(MOD_ID);
        return loaded;
    }

    /** 満腹エフェクト (未導入 / ID 変更時は null) */
    public static MobEffect nourishmentEffect() {
        if (!resolved) {
            resolved = true;
            if (isLoaded()) {
                nourishment = ForgeRegistries.MOB_EFFECTS.getValue(NOURISHMENT_ID);
            }
        }
        return nourishment;
    }

    /** 対象が Farmer's Delight の「満腹」エフェクト中か */
    public static boolean hasNourishment(LivingEntity entity) {
        if (entity == null) return false;
        MobEffect eff = nourishmentEffect();
        return eff != null && entity.hasEffect(eff);
    }
}

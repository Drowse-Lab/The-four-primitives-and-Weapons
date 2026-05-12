package the_four_primitives_and_weapons.compat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;

/**
 * Iron's Spells 'n Spellbooks (irons_spellbooks) 連携。
 *
 * ロードされていれば:
 *   - 本 MOD の Mana 消費を Iron's Spellbooks の PlayerMagicData.setMana/getMana に転送
 *   - 本 MOD の Mana HUD 描画を抑制 (二重バーを避ける)
 *
 * ロードされていなければ: 既存の Attribute ベース ManaHelper のまま動作。
 *
 * 結合は **リフレクションのみ** で行うため、開発時の compile 依存は不要。
 * Iron's Spellbooks の API が変わっても本 MOD はロード失敗しない (getMana が -1 を返す)。
 */
public final class SpellbooksCompat {
    public static final String MOD_ID = "irons_spellbooks";

    private static Boolean loaded;
    private static Method mGetPlayerMagicData;
    private static Method mGetMana;
    private static Method mSetMana;

    private SpellbooksCompat() {}

    public static boolean isLoaded() {
        if (loaded == null) {
            boolean present = ModList.get().isLoaded(MOD_ID);
            if (present) present = initReflection();
            loaded = present;
        }
        return loaded;
    }

    private static boolean initReflection() {
        try {
            Class<?> cls = Class.forName(
                "io.redspace.ironsspellbooks.capabilities.magic.PlayerMagicData");
            // PlayerMagicData.getPlayerMagicData(LivingEntity) → static
            mGetPlayerMagicData = cls.getMethod("getPlayerMagicData", LivingEntity.class);
            Class<?> dataType = mGetPlayerMagicData.getReturnType();
            mGetMana = dataType.getMethod("getMana");
            mSetMana = dataType.getMethod("setMana", int.class);
            return true;
        } catch (Throwable t) {
            // クラス/メソッドが見つからなければ無効化 (バージョン不整合でも落ちない)
            return false;
        }
    }

    /** @return Iron's 管理下の Mana、未ロードや失敗時は -1 */
    public static double getMana(Player p) {
        if (!isLoaded()) return -1;
        try {
            Object data = mGetPlayerMagicData.invoke(null, p);
            if (data == null) return -1;
            return ((Number) mGetMana.invoke(data)).doubleValue();
        } catch (Throwable t) {
            return -1;
        }
    }

    /** Iron's 側の Mana を直接設定。未ロードなら何もしない。 */
    public static void setMana(Player p, double value) {
        if (!isLoaded()) return;
        try {
            Object data = mGetPlayerMagicData.invoke(null, p);
            if (data == null) return;
            int clamped = (int) Math.max(0, Math.floor(value));
            mSetMana.invoke(data, clamped);
        } catch (Throwable ignored) {}
    }
}

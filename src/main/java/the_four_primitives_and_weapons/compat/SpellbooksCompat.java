package the_four_primitives_and_weapons.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

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
            // Iron's Spellbooks 3.15.6 の実 API:
            //   io.redspace.ironsspellbooks.api.magic.MagicData
            //     static MagicData getPlayerMagicData(LivingEntity)
            //     float  getMana()
            //     void   setMana(float)
            Class<?> cls = Class.forName(
                "io.redspace.ironsspellbooks.api.magic.MagicData");
            mGetPlayerMagicData = cls.getMethod("getPlayerMagicData", LivingEntity.class);
            mGetMana = cls.getMethod("getMana");
            mSetMana = cls.getMethod("setMana", float.class);
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
            float clamped = (float) Math.max(0.0, value);
            mSetMana.invoke(data, clamped);
        } catch (Throwable ignored) {}
    }

    // ─── Attribute フォワーディング ( max_mana / mana_regen ) ─────────────
    //   Iron's Spellbooks の attribute を ResourceLocation 経由で引いて値を返す。
    //   compile-time dep なしで動くように ForgeRegistries.ATTRIBUTES を走査する。

    private static final ResourceLocation MAX_MANA_ATTR_ID    =
            new ResourceLocation(MOD_ID, "max_mana");
    private static final ResourceLocation MANA_REGEN_ATTR_ID  =
            new ResourceLocation(MOD_ID, "mana_regen");

    private static AttributeInstance lookupAttr(Player p, ResourceLocation id) {
        Attribute attr = ForgeRegistries.ATTRIBUTES.getValue(id);
        return attr == null ? null : p.getAttribute(attr);
    }

    /** @return Iron's 管理下の最大 Mana、未ロード / attribute 無しは -1 */
    public static double getMaxMana(Player p) {
        if (!isLoaded()) return -1;
        AttributeInstance ai = lookupAttr(p, MAX_MANA_ATTR_ID);
        return ai == null ? -1 : ai.getValue();
    }

    /** @return Iron's 管理下のマナ回復速度 ( 1秒あたり )、未ロード / attribute 無しは -1 */
    public static double getManaRegenPerSec(Player p) {
        if (!isLoaded()) return -1;
        AttributeInstance ai = lookupAttr(p, MANA_REGEN_ATTR_ID);
        return ai == null ? -1 : ai.getValue();
    }
}

package minecraftarmorweapon.mana;

import minecraftarmorweapon.compat.SpellbooksCompat;
import minecraftarmorweapon.init.MinecraftArmorWeaponModAttributes;

import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;

/**
 * Manaリソース管理。MANAアトリビュート(0〜MAX)をプレイヤーのMP残量として扱う。
 *
 *   getMana(p)          — 現在値
 *   setMana(p, v)       — 設定 (clamped)
 *   addMana(p, v)       — 加算 (clamped)
 *   tryConsume(p, cost) — 足りれば消費してtrue / 足りなければfalse
 *
 * 最大値は MANA_MAX 定数。クリエイティブは消費なし扱い。
 */
public final class ManaHelper {
    public static final double MANA_MAX = 100.0;
    public static final double MANA_REGEN_PER_TICK = 0.1; // 2秒で4回復程度

    private ManaHelper() {}

    public static double getMana(Player player) {
        // Iron's Spellbooks が入っていたら向こうの Mana を参照
        if (SpellbooksCompat.isLoaded()) {
            double v = SpellbooksCompat.getMana(player);
            if (v >= 0) return v;
        }
        AttributeInstance ai = player.getAttribute(MinecraftArmorWeaponModAttributes.MANA.get());
        return ai == null ? 0 : ai.getBaseValue();
    }

    public static void setMana(Player player, double v) {
        if (SpellbooksCompat.isLoaded()) {
            SpellbooksCompat.setMana(player, v);
            return;
        }
        AttributeInstance ai = player.getAttribute(MinecraftArmorWeaponModAttributes.MANA.get());
        if (ai == null) return;
        ai.setBaseValue(Math.max(0, Math.min(MANA_MAX, v)));
    }

    public static void addMana(Player player, double v) {
        setMana(player, getMana(player) + v);
    }

    /** 足りるなら消費してtrue。クリエイティブは常にtrue (消費なし)。 */
    public static boolean tryConsume(Player player, double cost) {
        if (player.getAbilities().instabuild) return true;
        double cur = getMana(player);
        if (cur < cost) return false;
        setMana(player, cur - cost);
        return true;
    }

    public static double maxMana(Player player) {
        return MANA_MAX;
    }
}

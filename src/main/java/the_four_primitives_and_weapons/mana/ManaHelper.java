package the_four_primitives_and_weapons.mana;

import the_four_primitives_and_weapons.compat.SpellbooksCompat;
import the_four_primitives_and_weapons.init.MawExtraAttributes;
import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModAttributes;

import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;

/**
 * Manaリソース管理。MANAアトリビュートをプレイヤーのMP残量として扱う。
 *
 *   getMana(p)          — 現在値
 *   setMana(p, v)       — 設定 (0 〜 maxMana(p) に clamp)
 *   addMana(p, v)       — 加算
 *   tryConsume(p, cost) — 足りれば消費してtrue / 足りなければfalse
 *   maxMana(p)          — 最大値 (attribute mana_max で決まる)
 *   regenPerTick(p)     — 1tick回復量 (attribute mana_regen)
 *
 * 最大値・回復量は attribute 経由なので装備/効果/コマンドで変更可能。
 * クリエイティブは消費なし扱い。
 */
public final class ManaHelper {
    /** 後方互換のためのデフォルト値。実際の最大/回復は attribute を参照。 */
    public static final double MANA_MAX = 100.0;
    public static final double MANA_REGEN_PER_TICK = 0.1;

    private ManaHelper() {}

    public static double getMana(Player player) {
        if (SpellbooksCompat.isLoaded()) {
            double v = SpellbooksCompat.getMana(player);
            if (v >= 0) return v;
        }
        AttributeInstance ai = player.getAttribute(TheFourPrimitivesAndWeaponsModAttributes.MANA.get());
        return ai == null ? 0 : ai.getBaseValue();
    }

    public static void setMana(Player player, double v) {
        if (SpellbooksCompat.isLoaded()) {
            SpellbooksCompat.setMana(player, v);
            return;
        }
        AttributeInstance ai = player.getAttribute(TheFourPrimitivesAndWeaponsModAttributes.MANA.get());
        if (ai == null) return;
        double cap = maxMana(player);
        ai.setBaseValue(Math.max(0, Math.min(cap, v)));
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

    /** 最大 MP: Spellbooks loaded なら irons_spellbooks:max_mana、 そうでなければ自前 attribute。 */
    public static double maxMana(Player player) {
        if (SpellbooksCompat.isLoaded()) {
            double v = SpellbooksCompat.getMaxMana(player);
            if (v >= 0) return v;
        }
        AttributeInstance ai = player.getAttribute(MawExtraAttributes.MANA_MAX.get());
        return ai == null ? MANA_MAX : ai.getValue();
    }

    /**
     * 1 tick あたり回復量:
     *   Spellbooks loaded → irons_spellbooks:mana_regen ( 1秒値 ) を /20 した値
     *   なければ 自前 attribute "mana_regen" ( 元から 1tick値 ) を返す
     */
    public static double regenPerTick(Player player) {
        if (SpellbooksCompat.isLoaded()) {
            double perSec = SpellbooksCompat.getManaRegenPerSec(player);
            if (perSec >= 0) return perSec / 20.0;
        }
        AttributeInstance ai = player.getAttribute(MawExtraAttributes.MANA_REGEN.get());
        return ai == null ? MANA_REGEN_PER_TICK : ai.getValue();
    }
}

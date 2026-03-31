package minecraftarmorweapon.damage;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 瘴気属性ダメージハンドラー
 *
 * 効果:
 *   1. 回復阻害 — レベルに応じて回復量をカット（MiasmaHealMixin が heal() に介入）
 *        Lv1 = 25%カット  持続 5秒
 *        Lv2 = 50%カット  持続 8秒
 *        Lv3 = 75%カット  持続 11秒
 *        Lv4+ = 100%カット（完全阻害） 持続 14秒〜
 *
 *   2. 持続ダメージ（DoT）— ElementalDoTHandler を使用
 *        Lv1 = 0.5/tick  3秒
 *        Lv2 = 1.0/tick  5秒
 *        Lv3 = 1.5/tick  7秒
 *        Lv4+ = +0.5/tick  +2秒/Lv
 *
 * Wither effect / wither DamageSource は使用しない。
 */
public class MiasmaElementDamageHandler {

    // 基礎ダメージ倍率
    private static final float BASE_MULTIPLIER           = 1.1f;

    // ── 回復阻害 ──────────────────────────────────────────────────
    private static final int   HEAL_DURATION_BASE        = 100; // 5秒(tick)
    private static final int   HEAL_DURATION_PER_LEVEL   = 60;  // +3秒/Lv
    private static final float REDUCTION_PER_LEVEL       = 0.25f;
    private static final float REDUCTION_MAX             = 1.0f;

    // ── DoT ───────────────────────────────────────────────────────
    private static final int   DOT_DURATION_BASE         = 60;  // 3秒(tick)
    private static final int   DOT_DURATION_PER_LEVEL    = 40;  // +2秒/Lv
    private static final float DOT_DAMAGE_BASE           = 0.5f;
    private static final float DOT_DAMAGE_PER_LEVEL      = 0.5f;

    // ────────────────────────────────────────────────────────────────

    /**
     * 瘴気エントリ（MiasmaHealMixin から参照）
     */
    public static class MiasmaEntry {
        public int   remainingTick;
        public float reductionRate;

        public MiasmaEntry(int remainingTick, float reductionRate) {
            this.remainingTick = remainingTick;
            this.reductionRate = reductionRate;
        }
    }

    // entityUUID → MiasmaEntry  (MiasmaHealMixin / MiasmaTickHandler からも参照)
    public static final Map<UUID, MiasmaEntry> miasmaMap = new ConcurrentHashMap<>();

    // ────────────────────────────────────────────────────────────────
    // 公開API（Mixin から参照）
    // ────────────────────────────────────────────────────────────────

    /** 対象が瘴気状態かどうかを返す */
    public static boolean isUnderMiasma(LivingEntity entity) {
        return miasmaMap.containsKey(entity.getUUID());
    }

    /** 対象の回復阻害率を返す（0.0〜1.0）。瘴気状態でない場合は 0.0 */
    public static float getHealReductionRate(LivingEntity entity) {
        MiasmaEntry entry = miasmaMap.get(entity.getUUID());
        return entry != null ? entry.reductionRate : 0.0f;
    }

    /**
     * 回復阻害を付与する。
     * 既存の瘴気がある場合は残り時間が長い方・阻害率が高い方を採用する。
     */
    public static void apply(LivingEntity target, int duration, float reductionRate) {
        UUID id = target.getUUID();
        MiasmaEntry existing = miasmaMap.get(id);

        if (existing != null) {
            existing.remainingTick = Math.max(existing.remainingTick, duration);
            existing.reductionRate = Math.min(
                    Math.max(existing.reductionRate, reductionRate), REDUCTION_MAX);
        } else {
            miasmaMap.put(id, new MiasmaEntry(duration, Math.min(reductionRate, REDUCTION_MAX)));
        }
    }

    /** 瘴気を即時解除する */
    public static void clear(LivingEntity target) {
        miasmaMap.remove(target.getUUID());
    }

    // ────────────────────────────────────────────────────────────────
    // ダメージハンドラー
    // ────────────────────────────────────────────────────────────────

    /**
     * 瘴気属性ダメージを計算して返す。
     * 命中時に回復阻害 + DoT を同時付与する。
     */
    public static float handleMiasmaDamage(LivingEntity attacker,
                                           LivingEntity target,
                                           ItemStack weapon,
                                           float baseDmg) {
        int level = ElementalDamageUtils.getElementLevel(weapon);

        // 1. 回復阻害
        int   healDuration   = HEAL_DURATION_BASE + HEAL_DURATION_PER_LEVEL * (level - 1);
        float reductionRate  = Math.min(REDUCTION_PER_LEVEL * level, REDUCTION_MAX);
        apply(target, healDuration, reductionRate);

        // 2. 持続ダメージ（DoT）
        int   dotDuration  = DOT_DURATION_BASE + DOT_DURATION_PER_LEVEL * (level - 1);
        float dotDmgPerTick = DOT_DAMAGE_BASE + DOT_DAMAGE_PER_LEVEL * (level - 1);
        ElementalDoTHandler.apply(target, dotDuration, dotDmgPerTick, ElementType.MIASMA);

        return baseDmg * BASE_MULTIPLIER;
    }
}

package the_four_primitives_and_weapons.damage;

import the_four_primitives_and_weapons.util.VersionHelper;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import org.joml.Vector3f;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 瘴気属性ダメージハンドラー
 *
 * 効果:
 *   1. 回復阻害 — レベルに応じて回復量をカット (MiasmaHealMixin が heal() に介入)
 *        Lv1 = 25%カット  持続 5秒
 *        Lv2 = 50%カット  持続 8秒
 *        Lv3 = 75%カット  持続 11秒
 *        Lv4+ = 100%カット (完全阻害) 持続 14秒〜
 *
 *   2. ビジュアル — 紫系 dust + WITCH パーティクル
 *
 * 旧仕様の DoT (毒様の継続ダメージ) と Wither effect は使用しない。
 * MobEffect の除去/付与にも依存しない。
 */
public class MiasmaElementDamageHandler {

    // 基礎ダメージ倍率
    private static final float BASE_MULTIPLIER           = 1.1f;

    // ── 回復阻害 ──────────────────────────────────────────────────
    private static final int   HEAL_DURATION_BASE        = 100; // 5秒(tick)
    private static final int   HEAL_DURATION_PER_LEVEL   = 60;  // +3秒/Lv
    private static final float REDUCTION_PER_LEVEL       = 0.25f;
    private static final float REDUCTION_MAX             = 1.0f;

    // ────────────────────────────────────────────────────────────────

    /**
     * 瘴気エントリ (MiasmaHealMixin から参照)
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
    // 公開API (Mixin から参照)
    // ────────────────────────────────────────────────────────────────

    /** 対象が瘴気状態かどうかを返す */
    public static boolean isUnderMiasma(LivingEntity entity) {
        return miasmaMap.containsKey(entity.getUUID());
    }

    /** 対象の回復阻害率を返す (0.0〜1.0)。 瘴気状態でない場合は 0.0 */
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

    /** 瘴気を即時解除する (ResetMax 等から呼ばれる) */
    public static void clear(LivingEntity target) {
        miasmaMap.remove(target.getUUID());
    }

    // ────────────────────────────────────────────────────────────────
    // 内部: ビジュアル
    // ────────────────────────────────────────────────────────────────

    private static void spawnMiasmaParticles(LivingEntity target) {
        try {
            if (VersionHelper.getLevel(target) instanceof ServerLevel sl) {
                double mid = target.getY() + target.getBbHeight() / 2.0;
                // 濃い紫 (メイン)
                DustParticleOptions purple = new DustParticleOptions(
                        new Vector3f(0.45f, 0.1f, 0.7f), 1.3f);
                sl.sendParticles(purple,
                        target.getX(), mid, target.getZ(),
                        18, 0.3, 0.5, 0.3, 0.05);
                // 明るめの紫 (ハイライト)
                DustParticleOptions lightPurple = new DustParticleOptions(
                        new Vector3f(0.75f, 0.4f, 0.95f), 1.0f);
                sl.sendParticles(lightPurple,
                        target.getX(), mid, target.getZ(),
                        12, 0.4, 0.4, 0.4, 0.03);
                // 魔女パーティクル (紫色のオカルト感)
                sl.sendParticles(ParticleTypes.WITCH,
                        target.getX(), mid, target.getZ(),
                        8, 0.3, 0.3, 0.3, 0.02);
            }
        } catch (Throwable ignored) {}
    }

    // ────────────────────────────────────────────────────────────────
    // ダメージハンドラー
    // ────────────────────────────────────────────────────────────────

    /**
     * 瘴気属性ダメージを計算して返す。
     * 命中時に独自回復阻害 + ビジュアルを行う。 DoT は付与しない。
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

        // 2. パーティクル
        spawnMiasmaParticles(target);

        return baseDmg * BASE_MULTIPLIER;
    }
}

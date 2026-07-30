package the_four_primitives_and_weapons.damage;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.item.ItemStack;

/**
 * 血属性ダメージハンドラー
 *
 * 効果:
 *   1. 基礎倍率 1.1 倍。 既に出血している対象へは追撃ボーナス (1.15 倍)。
 *   2. 出血 (Bleed) — blood_dot の独自 DoT。 連撃で積み上がるが {@link #BLEED_DAMAGE_CAP} で打ち止め。
 *   3. 吸血 — 与ダメージの一部を攻撃者に還元 (Lvで上昇 / 1撃あたり上限あり)。
 *
 * 血の無い対象 ({@link MobType#UNDEAD}) には出血も吸血も発生せず、倍率も下がる。
 * MobEffect は使わない ( = 牛乳で消えない / swirl も出ない)。 持ち歩きデバフ側は
 * {@link ElementalCarryDebuffHandler} が担当する。
 */
public class BloodElementDamageHandler {

    // ── ダメージ倍率 ────────────────────────────────────────────────
    private static final float BASE_MULTIPLIER      = 1.10f;
    /** 既に出血中の対象への追撃。 */
    private static final float BLEEDING_MULTIPLIER  = 1.15f;
    /** 血の無い対象 (アンデッド) には効きが悪い。 */
    private static final float BLOODLESS_MULTIPLIER = 0.90f;

    // ── 出血 DoT ───────────────────────────────────────────────────
    private static final int   BLEED_BASE_DURATION      = 60;   // 3秒
    private static final int   BLEED_DURATION_PER_LEVEL = 20;   // +1秒/Lv
    private static final int   BLEED_DURATION_MAX       = 200;  // 10秒
    private static final float BLEED_DAMAGE_BASE        = 0.50f;
    private static final float BLEED_DAMAGE_PER_LEVEL   = 0.15f;
    /** 連撃で加算される出血ダメージの上限 (/tick)。 */
    private static final float BLEED_DAMAGE_CAP         = 2.50f;

    // ── 吸血 ───────────────────────────────────────────────────────
    private static final float LIFESTEAL_RATE_PER_LEVEL = 0.04f; // 与ダメの4%/Lv
    private static final float LIFESTEAL_RATE_MAX       = 0.30f;
    private static final float LIFESTEAL_HEAL_MAX       = 4.0f;  // 1撃あたりの回復上限

    // ────────────────────────────────────────────────────────────────

    /**
     * 血属性ダメージを計算して返す。
     */
    public static float handleBloodDamage(LivingEntity attacker,
                                         LivingEntity target,
                                         ItemStack weapon,
                                         float baseDmg) {
        return calculateDamage(attacker, target,
                baseDmg, ElementalDamageUtils.getEffectiveElementLevel(weapon));
    }

    /**
     * レベル指定で血属性ダメージ計算（魔導書 / スキル経由用）
     */
    public static float calculateDamage(LivingEntity attacker, LivingEntity target,
                                        float baseDmg, int level) {
        if (target == null || level <= 0) return baseDmg;

        // 血の無い相手には出血も吸血も乗らない
        if (isBloodless(target)) {
            return baseDmg * BLOODLESS_MULTIPLIER;
        }

        float multiplier = ElementalDoTHandler.isActive(target, ElementType.BLOOD)
                ? BLEEDING_MULTIPLIER
                : BASE_MULTIPLIER;
        float damage = baseDmg * multiplier;

        applyBleed(target, level);
        applyLifesteal(attacker, damage, level);

        return damage;
    }

    // ────────────────────────────────────────────────────────────────

    /** {@link MobType#UNDEAD} は血が無いものとして扱う。 */
    private static boolean isBloodless(LivingEntity target) {
        return target.getMobType() == MobType.UNDEAD;
    }

    private static void applyBleed(LivingEntity target, int level) {
        int duration = Math.min(BLEED_DURATION_MAX,
                BLEED_BASE_DURATION + BLEED_DURATION_PER_LEVEL * (level - 1));
        float dmgPerTick = BLEED_DAMAGE_BASE + BLEED_DAMAGE_PER_LEVEL * (level - 1);

        // 連撃 (刀など) で無限に積み上がらないよう上限付きで加算する
        ElementalDoTHandler.applyCapped(target, duration, dmgPerTick,
                ElementType.BLOOD, BLEED_DAMAGE_CAP);
    }

    private static void applyLifesteal(LivingEntity attacker, float dealtDamage, int level) {
        if (attacker == null || dealtDamage <= 0.0F) return;
        if (attacker.getHealth() >= attacker.getMaxHealth()) return;

        float rate = Math.min(LIFESTEAL_RATE_MAX, LIFESTEAL_RATE_PER_LEVEL * level);
        float heal = Math.min(LIFESTEAL_HEAL_MAX, dealtDamage * rate);
        if (heal <= 0.0F) return;

        attacker.heal(heal);

        if (attacker.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HEART,
                    attacker.getX(), attacker.getY() + attacker.getBbHeight() * 0.9, attacker.getZ(),
                    1, 0.25, 0.15, 0.25, 0.0);
            ElementalParticles.spawn(serverLevel, ElementType.BLOOD,
                    attacker.getX(), attacker.getY() + attacker.getBbHeight() * 0.5, attacker.getZ(),
                    Math.min(8, 2 + level / 2));
        }
    }
}

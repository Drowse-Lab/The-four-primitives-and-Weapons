package the_four_primitives_and_weapons.damage;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class DarkElementDamageHandler {

    // 基礎倍率
    private static final float BASE_MULTIPLIER          = 1.1f;
    // 暗所ボーナス（光レベル7以下）
    private static final float DARKNESS_MULTIPLIER      = 1.4f;
    private static final int   DARKNESS_LIGHT_THRESHOLD = 7;

    // Blindness持続時間(tick)
    private static final int BASE_BLINDNESS_DURATION    = 60;  // 3秒
    private static final int BLINDNESS_DURATION_PER_LV  = 40;  // +2秒/Lv

    // 持続ダメージ設定
    private static final int   DOT_MIN_LEVEL          = 3;    // 発動最低レベル
    private static final int   DOT_BASE_DURATION      = 60;   // 基礎持続tick (3秒)
    private static final int   DOT_DURATION_PER_LEVEL = 40;   // +2秒/Lv
    private static final float DOT_DAMAGE_BASE        = 0.5f; // 基礎ダメージ/tick
    private static final float DOT_DAMAGE_PER_LEVEL   = 0.5f; // +0.5/tick per Lv

    // ────────────────────────────────────────────────────────────────

    /**
     * 闇属性ダメージを計算して返す。
     */
    public static float handleDarkDamage(LivingEntity attacker,
                                         LivingEntity target,
                                         ItemStack weapon,
                                         float baseDmg) {
        int level = ElementalDamageUtils.getElementLevel(weapon);
        return calculateDamage(attacker, target, baseDmg, level);
    }

    /**
     * レベル指定で闇属性ダメージ計算（魔導書経由用）
     */
    public static float calculateDamage(LivingEntity attacker, LivingEntity target,
                                        float baseDmg, int level) {
        float multiplier = BASE_MULTIPLIER;

        // 暗所ボーナス
        int lightLevel = attacker.level().getBrightness(
                net.minecraft.world.level.LightLayer.BLOCK,
                attacker.blockPosition());
        if (lightLevel <= DARKNESS_LIGHT_THRESHOLD) {
            multiplier = DARKNESS_MULTIPLIER;
        }

        // Blindness付与
        int blindDuration = BASE_BLINDNESS_DURATION
                + BLINDNESS_DURATION_PER_LV * Math.max(level - 1, 0);
        target.addEffect(new MobEffectInstance(
                MobEffects.BLINDNESS, blindDuration, 0, false, true));

        // 独自持続ダメージ（Wither effect / wither source 不使用）
        if (level >= DOT_MIN_LEVEL) {
            int   duration   = DOT_BASE_DURATION
                    + DOT_DURATION_PER_LEVEL * (level - DOT_MIN_LEVEL);
            float dmgPerTick = DOT_DAMAGE_BASE
                    + DOT_DAMAGE_PER_LEVEL * (level - DOT_MIN_LEVEL);
            ElementalDoTHandler.apply(target, duration, dmgPerTick, ElementType.DARK);
        }

        // 既存デバフを強化
        amplifyDebuffs(target, level);

        return baseDmg * multiplier;
    }

    // ────────────────────────────────────────────────────────────────

    /**
     * ターゲットの既存デバフ（有害エフェクト）を強化する。
     * 連続ヒットで延長・増幅が無制限に積み上がらないよう上限を設ける。
     */
    // 延長後の最大持続 (連打しても 30 秒で頭打ち)
    private static final int MAX_EXTENDED_DURATION = 600;
    // 増幅可能な amplifier の上限 (Slowness II 等のバニラ上限を超える事故防止)
    private static final int MAX_AMPLIFIER = 4;

    private static void amplifyDebuffs(LivingEntity target, int level) {
        List<MobEffectInstance> debuffs = new ArrayList<>();
        for (MobEffectInstance effect : target.getActiveEffects()) {
            if (effect.getEffect().getCategory() == MobEffectCategory.HARMFUL) {
                debuffs.add(effect);
            }
        }
        for (MobEffectInstance debuff : debuffs) {
            int extendedDuration = Math.min(debuff.getDuration() + 40 * level, MAX_EXTENDED_DURATION);
            int newAmplifier     = Math.min(debuff.getAmplifier() + (level >= 3 ? 1 : 0), MAX_AMPLIFIER);
            // 元より短くなるなら何もしない (MC の addEffect が無視するので無害だが明示)
            if (extendedDuration <= debuff.getDuration() && newAmplifier <= debuff.getAmplifier()) continue;
            target.addEffect(new MobEffectInstance(
                    debuff.getEffect(),
                    extendedDuration,
                    newAmplifier,
                    debuff.isAmbient(),
                    debuff.isVisible()
            ));
        }
    }
}

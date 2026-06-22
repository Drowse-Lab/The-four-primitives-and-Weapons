package the_four_primitives_and_weapons.damage;

import the_four_primitives_and_weapons.util.VersionHelper;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import org.joml.Vector3f;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 侵食/闇属性ダメージハンドラー
 * - 防御力を一時的に落とす（ダメージに比例）
 * - 高レベルでは ElementalDoTHandler で独自持続ダメージを付与
 *   （Wither effect も wither DamageSource も使用しない）
 */
public class CorrosionElementDamageHandler {

    // 基礎ダメージ倍率
    private static final float  BASE_DAMAGE_MULTIPLIER       = 1.1f;

    // 防御力減少
    private static final double ARMOR_REDUCTION_BASE         = 2.0;
    private static final double ARMOR_REDUCTION_PER_DAMAGE   = 0.5;
    private static final int    ARMOR_REDUCTION_DURATION     = 100; // 5秒(tick)
    private static final UUID   ARMOR_REDUCTION_UUID         =
            UUID.fromString("a3b4c5d6-e7f8-9012-3456-789abcdef012");

    // 攻撃力減少 (旧 WEAKNESS MobEffect の代替 — 牛乳で消えない attribute modifier)
    private static final double ATTACK_REDUCTION_PER_LEVEL   = 0.5; // -0.5 / Lv
    private static final double ATTACK_REDUCTION_MAX         = 4.0; // 上限
    private static final UUID   ATTACK_REDUCTION_UUID        =
            UUID.fromString("b4c5d6e7-f8a9-0123-4567-89abcdef0123");

    // 持続ダメージ設定
    private static final int   DOT_MIN_LEVEL          = 2;    // 発動最低レベル
    private static final int   DOT_BASE_DURATION      = 60;   // 基礎持続tick (3秒)
    private static final int   DOT_DURATION_PER_LEVEL = 40;   // +2秒/Lv
    private static final float DOT_DAMAGE_BASE        = 0.5f; // 基礎ダメージ/tick
    private static final float DOT_DAMAGE_PER_LEVEL   = 0.5f; // +0.5/tick per Lv

    // 防御力減少タイマー
    private static final Map<UUID, Integer> armorReductionTimers = new ConcurrentHashMap<>();

    // ────────────────────────────────────────────────────────────────

    public static float calculateDamage(LivingEntity target, float originalDamage, int elementLevel) {
        // null / 死亡 / クライアント側ガード — Essential 等の他 MOD とのレース対策で防御的に弾く
        if (target == null || !target.isAlive()) {
            return originalDamage * BASE_DAMAGE_MULTIPLIER;
        }
        if (target.level().isClientSide()) {
            return originalDamage * BASE_DAMAGE_MULTIPLIER;
        }

        // 防御力減少 (modifier 操作は throw する可能性があるので try/catch)
        try {
            double armorReduction = (ARMOR_REDUCTION_BASE + originalDamage * ARMOR_REDUCTION_PER_DAMAGE)
                    * (1.0 + elementLevel * 0.2);
            if (Double.isFinite(armorReduction) && armorReduction > 0) {
                applyArmorReduction(target, armorReduction, ARMOR_REDUCTION_DURATION);
            }
        } catch (Throwable t) {
            // 失敗してもダメージは通す
        }

        // 防具の耐久値を少し削る (侵食属性の追加効果)
        //   Lv1-2 = 1, Lv3-4 = 1-2, Lv10 = 5  amount/装備
        //   target 自身が onBroken 通知の対象 (broadcastBreakEvent はバニラ既定)
        try {
            int wear = Math.max(1, elementLevel / 2);
            for (ItemStack armor : target.getArmorSlots()) {
                if (armor == null || armor.isEmpty()) continue;
                if (!armor.isDamageableItem()) continue;
                try {
                    armor.hurtAndBreak(wear, target, e -> { /* no slot-specific break callback */ });
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}

        // 攻撃力減少 (MobEffect.WEAKNESS は牛乳で消されるので attribute modifier で実装)
        try {
            double atkReduction = Math.min(
                    ATTACK_REDUCTION_PER_LEVEL * Math.max(elementLevel, 1),
                    ATTACK_REDUCTION_MAX);
            applyAttackReduction(target, atkReduction, ARMOR_REDUCTION_DURATION);
        } catch (Throwable ignored) {}

        // 独自持続ダメージ（Wither effect / wither source 不使用）
        if (elementLevel >= DOT_MIN_LEVEL) {
            try {
                int   duration   = DOT_BASE_DURATION
                        + DOT_DURATION_PER_LEVEL * (elementLevel - DOT_MIN_LEVEL);
                float dmgPerTick = DOT_DAMAGE_BASE
                        + DOT_DAMAGE_PER_LEVEL * (elementLevel - DOT_MIN_LEVEL);
                ElementalDoTHandler.apply(target, duration, dmgPerTick, ElementType.CORROSION);
            } catch (Throwable ignored) {}
        }

        // パーティクル — 侵食属性のイメージカラー: 赤紫 (マゼンタ寄り)
        try {
            if (VersionHelper.getLevel(target) instanceof ServerLevel serverLevel) {
                double mid = target.getY() + target.getBbHeight() / 2.0;
                // 濃い赤紫 (メイン)
                DustParticleOptions magenta = new DustParticleOptions(
                        new Vector3f(0.75f, 0.1f, 0.55f), 1.3f);
                serverLevel.sendParticles(magenta,
                        target.getX(), mid, target.getZ(),
                        20, 0.3, 0.5, 0.3, 0.05);
                // 明るめの赤紫 (ハイライト、 ピンク寄り)
                DustParticleOptions pinkMagenta = new DustParticleOptions(
                        new Vector3f(1.0f, 0.35f, 0.7f), 1.0f);
                serverLevel.sendParticles(pinkMagenta,
                        target.getX(), mid, target.getZ(),
                        15, 0.4, 0.4, 0.4, 0.03);
                // 桜の花びら (アンビエント、 ピンクのアクセント)
                serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES,
                        target.getX(), mid, target.getZ(),
                        6, 0.3, 0.4, 0.3, 0.0);
            }
        } catch (Throwable ignored) {}

        return originalDamage * BASE_DAMAGE_MULTIPLIER;
    }

    // ────────────────────────────────────────────────────────────────

    /** ResetMax 等から呼ばれる: 侵食属性の attribute 系デバフを即時解除 */
    public static void clear(LivingEntity entity) {
        if (entity == null) return;
        UUID id = entity.getUUID();
        armorReductionTimers.remove(id);
        try {
            AttributeInstance attr = entity.getAttribute(Attributes.ARMOR);
            if (attr != null) {
                try { attr.removeModifier(ARMOR_REDUCTION_UUID); }
                catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
        try {
            AttributeInstance atk = entity.getAttribute(Attributes.ATTACK_DAMAGE);
            if (atk != null) {
                try { atk.removeModifier(ATTACK_REDUCTION_UUID); }
                catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
    }

    private static void applyAttackReduction(LivingEntity entity, double amount, int duration) {
        if (entity == null) return;
        AttributeInstance attr = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attr == null) return;
        AttributeModifier existing = attr.getModifier(ATTACK_REDUCTION_UUID);
        if (existing != null) {
            try { attr.removeModifier(existing); } catch (Throwable ignored) {}
        }
        try {
            attr.addTransientModifier(new AttributeModifier(
                    ATTACK_REDUCTION_UUID,
                    "corrosion_attack_reduction",
                    -amount,
                    AttributeModifier.Operation.ADDITION
            ));
        } catch (IllegalArgumentException dup) {
            // 既に付与済 → そのまま継続 (timer 共有で expire 時に両方除去される)
        }
        // armorReductionTimers にエントリがあれば自動で attribute modifier が removeModifier される (同じ timer 管理に乗せる)
        armorReductionTimers.put(entity.getUUID(), duration);
    }

    private static void applyArmorReduction(LivingEntity entity, double amount, int duration) {
        if (entity == null) return;
        AttributeInstance armorAttr = entity.getAttribute(Attributes.ARMOR);
        if (armorAttr == null) return;

        // 既存 modifier の確実な除去 (vanilla の addTransientModifier は同 UUID で throw する)
        AttributeModifier existing = armorAttr.getModifier(ARMOR_REDUCTION_UUID);
        if (existing != null) {
            try { armorAttr.removeModifier(existing); }
            catch (Throwable ignored) {}
        }
        // ↑ で残っている可能性に備えて add は try/catch
        try {
            armorAttr.addTransientModifier(new AttributeModifier(
                    ARMOR_REDUCTION_UUID,
                    "corrosion_armor_reduction",
                    -amount,
                    AttributeModifier.Operation.ADDITION
            ));
        } catch (IllegalArgumentException dup) {
            // すでに付与されている → そのまま継続 (timer だけ更新)
        }
        armorReductionTimers.put(entity.getUUID(), duration);
    }

    // ────────────────────────────────────────────────────────────────

    @Mod.EventBusSubscriber(modid = "the_four_primitives_and_weapons")
    public static class CorrosionTickHandler {

        @SubscribeEvent
        public static void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            if (event.getServer() == null) return;

            // ConcurrentHashMap の removeIf に渡される Entry が SimpleImmutableEntry で
            // entry.setValue() が UnsupportedOperationException を投げる JDK 実装がある
            // (実機で発生報告あり)。 そのため entry を変更せず、 削除対象だけ集めて
            // 一括 remove、 残りは put で値を更新するパターンに変更する。
            java.util.List<java.util.UUID> toExpire = new java.util.ArrayList<>();
            armorReductionTimers.forEach((id, remaining) -> {
                if (remaining == null) {
                    toExpire.add(id);
                    return;
                }
                int next = remaining - 1;
                if (next <= 0) {
                    toExpire.add(id);
                } else {
                    // ConcurrentHashMap の put はスレッドセーフ
                    armorReductionTimers.put(id, next);
                }
            });

            // 期限切れ entry: ARMOR / ATTACK_DAMAGE 両 modifier を取り除く + マップから削除
            for (java.util.UUID id : toExpire) {
                try {
                    for (ServerLevel level : event.getServer().getAllLevels()) {
                        net.minecraft.world.entity.Entity e = level.getEntity(id);
                        if (e instanceof LivingEntity living) {
                            AttributeInstance armorAttr = living.getAttribute(Attributes.ARMOR);
                            if (armorAttr != null) {
                                try { armorAttr.removeModifier(ARMOR_REDUCTION_UUID); }
                                catch (Throwable ignored) {}
                            }
                            AttributeInstance atkAttr = living.getAttribute(Attributes.ATTACK_DAMAGE);
                            if (atkAttr != null) {
                                try { atkAttr.removeModifier(ATTACK_REDUCTION_UUID); }
                                catch (Throwable ignored) {}
                            }
                            break;
                        }
                    }
                } catch (Throwable ignored) {
                    // 個別失敗はログにせず黙殺 (map からは下で必ず消す)
                }
                armorReductionTimers.remove(id);
            }
        }
    }

    // ────────────────────────────────────────────────────────────────

    public static void applyCorrosionDamage(LivingEntity target, float damage,
                                             LivingEntity source, int level) {
        // カスタム DamageType: the_four_primitives_and_weapons:corrosion
        DamageSource ds = ModDamageSources.ofElement(target.level(), ElementType.CORROSION, source);
        IElementalDamageSource elementalSource = (IElementalDamageSource) ds;
        elementalSource.setElementType(ElementType.CORROSION);
        elementalSource.setElementLevel(level);
        target.hurt(ds, damage);
    }

    public static float handleCorrosionDamage(LivingEntity attacker, LivingEntity target,
                                               ItemStack weapon, float baseDmg) {
        int   level  = ElementalDamageUtils.getElementLevel(weapon);
        float damage = calculateDamage(target, baseDmg, level);
        applyCorrosionDamage(target, damage - baseDmg, attacker, level);
        return damage;
    }
}

package the_four_primitives_and_weapons.damage;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 特殊技 / 武器固有スキルの "デバフ" を MobEffect (= 牛乳で消えるし swirl パーティクル出る) ではなく
 * Attribute Modifier + 内部 state + ElementalDoTHandler の組み合わせで実装するヘルパー。
 *
 * 公開 API:
 *   - {@link #applySlowness}: 移動速度低下 ( MOVEMENT_SPEED の transient modifier )
 *   - {@link #applyWeakness}: 攻撃力低下 ( ATTACK_DAMAGE の transient modifier )
 *   - {@link #applyWither}: 闇属性 DoT として ElementalDoTHandler 経由で持続ダメージ
 *   - {@link #clear}: 全ての SpecialDebuff を即時解除 ( ResetMax から )
 *
 * 既存の ElementalDamageHandler 個別実装 (Water / Corrosion / Ice) はそのまま残し、
 * 「特殊技」 で使う汎用パスとしてこのクラスを使う。
 */
public final class SpecialDebuffHandler {

    private SpecialDebuffHandler() {}

    // ── slow ───────────────────────────────────────────────────────
    private static final UUID SLOW_UUID =
            UUID.fromString("e1f2a3b4-c5d6-7890-1234-56789abcdef0");
    private static final double SLOW_AMP_PER_LEVEL = 0.15; // -15% / level
    private static final double SLOW_AMP_MAX       = 0.95;

    // ── weakness (attack reduction) ────────────────────────────────
    private static final UUID WEAK_UUID =
            UUID.fromString("f2a3b4c5-d6e7-8901-2345-6789abcdef01");
    private static final double WEAK_AMP_PER_LEVEL = 0.5;  // -0.5 atk / level (ADDITION)
    private static final double WEAK_AMP_MAX       = 5.0;

    public static class Entry {
        public int slowRemaining;
        public int weakRemaining;
    }
    /** UUID → 残り tick (slow + weak) */
    private static final Map<UUID, Entry> map = new ConcurrentHashMap<>();

    // ────────────────────────────────────────────────────────────────

    /**
     * 移動速度低下を付与。
     * @param target 対象
     * @param durationTicks 持続 tick ( 20 = 1 秒 )
     * @param level 強度 ( 1〜4 ぐらいを想定。 7 以上は -95% にキャップ )
     */
    public static void applySlowness(LivingEntity target, int durationTicks, int level) {
        if (target == null || target.level().isClientSide()) return;
        if (durationTicks <= 0 || level <= 0) return;
        UUID id = target.getUUID();
        Entry e = map.computeIfAbsent(id, k -> new Entry());
        e.slowRemaining = Math.max(e.slowRemaining, durationTicks);

        try {
            AttributeInstance attr = target.getAttribute(Attributes.MOVEMENT_SPEED);
            if (attr != null) {
                AttributeModifier existing = attr.getModifier(SLOW_UUID);
                if (existing != null) {
                    try { attr.removeModifier(existing); } catch (Throwable ignored) {}
                }
                double amount = Math.min(SLOW_AMP_PER_LEVEL * level, SLOW_AMP_MAX);
                try {
                    attr.addTransientModifier(new AttributeModifier(
                            SLOW_UUID,
                            "special_slow",
                            -amount,
                            AttributeModifier.Operation.MULTIPLY_TOTAL));
                } catch (IllegalArgumentException dup) { /* OK */ }
            }
        } catch (Throwable ignored) {}
    }

    /**
     * 攻撃力低下を付与。
     * @param target 対象
     * @param durationTicks 持続 tick
     * @param level 強度 ( -0.5 × level の ATTACK_DAMAGE 加算修飾 )
     */
    public static void applyWeakness(LivingEntity target, int durationTicks, int level) {
        if (target == null || target.level().isClientSide()) return;
        if (durationTicks <= 0 || level <= 0) return;
        UUID id = target.getUUID();
        Entry e = map.computeIfAbsent(id, k -> new Entry());
        e.weakRemaining = Math.max(e.weakRemaining, durationTicks);

        try {
            AttributeInstance attr = target.getAttribute(Attributes.ATTACK_DAMAGE);
            if (attr != null) {
                AttributeModifier existing = attr.getModifier(WEAK_UUID);
                if (existing != null) {
                    try { attr.removeModifier(existing); } catch (Throwable ignored) {}
                }
                double amount = Math.min(WEAK_AMP_PER_LEVEL * level, WEAK_AMP_MAX);
                try {
                    attr.addTransientModifier(new AttributeModifier(
                            WEAK_UUID,
                            "special_weakness",
                            -amount,
                            AttributeModifier.Operation.ADDITION));
                } catch (IllegalArgumentException dup) { /* OK */ }
            }
        } catch (Throwable ignored) {}
    }

    /**
     * "Wither 系" 持続ダメージ。 ElementalDoTHandler 経由で DARK 属性 DoT として処理。
     * 各 tick のダメージは custom DamageType (= dark_dot) で適用されるので
     * 死亡メッセージや debug 表記もカスタムダメージとして見える。
     */
    public static void applyWither(LivingEntity target, int durationTicks, float dmgPerTick) {
        if (target == null || target.level().isClientSide()) return;
        if (durationTicks <= 0 || dmgPerTick <= 0) return;
        try {
            ElementalDoTHandler.apply(target, durationTicks, dmgPerTick, ElementType.DARK);
        } catch (Throwable ignored) {}
    }

    /**
     * "出血 (Bleed)" 持続ダメージ。 BLOOD 属性 DoT (= blood_dot カスタム DamageType) として処理。
     * Rivers of Blood 等の血属性武器用。
     */
    public static void applyBleed(LivingEntity target, int durationTicks, float dmgPerTick) {
        if (target == null || target.level().isClientSide()) return;
        if (durationTicks <= 0 || dmgPerTick <= 0) return;
        try {
            ElementalDoTHandler.apply(target, durationTicks, dmgPerTick, ElementType.BLOOD);
        } catch (Throwable ignored) {}
    }

    /** ResetMax 等から呼ばれる: 全ての SpecialDebuff を即時解除 */
    public static void clear(LivingEntity target) {
        if (target == null) return;
        map.remove(target.getUUID());
        try {
            AttributeInstance ms = target.getAttribute(Attributes.MOVEMENT_SPEED);
            if (ms != null) {
                try { ms.removeModifier(SLOW_UUID); } catch (Throwable ignored) {}
            }
            AttributeInstance atk = target.getAttribute(Attributes.ATTACK_DAMAGE);
            if (atk != null) {
                try { atk.removeModifier(WEAK_UUID); } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
    }

    // ────────────────────────────────────────────────────────────────
    // Tick handler — タイマー減算 + 期限切れ時に modifier 除去
    // ────────────────────────────────────────────────────────────────

    @Mod.EventBusSubscriber(modid = "the_four_primitives_and_weapons")
    public static class SpecialDebuffTickHandler {
        @SubscribeEvent
        public static void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            if (event.getServer() == null) return;

            List<UUID> slowExpired = new ArrayList<>();
            List<UUID> weakExpired = new ArrayList<>();
            List<UUID> bothEmpty   = new ArrayList<>();

            map.forEach((id, e) -> {
                if (e == null) { bothEmpty.add(id); return; }
                if (e.slowRemaining > 0) {
                    e.slowRemaining--;
                    if (e.slowRemaining <= 0) slowExpired.add(id);
                }
                if (e.weakRemaining > 0) {
                    e.weakRemaining--;
                    if (e.weakRemaining <= 0) weakExpired.add(id);
                }
                if (e.slowRemaining <= 0 && e.weakRemaining <= 0) {
                    bothEmpty.add(id);
                }
            });

            for (UUID id : slowExpired) removeModifier(event, id, Attributes.MOVEMENT_SPEED, SLOW_UUID);
            for (UUID id : weakExpired) removeModifier(event, id, Attributes.ATTACK_DAMAGE, WEAK_UUID);
            for (UUID id : bothEmpty)   map.remove(id);
        }

        private static void removeModifier(TickEvent.ServerTickEvent event, UUID entityId,
                                           net.minecraft.world.entity.ai.attributes.Attribute attr, UUID modUuid) {
            try {
                for (ServerLevel level : event.getServer().getAllLevels()) {
                    Entity e = level.getEntity(entityId);
                    if (e instanceof LivingEntity living) {
                        AttributeInstance ai = living.getAttribute(attr);
                        if (ai != null) {
                            try { ai.removeModifier(modUuid); } catch (Throwable ignored) {}
                        }
                        break;
                    }
                }
            } catch (Throwable ignored) {}
        }
    }
}

package the_four_primitives_and_weapons.damage;

import the_four_primitives_and_weapons.util.VersionHelper;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 氷属性ダメージハンドラー
 *
 * 設計方針:
 *   - 牛乳で消える MobEffect (Slowness 等) には依存しない。
 *   - 凍結状態は内部 Map ({@link #frozenMap}) と Attribute Modifier で管理。
 *   - 砕氷追撃: 内部 frozen state + ticksFrozen の両方を元にボーナスダメージを計算。
 */
public class IceElementDamageHandler {

    // 基礎倍率
    private static final float BASE_DAMAGE_MULTIPLIER = 1.5f;
    private static final float LEVEL_DAMAGE_MULTIPLIER = 0.25f;
    private static final float MAX_TIME_BONUS = 0.5f;

    /** 砕氷追撃: ticksFrozen が getTicksRequiredToFreeze() * 0.7 を超えた時の追加倍率 */
    private static final float ICE_BREAK_BONUS = 0.5f;

    /** ice slow attribute modifier の UUID (常に同じ) */
    private static final UUID ICE_SLOW_UUID =
            UUID.fromString("d7e8f9a0-b1c2-3d4e-5f60-718293a4b5c6");

    /** レベルあたりの移動速度低減 (multiply_total) */
    private static final double ICE_SLOW_PER_LEVEL = 0.15;

    // 同一tickでの二重適用ガード
    private static final Map<UUID, Long> lastAppliedTick = new ConcurrentHashMap<>();

    // ────────────────────────────────────────────────────────────────
    // 内部 frozen state (牛乳で消えない)
    // ────────────────────────────────────────────────────────────────

    public static class FrozenEntry {
        public int remainingTick;
        public int frozenLevel; // 1-3
        public long startGameTime;

        public FrozenEntry(int remainingTick, int frozenLevel, long startGameTime) {
            this.remainingTick = remainingTick;
            this.frozenLevel = frozenLevel;
            this.startGameTime = startGameTime;
        }
    }

    public static final Map<UUID, FrozenEntry> frozenMap = new ConcurrentHashMap<>();

    /** ResetMax 等から呼ばれる: 氷状態を即時解除 */
    public static void clear(LivingEntity entity) {
        if (entity == null) return;
        UUID id = entity.getUUID();
        frozenMap.remove(id);
        try {
            AttributeInstance attr = entity.getAttribute(Attributes.MOVEMENT_SPEED);
            if (attr != null) {
                try { attr.removeModifier(ICE_SLOW_UUID); } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
        try { entity.setTicksFrozen(0); } catch (Throwable ignored) {}
    }

    // ────────────────────────────────────────────────────────────────
    // ダメージ計算
    // ────────────────────────────────────────────────────────────────

    /**
     * 氷属性ダメージを計算。 既存の frozenMap entry と ticksFrozen から砕氷追撃ボーナスを算出。
     */
    public static float calculateDamage(LivingEntity target, float originalDamage, int elementLevel) {
        float damageMultiplier = 1.0f;

        // 既存の凍結状態 → 追撃ボーナス
        FrozenEntry entry = frozenMap.get(target.getUUID());
        if (entry != null && entry.remainingTick > 0) {
            damageMultiplier = BASE_DAMAGE_MULTIPLIER + (entry.frozenLevel * LEVEL_DAMAGE_MULTIPLIER);
            float timeBonus = Math.min(entry.remainingTick / 60.0f, 1.0f) * MAX_TIME_BONUS;
            damageMultiplier += timeBonus;
        }

        // 砕氷追撃: ticksFrozen が高い (= 半凍結以上) → +50% ボーナス
        try {
            int maxFrozen = target.getTicksRequiredToFreeze();
            if (maxFrozen > 0 && target.getTicksFrozen() >= maxFrozen * 0.7) {
                damageMultiplier += ICE_BREAK_BONUS;
                spawnIceBreakParticles(target);
            }
        } catch (Throwable ignored) {}

        // 同一tickでの副作用 (delay / refresh) は 1 回だけ
        long currentTick = target.level().getGameTime();
        Long lastTick = lastAppliedTick.get(target.getUUID());
        if (lastTick != null && lastTick == currentTick) {
            return originalDamage * damageMultiplier;
        }
        lastAppliedTick.put(target.getUUID(), currentTick);

        // 凍結状態を更新 (内部 map)
        int appliedLevel = Math.max(1, Math.min(elementLevel, 3));
        if (entry == null) {
            frozenMap.put(target.getUUID(),
                    new FrozenEntry(60, appliedLevel, currentTick));
        } else {
            entry.remainingTick = Math.min(entry.remainingTick + 20, 120);
            entry.frozenLevel = Math.max(entry.frozenLevel, appliedLevel);
        }

        // 移動速度低減を attribute modifier で付与 (牛乳で消えない)
        applyIceSlowAttribute(target, appliedLevel);

        // 視覚効果: 凍結 ticks (vanilla の "氷漬け" 見た目)
        try {
            int maxFrozen = target.getTicksRequiredToFreeze();
            target.setTicksFrozen(Math.min(target.getTicksFrozen() + 40, maxFrozen));
        } catch (Throwable ignored) {}

        // パーティクル
        if (VersionHelper.getLevel(target) instanceof ServerLevel sl) {
            try {
                sl.sendParticles(ParticleTypes.SNOWFLAKE,
                        target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                        20, 0.3, 0.5, 0.3, 0.05);
                sl.sendParticles(ParticleTypes.ITEM_SNOWBALL,
                        target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                        15, 0.4, 0.4, 0.4, 0.1);
            } catch (Throwable ignored) {}
        }

        return originalDamage * damageMultiplier;
    }

    public static void applyIceDamage(LivingEntity target, float damage, LivingEntity source, int level) {
        net.minecraft.world.damagesource.DamageSource ds =
                ModDamageSources.ofElement(target.level(), ElementType.ICE, source);
        IElementalDamageSource elementalSource = (IElementalDamageSource) ds;
        elementalSource.setElementType(ElementType.ICE);
        elementalSource.setElementLevel(level);
        target.hurt(ds, damage);
    }

    public static float handleIceDamage(LivingEntity attacker, LivingEntity target, ItemStack weapon, float baseDmg) {
        int level = ElementalDamageUtils.getElementLevel(weapon);
        float damage = calculateDamage(target, baseDmg, level);
        applyIceDamage(target, damage - baseDmg, attacker, level);
        return damage;
    }

    // ────────────────────────────────────────────────────────────────
    // attribute modifier 管理
    // ────────────────────────────────────────────────────────────────

    private static void applyIceSlowAttribute(LivingEntity entity, int level) {
        AttributeInstance attr = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr == null) return;
        AttributeModifier existing = attr.getModifier(ICE_SLOW_UUID);
        if (existing != null) {
            try { attr.removeModifier(existing); } catch (Throwable ignored) {}
        }
        try {
            attr.addTransientModifier(new AttributeModifier(
                    ICE_SLOW_UUID,
                    "ice_slow",
                    -ICE_SLOW_PER_LEVEL * level,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
        } catch (IllegalArgumentException dup) {
            // 既に付与済 → 無視
        }
    }

    private static void spawnIceBreakParticles(LivingEntity target) {
        if (!(VersionHelper.getLevel(target) instanceof ServerLevel sl)) return;
        try {
            sl.sendParticles(ParticleTypes.ITEM_SNOWBALL,
                    target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                    30, 0.4, 0.5, 0.4, 0.2);
            sl.sendParticles(ParticleTypes.SNOWFLAKE,
                    target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                    40, 0.3, 0.4, 0.3, 0.15);
        } catch (Throwable ignored) {}
    }

    // ────────────────────────────────────────────────────────────────
    // Tick handler — frozenMap の期限管理 + attribute 後始末
    // ────────────────────────────────────────────────────────────────

    @Mod.EventBusSubscriber(modid = "the_four_primitives_and_weapons")
    public static class IceTickHandler {

        @SubscribeEvent
        public static void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            if (event.getServer() == null) return;

            List<UUID> toExpire = new ArrayList<>();
            frozenMap.forEach((id, entry) -> {
                if (entry == null) {
                    toExpire.add(id);
                    return;
                }
                entry.remainingTick--;
                if (entry.remainingTick <= 0) {
                    toExpire.add(id);
                }
            });

            for (UUID id : toExpire) {
                try {
                    for (ServerLevel level : event.getServer().getAllLevels()) {
                        Entity e = level.getEntity(id);
                        if (e instanceof LivingEntity living) {
                            AttributeInstance attr = living.getAttribute(Attributes.MOVEMENT_SPEED);
                            if (attr != null) {
                                try { attr.removeModifier(ICE_SLOW_UUID); }
                                catch (Throwable ignored) {}
                            }
                            break;
                        }
                    }
                } catch (Throwable ignored) {}
                frozenMap.remove(id);
            }
        }
    }
}

package minecraftarmorweapon.damage;

import minecraftarmorweapon.util.VersionHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
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

        // 防御力減少
        double armorReduction = (ARMOR_REDUCTION_BASE + originalDamage * ARMOR_REDUCTION_PER_DAMAGE)
                * (1.0 + elementLevel * 0.2);
        applyArmorReduction(target, armorReduction, ARMOR_REDUCTION_DURATION);

        // Weakness付与
        target.addEffect(new MobEffectInstance(
                MobEffects.WEAKNESS,
                ARMOR_REDUCTION_DURATION,
                Math.min(elementLevel, 3),
                false, true));

        // 独自持続ダメージ（Wither effect / wither source 不使用）
        if (elementLevel >= DOT_MIN_LEVEL) {
            int   duration   = DOT_BASE_DURATION
                    + DOT_DURATION_PER_LEVEL * (elementLevel - DOT_MIN_LEVEL);
            float dmgPerTick = DOT_DAMAGE_BASE
                    + DOT_DAMAGE_PER_LEVEL * (elementLevel - DOT_MIN_LEVEL);
            ElementalDoTHandler.apply(target, duration, dmgPerTick, ElementType.CORROSION);
        }

        // パーティクル
        if (VersionHelper.getLevel(target) instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SMOKE,
                    target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                    20, 0.3, 0.5, 0.3, 0.05);
            serverLevel.sendParticles(ParticleTypes.SOUL,
                    target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                    15, 0.4, 0.4, 0.4, 0.03);
            serverLevel.sendParticles(ParticleTypes.SQUID_INK,
                    target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                    10, 0.2, 0.3, 0.2, 0.1);
        }

        return originalDamage * BASE_DAMAGE_MULTIPLIER;
    }

    // ────────────────────────────────────────────────────────────────

    private static void applyArmorReduction(LivingEntity entity, double amount, int duration) {
        AttributeInstance armorAttr = entity.getAttribute(Attributes.ARMOR);
        if (armorAttr == null) return;

        if (armorAttr.getModifier(ARMOR_REDUCTION_UUID) != null) {
            armorAttr.removeModifier(ARMOR_REDUCTION_UUID);
        }
        armorAttr.addTransientModifier(new AttributeModifier(
                ARMOR_REDUCTION_UUID,
                "corrosion_armor_reduction",
                -amount,
                AttributeModifier.Operation.ADDITION
        ));
        armorReductionTimers.put(entity.getUUID(), duration);
    }

    // ────────────────────────────────────────────────────────────────

    @Mod.EventBusSubscriber(modid = "minecraft_armor_weapon")
    public static class CorrosionTickHandler {

        @SubscribeEvent
        public static void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            if (event.getServer() == null) return;

            armorReductionTimers.entrySet().removeIf(entry -> {
                int remaining = entry.getValue() - 1;
                if (remaining <= 0) {
                    for (ServerLevel level : event.getServer().getAllLevels()) {
                        net.minecraft.world.entity.Entity e = level.getEntity(entry.getKey());
                        if (e instanceof LivingEntity living) {
                            AttributeInstance attr = living.getAttribute(Attributes.ARMOR);
                            if (attr != null) attr.removeModifier(ARMOR_REDUCTION_UUID);
                            break;
                        }
                    }
                    return true;
                }
                entry.setValue(remaining);
                return false;
            });
        }
    }

    // ────────────────────────────────────────────────────────────────

    public static void applyCorrosionDamage(LivingEntity target, float damage,
                                             LivingEntity source, int level) {
        DamageSource ds = target.damageSources().magic();
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

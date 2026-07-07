package the_four_primitives_and_weapons.damage;

import the_four_primitives_and_weapons.util.VersionHelper;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import org.joml.Vector3f;
import net.minecraft.world.damagesource.DamageSource;
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
 * - MobEffectには依存せず、Attributes.ARMOR の一時modifierで管理
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

        // 侵食属性の効果は「防御力低下のみ ( 一定時間 )」 ( ユーザー仕様 )。
        //   - 旧: 防具耐久消費 → 削除
        //   - 旧: 攻撃力減少    → 削除
        //   - 旧: 持続ダメージ  → 削除
        // 既に上で applyArmorReduction を呼び 防御減少を 期限付きで 付与済み。

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

            // 期限切れ entry: ARMOR modifier を取り除く + マップから削除
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

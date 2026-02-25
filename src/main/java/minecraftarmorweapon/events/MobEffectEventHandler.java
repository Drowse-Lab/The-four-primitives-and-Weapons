package minecraftarmorweapon.events;

import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

/**
 * すべてのMobに盲目などのステータス効果が付与されたときに影響を与えるハンドラー
 *
 * このハンドラーは、Mobにステータス効果が付与された際に：
 * 1. 盲目効果 → 視界・攻撃範囲を大幅に縮小（70%減少）
 * 2. 混乱効果 → 移動速度を低下させ、ランダムな動きを追加
 * 3. 弱体化効果 → 攻撃力を追加で低下（さらに-4）
 * 4. 暗闇効果 → 視界を縮小（50%減少）
 * 5. 発光効果 → 敵に発見されやすくなる
 *
 * 効果が切れたときは元の状態に戻します
 */
@Mod.EventBusSubscriber(modid = "minecraft_armor_weapon")
public class MobEffectEventHandler {

    // 属性修正のUUID（効果ごとに固有）
    private static final UUID BLINDNESS_SPEED_UUID = UUID.fromString("7f3e7e0a-1234-4567-89ab-111111111111");
    private static final UUID BLINDNESS_ATTACK_UUID = UUID.fromString("7f3e7e0a-1234-4567-89ab-222222222222");
    private static final UUID NAUSEA_SPEED_UUID = UUID.fromString("7f3e7e0a-1234-4567-89ab-333333333333");
    private static final UUID WEAKNESS_ATTACK_UUID = UUID.fromString("7f3e7e0a-1234-4567-89ab-444444444444");
    private static final UUID DARKNESS_SPEED_UUID = UUID.fromString("7f3e7e0a-1234-4567-89ab-555555555555");

    // Mobの元の視界範囲を記録（効果が切れたときに復元）
    private static final Map<UUID, Double> originalFollowRanges = new HashMap<>();

    /**
     * ステータス効果が追加されたときのイベント
     */
    @SubscribeEvent
    public static void onMobEffectAdded(MobEffectEvent.Added event) {
        try {
            // サーバー側でのみ処理
            if (event.getEntity().level().isClientSide()) {
                return;
            }

            LivingEntity entity = event.getEntity();
            MobEffectInstance effectInstance = event.getEffectInstance();

            // Mobでない場合はスキップ（プレイヤーには影響しない）
            if (!(entity instanceof Mob mob)) {
                return;
            }

            // 効果の種類に応じて処理
            if (effectInstance.getEffect() == MobEffects.BLINDNESS) {
                applyBlindnessEffect(mob, effectInstance);
            } else if (effectInstance.getEffect() == MobEffects.CONFUSION) {
                applyNauseaEffect(mob, effectInstance);
            } else if (effectInstance.getEffect() == MobEffects.WEAKNESS) {
                applyWeaknessEffect(mob, effectInstance);
            } else if (effectInstance.getEffect() == MobEffects.DARKNESS) {
                applyDarknessEffect(mob, effectInstance);
            } else if (effectInstance.getEffect() == MobEffects.GLOWING) {
                applyGlowingEffect(mob, effectInstance);
            }
        } catch (Throwable e) {
            System.err.println("[MobEffect] Error in onMobEffectAdded: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * ステータス効果が切れたときのイベント
     */
    @SubscribeEvent
    public static void onMobEffectRemoved(MobEffectEvent.Remove event) {
        try {
            // サーバー側でのみ処理
            if (event.getEntity().level().isClientSide()) {
                return;
            }

            LivingEntity entity = event.getEntity();

            // Mobでない場合はスキップ
            if (!(entity instanceof Mob mob)) {
                return;
            }

            // 効果の種類に応じて元に戻す
            if (event.getEffect() == MobEffects.BLINDNESS) {
                removeBlindnessEffect(mob);
            } else if (event.getEffect() == MobEffects.CONFUSION) {
                removeNauseaEffect(mob);
            } else if (event.getEffect() == MobEffects.WEAKNESS) {
                removeWeaknessEffect(mob);
            } else if (event.getEffect() == MobEffects.DARKNESS) {
                removeDarknessEffect(mob);
            }
        } catch (Throwable e) {
            System.err.println("[MobEffect] Error in onMobEffectRemoved: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 盲目効果の適用
     * - 視界範囲を70%縮小
     * - 移動速度を30%低下
     * - 攻撃力を2低下
     */
    private static void applyBlindnessEffect(Mob mob, MobEffectInstance effect) {
        UUID mobId = mob.getUUID();

        // 元の視界範囲を記録
        AttributeInstance followRange = mob.getAttribute(Attributes.FOLLOW_RANGE);
        if (followRange != null && !originalFollowRanges.containsKey(mobId)) {
            originalFollowRanges.put(mobId, followRange.getBaseValue());
        }

        // 視界範囲を70%縮小（元の30%にする）
        if (followRange != null) {
            AttributeModifier modifier = new AttributeModifier(
                BLINDNESS_ATTACK_UUID,
                "blindness_follow_range",
                -0.7,
                AttributeModifier.Operation.MULTIPLY_TOTAL
            );
            followRange.removeModifier(BLINDNESS_ATTACK_UUID);
            followRange.addTransientModifier(modifier);
        }

        // 移動速度を30%低下
        AttributeInstance movementSpeed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed != null) {
            AttributeModifier speedModifier = new AttributeModifier(
                BLINDNESS_SPEED_UUID,
                "blindness_movement_speed",
                -0.3,
                AttributeModifier.Operation.MULTIPLY_TOTAL
            );
            movementSpeed.removeModifier(BLINDNESS_SPEED_UUID);
            movementSpeed.addTransientModifier(speedModifier);
        }

        // 攻撃力を2低下
        AttributeInstance attackDamage = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamage != null) {
            AttributeModifier attackModifier = new AttributeModifier(
                BLINDNESS_ATTACK_UUID,
                "blindness_attack_damage",
                -2.0,
                AttributeModifier.Operation.ADDITION
            );
            attackDamage.removeModifier(BLINDNESS_ATTACK_UUID);
            attackDamage.addTransientModifier(attackModifier);
        }

        System.out.println("[MobEffect] " + mob.getName().getString() +
                          " に盲目効果を適用しました（視界-70%, 速度-30%, 攻撃-2）");
    }

    /**
     * 盲目効果の解除
     */
    private static void removeBlindnessEffect(Mob mob) {
        // 視界範囲を元に戻す
        AttributeInstance followRange = mob.getAttribute(Attributes.FOLLOW_RANGE);
        if (followRange != null) {
            followRange.removeModifier(BLINDNESS_ATTACK_UUID);
        }

        // 移動速度を元に戻す
        AttributeInstance movementSpeed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed != null) {
            movementSpeed.removeModifier(BLINDNESS_SPEED_UUID);
        }

        // 攻撃力を元に戻す
        AttributeInstance attackDamage = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamage != null) {
            attackDamage.removeModifier(BLINDNESS_ATTACK_UUID);
        }

        // 記録を削除
        originalFollowRanges.remove(mob.getUUID());

        System.out.println("[MobEffect] " + mob.getName().getString() +
                          " の盲目効果を解除しました");
    }

    /**
     * 混乱効果の適用
     * - 移動速度を20%低下
     * - ランダムな動きを追加（AI側で処理）
     */
    private static void applyNauseaEffect(Mob mob, MobEffectInstance effect) {
        // 移動速度を20%低下
        AttributeInstance movementSpeed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed != null) {
            AttributeModifier speedModifier = new AttributeModifier(
                NAUSEA_SPEED_UUID,
                "nausea_movement_speed",
                -0.2,
                AttributeModifier.Operation.MULTIPLY_TOTAL
            );
            movementSpeed.removeModifier(NAUSEA_SPEED_UUID);
            movementSpeed.addTransientModifier(speedModifier);
        }

        System.out.println("[MobEffect] " + mob.getName().getString() +
                          " に混乱効果を適用しました（速度-20%）");
    }

    /**
     * 混乱効果の解除
     */
    private static void removeNauseaEffect(Mob mob) {
        AttributeInstance movementSpeed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed != null) {
            movementSpeed.removeModifier(NAUSEA_SPEED_UUID);
        }

        System.out.println("[MobEffect] " + mob.getName().getString() +
                          " の混乱効果を解除しました");
    }

    /**
     * 弱体化効果の適用
     * - 攻撃力をさらに4低下（バニラの効果に追加）
     */
    private static void applyWeaknessEffect(Mob mob, MobEffectInstance effect) {
        // 攻撃力をさらに4低下
        AttributeInstance attackDamage = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamage != null) {
            AttributeModifier attackModifier = new AttributeModifier(
                WEAKNESS_ATTACK_UUID,
                "weakness_attack_damage",
                -4.0,
                AttributeModifier.Operation.ADDITION
            );
            attackDamage.removeModifier(WEAKNESS_ATTACK_UUID);
            attackDamage.addTransientModifier(attackModifier);
        }

        System.out.println("[MobEffect] " + mob.getName().getString() +
                          " に弱体化効果を適用しました（攻撃-4）");
    }

    /**
     * 弱体化効果の解除
     */
    private static void removeWeaknessEffect(Mob mob) {
        AttributeInstance attackDamage = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamage != null) {
            attackDamage.removeModifier(WEAKNESS_ATTACK_UUID);
        }

        System.out.println("[MobEffect] " + mob.getName().getString() +
                          " の弱体化効果を解除しました");
    }

    /**
     * 暗闇効果の適用
     * - 視界範囲を50%縮小
     * - 移動速度を15%低下
     */
    private static void applyDarknessEffect(Mob mob, MobEffectInstance effect) {
        UUID mobId = mob.getUUID();

        // 元の視界範囲を記録
        AttributeInstance followRange = mob.getAttribute(Attributes.FOLLOW_RANGE);
        if (followRange != null && !originalFollowRanges.containsKey(mobId)) {
            originalFollowRanges.put(mobId, followRange.getBaseValue());
        }

        // 視界範囲を50%縮小
        if (followRange != null) {
            AttributeModifier modifier = new AttributeModifier(
                DARKNESS_SPEED_UUID,
                "darkness_follow_range",
                -0.5,
                AttributeModifier.Operation.MULTIPLY_TOTAL
            );
            followRange.removeModifier(DARKNESS_SPEED_UUID);
            followRange.addTransientModifier(modifier);
        }

        // 移動速度を15%低下
        AttributeInstance movementSpeed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed != null) {
            AttributeModifier speedModifier = new AttributeModifier(
                DARKNESS_SPEED_UUID,
                "darkness_movement_speed",
                -0.15,
                AttributeModifier.Operation.MULTIPLY_TOTAL
            );
            movementSpeed.removeModifier(DARKNESS_SPEED_UUID);
            movementSpeed.addTransientModifier(speedModifier);
        }

        System.out.println("[MobEffect] " + mob.getName().getString() +
                          " に暗闇効果を適用しました（視界-50%, 速度-15%）");
    }

    /**
     * 暗闇効果の解除
     */
    private static void removeDarknessEffect(Mob mob) {
        // 視界範囲を元に戻す
        AttributeInstance followRange = mob.getAttribute(Attributes.FOLLOW_RANGE);
        if (followRange != null) {
            followRange.removeModifier(DARKNESS_SPEED_UUID);
        }

        // 移動速度を元に戻す
        AttributeInstance movementSpeed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed != null) {
            movementSpeed.removeModifier(DARKNESS_SPEED_UUID);
        }

        // 記録を削除
        originalFollowRanges.remove(mob.getUUID());

        System.out.println("[MobEffect] " + mob.getName().getString() +
                          " の暗闇効果を解除しました");
    }

    /**
     * 発光効果の適用
     * - 敵に発見されやすくなる（視界範囲を記録するのみ）
     */
    private static void applyGlowingEffect(Mob mob, MobEffectInstance effect) {
        System.out.println("[MobEffect] " + mob.getName().getString() +
                          " に発光効果を適用しました（敵に発見されやすい）");
    }

    /**
     * 定期的に記録をクリーンアップ（メモリリーク防止）
     */
    public static void cleanupRecords() {
        if (originalFollowRanges.size() > 1000) {
            originalFollowRanges.clear();
            System.out.println("[MobEffect] 視界範囲の記録をクリアしました");
        }
    }
}

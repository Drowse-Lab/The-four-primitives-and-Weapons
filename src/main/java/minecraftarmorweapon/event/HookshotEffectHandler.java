package minecraftarmorweapon.event;

import minecraftarmorweapon.MinecraftArmorWeaponMod;
import minecraftarmorweapon.init.CustomMobEffectInit;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * フックショット用の独自 MobEffect の挙動を毎 tick で適用するハンドラ。
 *
 *   - {@link CustomMobEffectInit#HOOKSHOT_FLOAT}      : 浮遊
 *       - vertical 速度を 0.05 に固定 (ゆっくり上昇)
 *       - fallDistance を毎 tick リセット
 *   - {@link CustomMobEffectInit#HOOKSHOT_FALL_GUARD} : 落下ダメージ無効化
 *       - fallDistance を毎 tick リセット
 *       - {@link LivingFallEvent} で setCanceled(true) → 落下ダメージそのものを完全に消す
 *
 * vanilla LEVITATION / SLOW_FALLING を使わず独自 effect にすることで、他システムとの
 * effect 衝突 (Loki Decoy 等が SLOW_FALLING を使用) を避けている。
 */
@Mod.EventBusSubscriber(modid = MinecraftArmorWeaponMod.MODID)
public class HookshotEffectHandler {

    /** 浮遊効果中の上昇速度 (block/tick). */
    private static final double FLOAT_LIFT_VELOCITY = 0.05;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;

        // === 浮遊効果 ===
        if (sp.hasEffect(CustomMobEffectInit.HOOKSHOT_FLOAT.get())) {
            Vec3 motion = sp.getDeltaMovement();
            // 落下を打ち消して、わずかに上昇 (= 元データパックの "浮遊" 感)
            if (motion.y < FLOAT_LIFT_VELOCITY) {
                sp.setDeltaMovement(motion.x, FLOAT_LIFT_VELOCITY, motion.z);
                sp.hurtMarked = true;
            }
            sp.fallDistance = 0f;
        }

        // === 落下ダメージ無効化 ===
        if (sp.hasEffect(CustomMobEffectInit.HOOKSHOT_FALL_GUARD.get())) {
            sp.fallDistance = 0f;
        }
    }

    /** 落下ダメージ判定そのものをキャンセル。fallDistance リセットの取りこぼし保険。 */
    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (event.getEntity().hasEffect(CustomMobEffectInit.HOOKSHOT_FALL_GUARD.get())
            || event.getEntity().hasEffect(CustomMobEffectInit.HOOKSHOT_FLOAT.get())) {
            event.setCanceled(true);
        }
    }
}

package the_four_primitives_and_weapons.event;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.init.CustomMobEffectInit;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * フックショット用の挙動ハンドラ。
 *
 *   - {@link CustomMobEffectInit#HOOKSHOT_FLOAT} : 浮遊 (MobEffect)
 *       - vertical 速度を 0.05 に固定 (ゆっくり上昇)
 *       - fallDistance を毎 tick リセット
 *   - 落下ダメージ無効 grace : {@link RecrossPlayerHandler} 内の FallImmunity カウンタ
 *       (MobEffect で管理しない — client への effect 同期や他システム干渉を避けるため)
 *       - {@link LivingFallEvent} で setCanceled(true) → 落下ダメージそのものを完全に消す
 *
 * vanilla LEVITATION / SLOW_FALLING を使わず独自 effect / カウンタにすることで、他システムとの
 * effect 衝突 (Loki Decoy 等が SLOW_FALLING を使用) を避けている。
 */
@Mod.EventBusSubscriber(modid = TheFourPrimitivesAndWeaponsMod.MODID)
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
        // 落下ダメ無効 grace の tick 処理は RecrossPlayerHandler.onPlayerTick が担当
    }

    /** 落下ダメージ判定そのものをキャンセル。fallDistance リセットの取りこぼし保険。 */
    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (RecrossPlayerHandler.hasFallGuard(event.getEntity())
            || event.getEntity().hasEffect(CustomMobEffectInit.HOOKSHOT_FLOAT.get())) {
            event.setCanceled(true);
        }
    }
}

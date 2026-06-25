package the_four_primitives_and_weapons.event;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.client.event.ChargeParticleEmitter;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 通常武器の attack cooldown ( attackStrengthScale ) を Rivers of Blood と
 * 同じ {@link ChargeParticleEmitter} で演出する。
 *
 *   - 手元 / 足元の 控えめなパーティクル ( 視界を 塞がない )
 *   - フルチャージ ( = scale が 0 → 1 を超えた frame ) で flash + 音
 *
 * 色は 銀 / 鋼 系で Rivers of Blood の赤と差別化。
 */
@Mod.EventBusSubscriber(modid = TheFourPrimitivesAndWeaponsMod.MODID)
public class AttackChargeParticleHandler {

    private static final Vector3f BASE = new Vector3f(0.55f, 0.55f, 0.60f); // 暗銀
    private static final Vector3f PEAK = new Vector3f(0.95f, 0.95f, 1.00f); // 鮮銀

    /** 直前 tick の attackStrengthScale ( 閾値到達 = "前 < 1, 今 >= 1" 検出用 ) */
    private static final Map<UUID, Float> lastScale = new HashMap<>();
    /** 回転リング の位相 ( tick 単調増加 ) */
    private static final Map<UUID, Integer> tickPhase = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.side.isClient()) return; // server で 1 回だけ発火 → ChargeParticleEmitter が両側に配信
        Player player = event.player;
        if (player.isSpectator()) return;

        ItemStack main = player.getMainHandItem();
        if (!isMeleeWeapon(main)) {
            cleanup(player.getUUID());
            return;
        }

        // 使用中アイテム ( Rivers of Blood 等 ) のチャージとは別系統なので除外
        if (player.isUsingItem()) {
            cleanup(player.getUUID());
            return;
        }

        float scale = player.getAttackStrengthScale(1.0f);
        UUID id = player.getUUID();
        float prev = lastScale.getOrDefault(id, scale);
        boolean hitThreshold = prev < 1.0f && scale >= 1.0f;

        // 100% に達したら 1 frame だけ flash 演出を出して 以後は描画しない
        if (scale >= 1.0f && !hitThreshold) {
            lastScale.put(id, scale);
            return;
        }

        // 進行中 ( scale が 0 より大 かつ 1 未満 )、 もしくは 到達 frame
        if (scale > 0.0f || hitThreshold) {
            int phase = tickPhase.getOrDefault(id, 0) + 1;
            tickPhase.put(id, phase);
            ChargeParticleEmitter.emit(
                    player.level(), player, scale, BASE, PEAK, phase, hitThreshold);
        }

        lastScale.put(id, scale);
    }

    private static void cleanup(UUID id) {
        lastScale.remove(id);
        tickPhase.remove(id);
    }

    private static boolean isMeleeWeapon(ItemStack s) {
        if (s.isEmpty()) return false;
        return s.getItem() instanceof SwordItem
                || s.getItem() instanceof AxeItem
                || s.getItem() instanceof TridentItem;
    }
}

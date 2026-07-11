package the_four_primitives_and_weapons.enchantment;

import the_four_primitives_and_weapons.init.CustomEnchantmentInit;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 「強引き寄せ」エンチャントの効果を処理する。
 *
 * 釣竿で右クリック（引き戻し）した時、
 * 引っ掛かったMobに対して強い引き寄せ速度を加える。
 *
 * Lv1: 1.5倍, Lv2: 2.0倍, Lv3: 2.8倍, Lv4: 3.8倍, Lv5: 5.0倍
 */
@Mod.EventBusSubscriber(modid = "the_four_primitives_and_weapons")
public class ReelInHandler {

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;

        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof FishingRodItem)) return;

        // エンチャントレベル取得
        int level = EnchantmentHelper.getItemEnchantmentLevel(
            CustomEnchantmentInit.REEL_IN.get(), stack);
        if (level <= 0) return;

        // プレイヤーの釣針を探す
        FishingHook hook = player.fishing;
        if (hook == null) return;
        Entity hooked = hook.getHookedIn();
        if (hooked == null) return;

        // 引き寄せ係数（レベルが上がるごとに強く）
        double pullStrength = 0.5 + level * 0.6;
        // Lv1: 1.1 / Lv2: 1.7 / Lv3: 2.3 / Lv4: 2.9 / Lv5: 3.5（基本乗算分）
        double totalMultiplier = 1.0 + level * 0.8;

        // 方向ベクトル（引っ掛けた対象 → プレイヤー）
        Vec3 toPlayer = player.position().subtract(hooked.position());
        double distance = toPlayer.length();
        if (distance < 0.5) return; // 近すぎる

        Vec3 dir = toPlayer.normalize();

        // 引き寄せ速度を計算（距離にも比例、近いほど速度を抑える）
        double speed = Math.min(distance * 0.3, 2.0) * totalMultiplier;

        // 上方向の小さなブーストも入れる（ゲーム的に持ち上げる）
        Vec3 velocity = dir.scale(speed).add(0, 0.3 * level, 0);

        hooked.setDeltaMovement(velocity);
        hooked.hurtMarked = true; // クライアントに速度変化を通知

        // 釣られたMobはプレイヤーをターゲットにする
        if (hooked instanceof LivingEntity le && le != player) {
            if (le instanceof net.minecraft.world.entity.Mob mob) {
                mob.setTarget(player);
            }
        }
    }
}

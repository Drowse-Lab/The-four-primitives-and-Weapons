package the_four_primitives_and_weapons.event;

import the_four_primitives_and_weapons.item.ParryShieldItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * パリィシールドのパリィ判定＆Fキースワップ検出ハンドラ
 *
 * ＜パリィ発動の2経路＞
 *
 *   A. オフハンドパリィ
 *      オフハンドで右クリック（ブロック開始）後 PARRY_WINDOW_TICKS 以内に
 *      攻撃を受けると発動。
 *
 *   B. スワップパリィ（Fキー）
 *      Fキーでメインハンドにシールドが来た直後 PARRY_WINDOW_TICKS 以内に
 *      攻撃を受けると発動。
 *      スワップはティックごとに「前ティックのメインハンドアイテム」と
 *      「今ティックのメインハンドアイテム」を比較して検出する。
 *
 * ＜パリィ成功時＞
 *   - 受けたダメージをキャンセル
 *   - 攻撃者に受けたダメージ×1.5の魔法ダメージを反射
 *   - クリットパーティクル＋高音のシールドSE
 */
@Mod.EventBusSubscriber
public class ShieldParryHandler {

    // 前ティックのメインハンドアイテムIDをプレイヤーUUIDごとに保持
    private static final Map<UUID, String> prevMainHandId = new ConcurrentHashMap<>();

    // ===================================================================
    // Fキースワップ検出（サーバーティック）
    // ===================================================================
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (player.level().isClientSide) return;

        UUID uuid = player.getUUID();
        String currentMainId = itemId(player.getMainHandItem());
        String prevId = prevMainHandId.getOrDefault(uuid, "");

        boolean shieldNowInMain = player.getMainHandItem().getItem() instanceof ParryShieldItem;
        boolean shieldWasInMain = isParryShieldId(prevId);

        // メインハンドにシールドが来た瞬間 → パリィウィンドウ開始
        if (shieldNowInMain && !shieldWasInMain) {
            ParryShieldItem.recordSwapParry(player, player.level().getGameTime());
        }

        prevMainHandId.put(uuid, currentMainId);
    }

    // ===================================================================
    // パリィ判定（被ダメージ時）
    // ===================================================================
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide) return;

        long now = player.level().getGameTime();

        // --- 経路A: オフハンドパリィ ---
        ItemStack offhand = player.getItemInHand(InteractionHand.OFF_HAND);
        if (offhand.getItem() instanceof ParryShieldItem && player.isUsingItem()) {
            long blockStart = ParryShieldItem.getBlockStartTime(player);
            if (blockStart >= 0 && (now - blockStart) <= ParryShieldItem.PARRY_WINDOW_TICKS) {
                triggerParry(event, player);
                return;
            }
        }

        // --- 経路B: スワップパリィ（Fキー）---
        ItemStack mainhand = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (mainhand.getItem() instanceof ParryShieldItem) {
            long swapStart = ParryShieldItem.getSwapStartTime(player);
            if (swapStart >= 0 && (now - swapStart) <= ParryShieldItem.PARRY_WINDOW_TICKS) {
                triggerParry(event, player);
            }
        }
    }

    // ===================================================================
    // パリィ実行
    // ===================================================================
    private static void triggerParry(LivingHurtEvent event, Player player) {
        float incoming = event.getAmount();
        event.setCanceled(true);

        // 攻撃者へ反射ダメージ
        if (event.getSource().getEntity() instanceof LivingEntity attacker) {
            attacker.hurt(player.damageSources().magic(), incoming * 1.5f);
        }

        // エフェクト
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CRIT,
                player.getX(), player.getY() + 1.0, player.getZ(),
                20, 0.4, 0.4, 0.4, 0.25);
        }

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.5f, 2.0f);
    }

    // ===================================================================
    // ユーティリティ
    // ===================================================================

    private static String itemId(ItemStack stack) {
        if (stack.isEmpty()) return "";
        ResourceLocation loc = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return loc != null ? loc.toString() : "";
    }

    private static boolean isParryShieldId(String id) {
        return id.equals("the_four_primitives_and_weapons:parry_shield");
    }
}

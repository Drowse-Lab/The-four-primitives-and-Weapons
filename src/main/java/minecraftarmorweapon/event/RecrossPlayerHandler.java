package minecraftarmorweapon.event;

import minecraftarmorweapon.MinecraftArmorWeaponMod;
import minecraftarmorweapon.entity.RecrossHookEntity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Re:Cross Hookshot のサーバーサイド挙動.
 *
 *   1. プレイヤーごとの引き寄せ tick — フック ANCHORED 中はプレイヤーをアンカー方向に
 *      強制 teleport で移動 (元データパックの spectator gamemode + armor_stand 移動の代替).
 *   2. スニーク+空中の浮遊 — 元データパック {@code hold_hookshot} の float 機能を再現.
 *      ReCr_FloatFuel 上限 40 tick で打ち切り、着地でリセット.
 */
@Mod.EventBusSubscriber(modid = MinecraftArmorWeaponMod.MODID)
public class RecrossPlayerHandler {

    /**
     * Pull 中の player 移動速度 (block/tick).
     * 元データパック実測: Motion_Speed=20 + AEC offset 0.1 から velocity = 2 block/tick の direction.
     */
    public static final double PULL_SPEED = 2.0;
    /** 到達判定 — anchor との距離がこの値以下になったら pull 終了 (元データパック {@code distance=..1}). */
    public static final double ARRIVAL_DIST = 1.5;

    /** 浮遊の最大持続 tick の基本値 (rarity 無し時). rarity 付なら WeaponRarity の値が優先. */
    public static final int FLOAT_FUEL_MAX = 40;
    /** Sneak 解除後の落下ダメージ無効 grace 基本値 (rarity 無し時). */
    public static final int DEFAULT_FALL_IMMUNITY_TICKS = 3;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (!(sp.level() instanceof ServerLevel)) return;

        // === 1. Pull 処理 ===
        RecrossHookEntity anchored = findAnchoredHook(sp);
        if (anchored != null && anchored.getAnchorPos() != null) {
            tickPull(sp, anchored);
        } else {
            // pull 終了 (フック消失等)
            if (sp.isNoGravity() && !isHoldingHookshot(sp)) {
                sp.setNoGravity(false);
            }
            RecrossDebugLogger.endIfActive(sp, "no_anchor");
        }

        // === 2. スニーク浮遊 ===
        tickFloat(sp);
    }

    private static void tickPull(ServerPlayer sp, RecrossHookEntity hook) {
        // ★ Sneak で pull キャンセル (元データパック準拠 — 緊急停止用)
        if (sp.isShiftKeyDown()) {
            cancelPull(sp, hook, "user_sneak");
            return;
        }

        Vec3 anchor = hook.getAnchorPos();
        Vec3 cur = sp.position();
        Vec3 toAnchor = anchor.subtract(cur);
        double dist = toAnchor.length();

        // 到達 → fan-out 解除 + 軽い levitation jump
        if (dist <= ARRIVAL_DIST) {
            arrivePull(sp, hook);
            return;
        }

        Vec3 step = toAnchor.normalize().scale(Math.min(PULL_SPEED, dist));
        Vec3 next = cur.add(step);

        // === 壁衝突チェック ===
        // 引き寄せ中にブロックに詰まると、サーバーが送る velocity と
        // クライアントの衝突解決の綱引きで player が暴れる (= "荒ぶる").
        // 先回りで ray-cast して進路上にブロックがあれば pull を中断.
        if (isPathBlockedForPlayer(sp, cur, next)) {
            cancelPull(sp, hook, "wall_hit");
            return;
        }

        // === 滑らか pull の鍵 ===
        // 旧版は毎 tick connection.teleport(absolute) → クライアントが 50ms ごとに瞬間移動 = カクカク.
        // 新版は setDeltaMovement + hurtMarked=true で **velocity packet** を送信:
        //   - サーバー: 速度をセットするだけ
        //   - クライアント: 受信した速度をローカルで毎フレーム適用 → 60fps で滑らかに移動
        //   - サーバーは引き続き player の位置報告を信頼する (vanilla の通常 movement 同じ)
        if (!sp.isNoGravity()) sp.setNoGravity(true);
        sp.setDeltaMovement(step);
        sp.hurtMarked = true;        // ← この flag が立つと tick 末に ClientboundSetEntityMotionPacket が送信される
        sp.fallDistance = 0f;        // 着弾時の落下ダメージ防止

        RecrossDebugLogger.logTick("PULL_TICK", sp, hook);
    }

    /** 進路上 (足元 + 頭の高さ) にブロックがあるかチェック. */
    private static boolean isPathBlockedForPlayer(ServerPlayer sp, Vec3 cur, Vec3 next) {
        Vec3 feet = cur.add(0, 0.1, 0);            // 足は地面ギリギリだと既に in-block 判定になりがちなので少し浮かす
        Vec3 head = cur.add(0, sp.getEyeHeight(), 0);
        Vec3 nextFeet = next.add(0, 0.1, 0);
        Vec3 nextHead = next.add(0, sp.getEyeHeight(), 0);
        return clipBlocked(sp, feet, nextFeet) || clipBlocked(sp, head, nextHead);
    }

    private static boolean clipBlocked(ServerPlayer sp, Vec3 from, Vec3 to) {
        return sp.level().clip(new ClipContext(
            from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, sp))
            .getType() != HitResult.Type.MISS;
    }

    /** 壁衝突や中断時 — 暴れずに止める. arrival ジャンプ無し. */
    private static void cancelPull(ServerPlayer sp, RecrossHookEntity hook, String reason) {
        sp.setNoGravity(false);
        sp.setDeltaMovement(Vec3.ZERO);
        sp.hurtMarked = true;
        sp.fallDistance = 0f;
        RecrossDebugLogger.endIfActive(sp, reason);
        hook.discard();
    }

    private static void arrivePull(ServerPlayer sp, RecrossHookEntity hook) {
        sp.setNoGravity(false);
        sp.setDeltaMovement(0, 0.4, 0);   // 元データパック: 軽い jump (zombie.infect + dust effect)
        sp.hurtMarked = true;
        sp.level().playSound(null, sp.getX(), sp.getY(), sp.getZ(),
            net.minecraft.sounds.SoundEvents.ZOMBIE_INFECT,
            net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 2.0f);
        RecrossDebugLogger.endIfActive(sp, "arrival");
        hook.discard();
    }

    private static void tickFloat(ServerPlayer sp) {
        if (!isHoldingHookshot(sp)) {
            // hookshot を手放した → grace は維持しつつ消化 (落下中の場合の保険)
            tickFallImmunity(sp);
            return;
        }
        if (sp.onGround()) {
            // 着地で全リセット (浮遊残量・grace 共に)
            FloatFuel.reset(sp);
            FallImmunity.reset(sp);
            return;
        }

        int fuelMax = getFloatFuelMax(sp);
        int graceMax = getFallImmunityMax(sp);

        if (sp.isShiftKeyDown()) {
            int fuel = FloatFuel.get(sp);
            if (fuel >= fuelMax) {
                // 燃料切れ — levitation 解除. grace は与える (浮遊→落下の保護)
                sp.removeEffect(MobEffects.LEVITATION);
                FallImmunity.set(sp, graceMax);
                return;
            }
            // 浮遊中: levitation 適用 + 落下リセット + 燃料消費 (押し続けの間のみ).
            // ★ Sneak 離す → 燃料消費停止 (リセットしない).
            //    再度 Sneak すれば残り燃料で浮遊継続 → 着地までで何回でも分割使用可.
            sp.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 5, 1, false, false, false));
            sp.fallDistance = 0f;
            FloatFuel.add(sp, 1);
            FallImmunity.set(sp, graceMax);
        } else {
            // Sneak 離した → 燃料は据え置き (リセット無し). grace を消化.
            tickFallImmunity(sp);
        }
    }

    /** grace 残量 > 0 の間は fallDistance を 0 に保つ. 毎 tick 1 減算. */
    private static void tickFallImmunity(ServerPlayer sp) {
        int rem = FallImmunity.get(sp);
        if (rem <= 0) return;
        sp.fallDistance = 0f;
        FallImmunity.set(sp, rem - 1);
    }

    private static int getFloatFuelMax(ServerPlayer sp) {
        var r = getHeldHookshotRarity(sp);
        return (r != null) ? r.getHookshotFloatFuelMax() : FLOAT_FUEL_MAX;
    }

    private static int getFallImmunityMax(ServerPlayer sp) {
        var r = getHeldHookshotRarity(sp);
        return (r != null) ? r.getHookshotFallImmunityTicks() : DEFAULT_FALL_IMMUNITY_TICKS;
    }

    private static minecraftarmorweapon.item.rarity.WeaponRarity getHeldHookshotRarity(ServerPlayer sp) {
        var main = sp.getMainHandItem();
        if (main.getItem() instanceof minecraftarmorweapon.item.RecrossHookshotItem) {
            return minecraftarmorweapon.item.rarity.WeaponRarity.getFromStack(main);
        }
        var off = sp.getOffhandItem();
        if (off.getItem() instanceof minecraftarmorweapon.item.RecrossHookshotItem) {
            return minecraftarmorweapon.item.rarity.WeaponRarity.getFromStack(off);
        }
        return null;
    }

    private static RecrossHookEntity findAnchoredHook(ServerPlayer sp) {
        for (RecrossHookEntity h : sp.level().getEntitiesOfClass(
                RecrossHookEntity.class, sp.getBoundingBox().inflate(160.0))) {
            if (h.getOwnerPlayer() == sp && h.getState() == RecrossHookEntity.State.ANCHORED) {
                return h;
            }
        }
        return null;
    }

    private static boolean isHoldingHookshot(Player p) {
        return p.getMainHandItem().getItem() instanceof minecraftarmorweapon.item.RecrossHookshotItem
            || p.getOffhandItem().getItem() instanceof minecraftarmorweapon.item.RecrossHookshotItem;
    }

    /** 1 player 1 fuel カウンタ — 着地でリセット. */
    private static final class FloatFuel {
        private static final java.util.Map<java.util.UUID, Integer> map = new java.util.concurrent.ConcurrentHashMap<>();
        static int get(ServerPlayer p) { return map.getOrDefault(p.getUUID(), 0); }
        static void add(ServerPlayer p, int n) { map.merge(p.getUUID(), n, Integer::sum); }
        static void reset(ServerPlayer p) { map.remove(p.getUUID()); }
    }

    /** Sneak 解除後の落下ダメ無効 grace カウンタ — 着地 / 燃料切れ後リセット. */
    private static final class FallImmunity {
        private static final java.util.Map<java.util.UUID, Integer> map = new java.util.concurrent.ConcurrentHashMap<>();
        static int get(ServerPlayer p) { return map.getOrDefault(p.getUUID(), 0); }
        static void set(ServerPlayer p, int n) {
            if (n <= 0) map.remove(p.getUUID());
            else map.put(p.getUUID(), n);
        }
        static void reset(ServerPlayer p) { map.remove(p.getUUID()); }
    }
}

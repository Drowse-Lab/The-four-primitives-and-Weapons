package the_four_primitives_and_weapons.event;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.entity.RecrossHookEntity;
import the_four_primitives_and_weapons.events.DodgeAndBattouHandler;
import the_four_primitives_and_weapons.skill.WeaponTypeRegistry;
import the_four_primitives_and_weapons.util.DamageCalculator;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
@Mod.EventBusSubscriber(modid = TheFourPrimitivesAndWeaponsMod.MODID)
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

        // pull 中の縦回転斬り — 小型武器を持っていれば Riptide アニメ + 衝突ダメージ
        tickSpinSlash(sp);

        RecrossDebugLogger.logTick("PULL_TICK", sp, hook);
    }

    /** 螺旋刃の回転半径 (block) — player から刃中心までの距離 (基準値; 武器タイプで scale). */
    private static final double SPIRAL_BLADE_RADIUS_BASE = 2.5;
    /** 刃のヒット判定半径 (block) — 刃中心から検出する球の大きさ (基準値; 武器タイプで scale). */
    private static final double SPIRAL_HIT_RADIUS_BASE = 1.6;
    /** 刃の縦回転速度 (rad/tick) — 60°/tick = 1 秒で 600° 回る. */
    private static final double SPIRAL_ANGLE_PER_TICK = Math.toRadians(60);

    /**
     * Pull 中の縦回転スピン斬り — 進行方向を中心軸として、刃が forward-up 平面で
     * 縦回転する螺旋状の攻撃判定。
     *
     *   - 各 tick で player 進行方向 (delta motion) を {@code forward} とする
     *   - {@code forward} に垂直な {@code right} / {@code up} を作り、刃位置を
     *     {@code playerPos + (-cos θ)·up + (sin θ)·forward × radius} で計算
     *   - 角度 θ は session 中累積するので、player が pull で X 方向に進むと
     *     forward 軸を中心に螺旋を描く軌跡になる
     *   - 刃位置中心の小さな範囲で敵検出 → ダメージ (1 entity 1 hit)
     */
    private static void tickSpinSlash(ServerPlayer sp) {
        ItemStack weapon = pickSpinWeapon(sp);
        if (weapon.isEmpty() || isLargeWeapon(weapon)) {
            SpinHits.reset(sp);
            SpinPhase.reset(sp);
            return;
        }

        // Riptide アニメを毎 tick refresh で持続させる
        // (LivingEntity の autoSpinAttackTicks は private、getter は isAutoSpinAttack() しか
        //  public でないので、tick 数を見ずに毎 tick 上書きで OK)
        sp.startAutoSpinAttack(10);

        // (1) 進行方向 forward と、それに直交する right / up
        Vec3 motion = sp.getDeltaMovement();
        Vec3 forward = motion.lengthSqr() > 0.01 ? motion.normalize() : sp.getLookAngle();
        Vec3 right = forward.cross(new Vec3(0, 1, 0));
        if (right.lengthSqr() < 1.0E-4) {
            // forward が垂直 (= 真上 / 真下) の場合のフォールバック
            right = new Vec3(1, 0, 0);
        }
        right = right.normalize();
        Vec3 up = right.cross(forward).normalize();

        // (2) 縦回転角 θ — session 中累積
        int phase = SpinPhase.getAndIncrement(sp);
        double theta = phase * SPIRAL_ANGLE_PER_TICK;

        // (3) 刃位置 = player + (-cos θ)·up + (sin θ)·forward, 半径 bladeRadius
        //     武器タイプにより範囲がスケールする (大剣/槍 = 広い、短剣 = 狭い)
        //     θ=0 で player の足元 (-up), θ=90° で前方 (+forward), θ=180° で頭上 (+up), θ=270° で後方 (-forward)
        double scale = the_four_primitives_and_weapons.skill.WeaponTypeRegistry.getSpinRangeScale(weapon);
        double bladeRadius = SPIRAL_BLADE_RADIUS_BASE * scale;
        double hitRadius = SPIRAL_HIT_RADIUS_BASE * scale;
        Vec3 bladeOffset = up.scale(-Math.cos(theta) * bladeRadius)
                            .add(forward.scale(Math.sin(theta) * bladeRadius));
        Vec3 bladePos = sp.position().add(0, 1.0, 0).add(bladeOffset);   // y+1.0 = 体の中心付近

        // (4) 刃位置中心の球内の敵にダメージ (1 entity 1 hit)
        //     もう片方の手が "鞘" (= 抜刀していない状態) の場合は素手相当ダメージ。
        //     KB は applyNormalKnockback で統一 (saya 武器なら自動で +1.5 強化)。
        boolean otherIsSaya = isOtherHandSaya(sp, weapon);
        float dmg = otherIsSaya ? 1.0f : 6.0f;

        AABB bladeBox = new AABB(bladePos, bladePos).inflate(hitRadius);
        Set<UUID> hits = SpinHits.getOrCreate(sp);
        for (LivingEntity le : sp.level().getEntitiesOfClass(LivingEntity.class, bladeBox)) {
            if (le == sp) continue;
            if (!le.isAlive()) continue;
            if (hits.contains(le.getUUID())) continue;
            // bladePos から target の中心までの距離で球判定
            Vec3 leMid = le.position().add(0, le.getBbHeight() * 0.5, 0);
            if (leMid.distanceToSqr(bladePos) > (hitRadius + le.getBbWidth() * 0.5)
                    * (hitRadius + le.getBbWidth() * 0.5)) continue;
            DamageCalculator.dealDamage(sp, le, dmg, weapon);
            DamageCalculator.applyNormalKnockback(sp, le, weapon);

            hits.add(le.getUUID());
        }

        // (5) 刃の軌跡パーティクル (見た目の螺旋表現)
        if (sp.level() instanceof ServerLevel sw) {
            sw.sendParticles(net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK,
                bladePos.x, bladePos.y, bladePos.z, 1, 0.05, 0.05, 0.05, 0);
            sw.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT,
                bladePos.x, bladePos.y, bladePos.z, 2, 0.05, 0.05, 0.05, 0.05);
        }
    }

    /** 手の武器を 1 つ選ぶ — メインハンド優先、武器でなければオフハンド。 */
    private static ItemStack pickSpinWeapon(ServerPlayer sp) {
        ItemStack main = sp.getMainHandItem();
        if (DodgeAndBattouHandler.isWeapon(main)) return main;
        ItemStack off = sp.getOffhandItem();
        if (DodgeAndBattouHandler.isWeapon(off)) return off;
        return ItemStack.EMPTY;
    }

    /** 大剣 / 槍 = "大型" 判定 (両手で構える系)。それ以外 (刀 / 直刀 / 細剣 / 短剣 / 剣 等) は小型扱い。 */
    private static boolean isLargeWeapon(ItemStack stack) {
        WeaponTypeRegistry.WeaponTypeData type = WeaponTypeRegistry.getTypeForItem(stack);
        if (type == null) return false;
        String id = type.getId();
        return "greatsword".equals(id) || "spear".equals(id);
    }

    /** 武器の "もう片方の手" が saya (鞘) かどうか。 */
    private static boolean isOtherHandSaya(ServerPlayer sp, ItemStack weaponSelected) {
        ItemStack other = (sp.getMainHandItem() == weaponSelected) ? sp.getOffhandItem() : sp.getMainHandItem();
        return DodgeAndBattouHandler.isSaya(other);
    }

    /**
     * 進路上に壁があるかチェック (水平方向のみ).
     *
     * 旧版の足元/頭の高さの ray-cast は、地面に向けて hook を撃った場合に進路が地中を通過し、
     * 「飛ばない」バグの原因になっていた. 上下移動の衝突解決はバニラ物理に任せ、
     * ここでは XZ 平面の壁衝突だけ検出する.
     *
     * - 胸の高さ (cur.y + 1.0) で XZ 平面の path を見る
     * - 真下/真上に引っ張られる場合は XZ 距離 0 → ray 長 0 → 検出されない (= 続行)
     * - 水平に壁へ突っ込む場合 → ray が壁を貫通 → 中断
     */
    private static boolean isPathBlockedForPlayer(ServerPlayer sp, Vec3 cur, Vec3 next) {
        double chestY = cur.y + 1.0;
        Vec3 chestCur = new Vec3(cur.x, chestY, cur.z);
        Vec3 chestNextXZ = new Vec3(next.x, chestY, next.z);
        if (chestCur.distanceToSqr(chestNextXZ) < 1.0E-6) return false;
        return clipBlocked(sp, chestCur, chestNextXZ);
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
        SpinHits.reset(sp);
        SpinPhase.reset(sp);
        RecrossDebugLogger.endIfActive(sp, reason);
        hook.discard();
    }

    private static void arrivePull(ServerPlayer sp, RecrossHookEntity hook) {
        sp.setNoGravity(false);
        sp.setDeltaMovement(0, 0.4, 0);   // 元データパック: 軽い jump (zombie.infect + dust effect)
        sp.hurtMarked = true;
        SpinHits.reset(sp);
        SpinPhase.reset(sp);
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
            // 着地中 — 時間経過で fuel を回復 (multi_jump エンチャでペースアップ)
            // base 1/tick + level 分加算: lv0=1, lv1=2, lv2=3, lv3=4
            int recover = 1 + MultiJumpHandler.getHookshotLevel(sp);
            int cur = FloatFuel.get(sp);
            if (cur > 0) {
                int next = Math.max(0, cur - recover);
                if (next == 0) FloatFuel.reset(sp);
                else FloatFuel.set(sp, next);
            }
            FallImmunity.reset(sp);
            return;
        }

        int fuelMax = getFloatFuelMax(sp);
        int graceMax = getFallImmunityMax(sp);

        if (sp.isShiftKeyDown()) {
            int fuel = FloatFuel.get(sp);
            if (fuel >= fuelMax) {
                // 燃料切れ — float effect 解除. grace は独自 fall guard effect として付与
                sp.removeEffect(the_four_primitives_and_weapons.init.CustomMobEffectInit.HOOKSHOT_FLOAT.get());
                applyFallGuard(sp, graceMax);
                return;
            }
            // 浮遊中: 独自 HOOKSHOT_FLOAT effect 適用 + 燃料消費
            // ★ Sneak 離す → 燃料消費停止 (リセットしない). 再度 Sneak で継続。
            sp.addEffect(new MobEffectInstance(
                the_four_primitives_and_weapons.init.CustomMobEffectInit.HOOKSHOT_FLOAT.get(),
                5, 0, false, false, false));
            sp.fallDistance = 0f;
            FloatFuel.add(sp, 1);
            applyFallGuard(sp, graceMax);
        } else {
            // Sneak 離した → 燃料据え置き. fall guard が残っていれば自然減少 (effect 自体が duration で消える)
        }
    }

    /** 独自 HOOKSHOT_FALL_GUARD effect を {@code ticks} だけ player に付与 (上書き). */
    private static void applyFallGuard(ServerPlayer sp, int ticks) {
        if (ticks <= 0) return;
        sp.addEffect(new MobEffectInstance(
            the_four_primitives_and_weapons.init.CustomMobEffectInit.HOOKSHOT_FALL_GUARD.get(),
            ticks, 0, false, false, false));
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

    private static the_four_primitives_and_weapons.item.rarity.WeaponRarity getHeldHookshotRarity(ServerPlayer sp) {
        var main = sp.getMainHandItem();
        if (main.getItem() instanceof the_four_primitives_and_weapons.item.RecrossHookshotItem) {
            return the_four_primitives_and_weapons.item.rarity.WeaponRarity.getFromStack(main);
        }
        var off = sp.getOffhandItem();
        if (off.getItem() instanceof the_four_primitives_and_weapons.item.RecrossHookshotItem) {
            return the_four_primitives_and_weapons.item.rarity.WeaponRarity.getFromStack(off);
        }
        return null;
    }

    private static RecrossHookEntity findAnchoredHook(ServerPlayer sp) {
        // 検索半径は十分大きく取る — rarity 射程が 600+ ブロックに伸びる場合、
        // anchor 完了直後の hook が固定 160 ブロック範囲外に居て見つからず pull が始まらない問題を回避。
        for (RecrossHookEntity h : sp.level().getEntitiesOfClass(
                RecrossHookEntity.class, sp.getBoundingBox().inflate(2000.0))) {
            if (h.getOwnerPlayer() != sp) continue;
            if (h.getState() != RecrossHookEntity.State.ANCHORED) continue;
            // light entity を継続 pull 中の hook は player 側 pull の対象外
            // (= 敵をこちらへ引き寄せている最中、player は通常移動できる)
            if (h.isPullingEntity()) continue;
            return h;
        }
        return null;
    }

    private static boolean isHoldingHookshot(Player p) {
        return p.getMainHandItem().getItem() instanceof the_four_primitives_and_weapons.item.RecrossHookshotItem
            || p.getOffhandItem().getItem() instanceof the_four_primitives_and_weapons.item.RecrossHookshotItem;
    }

    /** 1 player 1 fuel カウンタ — 着地で時間経過回復. */
    private static final class FloatFuel {
        private static final java.util.Map<java.util.UUID, Integer> map = new java.util.concurrent.ConcurrentHashMap<>();
        static int get(ServerPlayer p) { return map.getOrDefault(p.getUUID(), 0); }
        static void add(ServerPlayer p, int n) { map.merge(p.getUUID(), n, Integer::sum); }
        static void set(ServerPlayer p, int n) {
            if (n <= 0) map.remove(p.getUUID());
            else map.put(p.getUUID(), n);
        }
        static void reset(ServerPlayer p) { map.remove(p.getUUID()); }
    }

    /** Pull 中スピン斬りで既にヒットさせた敵の集合 — pull 開始/終了でリセット. */
    private static final class SpinHits {
        private static final java.util.Map<UUID, Set<UUID>> map = new ConcurrentHashMap<>();
        static Set<UUID> getOrCreate(ServerPlayer p) {
            return map.computeIfAbsent(p.getUUID(), k -> ConcurrentHashMap.newKeySet());
        }
        static void reset(ServerPlayer p) { map.remove(p.getUUID()); }
    }

    /** Pull 中の螺旋刃の累積回転 phase (tick 数) — pull 開始/終了でリセット. */
    private static final class SpinPhase {
        private static final java.util.Map<UUID, java.util.concurrent.atomic.AtomicInteger> map = new ConcurrentHashMap<>();
        static int getAndIncrement(ServerPlayer p) {
            return map.computeIfAbsent(p.getUUID(),
                k -> new java.util.concurrent.atomic.AtomicInteger(0)).getAndIncrement();
        }
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

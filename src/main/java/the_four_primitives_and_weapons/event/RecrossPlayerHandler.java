package the_four_primitives_and_weapons.event;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.entity.RecrossHookEntity;
import the_four_primitives_and_weapons.events.DodgeAndBattouHandler;
import the_four_primitives_and_weapons.init.CustomMobEffectInit;
import the_four_primitives_and_weapons.item.RecrossHookshotItem;
import the_four_primitives_and_weapons.item.rarity.WeaponRarity;
import the_four_primitives_and_weapons.skill.WeaponTypeRegistry;
import the_four_primitives_and_weapons.util.DamageCalculator;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Re:Cross Hookshot のサーバーサイド挙動 (slim 版).
 *
 *   1. Pull — ANCHORED 中の hook 方向に setDeltaMovement で player を加速
 *   2. Sneak 浮遊 — 持っている間 Sneak で空中静止 (HOOKSHOT_FLOAT effect)
 *   3. 落下ダメ無効 — 発射直後 + 浮遊燃料切れ後の grace 期間
 *   4. Pull 中スピン斬り — 小型武器持ちなら Riptide アニメ + 周辺攻撃
 */
@Mod.EventBusSubscriber(modid = TheFourPrimitivesAndWeaponsMod.MODID)
public class RecrossPlayerHandler {

    public static final double PULL_SPEED = 2.0;
    public static final double ARRIVAL_DIST = 1.5;
    public static final int FLOAT_FUEL_MAX = 40;
    public static final int DEFAULT_FALL_IMMUNITY_TICKS = 3;

    private static final double SPIRAL_BLADE_RADIUS_BASE = 2.5;
    private static final double SPIRAL_HIT_RADIUS_BASE = 1.6;
    private static final double SPIRAL_ANGLE_PER_TICK = Math.toRadians(60);
    private static final double FLOAT_LIFT_VELOCITY = 0.05;

    private static final Map<UUID, RecrossHookEntity> ANCHORED = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> FLOAT_FUEL = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> FALL_IMMUNITY = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<UUID>> SPIN_HITS = new ConcurrentHashMap<>();
    private static final Map<UUID, AtomicInteger> SPIN_PHASE = new ConcurrentHashMap<>();

    public static void registerAnchoredHook(Player owner, RecrossHookEntity hook) {
        if (owner instanceof ServerPlayer) ANCHORED.put(owner.getUUID(), hook);
    }
    public static void unregisterAnchoredHook(Player owner) {
        if (owner != null) ANCHORED.remove(owner.getUUID());
    }
    public static boolean hasFallGuard(LivingEntity entity) {
        return entity instanceof ServerPlayer sp && FALL_IMMUNITY.getOrDefault(sp.getUUID(), 0) > 0;
    }
    public static void applyFallGuard(ServerPlayer sp, int ticks) {
        if (ticks <= 0) return;
        FALL_IMMUNITY.merge(sp.getUUID(), ticks, Math::max);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (!(sp.level() instanceof ServerLevel)) return;

        UUID id = sp.getUUID();
        RecrossHookEntity hook = ANCHORED.get(id);
        boolean holding = isHoldingHookshot(sp);
        int fallGuard = FALL_IMMUNITY.getOrDefault(id, 0);
        boolean hasFloat = sp.hasEffect(CustomMobEffectInit.HOOKSHOT_FLOAT.get());

        // 何もすることがなければ完全スキップ (大半の player はこの分岐)
        if (hook == null && !holding && fallGuard <= 0 && !hasFloat && !sp.isNoGravity()) return;

        // 1. Pull
        if (hook != null) {
            if (!hook.isAlive() || hook.getAnchorPos() == null
                || hook.getState() != RecrossHookEntity.State.ANCHORED
                || hook.isPullingEntity()) {
                ANCHORED.remove(id);
                hook = null;
            }
        }
        if (hook != null) tickPull(sp, hook);
        else if (sp.isNoGravity() && !holding) sp.setNoGravity(false);

        // 2. 浮遊効果 (HOOKSHOT_FLOAT) — 上昇速度を維持
        if (hasFloat) {
            Vec3 m = sp.getDeltaMovement();
            if (m.y < FLOAT_LIFT_VELOCITY) {
                sp.setDeltaMovement(m.x, FLOAT_LIFT_VELOCITY, m.z);
                sp.hurtMarked = true;
            }
            sp.fallDistance = 0f;
        }

        // 3. Sneak で浮遊状態を制御 + 燃料管理
        if (holding) tickFloat(sp);

        // 4. fall guard カウンタ消化
        if (fallGuard > 0) {
            sp.fallDistance = 0f;
            int next = fallGuard - 1;
            if (next <= 0) FALL_IMMUNITY.remove(id);
            else FALL_IMMUNITY.put(id, next);
        }
    }

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (hasFallGuard(event.getEntity())
            || event.getEntity().hasEffect(CustomMobEffectInit.HOOKSHOT_FLOAT.get())) {
            event.setCanceled(true);
        }
    }

    private static void tickPull(ServerPlayer sp, RecrossHookEntity hook) {
        if (sp.isShiftKeyDown()) { cancelPull(sp, hook); return; }

        Vec3 anchor = hook.getAnchorPos();
        Vec3 cur = sp.position();
        Vec3 toAnchor = anchor.subtract(cur);
        double dist = toAnchor.length();

        if (dist <= ARRIVAL_DIST) { arrivePull(sp, hook); return; }

        Vec3 step = toAnchor.normalize().scale(Math.min(PULL_SPEED, dist));
        if (isPathBlocked(sp, cur, cur.add(step))) { cancelPull(sp, hook); return; }

        if (!sp.isNoGravity()) sp.setNoGravity(true);
        sp.setDeltaMovement(step);
        sp.hurtMarked = true;
        sp.fallDistance = 0f;

        tickSpinSlash(sp);
    }

    /**
     * Pull 中の縦回転スピン斬り — 進行方向を中心軸に刃が forward-up 平面で螺旋を描く.
     * 小型武器 (剣/刀/レイピア/短剣等) 持ち時のみ。大剣/槍は除外。
     */
    private static void tickSpinSlash(ServerPlayer sp) {
        ItemStack weapon = pickSpinWeapon(sp);
        if (weapon.isEmpty() || isLargeWeapon(weapon)) {
            SPIN_HITS.remove(sp.getUUID());
            SPIN_PHASE.remove(sp.getUUID());
            return;
        }
        sp.startAutoSpinAttack(10);

        Vec3 motion = sp.getDeltaMovement();
        Vec3 forward = motion.lengthSqr() > 0.01 ? motion.normalize() : sp.getLookAngle();
        Vec3 right = forward.cross(new Vec3(0, 1, 0));
        if (right.lengthSqr() < 1.0E-4) right = new Vec3(1, 0, 0);
        right = right.normalize();
        Vec3 up = right.cross(forward).normalize();

        int phase = SPIN_PHASE.computeIfAbsent(sp.getUUID(), k -> new AtomicInteger(0)).getAndIncrement();
        double theta = phase * SPIRAL_ANGLE_PER_TICK;

        double scale = WeaponTypeRegistry.getSpinRangeScale(weapon);
        double bladeRadius = SPIRAL_BLADE_RADIUS_BASE * scale;
        double hitRadius = SPIRAL_HIT_RADIUS_BASE * scale;
        Vec3 bladePos = sp.position().add(0, 1.0, 0)
            .add(up.scale(-Math.cos(theta) * bladeRadius))
            .add(forward.scale(Math.sin(theta) * bladeRadius));

        boolean otherIsSaya = isOtherHandSaya(sp, weapon);
        float dmg = otherIsSaya ? 1.0f : 6.0f;
        AABB box = new AABB(bladePos, bladePos).inflate(hitRadius);
        Set<UUID> hits = SPIN_HITS.computeIfAbsent(sp.getUUID(), k -> ConcurrentHashMap.newKeySet());
        for (LivingEntity le : sp.level().getEntitiesOfClass(LivingEntity.class, box)) {
            if (le == sp || !le.isAlive() || hits.contains(le.getUUID())) continue;
            Vec3 mid = le.position().add(0, le.getBbHeight() * 0.5, 0);
            double r = hitRadius + le.getBbWidth() * 0.5;
            if (mid.distanceToSqr(bladePos) > r * r) continue;
            DamageCalculator.dealDamage(sp, le, dmg, weapon);
            DamageCalculator.applyNormalKnockback(sp, le, weapon);
            hits.add(le.getUUID());
        }

        if (sp.level() instanceof ServerLevel sw && (phase & 1) == 0) {
            sw.sendParticles(ParticleTypes.SWEEP_ATTACK, bladePos.x, bladePos.y, bladePos.z, 1, 0.05, 0.05, 0.05, 0);
            sw.sendParticles(ParticleTypes.CRIT, bladePos.x, bladePos.y, bladePos.z, 2, 0.05, 0.05, 0.05, 0.05);
        }
    }

    private static ItemStack pickSpinWeapon(ServerPlayer sp) {
        ItemStack main = sp.getMainHandItem();
        if (DodgeAndBattouHandler.isWeapon(main)) return main;
        ItemStack off = sp.getOffhandItem();
        if (DodgeAndBattouHandler.isWeapon(off)) return off;
        return ItemStack.EMPTY;
    }

    private static boolean isLargeWeapon(ItemStack stack) {
        WeaponTypeRegistry.WeaponTypeData type = WeaponTypeRegistry.getTypeForItem(stack);
        if (type == null) return false;
        String id = type.getId();
        return "greatsword".equals(id) || "spear".equals(id);
    }

    private static boolean isOtherHandSaya(ServerPlayer sp, ItemStack selected) {
        ItemStack other = sp.getMainHandItem() == selected ? sp.getOffhandItem() : sp.getMainHandItem();
        return DodgeAndBattouHandler.isSaya(other);
    }

    /**
     * 進路上に壁があるかチェック (XZ 平面、胸の高さの ray-cast のみ).
     * 上下方向はバニラ衝突解決に任せる — 全 bbox 検査だと壁面 anchor 自体を「障害物」と
     * 誤判定して player が壁の手前で急停止し、vanilla 衝突応答で変な方向に飛ぶ。
     */
    private static boolean isPathBlocked(ServerPlayer sp, Vec3 cur, Vec3 next) {
        double y = cur.y + 1.0;
        Vec3 from = new Vec3(cur.x, y, cur.z);
        Vec3 to = new Vec3(next.x, y, next.z);
        if (from.distanceToSqr(to) < 1.0E-6) return false;
        return sp.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, sp))
            .getType() != HitResult.Type.MISS;
    }

    private static void cancelPull(ServerPlayer sp, RecrossHookEntity hook) {
        sp.setNoGravity(false);
        sp.setDeltaMovement(Vec3.ZERO);
        sp.hurtMarked = true;
        sp.fallDistance = 0f;
        SPIN_HITS.remove(sp.getUUID());
        SPIN_PHASE.remove(sp.getUUID());
        ANCHORED.remove(sp.getUUID());
        hook.discard();
    }

    private static void arrivePull(ServerPlayer sp, RecrossHookEntity hook) {
        sp.setNoGravity(false);
        sp.setDeltaMovement(0, 0.4, 0);   // 軽い jump
        sp.hurtMarked = true;
        sp.fallDistance = 0f;
        SPIN_HITS.remove(sp.getUUID());
        SPIN_PHASE.remove(sp.getUUID());
        ANCHORED.remove(sp.getUUID());
        sp.level().playSound(null, sp.getX(), sp.getY(), sp.getZ(),
            SoundEvents.ZOMBIE_INFECT, SoundSource.PLAYERS, 1.0f, 2.0f);
        hook.discard();
    }

    private static void tickFloat(ServerPlayer sp) {
        UUID id = sp.getUUID();
        if (sp.onGround()) {
            int recover = 1 + MultiJumpHandler.getHookshotLevel(sp);
            int cur = FLOAT_FUEL.getOrDefault(id, 0);
            if (cur > 0) {
                int next = Math.max(0, cur - recover);
                if (next == 0) FLOAT_FUEL.remove(id);
                else FLOAT_FUEL.put(id, next);
            }
            FALL_IMMUNITY.remove(id);
            return;
        }

        WeaponRarity r = getHeldHookshotRarity(sp);
        int fuelMax = r != null ? r.getHookshotFloatFuelMax() : FLOAT_FUEL_MAX;
        int graceMax = r != null ? r.getHookshotFallImmunityTicks() : DEFAULT_FALL_IMMUNITY_TICKS;

        if (sp.isShiftKeyDown()) {
            int fuel = FLOAT_FUEL.getOrDefault(id, 0);
            if (fuel >= fuelMax) {
                sp.removeEffect(CustomMobEffectInit.HOOKSHOT_FLOAT.get());
                applyFallGuard(sp, graceMax);
                return;
            }
            sp.addEffect(new MobEffectInstance(
                CustomMobEffectInit.HOOKSHOT_FLOAT.get(), 5, 0, false, false, false));
            sp.fallDistance = 0f;
            FLOAT_FUEL.merge(id, 1, Integer::sum);
            applyFallGuard(sp, graceMax);
        }
        // Sneak 離した → fuel 据え置き、grace 自然消化
    }

    private static WeaponRarity getHeldHookshotRarity(ServerPlayer sp) {
        ItemStack main = sp.getMainHandItem();
        if (main.getItem() instanceof RecrossHookshotItem) return WeaponRarity.getFromStack(main);
        ItemStack off = sp.getOffhandItem();
        if (off.getItem() instanceof RecrossHookshotItem) return WeaponRarity.getFromStack(off);
        return null;
    }

    private static boolean isHoldingHookshot(Player p) {
        return p.getMainHandItem().getItem() instanceof RecrossHookshotItem
            || p.getOffhandItem().getItem() instanceof RecrossHookshotItem;
    }
}

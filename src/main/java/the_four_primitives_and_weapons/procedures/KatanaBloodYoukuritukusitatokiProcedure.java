package the_four_primitives_and_weapons.procedures;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.joml.Vector3f;

import the_four_primitives_and_weapons.damage.ElementType;
import the_four_primitives_and_weapons.damage.ElementalDamageUtils;
import the_four_primitives_and_weapons.damage.IElementalDamageSource;
import the_four_primitives_and_weapons.damage.ModDamageSources;
import the_four_primitives_and_weapons.damage.SpecialDebuffHandler;
import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModItems;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rivers of Blood の長押し特殊技 — 多段チャネル → TP 連続 → 最終バースト。
 *
 * フェーズ:
 *   1. CHANNEL  ( ~60 tick ): 自己 slowness + blindness、 半径 30 内の最寄り 3 体を
 *                              glowing でマーク、 周囲に redstone/sweep_attack 粒子。
 *   2. STRIKE   ( ~30 tick ): slowness/blindness 解除、 resistance + slow_falling、
 *                              マーク敵 1 体ずつに TP しながら sweep_attack。
 *   3. BURST    ( 1 tick   ): マーク敵全員に血属性ダメージ + wither + 派手な粒子バースト。
 *
 * シフト ( Sneak ) でキャンセル可能 → ダメージなしで元の位置に戻る。
 */
public class KatanaBloodYoukuritukusitatokiProcedure {

    private static final double SEARCH_RADIUS  = 30.0;
    private static final int    MAX_TARGETS    = 3;
    private static final int    CHANNEL_TICKS  = 60;   // チャネル ( 自己拘束 )
    private static final int    TICKS_PER_TP   = 5;    // STRIKE 中の TP 間隔
    private static final float  STRIKE_DAMAGE  = 4.0f; // TP 時にも軽くダメージ
    private static final float  BURST_DAMAGE   = 12.0f;// 最終バースト本命ダメージ
    private static final int    BLEED_DURATION = 80;
    private static final float  BLEED_PER_TICK = 0.6f;
    private static final int    WITHER_DURATION_TICKS = 100;
    private static final float  WITHER_PER_TICK       = 0.5f;
    private static final float  HEAL_PER_HIT   = 2.0f;
    private static final int    COOLDOWN_TICKS = 240;
    private static final float  SELF_INITIAL_DAMAGE = 1.0f;

    private enum Phase { CHANNEL, STRIKE, BURST }

    private static final class State {
        Vec3 originalPos;
        List<UUID> targets;
        int currentIdx;
        int tickInPhase;
        Phase phase;
        boolean wasInvulnerable;
    }
    private static final Map<UUID, State> active = new ConcurrentHashMap<>();

    public static void execute(Level world, Entity entity) {
        if (entity == null) return;
        if (!(entity instanceof Player player)) return;
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty() || held.getItem() != TheFourPrimitivesAndWeaponsModItems.RIVERS_OF_BLOOD.get()) return;

        if (!the_four_primitives_and_weapons.skill.PlayerSkillData.isMotionEnabled(player, "rivers_of_blood_special")) {
            return;
        }
        if (player.getCooldowns().isOnCooldown(held.getItem())) return;
        if (active.containsKey(player.getUUID())) return;

        Vec3 pos = player.position();
        AABB box = new AABB(
                pos.x - SEARCH_RADIUS, pos.y - 5, pos.z - SEARCH_RADIUS,
                pos.x + SEARCH_RADIUS, pos.y + 5, pos.z + SEARCH_RADIUS);
        List<LivingEntity> nearby = world.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isAlive() && e.distanceTo(player) <= SEARCH_RADIUS);
        if (nearby.isEmpty()) {
            // ターゲット不在でも自損のみ発動 ( チャネルの「血を流す」 演出 )
            if (world instanceof ServerLevel sl) {
                spawnInitialBurst(sl, pos);
            }
            applySelfChannelCost(player);
            return;
        }
        nearby.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));

        player.getCooldowns().addCooldown(held.getItem(), COOLDOWN_TICKS);

        State s = new State();
        s.originalPos = pos;
        s.targets = new ArrayList<>();
        for (int i = 0; i < Math.min(nearby.size(), MAX_TARGETS); i++) {
            s.targets.add(nearby.get(i).getUUID());
        }
        s.currentIdx = 0;
        s.tickInPhase = 0;
        s.phase = Phase.CHANNEL;
        s.wasInvulnerable = player.isInvulnerable();
        active.put(player.getUUID(), s);

        // チャネル開始の自己コスト ( slowness + blindness + 微量自損 )
        applySelfChannelCost(player);

        // マーク対象に glowing
        if (world instanceof ServerLevel sl) {
            for (UUID id : s.targets) {
                Entity e = sl.getEntity(id);
                if (e instanceof LivingEntity le) {
                    le.addEffect(new MobEffectInstance(MobEffects.GLOWING, CHANNEL_TICKS + 60, 0, false, false));
                }
            }
            spawnInitialBurst(sl, pos);
        }
        world.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.DROWNED_SHOOT, SoundSource.PLAYERS, 1.5f, 0.5f);
        world.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.SLIME_DEATH, SoundSource.PLAYERS, 1.2f, 0.5f);
    }

    /** ターゲット 0 の時でも呼ぶチャネル開始コスト. */
    private static void applySelfChannelCost(Player player) {
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, CHANNEL_TICKS, 9, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,         CHANNEL_TICKS, 0, false, false));
        // 自己の血を捧げる演出 ( 軽い自損 )
        try {
            player.hurt(player.damageSources().magic(), SELF_INITIAL_DAMAGE);
        } catch (Throwable ignored) {}
    }

    private static void spawnInitialBurst(ServerLevel sl, Vec3 pos) {
        DustParticleOptions burst = new DustParticleOptions(
                new Vector3f(0.75f, 0.06f, 0.06f), 1.6f);
        sl.sendParticles(burst, pos.x, pos.y + 1.0, pos.z,
                30, 0.45, 0.85, 0.45, 0.05);
        sl.sendParticles(ParticleTypes.SWEEP_ATTACK,
                pos.x, pos.y + 1.2, pos.z, 3, 0.3, 0.3, 0.3, 0.0);
    }

    public static void execute() { /* legacy */ }

    @Mod.EventBusSubscriber(modid = "the_four_primitives_and_weapons")
    public static class TeleportStrikeTick {
        @SubscribeEvent
        public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Player player = event.player;
            if (player.level().isClientSide()) return;

            State s = active.get(player.getUUID());
            if (s == null) return;

            if (!player.isAlive()) {
                player.setInvulnerable(s.wasInvulnerable);
                active.remove(player.getUUID());
                return;
            }

            // Sneak で即キャンセル ( STRIKE フェーズ中のみ受付 ; CHANNEL 中は拘束されてる前提 )
            if (player.isShiftKeyDown() && s.phase == Phase.STRIKE) {
                ServerLevel slC = (ServerLevel) player.level();
                player.teleportTo(s.originalPos.x, s.originalPos.y, s.originalPos.z);
                player.setInvulnerable(s.wasInvulnerable);
                active.remove(player.getUUID());
                slC.sendParticles(ParticleTypes.SMOKE,
                        s.originalPos.x, s.originalPos.y + 1.0, s.originalPos.z,
                        15, 0.3, 0.5, 0.3, 0.05);
                return;
            }

            ServerLevel sl = (ServerLevel) player.level();
            s.tickInPhase++;

            switch (s.phase) {
                case CHANNEL: tickChannel(sl, player, s); break;
                case STRIKE:  tickStrike(sl, player, s);  break;
                case BURST:   tickBurst(sl, player, s);   break;
            }
        }
    }

    // ─── CHANNEL: 自己拘束 + 周囲に血粒子 ────────────────────────────────
    private static void tickChannel(ServerLevel sl, Player player, State s) {
        Vec3 p = player.position();
        // 周囲に血しぶき粒子 ( 2 tick おき )
        if ((s.tickInPhase % 2) == 0) {
            DustParticleOptions dust = new DustParticleOptions(
                    new Vector3f(0.65f, 0.05f, 0.05f), 1.2f);
            sl.sendParticles(dust, p.x, p.y + 0.5, p.z,
                    6, 0.7, 0.4, 0.7, 0.05);
            sl.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    p.x, p.y + 0.8, p.z, 1, 0.5, 0.2, 0.5, 0.0);
        }
        // マーク対象がチャネル中に死んだ場合は除外
        s.targets.removeIf(id -> {
            Entity e = sl.getEntity(id);
            return !(e instanceof LivingEntity le) || !le.isAlive();
        });

        if (s.tickInPhase >= CHANNEL_TICKS) {
            // STRIKE フェーズへ
            s.phase = Phase.STRIKE;
            s.tickInPhase = 0;
            s.currentIdx = 0;
            // 拘束を解除 + 攻撃用バフ付与
            player.removeEffect(MobEffects.BLINDNESS);
            player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 1, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING,      60, 0, false, false));
            player.setInvulnerable(true);
            sl.playSound(null, p.x, p.y, p.z,
                    SoundEvents.DROWNED_SHOOT, SoundSource.PLAYERS, 2.0f, 1.0f);
            sl.playSound(null, p.x, p.y, p.z,
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.6f);
        }
    }

    // ─── STRIKE: マーク敵 1 体ずつ TP しながら sweep ──────────────────────
    private static void tickStrike(ServerLevel sl, Player player, State s) {
        if (s.tickInPhase < TICKS_PER_TP) return;
        s.tickInPhase = 0;

        if (s.currentIdx >= s.targets.size()) {
            // 全 TP 完了 → BURST へ
            s.phase = Phase.BURST;
            s.tickInPhase = 0;
            return;
        }

        UUID tid = s.targets.get(s.currentIdx);
        s.currentIdx++;
        Entity te = sl.getEntity(tid);
        if (!(te instanceof LivingEntity target) || !target.isAlive()) return;

        Vec3 tpos = target.position();
        Vec3 from = player.position();
        Vec3 dir = tpos.subtract(from);
        if (dir.lengthSqr() < 1.0E-4) dir = new Vec3(1, 0, 0);
        dir = dir.normalize();
        Vec3 land = tpos.subtract(dir.scale(0.8));
        player.teleportTo(land.x, land.y, land.z);
        player.setYRot((float) Math.toDegrees(Math.atan2(
                -(target.getX() - player.getX()),
                target.getZ() - player.getZ())));
        player.setXRot(0);

        // 軽いダメージ + slowness ( BURST に向けて削り )
        int elemLevel = Math.max(ElementalDamageUtils.getElementLevel(player.getMainHandItem()), 1);
        applyBloodDamage(target, player, STRIKE_DAMAGE, elemLevel);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2, false, false));

        // 演出
        sl.sendParticles(ParticleTypes.SWEEP_ATTACK,
                target.getX(), target.getY() + 1.0, target.getZ(),
                3, 0.4, 0.4, 0.4, 0.0);
        DustParticleOptions trail = new DustParticleOptions(
                new Vector3f(0.65f, 0.05f, 0.05f), 1.4f);
        sl.sendParticles(trail, land.x, land.y + 0.5, land.z,
                15, 0.3, 0.5, 0.3, 0.05);
        sl.sendParticles(ParticleTypes.PORTAL, land.x, land.y + 1.0, land.z,
                10, 0.2, 0.5, 0.2, 0.3);
        sl.playSound(null, land.x, land.y, land.z,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.5f, 1.8f);
    }

    // ─── BURST: 全マーク敵に最終 AoE ────────────────────────────────────
    private static void tickBurst(ServerLevel sl, Player player, State s) {
        // 発動位置に帰還 ( BURST の直前に TP back )
        player.teleportTo(s.originalPos.x, s.originalPos.y, s.originalPos.z);
        // 帰還演出
        DustParticleOptions returnBurst = new DustParticleOptions(
                new Vector3f(0.8f, 0.05f, 0.05f), 1.8f);
        sl.sendParticles(returnBurst, s.originalPos.x, s.originalPos.y + 1.0, s.originalPos.z,
                40, 0.5, 0.8, 0.5, 0.1);
        sl.sendParticles(ParticleTypes.PORTAL,
                s.originalPos.x, s.originalPos.y + 1.0, s.originalPos.z,
                20, 0.3, 0.6, 0.3, 0.3);
        sl.playSound(null, s.originalPos.x, s.originalPos.y, s.originalPos.z,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.4f);

        int elemLevel = Math.max(ElementalDamageUtils.getElementLevel(player.getMainHandItem()), 1);
        int hits = 0;
        for (UUID id : s.targets) {
            Entity e = sl.getEntity(id);
            if (!(e instanceof LivingEntity target) || !target.isAlive()) continue;
            target.invulnerableTime = 0;
            applyBloodDamage(target, player, BURST_DAMAGE, elemLevel);
            SpecialDebuffHandler.applyBleed(target, BLEED_DURATION, BLEED_PER_TICK);
            SpecialDebuffHandler.applyWither(target, WITHER_DURATION_TICKS, WITHER_PER_TICK);

            // 派手な burst particles
            DustParticleOptions deep = new DustParticleOptions(
                    new Vector3f(0.55f, 0.03f, 0.03f), 1.6f);
            DustParticleOptions bright = new DustParticleOptions(
                    new Vector3f(0.95f, 0.12f, 0.12f), 1.4f);
            double cx = target.getX();
            double cy = target.getY() + target.getBbHeight() / 2.0;
            double cz = target.getZ();
            sl.sendParticles(deep, cx, cy, cz, 60, 0.5, 0.7, 0.5, 0.08);
            sl.sendParticles(bright, cx, cy, cz, 30, 0.5, 0.7, 0.5, 0.10);
            sl.sendParticles(ParticleTypes.SWEEP_ATTACK, cx, cy, cz, 6, 0.3, 0.3, 0.3, 0.0);
            sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                    cx, cy, cz, 20, 0.3, 0.4, 0.3, 0.15);
            sl.sendParticles(ParticleTypes.EXPLOSION, cx, cy, cz, 1, 0, 0, 0, 0);
            hits++;
        }
        if (hits > 0) {
            player.heal(HEAL_PER_HIT * hits);
        }
        sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WITHER_BREAK_BLOCK, SoundSource.PLAYERS, 1.4f, 0.5f);
        sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.DROWNED_SHOOT, SoundSource.PLAYERS, 2.0f, 1.5f);

        // 終了
        player.setInvulnerable(s.wasInvulnerable);
        active.remove(player.getUUID());
    }

    private static void applyBloodDamage(LivingEntity target, Player player, float amount, int level) {
        DamageSource ds = ModDamageSources.ofElement(player.level(), ElementType.BLOOD, player);
        try {
            IElementalDamageSource elem = (IElementalDamageSource) ds;
            elem.setElementType(ElementType.BLOOD);
            elem.setElementLevel(level);
        } catch (Throwable ignored) {}
        target.hurt(ds, amount);
    }
}

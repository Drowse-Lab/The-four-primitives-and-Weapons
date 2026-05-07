package minecraftarmorweapon.skill;

import minecraftarmorweapon.MinecraftArmorWeaponMod;
import minecraftarmorweapon.util.DamageCalculator;
import minecraftarmorweapon.init.MinecraftArmorWeaponModItems;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import top.theillusivec4.curios.api.CuriosApi;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 回転斬り (spin_slash) の multi-tick 制御ハンドラ.
 *
 *   - {@link #start} で session 開始 → 規定 tick 数かけて player を 720° 回転
 *   - 毎 tick: (1) player yaw を更新、(2) 開始角→現在角のアーク内の敵にダメージ
 *   - 各敵は 1 度のみヒット (hitEntities セットで管理)
 *
 * 速度ブースト:
 *   - {@code electric_book} または {@code thunder_book} を手 (main/off) または
 *     curios の "book" スロットに装備中 → 回転 tick 数が半減 (= 倍速)
 */
@Mod.EventBusSubscriber(modid = MinecraftArmorWeaponMod.MODID)
public class SpinSlashTickHandler {

    private static final Map<UUID, SpinSession> ACTIVE = new ConcurrentHashMap<>();

    /** 通常時 — 720° を 8 tick (0.4 秒) で回転. */
    public static final int BASE_TOTAL_TICKS = 8;
    /** electric_book / thunder_book 装備時 — 720° を 4 tick (0.2 秒) で回転. */
    public static final int BOOSTED_TOTAL_TICKS = 4;
    /** 1 回転で player が回る角度 (= 2 周). */
    public static final float TOTAL_ROTATION_DEG = 720f;

    /** 回転中の player は dodge 等他のスキル発動を抑止したい場合に参照. */
    public static boolean isSpinning(Player p) {
        return ACTIVE.containsKey(p.getUUID());
    }

    /** MotionExecutor.performSpinSlash から呼び出される session 開始. */
    public static void start(Player player, float damage, double range, boolean charged) {
        UUID id = player.getUUID();
        if (ACTIVE.containsKey(id)) return;   // 既に回転中なら無視
        int total = computeTotalTicks(player);
        ACTIVE.put(id, new SpinSession(player.getYRot(), total, damage, range, charged));
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        SpinSession s = ACTIVE.get(event.player.getUUID());
        if (s == null) return;

        Player p = event.player;
        s.elapsed++;

        float anglePerTick = TOTAL_ROTATION_DEG / s.totalTicks;
        float totalSwept = s.elapsed * anglePerTick;
        float newYaw = s.startYaw + totalSwept;

        // === 1. Visual rotation (server-authoritative + sync to client) ===
        if (p instanceof ServerPlayer sp) {
            sp.connection.teleport(sp.getX(), sp.getY(), sp.getZ(), newYaw, sp.getXRot(),
                java.util.Set.of());
        } else {
            p.setYRot(newYaw);
        }

        // === 2. アーク内ダメージ判定 (server only) ===
        if (!p.level().isClientSide) {
            damageInArc(p, s, totalSwept);
        }

        // === 3. パーティクル (現在の "刃先" 位置に) ===
        if (p.level() instanceof ServerLevel sw) {
            double rad = Math.toRadians(newYaw + 90);  // yaw 0 = -Z, +90 = +X 補正
            for (int ring = 0; ring < 2; ring++) {
                double r = s.range * (ring + 1) / 2.0;
                sw.sendParticles(ring == 0 ? ParticleTypes.SWEEP_ATTACK : ParticleTypes.CRIT,
                    p.getX() + Math.cos(rad) * r,
                    p.getY() + 1 + ring * 0.3,
                    p.getZ() + Math.sin(rad) * r,
                    1, 0, 0, 0, 0);
            }
        }

        // === 4. 終了判定 ===
        if (s.elapsed >= s.totalTicks) {
            ACTIVE.remove(event.player.getUUID());
            p.level().playSound(null, p.getX(), p.getY(), p.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.5f, 0.7f);
        }
    }

    /** 開始角 → 現在角のアーク内にいて、まだヒットしてない敵にダメージ. */
    private static void damageInArc(Player player, SpinSession s, float totalSwept) {
        Vec3 playerPos = player.position();
        Level world = player.level();
        AABB area = new AABB(playerPos.x - s.range, playerPos.y - 1, playerPos.z - s.range,
                              playerPos.x + s.range, playerPos.y + 3, playerPos.z + s.range);

        for (LivingEntity target : world.getEntitiesOfClass(LivingEntity.class, area, e -> e != player)) {
            if (s.hitEntities.contains(target.getUUID())) continue;
            if (target.distanceTo(player) > s.range) continue;

            Vec3 toTarget = target.position().subtract(playerPos);
            // Minecraft yaw 規約: 0° = -Z 方向, +90° = +X. atan2 を yaw 系に変換.
            double targetYaw = -Math.toDegrees(Math.atan2(toTarget.x, toTarget.z));
            double relativeAngle = wrap360(targetYaw - s.startYaw);

            // 開始角からの相対角が「これまでに掃いたアーク」内なら hit.
            // (totalSwept は 720° まで上がるので 360°超でも relativeAngle <= totalSwept で OK)
            if (totalSwept >= relativeAngle) {
                ItemStack weapon = player.getItemInHand(InteractionHand.MAIN_HAND);
                DamageCalculator.dealDamage(player, target, s.damage, weapon);
                Vec3 kb = target.position().subtract(playerPos).normalize().scale(s.charged ? 1.0 : 0.6);
                target.setDeltaMovement(kb.x, 0.4, kb.z);
                s.hitEntities.add(target.getUUID());

                if (world instanceof ServerLevel sw) {
                    sw.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                        2, 0.1, 0.1, 0.1, 0);
                }
            }
        }
    }

    /** electric_book / thunder_book 装備中なら brboost. */
    private static int computeTotalTicks(Player p) {
        return hasElectricBook(p) ? BOOSTED_TOTAL_TICKS : BASE_TOTAL_TICKS;
    }

    /** main/off hand または curios の "book" スロットに electric/thunder book があるか. */
    private static boolean hasElectricBook(Player p) {
        Item electric = MinecraftArmorWeaponModItems.ELECTRIC_BOOK.get();
        Item thunder = MinecraftArmorWeaponModItems.THUNDERBOLT.get();
        if (p.getMainHandItem().getItem() == electric || p.getMainHandItem().getItem() == thunder) return true;
        if (p.getOffhandItem().getItem() == electric || p.getOffhandItem().getItem() == thunder) return true;

        AtomicBoolean found = new AtomicBoolean(false);
        try {
            CuriosApi.getCuriosHelper().getCuriosHandler(p).ifPresent(handler -> {
                handler.getStacksHandler("book").ifPresent(sh -> {
                    for (int i = 0; i < sh.getStacks().getSlots(); i++) {
                        Item it = sh.getStacks().getStackInSlot(i).getItem();
                        if (it == electric || it == thunder) {
                            found.set(true);
                            return;
                        }
                    }
                });
            });
        } catch (Throwable ignored) {
            // Curios 未導入なら ignore
        }
        return found.get();
    }

    /** angle を [0, 360) に正規化. */
    private static double wrap360(double angle) {
        return ((angle % 360) + 360) % 360;
    }

    /** 進行中の回転 session 1 個分の状態. */
    private static class SpinSession {
        final float startYaw;
        int elapsed = 0;
        final int totalTicks;
        final float damage;
        final double range;
        final boolean charged;
        final Set<UUID> hitEntities = new HashSet<>();

        SpinSession(float startYaw, int totalTicks, float damage, double range, boolean charged) {
            this.startYaw = startYaw;
            this.totalTicks = totalTicks;
            this.damage = damage;
            this.range = range;
            this.charged = charged;
        }
    }
}

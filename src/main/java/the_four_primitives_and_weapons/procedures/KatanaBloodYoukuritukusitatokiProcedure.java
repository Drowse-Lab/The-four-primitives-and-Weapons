package the_four_primitives_and_weapons.procedures;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
 * Rivers of Blood の右クリック特殊技 — テレポート連続斬撃。
 *
 * フロー:
 *   1. 右クリックで周囲 10 ブロック圏内の敵 ( 最大 6 体 ) を target list に登録
 *   2. 元の位置を save し、 invulnerable = true
 *   3. tick handler で 1 体ずつ 3 tick おきに敵の隣に TP ( 視覚のみ、 ダメージは保留 )
 *   4. 全 target 訪問後、 元の位置に戻る → 戻った瞬間にダメージ + bleed を一斉適用
 *   5. invulnerable 解除 + クールダウン
 *
 * シフト ( Sneak ) を押すと TP 連続中にキャンセル可能 → ダメージなしで元の位置に戻る。
 */
public class KatanaBloodYoukuritukusitatokiProcedure {

    private static final double SEARCH_RADIUS  = 10.0;
    private static final int    MAX_TARGETS    = 6;
    private static final int    TICKS_PER_TP   = 3;
    private static final float  DAMAGE_PER_HIT = 8.0f;
    private static final float  BLEED_PER_TICK = 0.6f;
    private static final int    BLEED_DURATION = 80;
    private static final float  HEAL_PER_HIT   = 2.0f;
    private static final int    COOLDOWN_TICKS = 200;

    private static final class State {
        Vec3 originalPos;
        List<UUID> targets;
        int currentIdx;
        int tickInPhase;
        boolean returning;
        boolean wasInvulnerable;
    }
    private static final Map<UUID, State> active = new ConcurrentHashMap<>();

    public static void execute(Level world, Entity entity) {
        if (entity == null) return;
        if (!(entity instanceof Player player)) return;
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty() || held.getItem() != TheFourPrimitivesAndWeaponsModItems.RIVERS_OF_BLOOD.get()) return;

        // motion トグル: 無効化されていれば何もしない
        if (!the_four_primitives_and_weapons.skill.PlayerSkillData.isMotionEnabled(player, "rivers_of_blood_special")) {
            return;
        }

        if (player.getCooldowns().isOnCooldown(held.getItem())) return;
        if (active.containsKey(player.getUUID())) return; // 既に発動中

        Vec3 pos = player.position();
        AABB box = new AABB(
                pos.x - SEARCH_RADIUS, pos.y - 3, pos.z - SEARCH_RADIUS,
                pos.x + SEARCH_RADIUS, pos.y + 3, pos.z + SEARCH_RADIUS);
        List<LivingEntity> nearby = world.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isAlive() && e.distanceTo(player) <= SEARCH_RADIUS);
        if (nearby.isEmpty()) return;
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
        s.returning = false;
        s.wasInvulnerable = player.isInvulnerable();
        active.put(player.getUUID(), s);

        player.setInvulnerable(true);

        if (world instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.LARGE_SMOKE,
                    pos.x, pos.y + 1.0, pos.z, 8, 0.3, 0.5, 0.3, 0.05);
            DustParticleOptions burst = new DustParticleOptions(
                    new Vector3f(0.7f, 0.05f, 0.05f), 1.5f);
            sl.sendParticles(burst, pos.x, pos.y + 1.0, pos.z,
                    20, 0.4, 0.8, 0.4, 0.02);
        }
        world.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.7f, 1.6f);
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

            // 死亡時は中断
            if (!player.isAlive()) {
                player.setInvulnerable(s.wasInvulnerable);
                active.remove(player.getUUID());
                return;
            }

            // Sneak ( シフト ) で強制キャンセル → 即座に元の位置へ戻ってダメージ無しで終了
            if (player.isShiftKeyDown() && !s.returning) {
                ServerLevel slCancel = (ServerLevel) player.level();
                player.teleportTo(s.originalPos.x, s.originalPos.y, s.originalPos.z);
                player.setInvulnerable(s.wasInvulnerable);
                active.remove(player.getUUID());
                slCancel.sendParticles(ParticleTypes.SMOKE,
                        s.originalPos.x, s.originalPos.y + 1.0, s.originalPos.z,
                        15, 0.3, 0.5, 0.3, 0.05);
                slCancel.playSound(null, s.originalPos.x, s.originalPos.y, s.originalPos.z,
                        SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.5f, 0.8f);
                return;
            }

            s.tickInPhase++;
            if (s.tickInPhase < TICKS_PER_TP) return;
            s.tickInPhase = 0;

            ServerLevel sl = (ServerLevel) player.level();

            if (s.returning) {
                // 戻り完了 — ダメージ適用 + state 解除
                player.teleportTo(s.originalPos.x, s.originalPos.y, s.originalPos.z);
                applyFinalDamage(sl, player, s);
                player.setInvulnerable(s.wasInvulnerable);
                active.remove(player.getUUID());
                sl.sendParticles(ParticleTypes.EXPLOSION,
                        s.originalPos.x, s.originalPos.y + 1.0, s.originalPos.z, 1, 0, 0, 0, 0);
                DustParticleOptions burst = new DustParticleOptions(
                        new Vector3f(0.8f, 0.05f, 0.05f), 2.0f);
                sl.sendParticles(burst, s.originalPos.x, s.originalPos.y + 1.0, s.originalPos.z,
                        40, 0.6, 1.0, 0.6, 0.1);
                sl.playSound(null, s.originalPos.x, s.originalPos.y, s.originalPos.z,
                        SoundEvents.WITHER_BREAK_BLOCK, SoundSource.PLAYERS, 1.2f, 0.5f);
                return;
            }

            // 次の target に TP
            if (s.currentIdx >= s.targets.size()) {
                s.returning = true;
                return;
            }

            UUID targetId = s.targets.get(s.currentIdx);
            Entity targetEntity = sl.getEntity(targetId);
            s.currentIdx++;

            if (!(targetEntity instanceof LivingEntity target) || !target.isAlive()) {
                return;
            }

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

            DustParticleOptions trail = new DustParticleOptions(
                    new Vector3f(0.65f, 0.05f, 0.05f), 1.4f);
            sl.sendParticles(trail, land.x, land.y + 0.5, land.z,
                    12, 0.3, 0.5, 0.3, 0.05);
            sl.sendParticles(ParticleTypes.PORTAL, land.x, land.y + 1.0, land.z,
                    10, 0.2, 0.5, 0.2, 0.3);
            sl.playSound(null, land.x, land.y, land.z,
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.5f, 1.8f);
        }
    }

    private static void applyFinalDamage(ServerLevel sl, Player player, State s) {
        int hits = 0;
        for (UUID id : s.targets) {
            Entity e = sl.getEntity(id);
            if (!(e instanceof LivingEntity target) || !target.isAlive()) continue;
            target.invulnerableTime = 0;
            target.hurt(ModDamageSources.ofElement(player.level(), ElementType.BLOOD, player),
                    DAMAGE_PER_HIT);
            SpecialDebuffHandler.applyBleed(target, BLEED_DURATION, BLEED_PER_TICK);
            sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                    target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                    20, 0.3, 0.4, 0.3, 0.15);
            DustParticleOptions blood = new DustParticleOptions(
                    new Vector3f(0.65f, 0.05f, 0.05f), 1.5f);
            sl.sendParticles(blood, target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                    20, 0.3, 0.4, 0.3, 0.05);
            hits++;
        }
        if (hits > 0) {
            player.heal(HEAL_PER_HIT * hits);
        }
    }
}

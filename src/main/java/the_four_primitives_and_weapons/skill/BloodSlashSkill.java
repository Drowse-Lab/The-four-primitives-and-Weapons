package the_four_primitives_and_weapons.skill;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Rivers of Blood の単押し右クリック技 — 横一文字の血の刃を前方に飛ばす。
 *
 * 振る舞い:
 *   - 横長・縦薄の「板状の刃」が プレイヤー目線高 から forward 方向へ 1.5 ブロック/tick で進む
 *   - lifetime = 10 tick → 約 15 ブロック前方まで到達
 *   - 通過したエンティティに血属性ダメージ + bleed ( 1 体 1 回限定 )
 *   - エッジ outline を dust particles で描画し、 刃が飛んでいく見た目に
 */
public final class BloodSlashSkill {

    private BloodSlashSkill() {}

    // 刃の形状 ( 局所座標 )
    private static final double HALF_LEN    = 1.0;   // 進行方向の厚み ( 局所 forward )
    private static final double HALF_WIDTH  = 2.5;   // 左右の広がり
    private static final double HALF_HEIGHT = 0.25;  // 上下の薄さ

    private static final double SPEED          = 1.5;   // ブロック / tick
    private static final int    LIFETIME_TICKS = 10;
    private static final float  DAMAGE         = 7.0f;
    private static final int    BLEED_DURATION = 60;
    private static final float  BLEED_PER_TICK = 0.5f;
    private static final float  HEAL_PER_HIT   = 1.5f;

    /** 飛んでいる斬撃 1 つ分の state */
    private static final class Wave {
        UUID ownerId;
        UUID levelKey;
        Vec3 origin;
        Vec3 forward;
        Vec3 right;
        Vec3 up;
        int  elemLevel;
        int  age;
        Set<Integer> hit = new HashSet<>();
    }

    private static final ConcurrentLinkedQueue<Wave> active = new ConcurrentLinkedQueue<>();

    public static void fire(Player player) {
        if (player.level().isClientSide()) return;
        ServerLevel level = (ServerLevel) player.level();

        Vec3 forward = player.getLookAngle().normalize();
        Vec3 worldUp = new Vec3(0, 1, 0);
        Vec3 right = forward.cross(worldUp);
        if (right.lengthSqr() < 1.0E-6) right = new Vec3(1, 0, 0);
        right = right.normalize();
        Vec3 up = right.cross(forward).normalize();

        Wave w = new Wave();
        w.ownerId  = player.getUUID();
        w.levelKey = level.dimension().location() == null ? null : null; // dimension 用 (今は使わない)
        w.origin   = player.getEyePosition();
        w.forward  = forward;
        w.right    = right;
        w.up       = up;
        w.elemLevel = Math.max(ElementalDamageUtils.getElementLevel(player.getMainHandItem()), 1);
        w.age = 0;
        active.add(w);

        // 起点で「鞘走り」演出
        DustParticleOptions burst = new DustParticleOptions(new Vector3f(0.95f, 0.12f, 0.12f), 1.5f);
        level.sendParticles(burst, w.origin.x, w.origin.y, w.origin.z, 12, 0.3, 0.2, 0.3, 0.06);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 0.6f);
    }

    @Mod.EventBusSubscriber(modid = "the_four_primitives_and_weapons")
    public static class WaveTickHandler {
        @SubscribeEvent
        public static void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            if (event.getServer() == null) return;
            if (active.isEmpty()) return;

            Iterator<Wave> it = active.iterator();
            while (it.hasNext()) {
                Wave w = it.next();
                w.age++;
                // 進行中心 ( origin + forward * SPEED * age )
                Vec3 center = w.origin.add(w.forward.scale(SPEED * w.age));
                // 寿命が来たら除去
                if (w.age > LIFETIME_TICKS) {
                    it.remove();
                    continue;
                }
                // 各 level で owner を探して同 level だけ処理
                ServerLevel sl = findOwnerLevel(event.getServer(), w.ownerId);
                if (sl == null) {
                    it.remove();
                    continue;
                }
                Player owner = sl.getServer().getPlayerList().getPlayer(w.ownerId);

                renderEdges(sl, center, w.forward, w.right, w.up);
                applyHits(sl, owner, w, center);
            }
        }

        private static ServerLevel findOwnerLevel(net.minecraft.server.MinecraftServer server, UUID ownerId) {
            for (ServerLevel sl : server.getAllLevels()) {
                if (sl.getEntity(ownerId) != null) return sl;
            }
            // owner が居なくても進行は継続したいので任意の overworld を返す
            return server.overworld();
        }
    }

    private static void applyHits(ServerLevel sl, Player owner, Wave w, Vec3 center) {
        // 大まかな AABB で絞り込み → 局所座標で OBB 判定
        double r = Math.max(HALF_LEN, Math.max(HALF_WIDTH, HALF_HEIGHT)) + 1.0;
        AABB pick = new AABB(
                center.x - r, center.y - r, center.z - r,
                center.x + r, center.y + r, center.z + r);

        List<LivingEntity> nearby = new ArrayList<>(sl.getEntitiesOfClass(LivingEntity.class, pick,
                e -> (owner == null || e != owner) && e.isAlive()));

        int hits = 0;
        for (LivingEntity target : nearby) {
            if (w.hit.contains(target.getId())) continue;
            Vec3 rel = target.position().add(0, target.getBbHeight() / 2.0, 0).subtract(center);
            double lf = rel.dot(w.forward);
            double lr = rel.dot(w.right);
            double lu = rel.dot(w.up);
            if (Math.abs(lf) > HALF_LEN)    continue;
            if (Math.abs(lr) > HALF_WIDTH)  continue;
            if (Math.abs(lu) > HALF_HEIGHT) continue;

            w.hit.add(target.getId());

            target.invulnerableTime = 0;
            DamageSource ds = ModDamageSources.ofElement(sl, ElementType.BLOOD, owner);
            try {
                IElementalDamageSource elem = (IElementalDamageSource) ds;
                elem.setElementType(ElementType.BLOOD);
                elem.setElementLevel(w.elemLevel);
            } catch (Throwable ignored) {}
            target.hurt(ds, DAMAGE);
            SpecialDebuffHandler.applyBleed(target, BLEED_DURATION, BLEED_PER_TICK);

            sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                    target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                    10, 0.3, 0.3, 0.3, 0.1);
            hits++;
        }
        if (hits > 0 && owner != null) {
            owner.heal(HEAL_PER_HIT * hits);
        }
    }

    private static void renderEdges(ServerLevel sl, Vec3 center, Vec3 forward, Vec3 right, Vec3 up) {
        DustParticleOptions dark   = new DustParticleOptions(new Vector3f(0.55f, 0.03f, 0.03f), 1.2f);
        DustParticleOptions bright = new DustParticleOptions(new Vector3f(0.95f, 0.12f, 0.12f), 1.0f);

        Vec3[] corners = new Vec3[8];
        int k = 0;
        for (int fi = 0; fi < 2; fi++) {
            double lf = fi == 0 ? -HALF_LEN : HALF_LEN;
            for (int ri = 0; ri < 2; ri++) {
                double lr = ri == 0 ? -HALF_WIDTH : HALF_WIDTH;
                for (int ui = 0; ui < 2; ui++) {
                    double lu = ui == 0 ? -HALF_HEIGHT : HALF_HEIGHT;
                    corners[k++] = center
                            .add(forward.scale(lf))
                            .add(right.scale(lr))
                            .add(up.scale(lu));
                }
            }
        }

        int[][] edges = {
                {0,1},{0,2},{1,3},{2,3},
                {4,5},{4,6},{5,7},{6,7},
                {0,4},{1,5},{2,6},{3,7}
        };
        for (int[] e : edges) {
            Vec3 a = corners[e[0]];
            Vec3 b = corners[e[1]];
            double dist = a.distanceTo(b);
            int steps = Math.max(2, (int) Math.ceil(dist * 4));
            for (int i = 0; i <= steps; i++) {
                double t = (double) i / steps;
                double x = a.x + (b.x - a.x) * t;
                double y = a.y + (b.y - a.y) * t;
                double z = a.z + (b.z - a.z) * t;
                sl.sendParticles(dark, x, y, z, 1, 0, 0, 0, 0);
                if ((i % 2) == 0) sl.sendParticles(bright, x, y, z, 1, 0, 0, 0, 0);
            }
        }
        // 進行方向の余韻 ( 軌跡 ) を 中心点周りに少し撒く
        sl.sendParticles(ParticleTypes.SWEEP_ATTACK, center.x, center.y, center.z, 1, 0.1, 0.1, 0.1, 0.0);
    }
}

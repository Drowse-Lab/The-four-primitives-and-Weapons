package the_four_primitives_and_weapons.damage;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;

/**
 * 魂属性ヒット時の "Soul Edge" 演出。
 *
 * <p>元ネタは soul-sword データパックの技 (attack.mcfunction の so1/so2 精霊)。
 * 両脇から召喚した魂の刃が {@code soul} / {@code soul_fire_flame} を撒きながら対象へ
 * ホーミングし、最後に魂の衝撃波リングで弾ける。 <b>地形破壊はしない</b> ため、
 * データパックのような fireball 爆発ではなく純粋なパーティクル演出のみで表現する。</p>
 *
 * <p>複数 tick に渡るアニメーションは {@link TheFourPrimitivesAndWeaponsMod#queueServerWork}
 * で駆動する。 対象が生存中はその位置を追従し、死亡/消滅後はヒット地点に留まる。</p>
 */
public final class SoulEdgeEffect {

    private static final int DURATION = 16;            // ホーミングにかける tick 数
    private static final double START_RADIUS = 2.4;    // 精霊が周回を始める半径
    private static final double END_RADIUS = 0.15;     // 収束する半径

    private SoulEdgeEffect() {}

    /** 魂属性ヒット時に呼ぶ。 attacker は不明なら null 可。 */
    public static void play(LivingEntity target, LivingEntity attacker, int level) {
        if (target == null || !(target.level() instanceof ServerLevel level3d)) return;

        int lv = Math.max(1, level);
        int blades = Math.min(4, 2 + lv / 4);          // Lvが上がると魂の刃が増える (2→4)
        double[] fallback = center(target);

        // 詠唱: 召喚音 + 両脇からの launch。
        playSound(level3d, fallback, SoundEvents.ELDER_GUARDIAN_CURSE, 0.6f, 1.3f);
        playSound(level3d, fallback, SoundEvents.VEX_CHARGE, 0.8f, 0.7f);
        if (attacker != null && attacker != target) {
            launchFromAttacker(level3d, attacker, fallback);
        }

        for (int t = 0; t <= DURATION; t++) {
            final int tick = t;
            TheFourPrimitivesAndWeaponsMod.queueServerWork(t + 1, () ->
                    step(level3d, target, fallback, blades, lv, tick));
        }
    }

    private static void step(ServerLevel level3d, LivingEntity target, double[] fallback,
                             int blades, int lv, int t) {
        double[] c = target.isAlive() && !target.isRemoved() ? center(target) : fallback;
        double cx = c[0], cy = c[1], cz = c[2];

        if (t >= DURATION) {
            burst(level3d, cx, cy, cz, lv);
            return;
        }
        if (t == DURATION - 3) {
            playSound(level3d, c, SoundEvents.VEX_CHARGE, 0.9f, 1.4f);
        }

        float prog = (float) t / DURATION;               // 0..1
        double radius = lerp(START_RADIUS, END_RADIUS, easeIn(prog));
        double spin = Math.toRadians(240) * prog;        // 収束しながら 240° 回る
        for (int b = 0; b < blades; b++) {
            double ang = (2 * Math.PI * b) / blades + spin;
            double bx = cx + Math.cos(ang) * radius;
            double bz = cz + Math.sin(ang) * radius;
            double by = cy + (1.0 - prog) * 0.9;         // 上から中心高さへ降りてくる
            // 魂の刃: 中心へ向かう短い線 + 先端の魂炎。
            drawStreak(level3d, bx, by, bz, cx, cy, cz, 0.55, ParticleTypes.SOUL, 3);
            spawn(level3d, ParticleTypes.SOUL_FIRE_FLAME, bx, by, bz, 1, 0.0);
        }
    }

    /** 最後の魂の大爆発 (地形破壊なし)。 ブロックを壊さない本物の爆発 + 魂の衝撃波リング。 */
    private static void burst(ServerLevel level3d, double cx, double cy, double cz, int lv) {
        // ブロックを壊さない爆発本体: ノックバック・爆発粒子・爆発音つき、地形破壊なし。
        float power = (float) Math.min(4.0, 2.2 + lv * 0.18);
        level3d.explode(null, cx, cy, cz, power, Level.ExplosionInteraction.NONE);

        // 大爆発の閃光 + 音。
        level3d.sendParticles(ParticleTypes.EXPLOSION_EMITTER, cx, cy, cz, 1, 0.0, 0.0, 0.0, 0.0);
        level3d.sendParticles(ParticleTypes.EXPLOSION, cx, cy, cz, 8, 0.9, 0.7, 0.9, 0.0);
        playSound(level3d, new double[]{cx, cy, cz}, SoundEvents.GENERIC_EXPLODE, 1.6f, 0.7f);
        playSound(level3d, new double[]{cx, cy, cz}, SoundEvents.VEX_DEATH, 1.4f, 0.5f);
        playSound(level3d, new double[]{cx, cy, cz}, SoundEvents.WITHER_DEATH, 0.6f, 1.5f);

        // 大きく広がる魂の衝撃波: 三重リング。
        double outer = 2.2 + lv * 0.12;
        int steps = 64;
        for (int i = 0; i < steps; i++) {
            double ang = (2 * Math.PI * i) / steps;
            double cos = Math.cos(ang), sin = Math.sin(ang);
            spawn(level3d, ParticleTypes.SOUL, cx + cos * outer, cy, cz + sin * outer, 1, 0.06);
            spawn(level3d, ParticleTypes.SOUL_FIRE_FLAME, cx + cos * (outer * 0.66), cy, cz + sin * (outer * 0.66), 1, 0.04);
            spawn(level3d, ParticleTypes.SOUL, cx + cos * (outer * 0.33), cy, cz + sin * (outer * 0.33), 1, 0.02);
        }
        // 立ち上る魂炎の柱。
        for (int i = 0; i < 12; i++) {
            spawn(level3d, ParticleTypes.SOUL_FIRE_FLAME, cx, cy - 0.3 + i * 0.3, cz, 1, 0.015);
        }
        // 中心から四方へ吹き出す魂。
        level3d.sendParticles(ParticleTypes.SOUL, cx, cy, cz, 80, 0.6, 0.7, 0.6, 0.12);
        level3d.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, cx, cy, cz, 40, 0.5, 0.6, 0.5, 0.1);
    }

    /** attacker の両脇から対象へ伸びる召喚ストリーク (so1/so2 の launch)。 */
    private static void launchFromAttacker(ServerLevel level3d, LivingEntity attacker, double[] target) {
        double ax = attacker.getX();
        double ay = attacker.getY() + attacker.getBbHeight() * 0.6;
        double az = attacker.getZ();
        double dx = target[0] - ax, dz = target[2] - az;
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1.0e-3) return;
        // 進行方向に対する水平の垂直ベクトルで左右にオフセット。
        double px = -dz / len, pz = dx / len;
        for (int side = -1; side <= 1; side += 2) {
            double sx = ax + px * 0.6 * side;
            double sz = az + pz * 0.6 * side;
            drawStreak(level3d, sx, ay, sz, target[0], target[1], target[2], 1.0, ParticleTypes.SOUL, 10);
        }
    }

    /** from→to を fraction ぶんだけ steps 個の粒子で結ぶ (刃/軌跡)。 */
    private static void drawStreak(ServerLevel level3d, double fx, double fy, double fz,
                                   double tx, double ty, double tz,
                                   double fraction, ParticleOptions particle, int steps) {
        for (int i = 0; i <= steps; i++) {
            double f = (fraction * i) / steps;
            spawn(level3d, particle, fx + (tx - fx) * f, fy + (ty - fy) * f, fz + (tz - fz) * f, 1, 0.0);
        }
    }

    private static void spawn(ServerLevel level3d, ParticleOptions particle,
                              double x, double y, double z, int count, double speed) {
        level3d.sendParticles(particle, x, y, z, count, 0.0, 0.0, 0.0, speed);
    }

    private static void playSound(ServerLevel level3d, double[] pos,
                                  net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        level3d.playSound(null, pos[0], pos[1], pos[2], sound, SoundSource.HOSTILE, volume, pitch);
    }

    private static double[] center(LivingEntity e) {
        return new double[]{e.getX(), e.getY() + e.getBbHeight() * 0.55, e.getZ()};
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    /** 序盤ゆっくり→終盤一気に収束する ease-in。 */
    private static float easeIn(float t) {
        return t * t;
    }
}

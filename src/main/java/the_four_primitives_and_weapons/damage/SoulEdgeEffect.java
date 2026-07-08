package the_four_primitives_and_weapons.damage;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;

/**
 * 魂属性ヒット時の "Soul Edge" 演出。
 *
 * <p>元ネタは soul-sword データパックの技 (attack.mcfunction の so1/so2 精霊)。
 * 両脇から召喚した魂の刃が {@code soul} / {@code soul_fire_flame} を撒きながら対象へ
 * ホーミングし、最後に地形破壊なしの大爆発で弾ける。</p>
 *
 * <p>複数 tick に渡るアニメーションは {@link TheFourPrimitivesAndWeaponsMod#queueServerWork}
 * で駆動する。 対象が生存中はその位置を追従し、死亡/消滅後はヒット地点に留まる。</p>
 *
 * <p><b>性能上の注意:</b> {@code sendParticles} は呼ぶたびに全観測プレイヤーへ 1 パケット
 * 送られる。 リングや軌跡を 1 点ずつ送るとヒット毎に数百パケットになりラグの原因になるため、
 * 拡散粒子は {@code count} 指定で 1 パケットにまとめ、点描はごく少数に抑えている。</p>
 */
public final class SoulEdgeEffect {

    private static final int DURATION = 14;            // ホーミングにかける tick 数
    private static final double START_RADIUS = 2.4;    // 精霊が周回を始める半径
    private static final double END_RADIUS = 0.15;     // 収束する半径

    private SoulEdgeEffect() {}

    /** 魂属性ヒット時に呼ぶ。 attacker は不明なら null 可。 */
    public static void play(LivingEntity target, LivingEntity attacker, int level) {
        if (target == null || !(target.level() instanceof ServerLevel level3d)) return;

        int lv = Math.max(1, level);
        int blades = Math.min(3, 2 + lv / 6);          // 魂の刃 2〜3 本
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
        boolean flameTick = (t % 3) == 0;                // 魂炎は 3 tick に 1 回だけ (パケット節約)
        for (int b = 0; b < blades; b++) {
            double ang = (2 * Math.PI * b) / blades + spin;
            double bx = cx + Math.cos(ang) * radius;
            double bz = cz + Math.sin(ang) * radius;
            double by = cy + (1.0 - prog) * 0.9;         // 上から中心高さへ降りてくる
            // 魂の刃: 移動する 1 点が tick を跨いで螺旋の軌跡になる。
            spawn(level3d, ParticleTypes.SOUL, bx, by, bz, 1, 0.0);
            if (flameTick) {
                spawn(level3d, ParticleTypes.SOUL_FIRE_FLAME, bx, by, bz, 1, 0.0);
            }
        }
    }

    /** 最後の魂の大爆発 (地形破壊なし)。 ブロックを壊さない本物の爆発 + 魂の衝撃波。 */
    private static void burst(ServerLevel level3d, double cx, double cy, double cz, int lv) {
        // 通常攻撃のたびに出るため、爆発 (ノックバック/ダメージ/爆発粒子/爆発音) は付けない。
        // 魂テーマの粒子と音だけで締めの一撃を表現する。
        playSound(level3d, cx, cy, cz, SoundEvents.VEX_DEATH, 1.0f, 0.5f);
        playSound(level3d, cx, cy, cz, SoundEvents.WITHER_DEATH, 0.4f, 1.5f);

        // 拡散粒子は count 指定で 1 パケットにまとめる: 魂の雲 + 魂炎 + 立ち上る柱。
        level3d.sendParticles(ParticleTypes.SOUL, cx, cy, cz, 70, 0.9, 0.7, 0.9, 0.12);
        level3d.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, cx, cy, cz, 35, 0.7, 0.6, 0.7, 0.1);
        level3d.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, cx, cy + 0.9, cz, 12, 0.05, 0.8, 0.05, 0.02);

        // 衝撃波リングの形は少数の点描で示す (点数を絞ってパケットを抑える)。
        double outer = 2.0 + lv * 0.1;
        int steps = 24;
        for (int i = 0; i < steps; i++) {
            double ang = (2 * Math.PI * i) / steps;
            spawn(level3d, ParticleTypes.SOUL,
                    cx + Math.cos(ang) * outer, cy, cz + Math.sin(ang) * outer, 1, 0.05);
        }
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
            drawStreak(level3d, sx, ay, sz, target[0], target[1], target[2], 4);
        }
    }

    /** from→to を steps 個の粒子で結ぶ (launch の軌跡)。 */
    private static void drawStreak(ServerLevel level3d, double fx, double fy, double fz,
                                   double tx, double ty, double tz, int steps) {
        for (int i = 0; i <= steps; i++) {
            double f = (double) i / steps;
            spawn(level3d, ParticleTypes.SOUL, fx + (tx - fx) * f, fy + (ty - fy) * f, fz + (tz - fz) * f, 1, 0.0);
        }
    }

    private static void spawn(ServerLevel level3d, ParticleOptions particle,
                              double x, double y, double z, int count, double speed) {
        level3d.sendParticles(particle, x, y, z, count, 0.0, 0.0, 0.0, speed);
    }

    private static void playSound(ServerLevel level3d, double x, double y, double z,
                                  net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        level3d.playSound(null, x, y, z, sound, SoundSource.HOSTILE, volume, pitch);
    }

    private static void playSound(ServerLevel level3d, double[] pos,
                                  net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        playSound(level3d, pos[0], pos[1], pos[2], sound, volume, pitch);
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

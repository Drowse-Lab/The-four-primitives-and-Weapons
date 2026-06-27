package the_four_primitives_and_weapons.skill;

import the_four_primitives_and_weapons.block.ElectricConductBlock;
import the_four_primitives_and_weapons.damage.ElectricElementDamageHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/**
 * ELECTRIC 地走り放電スキル
 * プレイヤーの正面方向へ、地面の起伏に沿って電撃を走らせ、経路上のエンティティを感電させる。
 */
public final class ElectricBeamSkill {

    private ElectricBeamSkill() {}

    private static final double MAX_DISTANCE = 24.0; // 最大到達距離
    private static final double STEP = 0.75;         // 1ステップの前進量
    private static final float DAMAGE = 8.0f;
    private static final double HIT_RADIUS = 1.2;    // 地面ポイント周辺の当たり判定半径
    private static final int SCAN_UP = 2;            // 地面探索: 上方向
    private static final int SCAN_DOWN = 4;          // 地面探索: 下方向

    public static void fire(Player player) {
        if (player.level().isClientSide()) return;

        ServerLevel level = (ServerLevel) player.level();
        RandomSource rng = level.random;

        Vec3 look = player.getLookAngle();
        Vec3 horiz = new Vec3(look.x, 0, look.z);
        if (horiz.lengthSqr() < 1.0e-4) {
            horiz = new Vec3(1, 0, 0);
        }
        horiz = horiz.normalize();

        Vec3 cur = player.position(); // 足元
        double groundY = cur.y;

        Set<Integer> damaged = new HashSet<>();

        level.playSound(null, cur.x, cur.y, cur.z,
                SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.8f, 1.5f);

        int steps = (int) (MAX_DISTANCE / STEP);
        for (int i = 0; i < steps; i++) {
            // 横にわずかに蛇行させて電撃らしさを出す
            double wobble = (rng.nextDouble() - 0.5) * 0.3;
            Vec3 side = new Vec3(-horiz.z, 0, horiz.x).scale(wobble);
            Vec3 flat = cur.add(horiz.scale(STEP)).add(side);

            // 地面の高さを探す（見つからなければ前の高さを維持）
            Double surface = findGroundY(level, flat.x, flat.z, groundY);
            if (surface == null) {
                // 大きな崖や空中 → そこで終了
                break;
            }
            groundY = surface;

            Vec3 point = new Vec3(flat.x, groundY + 0.1, flat.z);

            renderGroundSpark(level, cur, point);
            damageAround(level, player, point, damaged);

            // 導体ブロックがあればさらに通電
            BlockPos groundBlock = BlockPos.containing(point.x, groundY - 0.5, point.z);
            ElectricConductBlock.conduct(level, groundBlock, DAMAGE);

            cur = point;
        }
    }

    /** (x,z) 付近で refY を基準に最寄りの地面の上面 Y を探す。見つからなければ null。 */
    private static Double findGroundY(ServerLevel level, double x, double z, double refY) {
        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);
        int baseY = (int) Math.floor(refY);

        for (int dy = SCAN_UP; dy >= -SCAN_DOWN; dy--) {
            BlockPos solid = new BlockPos(bx, baseY + dy, bz);
            BlockPos above = solid.above();
            boolean solidHere = !level.getBlockState(solid).getCollisionShape(level, solid).isEmpty();
            boolean openAbove = level.getBlockState(above).getCollisionShape(level, above).isEmpty();
            if (solidHere && openAbove) {
                return (double) (solid.getY() + 1);
            }
        }
        return null;
    }

    private static void damageAround(ServerLevel level, Player player, Vec3 point, Set<Integer> damaged) {
        AABB box = new AABB(point, point).inflate(HIT_RADIUS, HIT_RADIUS + 0.6, HIT_RADIUS);
        for (LivingEntity entity : level.getEntitiesOfClass(
                LivingEntity.class, box,
                e -> e != player && e.isAlive()
        )) {
            if (damaged.add(entity.getId())) {
                ElectricElementDamageHandler.applyElectricDamage(entity, DAMAGE, player, 1);
            }
        }
    }

    private static void renderGroundSpark(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 diff = to.subtract(from);
        int sub = 2;
        for (int i = 1; i <= sub; i++) {
            double t = (double) i / sub;
            double x = from.x + diff.x * t;
            double y = from.y + diff.y * t;
            double z = from.z + diff.z * t;
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 2, 0.1, 0.05, 0.1, 0.02);
        }
        level.sendParticles(ParticleTypes.END_ROD, to.x, to.y + 0.05, to.z, 1, 0.05, 0.02, 0.05, 0.0);
    }
}

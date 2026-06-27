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
 * ELECTRIC 根状放電スキル
 * プレイヤーを基準に、ランダムな方向へ根っこ状に枝分かれする電撃を放電する。
 */
public final class ElectricSlashSkill {

    private ElectricSlashSkill() {}

    private static final int ROOT_COUNT = 14;        // 主根の本数
    private static final int MAX_STEP = 16;          // 1本あたりの最大ステップ数
    private static final double STEP_LENGTH = 0.55;  // 1ステップの長さ
    private static final double WANDER = 0.5;        // 進行方向のふらつき量
    private static final double BRANCH_CHANCE = 0.22;// 枝分かれ確率
    private static final int MAX_DEPTH = 2;          // 枝分かれの最大深さ
    private static final float DAMAGE = 10.0f;
    private static final double HIT_RADIUS = 0.8;

    public static void fire(Player player) {
        if (player.level().isClientSide()) return;

        ServerLevel level = (ServerLevel) player.level();
        RandomSource rng = level.random;
        // プレイヤーの足元付近を放電の基準点にする
        Vec3 origin = player.position().add(0, 0.3, 0);

        Set<Integer> damaged = new HashSet<>();

        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.7f, 1.7f);

        for (int i = 0; i < ROOT_COUNT; i++) {
            // 全方位ランダム（やや外側へ広がるよう垂直成分を抑える）
            Vec3 dir = randomDirection(rng);
            growBranch(level, player, origin, dir, MAX_STEP, damaged, rng, 0);
        }
    }

    private static void growBranch(ServerLevel level, Player player, Vec3 start, Vec3 dir,
                                   int steps, Set<Integer> damaged, RandomSource rng, int depth) {
        Vec3 pos = start;
        Vec3 d = dir.lengthSqr() < 1.0e-6 ? new Vec3(1, 0, 0) : dir.normalize();

        for (int s = 0; s < steps; s++) {
            // 進行方向をふらつかせて根っこらしい不規則な伸び方にする
            d = d.add(randomVec(rng).scale(WANDER)).normalize();
            Vec3 next = pos.add(d.scale(STEP_LENGTH));

            spawnLine(level, pos, next);
            damageAround(level, player, next, damaged);

            pos = next;

            // 枝分かれ
            if (depth < MAX_DEPTH && rng.nextDouble() < BRANCH_CHANCE) {
                Vec3 branchDir = d.add(randomVec(rng).scale(0.9)).normalize();
                int remaining = Math.max(3, steps - s - 2);
                growBranch(level, player, pos, branchDir, remaining, damaged, rng, depth + 1);
            }
        }

        // 根の先端で導体ブロックがあれば通電
        BlockPos endBlock = BlockPos.containing(pos.x, pos.y, pos.z);
        ElectricConductBlock.conduct(level, endBlock, DAMAGE);
    }

    private static void damageAround(ServerLevel level, Player player, Vec3 point, Set<Integer> damaged) {
        AABB box = new AABB(point, point).inflate(HIT_RADIUS);
        for (LivingEntity entity : level.getEntitiesOfClass(
                LivingEntity.class, box,
                e -> e != player && e.isAlive()
        )) {
            if (damaged.add(entity.getId())) {
                ElectricElementDamageHandler.applyElectricDamage(entity, DAMAGE, player, 2);
            }
        }
    }

    private static void spawnLine(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 diff = to.subtract(from);
        int sub = 2;
        for (int i = 1; i <= sub; i++) {
            double t = (double) i / sub;
            double x = from.x + diff.x * t;
            double y = from.y + diff.y * t;
            double z = from.z + diff.z * t;
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 1, 0.02, 0.02, 0.02, 0.01);
        }
        level.sendParticles(ParticleTypes.END_ROD, to.x, to.y, to.z, 1, 0.01, 0.01, 0.01, 0.0);
    }

    /** 全方位のランダムな単位ベクトル（やや水平寄り） */
    private static Vec3 randomDirection(RandomSource rng) {
        double x = rng.nextDouble() * 2 - 1;
        double y = (rng.nextDouble() * 2 - 1) * 0.6; // 垂直成分を抑えて外へ広げる
        double z = rng.nextDouble() * 2 - 1;
        Vec3 v = new Vec3(x, y, z);
        return v.lengthSqr() < 1.0e-6 ? new Vec3(1, 0, 0) : v.normalize();
    }

    /** ふらつき用のランダムベクトル */
    private static Vec3 randomVec(RandomSource rng) {
        return new Vec3(
                rng.nextDouble() * 2 - 1,
                rng.nextDouble() * 2 - 1,
                rng.nextDouble() * 2 - 1
        );
    }
}

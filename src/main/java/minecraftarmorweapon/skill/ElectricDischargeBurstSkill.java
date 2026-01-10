package minecraftarmorweapon.skill;

import minecraftarmorweapon.block.ElectricConductBlock;
import minecraftarmorweapon.damage.ElementType;
import minecraftarmorweapon.damage.ElectricElementDamageHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * 放射型 ELECTRIC 放電バースト
 */
public final class ElectricDischargeBurstSkill {

    private static final Random RANDOM = new Random();

    private ElectricDischargeBurstSkill() {}

    public static void fire(Player player) {
        if (player.level.isClientSide()) return;

        ServerLevel level = (ServerLevel) player.level;

        Vec3 origin = player.position().add(0, 1.0, 0);

        int branchCount = 16;          // 放射本数
        int maxStep = 30;              // 1本あたりの長さ
        double damage = 6.0;

        for (int i = 0; i < branchCount; i++) {
            Vec3 direction = randomUnitVector();
            dischargeBranch(level, origin, direction, maxStep, damage);
        }
    }

    /* ===============================
     * 1本の放電処理
     * =============================== */

    private static void dischargeBranch(
            ServerLevel level,
            Vec3 start,
            Vec3 direction,
            int maxStep,
            double damage
    ) {
        Vec3 pos = start;
        Set<BlockPos> visitedBlocks = new HashSet<>();

        for (int i = 0; i < maxStep; i++) {

            // 少しランダムに曲げる（雷っぽさ）
            direction = direction.add(
                    (RANDOM.nextDouble() - 0.5) * 0.3,
                    (RANDOM.nextDouble() - 0.5) * 0.3,
                    (RANDOM.nextDouble() - 0.5) * 0.3
            ).normalize();

            pos = pos.add(direction.scale(0.6));

            // パーティクル描画
            level.sendParticles(
                    ParticleTypes.END_ROD,
                    pos.x, pos.y, pos.z,
                    1,
                    0, 0, 0,
                    0
            );

            // エンティティ感電
            for (LivingEntity entity : level.getEntitiesOfClass(
                    LivingEntity.class,
                    new AABB(pos, pos).inflate(0.6),
                    e -> e.isAlive()
            )) {
                ElectricElementDamageHandler.apply(
                        entity,
                        damage,
                        ElementType.ELECTRIC
                );
            }

            // ブロック通電
            BlockPos blockPos = new BlockPos(pos);
            if (visitedBlocks.add(blockPos)) {
                if (ElectricConductBlock.isConductive(level.getBlockState(blockPos).getBlock())) {
                    ElectricConductBlock.conduct(
                            level.getBlockState(blockPos).getBlock(),
                            damage
                    );
                }
            }
        }
    }

    /* ===============================
     * ランダム方向ベクトル
     * =============================== */

    private static Vec3 randomUnitVector() {
        double theta = RANDOM.nextDouble() * Math.PI * 2;
        double phi = RANDOM.nextDouble() * Math.PI;

        double x = Math.sin(phi) * Math.cos(theta);
        double y = Math.cos(phi);
        double z = Math.sin(phi) * Math.sin(theta);

        return new Vec3(x, y, z);
    }
}

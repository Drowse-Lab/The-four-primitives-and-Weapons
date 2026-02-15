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
 * 放射型 ELECTRIC 放電バースト（最終版）
 */
public final class ElectricDischargeBurstSkill {

    private static final Random RANDOM = new Random();

    private static final int BRANCH_COUNT = 16; // 本数
    private static final int MAX_STEP = 22;     // 距離
    private static final double DAMAGE = 6.0;  // ダメージ

    private ElectricDischargeBurstSkill() {}

    public static void fire(Player player) {
        if (player.level.isClientSide()) return;

        ServerLevel level = (ServerLevel) player.level();
        Vec3 origin = player.position().add(0, 1.0, 0);

        // 多段ヒット防止
        Set<Integer> damagedEntities = new HashSet<>();

        for (int i = 0; i < BRANCH_COUNT; i++) {
            Vec3 direction = randomUnitVector();
            dischargeBranch(level, origin, direction, damagedEntities);
        }
    }

    private static void dischargeBranch(
            ServerLevel level,
            Vec3 start,
            Vec3 direction,
            Set<Integer> damagedEntities
    ) {
        Vec3 pos = start;

        for (int step = 0; step < MAX_STEP; step++) {

            // 雷っぽい揺らぎ
            direction = direction.add(
                    (RANDOM.nextDouble() - 0.5) * 0.3,
                    (RANDOM.nextDouble() - 0.5) * 0.3,
                    (RANDOM.nextDouble() - 0.5) * 0.3
            ).normalize();

            pos = pos.add(direction.scale(0.6));

            // パーティクル間引き
            if ((step & 1) == 0) {
                level.sendParticles(
                        ParticleTypes.END_ROD,
                        pos.x, pos.y, pos.z,
                        1, 0, 0, 0, 0
                );
            }

            // エンティティ感電（1回のみ）
            for (LivingEntity entity : level.getEntitiesOfClass(
                    LivingEntity.class,
                    new AABB(pos, pos).inflate(0.6),
                    LivingEntity::isAlive
            )) {
                if (damagedEntities.add(entity.getId())) {
                    ElectricElementDamageHandler.apply(
                            entity,
                            DAMAGE,
                            ElementType.ELECTRIC
                    );
                }
            }

            // ブロック通電（半径制限付き）
            BlockPos blockPos = new BlockPos(pos);
            ElectricConductBlock.conduct(level, blockPos, DAMAGE);
        }
    }

    private static Vec3 randomUnitVector() {
        double theta = RANDOM.nextDouble() * Math.PI * 2;
        double phi = RANDOM.nextDouble() * Math.PI;

        return new Vec3(
                Math.sin(phi) * Math.cos(theta),
                Math.cos(phi),
                Math.sin(phi) * Math.sin(theta)
        );
    }
}

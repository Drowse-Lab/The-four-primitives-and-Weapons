package the_four_primitives_and_weapons.skill;

import the_four_primitives_and_weapons.block.ElectricConductBlock;
import the_four_primitives_and_weapons.damage.ElectricElementDamageHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * ELECTRIC ビームスキル（瞬間到達）
 */
public final class ElectricBeamSkill {

    private ElectricBeamSkill() {}

    private static final double MAX_DISTANCE = 50.0;
    private static final float DAMAGE = 8.0f;

    public static void fire(Player player) {
        if (player.level().isClientSide()) return;

        ServerLevel level = (ServerLevel) player.level();
        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getLookAngle().normalize();
        Vec3 end = start.add(direction.scale(MAX_DISTANCE));

        // ブロックへのレイトレース
        BlockHitResult blockHit = level.clip(new ClipContext(
                start, end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        ));

        Vec3 hitPos = (blockHit.getType() != HitResult.Type.MISS)
                ? blockHit.getLocation()
                : end;

        // ビーム経路上のエンティティをチェック
        AABB beamBox = new AABB(start, hitPos).inflate(0.3);
        List<LivingEntity> entities = level.getEntitiesOfClass(
                LivingEntity.class, beamBox,
                e -> e != player && e.isAlive()
        );

        LivingEntity closestTarget = null;
        double closestDist = Double.MAX_VALUE;

        for (LivingEntity entity : entities) {
            // エンティティがビームの経路上にあるか簡易チェック
            Vec3 toEntity = entity.position().add(0, entity.getBbHeight() / 2, 0).subtract(start);
            double proj = toEntity.dot(direction);
            if (proj < 0 || proj > start.distanceTo(hitPos)) continue;

            Vec3 closest = start.add(direction.scale(proj));
            double dist = closest.distanceTo(entity.position().add(0, entity.getBbHeight() / 2, 0));
            if (dist < 1.0 && proj < closestDist) {
                closestDist = proj;
                closestTarget = entity;
            }
        }

        // 終点を調整（エンティティの方がブロックより近い場合）
        if (closestTarget != null) {
            hitPos = closestTarget.position().add(0, closestTarget.getBbHeight() / 2, 0);
        }

        // パーティクルでビームを描画
        renderBeamParticles(level, start, hitPos);

        // エンティティ命中
        if (closestTarget != null) {
            ElectricElementDamageHandler.applyElectricDamage(
                    closestTarget, DAMAGE, player, 1
            );
        }

        // ブロック命中 → 通電
        if (blockHit.getType() != HitResult.Type.MISS && closestTarget == null) {
            BlockPos blockPos = blockHit.getBlockPos();
            ElectricConductBlock.conduct(level, blockPos, DAMAGE);
        }
    }

    private static void renderBeamParticles(ServerLevel level, Vec3 start, Vec3 end) {
        Vec3 diff = end.subtract(start);
        double length = diff.length();
        Vec3 step = diff.normalize().scale(0.5);
        Vec3 pos = start;

        for (double d = 0; d < length; d += 0.5) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    pos.x, pos.y, pos.z,
                    2, 0.05, 0.05, 0.05, 0.01);
            pos = pos.add(step);
        }
    }
}

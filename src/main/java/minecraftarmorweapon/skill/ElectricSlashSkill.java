package minecraftarmorweapon.skill;

import minecraftarmorweapon.block.ElectricConductBlock;
import minecraftarmorweapon.damage.ElectricElementDamageHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/**
 * ELECTRIC 斬撃波スキル
 * プレイヤーの正面に扇形の電撃斬撃波を放つ
 */
public final class ElectricSlashSkill {

    private ElectricSlashSkill() {}

    private static final double RANGE = 8.0;
    private static final float DAMAGE = 10.0f;
    private static final double HALF_ANGLE = Math.toRadians(45);

    public static void fire(Player player) {
        if (player.level().isClientSide()) return;

        ServerLevel level = (ServerLevel) player.level();
        Vec3 origin = player.position().add(0, 1.0, 0);
        Vec3 look = player.getLookAngle().normalize();
        Vec3 lookHorizontal = new Vec3(look.x, 0, look.z).normalize();

        Set<Integer> damagedEntities = new HashSet<>();

        // 扇形の斬撃波パーティクル描画
        renderSlashWave(level, origin, look, lookHorizontal);

        // 扇形範囲内のエンティティにダメージ
        AABB searchBox = new AABB(origin, origin).inflate(RANGE);
        for (LivingEntity entity : level.getEntitiesOfClass(
                LivingEntity.class, searchBox,
                e -> e != player && e.isAlive()
        )) {
            Vec3 toEntity = entity.position().add(0, entity.getBbHeight() / 2, 0).subtract(origin);
            double dist = toEntity.length();
            if (dist > RANGE || dist < 0.5) continue;

            // 扇形の角度チェック
            Vec3 toEntityNorm = toEntity.normalize();
            double dot = lookHorizontal.x * toEntityNorm.x + lookHorizontal.z * toEntityNorm.z;
            double angle = Math.acos(Math.min(1.0, Math.max(-1.0, dot)));
            if (angle > HALF_ANGLE) continue;

            // 高さチェック（上下3ブロック以内）
            if (Math.abs(toEntity.y) > 3.0) continue;

            if (damagedEntities.add(entity.getId())) {
                ElectricElementDamageHandler.applyElectricDamage(
                        entity, DAMAGE, player, 2
                );
            }
        }

        // 斬撃波の先端付近のブロック通電
        for (double d = RANGE - 1; d <= RANGE; d += 0.5) {
            Vec3 pos = origin.add(look.scale(d));
            BlockPos blockPos = new BlockPos((int) pos.x, (int) pos.y, (int) pos.z);
            ElectricConductBlock.conduct(level, blockPos, DAMAGE);
        }
    }

    private static void renderSlashWave(ServerLevel level, Vec3 origin, Vec3 look, Vec3 lookH) {
        // 斬撃波の弧を描く
        double perpX = -lookH.z;
        double perpZ = lookH.x;

        for (double dist = 1.0; dist <= RANGE; dist += 0.4) {
            int arcSteps = (int) (8 * (dist / RANGE));
            for (int i = -arcSteps; i <= arcSteps; i++) {
                double t = (double) i / arcSteps;
                double spreadAngle = t * HALF_ANGLE;

                double dx = lookH.x * Math.cos(spreadAngle) - perpX * Math.sin(spreadAngle);
                double dz = lookH.z * Math.cos(spreadAngle) - perpZ * Math.sin(spreadAngle);

                double x = origin.x + dx * dist;
                double y = origin.y + look.y * dist * 0.3;
                double z = origin.z + dz * dist;

                // 外側は密に、内側は疎に
                if (dist > RANGE * 0.6) {
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            x, y, z, 2, 0.05, 0.1, 0.05, 0.02);
                }
                level.sendParticles(ParticleTypes.END_ROD,
                        x, y, z, 1, 0.02, 0.05, 0.02, 0.005);
            }
        }
    }
}

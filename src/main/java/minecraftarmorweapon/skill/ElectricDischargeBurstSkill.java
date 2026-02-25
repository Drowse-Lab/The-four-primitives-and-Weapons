package minecraftarmorweapon.skill;

import minecraftarmorweapon.block.ElectricConductBlock;
import minecraftarmorweapon.damage.ElectricElementDamageHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * 前方集中型 ELECTRIC 放電バースト
 * プレイヤーの前方に複数の稲妻を放射する（ショットガン的）
 */
public final class ElectricDischargeBurstSkill {

    private static final Random RANDOM = new Random();

    private static final int BRANCH_COUNT = 12;   // 稲妻の本数
    private static final int MAX_STEP = 35;        // 各稲妻のステップ数
    private static final double STEP_LENGTH = 0.7; // 1ステップの距離
    private static final float DAMAGE = 10.0f;     // ダメージ
    private static final double SPREAD_ANGLE = Math.toRadians(25); // 前方への広がり角度

    private ElectricDischargeBurstSkill() {}

    public static void fire(Player player) {
        if (player.level().isClientSide()) return;

        ServerLevel level = (ServerLevel) player.level();
        Vec3 origin = player.position().add(0, 1.0, 0);
        Vec3 look = player.getLookAngle().normalize();

        // 右ベクトルと上ベクトルを計算
        Vec3 right;
        Vec3 up;
        if (Math.abs(look.y) > 0.99) {
            float yaw = player.getYRot() * 0.017453292F;
            right = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
            up = look.cross(right).normalize();
        } else {
            right = new Vec3(-look.z, 0, look.x).normalize();
            up = look.cross(right).normalize();
        }

        // 多段ヒット防止
        Set<Integer> damagedEntities = new HashSet<>();

        for (int i = 0; i < BRANCH_COUNT; i++) {
            // 前方円錐内のランダム方向を生成
            Vec3 direction = randomConeDirection(look, right, up);
            dischargeBranch(level, origin, direction, damagedEntities, player);
        }

        // サウンド
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 1.5f, 1.2f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 0.8f, 1.5f);

        // 発動時の足元エフェクト
        for (int i = 0; i < 360; i += 30) {
            double rad = Math.toRadians(i);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    origin.x + Math.cos(rad) * 1.2,
                    origin.y - 0.5,
                    origin.z + Math.sin(rad) * 1.2,
                    3, 0.05, 0.1, 0.05, 0.02);
        }
    }

    private static void dischargeBranch(
            ServerLevel level,
            Vec3 start,
            Vec3 direction,
            Set<Integer> damagedEntities,
            Player player
    ) {
        Vec3 pos = start;

        for (int step = 0; step < MAX_STEP; step++) {

            // 雷っぽい揺らぎ（前方向を維持しつつジグザグ）
            direction = direction.add(
                    (RANDOM.nextDouble() - 0.5) * 0.25,
                    (RANDOM.nextDouble() - 0.5) * 0.15,
                    (RANDOM.nextDouble() - 0.5) * 0.25
            ).normalize();

            pos = pos.add(direction.scale(STEP_LENGTH));

            // パーティクル（メイン: ELECTRIC_SPARK、アクセント: END_ROD）
            if ((step & 1) == 0) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        pos.x, pos.y, pos.z,
                        2, 0.05, 0.05, 0.05, 0.01);
            }
            if (step % 3 == 0) {
                level.sendParticles(ParticleTypes.END_ROD,
                        pos.x, pos.y, pos.z,
                        1, 0, 0, 0, 0);
            }

            // エンティティ感電（1回のみ）
            for (LivingEntity entity : level.getEntitiesOfClass(
                    LivingEntity.class,
                    new AABB(pos, pos).inflate(0.8),
                    e -> e != player && e.isAlive()
            )) {
                if (damagedEntities.add(entity.getId())) {
                    ElectricElementDamageHandler.applyElectricDamage(
                            entity, DAMAGE, player, 2
                    );

                    // 命中エフェクト
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            entity.getX(),
                            entity.getY() + entity.getBbHeight() / 2,
                            entity.getZ(),
                            10, 0.3, 0.3, 0.3, 0.05);

                    // 命中音
                    level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                            SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.6f, 1.8f);
                }
            }

            // ブロック通電
            BlockPos blockPos = new BlockPos((int) pos.x, (int) pos.y, (int) pos.z);
            ElectricConductBlock.conduct(level, blockPos, DAMAGE);
        }
    }

    /**
     * 前方円錐内のランダム方向を生成
     */
    private static Vec3 randomConeDirection(Vec3 forward, Vec3 right, Vec3 up) {
        // 円錐内のランダム角度
        double angle = RANDOM.nextDouble() * SPREAD_ANGLE;
        double rotation = RANDOM.nextDouble() * Math.PI * 2;

        // 円錐内の方向を計算
        double sinAngle = Math.sin(angle);
        Vec3 offset = right.scale(sinAngle * Math.cos(rotation))
                .add(up.scale(sinAngle * Math.sin(rotation)));

        return forward.scale(Math.cos(angle)).add(offset).normalize();
    }
}

package the_four_primitives_and_weapons.procedures;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import the_four_primitives_and_weapons.skill.WeaponStatsRegistry.ThrustConfig;

import java.util.List;

/**
 * JSON ( weapon_stats の "thrust" ) 駆動の「突き連撃」チャージ攻撃。
 *
 * <p>短い前方への踏み込み＋前方コーン内の敵へ {@code hits} 回の多段ヒット。
 * {@code range} を小さくすると「奥行きの短い突き」になる ( ダガー向け )。
 * 既存の直刀突き ( {@link TyokutouThrustAttackProcedure} ) には触れない独立実装。</p>
 */
public final class JsonThrustProcedure {

    private JsonThrustProcedure() {}

    public static void execute(Player player, float chargePercent, ThrustConfig cfg) {
        if (player == null || cfg == null) return;
        Level world = player.level();
        if (world.isClientSide) return;

        Vec3 look = player.getLookAngle();
        Vec3 eye = player.position().add(0, player.getEyeHeight() * 0.6, 0);
        double range = cfg.range * (1.0 + chargePercent * 0.3); // チャージで少しだけ伸びる

        // 短い踏み込み ( 奥行きが短い突き )
        player.setDeltaMovement(player.getDeltaMovement().add(look.scale(cfg.dash)));
        player.hurtMarked = true;

        // 1ヒットのダメージ: JSON指定が無ければ武器の攻撃力
        float dmg = cfg.damage > 0f
                ? cfg.damage
                : (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);

        Vec3 origin = player.position();
        Vec3 end = origin.add(look.scale(range));
        AABB area = new AABB(origin, end).inflate(1.0, 1.0, 1.0);
        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != player && e.isAlive() && !e.isSpectator() && !e.isAlliedTo(player));

        for (LivingEntity target : targets) {
            Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(eye);
            if (to.length() > range + 1.0) continue;
            if (to.normalize().dot(look) < 0.4) continue; // 前方コーンのみ

            for (int h = 0; h < Math.max(1, cfg.hits); h++) {
                target.invulnerableTime = 0; // 多段ヒットを通す
                target.hurt(world.damageSources().playerAttack(player), dmg);
            }
            target.knockback((float) cfg.knockback, -look.x, -look.z);
        }

        // 演出
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.7f, 1.7f);
        if (world instanceof ServerLevel sl) {
            for (double d = 0.5; d <= range; d += 0.4) {
                Vec3 p = eye.add(look.scale(d));
                sl.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 2, 0.03, 0.03, 0.03, 0.0);
            }
        }
    }
}

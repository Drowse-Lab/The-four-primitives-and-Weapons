package the_four_primitives_and_weapons.procedures;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.joml.Vector3f;

import the_four_primitives_and_weapons.damage.SpecialDebuffHandler;
import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModItems;

import java.util.List;

/**
 * Rivers of Blood の右クリック特殊技 (Corpse Piler 風 ).
 *
 * Elden Ring 「血の貴族の暴行」 のミニマム化:
 *   - 右クリックで前方扇形に血斬撃を 1 発放つ
 *   - 命中した敵に基礎ダメージ + DoT bleed ( = SpecialDebuffHandler.applyWither )
 *   - 命中数 × 1.5 HP のライフスティール
 *   - 短いクールダウン ( 10 tick = 0.5 秒 ) → 連打で多段コンボ可能
 *   - 赤い dust の弧 + DAMAGE_INDICATOR で血しぶき演出
 */
public class KatanaBloodYoukuritukusitatokiProcedure {

    /** 斬撃の射程 */
    private static final double RANGE = 6.0;
    /** 扇形の半角 (deg) — 狭めの斬撃線 */
    private static final double CONE_HALF_DEG = 25.0;
    /** 基礎ダメージ ( 1 斬撃当たり ) */
    private static final float BASE_DAMAGE = 6.0f;
    /** 命中ごとの自己回復 (HP) */
    private static final float HEAL_PER_HIT = 1.5f;
    /** クールダウン — 連打コンボ可能な短め */
    private static final int COOLDOWN_TICKS = 10; // 0.5 sec

    public static void execute(Level world, Entity entity) {
        if (entity == null) return;
        if (!(entity instanceof Player player)) return;
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty() || held.getItem() != TheFourPrimitivesAndWeaponsModItems.RIVERS_OF_BLOOD.get()) return;

        // クールダウン中なら何もしない (連打可能だが超高速連射は防ぐ)
        if (player.getCooldowns().isOnCooldown(held.getItem())) return;
        player.getCooldowns().addCooldown(held.getItem(), COOLDOWN_TICKS);

        Vec3 eyePos  = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle().normalize();
        double cosHalfAngle = Math.cos(Math.toRadians(CONE_HALF_DEG));

        // 前方扇形内の敵をピックアップ
        AABB searchBox = new AABB(eyePos, eyePos).inflate(RANGE + 1.5);
        List<LivingEntity> nearby = world.getEntitiesOfClass(LivingEntity.class, searchBox);
        int hits = 0;
        for (LivingEntity le : nearby) {
            if (le == player) continue;
            if (!le.isAlive()) continue;
            Vec3 toCenter = le.getBoundingBox().getCenter().subtract(eyePos);
            double dist = toCenter.length();
            if (dist > RANGE || dist < 0.3) continue;
            if (lookVec.dot(toCenter.normalize()) < cosHalfAngle) continue;

            // 直接ダメージ + bleed DoT
            le.invulnerableTime = 0;
            le.hurt(le.damageSources().playerAttack(player), BASE_DAMAGE);
            SpecialDebuffHandler.applyWither(le, 60, 0.5f); // 3 秒 / 0.5 ダメージ/tick
            SpecialDebuffHandler.applyWeakness(le, 100, 1);  // 5 秒 / -0.5 atk

            // 命中位置に血しぶき
            if (world instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                        le.getX(), le.getY() + le.getBbHeight() / 2, le.getZ(),
                        15, 0.3, 0.4, 0.3, 0.15);
            }
            hits++;
        }

        // 命中数に応じてライフスティール
        if (hits > 0) {
            player.heal(HEAL_PER_HIT * hits);
        }

        // 斬撃エフェクト — 前方に赤い弧 ( blood arc )
        if (world instanceof ServerLevel sl) {
            DustParticleOptions bloodRed = new DustParticleOptions(
                    new Vector3f(0.65f, 0.05f, 0.05f), 1.6f);
            DustParticleOptions bloodCrimson = new DustParticleOptions(
                    new Vector3f(0.45f, 0.0f, 0.0f), 1.2f);
            // look 方向と直交する right ベクトル
            Vec3 right = lookVec.cross(new Vec3(0, 1, 0));
            if (right.lengthSqr() < 1.0E-4) right = new Vec3(1, 0, 0);
            right = right.normalize();
            // 弧を 5 段に分けて、 各段で width を広く取る
            for (double d = 0.5; d <= RANGE; d += 0.35) {
                double spread = d * Math.tan(Math.toRadians(CONE_HALF_DEG));
                // 横方向に -spread .. +spread 5 段
                for (int i = -2; i <= 2; i++) {
                    double ratio = i / 2.0;
                    Vec3 offset = right.scale(spread * ratio);
                    Vec3 p = eyePos.add(lookVec.scale(d)).add(offset);
                    // 弧形 — 中央ほど低く、 端ほど高くなるような Y 補正
                    double yArc = 0.1 * (1 - Math.abs(ratio));
                    sl.sendParticles(bloodRed,
                            p.x, p.y + yArc, p.z,
                            1, 0.04, 0.04, 0.04, 0.0);
                    if (i == 0) {
                        // 中央ライン強調
                        sl.sendParticles(bloodCrimson,
                                p.x, p.y + yArc, p.z,
                                1, 0.02, 0.02, 0.02, 0.0);
                    }
                }
            }
            // 振り始めの足元に血しぶき
            sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                    eyePos.x, eyePos.y - 1.0, eyePos.z,
                    8, 0.3, 0.1, 0.3, 0.05);
        }

        // 振り音 + 血の音
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.2f);
        if (hits > 0) {
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.GENERIC_HURT, SoundSource.PLAYERS, 0.7f, 1.4f);
        }
    }

    /** 旧シグネチャ互換 (引数なし版). 何もしない. */
    public static void execute() {
        // empty - kept for backward call compatibility
    }
}

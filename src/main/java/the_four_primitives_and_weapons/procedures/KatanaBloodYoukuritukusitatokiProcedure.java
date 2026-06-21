package the_four_primitives_and_weapons.procedures;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModItems;

/**
 * Rivers of Blood の特殊技 (右クリック発動).
 *
 * 「血の咆哮」:
 *   - 周囲 6 ブロック圏内の全 LivingEntity に magic ダメージ + Wither + Weakness
 *   - 命中数 × 2 HP をプレイヤーに吸収回復
 *   - 8 秒のクールダウン (item cooldown)
 *   - 血のパーティクルと soul sound で視覚/聴覚演出
 */
public class KatanaBloodYoukuritukusitatokiProcedure {

    /** 範囲半径 (ブロック) */
    private static final double RADIUS = 6.0;
    /** 基礎ダメージ */
    private static final float BASE_DAMAGE = 8.0f;
    /** 命中ごとの自己回復 (HP) */
    private static final float HEAL_PER_HIT = 2.0f;
    /** クールダウン (tick) */
    private static final int COOLDOWN_TICKS = 160; // 8 sec

    public static void execute(Level world, Entity entity) {
        if (entity == null) return;
        if (!(entity instanceof Player player)) return;
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty() || held.getItem() != TheFourPrimitivesAndWeaponsModItems.RIVERS_OF_BLOOD.get()) return;

        // クールダウン中なら何もしない
        if (player.getCooldowns().isOnCooldown(held.getItem())) return;
        player.getCooldowns().addCooldown(held.getItem(), COOLDOWN_TICKS);

        // 範囲内の敵を取得
        Vec3 pos = player.position();
        AABB box = new AABB(
                pos.x - RADIUS, pos.y - 2, pos.z - RADIUS,
                pos.x + RADIUS, pos.y + 4, pos.z + RADIUS);
        int hits = 0;
        for (LivingEntity le : world.getEntitiesOfClass(LivingEntity.class, box)) {
            if (le == player) continue;
            if (le.distanceToSqr(pos) > RADIUS * RADIUS) continue;
            // 自分のペット/騎乗物は除外したい場合: チームチェックを追加可能
            le.hurt(le.damageSources().magic(), BASE_DAMAGE);
            le.addEffect(new MobEffectInstance(MobEffects.WITHER, 120, 1, false, true));
            le.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1, false, true));
            hits++;
        }

        // 命中数に応じて自己回復
        if (hits > 0) {
            player.heal(HEAL_PER_HIT * hits);
        }

        // 演出
        if (world instanceof ServerLevel server) {
            // 血のパーティクル (発動時の中心)
            for (int i = 0; i < 40; i++) {
                double a = Math.random() * Math.PI * 2;
                double r = Math.random() * RADIUS;
                double dx = Math.cos(a) * r;
                double dz = Math.sin(a) * r;
                server.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                        pos.x + dx, pos.y + 1.2, pos.z + dz, 1, 0, 0, 0, 0.1);
            }
            // 暗黒オーラ
            server.sendParticles(ParticleTypes.SOUL,
                    pos.x, pos.y + 1, pos.z, 25, RADIUS * 0.5, 1.0, RADIUS * 0.5, 0.05);
        }
        // サウンド
        world.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.WITHER_HURT, SoundSource.PLAYERS, 0.8f, 0.7f);
        world.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.6f, 0.8f);
    }

    /** 旧シグネチャ互換 (引数なし版). 何もしない. RiversOfBloodItem.use() で新シグネチャ呼び出しに置き換え推奨。 */
    public static void execute() {
        // empty - kept for backward call compatibility
    }
}

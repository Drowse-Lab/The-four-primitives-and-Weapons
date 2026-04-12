package minecraftarmorweapon.ai.lisp;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AIレベルに比例した確率で攻撃を回避するハンドラ。
 *
 * 回避率の基準曲線:
 *   f(lvl) = 1 - exp(-lvl / K)  ただし K = 500 / -ln(1 - 0.95) ≈ 166.91
 *   → Lv 500 で 95% になるよう調整
 *
 * 参考値 (連続関数):
 *   Lv   1 :  0.6%
 *   Lv  10 :  5.8%
 *   Lv  50 : 25.9%
 *   Lv 100 : 45.1%
 *   Lv 200 : 69.9%
 *   Lv 500 : 95.0%  ← 基準点
 *   Lv1000 : 99.75%
 *
 * 仕組み:
 *  - ダメージを受ける瞬間にイベントをキャンセル
 *  - レベルに応じた距離だけ横ステップ（小: 1〜最大3ブロック）
 *  - 1回回避したら 3 秒間は連続回避不可
 *  - /kill 等の無敵貫通ダメージは回避不可
 *  - 回避率が 1% 未満のMobは発動自体しない（処理省略）
 */
@Mod.EventBusSubscriber(modid = "minecraft_armor_weapon")
public class AngelTierDodgeHandler {

    /** Lv 500 で 95% になるよう決定したスケール定数: K = -500 / ln(0.05) */
    private static final double LEVEL_SCALE = 166.9104625613108;

    /** 最小発動AIレベル（これ未満は常に回避しない） */
    private static final int MIN_LEVEL = 3;
    /** 回避率がこの値未満なら発動しない（計算/エフェクトのノイズ削減） */
    private static final float MIN_DODGE_RATE = 0.01f;

    /** 連続回避防止クールダウン (tick): 3秒 */
    private static final long DODGE_COOLDOWN_TICKS = 60L;

    private static final Map<UUID, Long> lastDodgeTick = new ConcurrentHashMap<>();

    /** AIレベルから回避率 (0.0〜0.95) を計算。Lv 500 で 0.95 に到達。 */
    public static float dodgeRateForLevel(int aiLevel) {
        if (aiLevel < MIN_LEVEL) return 0f;
        double raw = 1.0 - Math.exp(-aiLevel / LEVEL_SCALE);
        // 上限を 0.95 にキャップ（どれだけLvが上がっても完全回避はしない）
        return (float) Math.min(0.95, raw);
    }

    /**
     * ディメンションキャップと進行度ボーナスを適用した最終回避率を計算。
     *   overworld: 最大 20%, nether: 最大 25%, end: 最大 30%
     * 実効Lv = mobのAILv + 進行度ボーナス (オーバーワールドは半減)
     */
    public static float dodgeRateForMob(Mob mob) {
        int aiLevel = MobAILevelHandler.getAILevel(mob);
        if (aiLevel < MIN_LEVEL) return 0f;
        float bonus = ProgressionTracker.getEffectiveBonus(mob.level());
        float rawRate = dodgeRateForLevel(Math.round(aiLevel + bonus));
        float cap = ProgressionTracker.getDodgeCapForDimension(mob.level());
        return Math.min(rawRate, cap);
    }

    /** AIレベルから横跳び距離 (1.0〜3.0) を計算 */
    private static double stepDistanceForLevel(int aiLevel) {
        // Lv  3 : 1.0
        // Lv 100: ~1.8
        // Lv 500: 3.0 (上限)
        return 1.0 + Math.min(2.0, aiLevel / 250.0);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (mob.level().isClientSide) return;

        // 無敵貫通ダメージは回避しない
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;

        int aiLevel = MobAILevelHandler.getAILevel(mob);
        if (aiLevel < MIN_LEVEL) return;

        // 進行度ボーナス + ディメンションキャップを適用した最終回避率
        float dodgeRate = dodgeRateForMob(mob);
        if (dodgeRate < MIN_DODGE_RATE) return;

        // クールダウン: 直近3秒以内に回避したばかりなら発動不可
        long now = mob.level().getGameTime();
        Long last = lastDodgeTick.get(mob.getUUID());
        if (last != null && now - last < DODGE_COOLDOWN_TICKS) return;

        if (mob.getRandom().nextFloat() >= dodgeRate) return;

        // === 回避成功 ===
        event.setCanceled(true);
        lastDodgeTick.put(mob.getUUID(), now);

        LivingEntity attacker = null;
        if (event.getSource().getEntity() instanceof LivingEntity le) {
            attacker = le;
        }

        Vec3 dodgeDir;
        if (attacker != null) {
            Vec3 attackDir = mob.position().subtract(attacker.position()).normalize();
            dodgeDir = mob.getRandom().nextBoolean()
                ? new Vec3(-attackDir.z, 0, attackDir.x)
                : new Vec3(attackDir.z, 0, -attackDir.x);
        } else {
            double angle = mob.getRandom().nextDouble() * Math.PI * 2;
            dodgeDir = new Vec3(Math.cos(angle), 0, Math.sin(angle));
        }

        double stepDist = stepDistanceForLevel(aiLevel);
        double newX = mob.getX() + dodgeDir.x * stepDist;
        double newZ = mob.getZ() + dodgeDir.z * stepDist;
        double newY = mob.getY();

        if (mob.level() instanceof ServerLevel serverLevel) {
            // 高Lvほど派手に
            int particleCount = 3 + Math.min(12, aiLevel / 50);
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                mob.getX(), mob.getY() + 0.5, mob.getZ(),
                particleCount, 0.2, 0.3, 0.2, 0.03);
        }

        mob.teleportTo(newX, newY, newZ);

        mob.level().playSound(null, mob.blockPosition(),
            SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE,
            0.4f, 1.3f + mob.getRandom().nextFloat() * 0.3f);

        if (attacker != null) {
            mob.setTarget(attacker);
            mob.getLookControl().setLookAt(attacker, 180f, 30f);
        }
    }
}

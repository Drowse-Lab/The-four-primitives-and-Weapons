package minecraftarmorweapon.ai.lisp;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 天使級(ティア6)以上のMobが攻撃を自動回避するハンドラ。
 *
 * 仕組み:
 *  - ダメージを受ける瞬間にイベントをキャンセル
 *  - 横にテレポートして残像パーティクルを出す
 *  - AIレベルが高いほど回避率が上がる（Lv8以上は99%）
 *  - 移動速度も常時ブースト
 */
@Mod.EventBusSubscriber(modid = "minecraft_armor_weapon")
public class AngelTierDodgeHandler {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (mob.level().isClientSide) return;

        int aiLevel = MobAILevelHandler.getAILevel(mob);
        if (aiLevel < 8) return; // Lv8以上のみ自動回避

        // 回避率: Lv8=90%, Lv9=95%, Lv10=99%
        float dodgeRate = 0.80f + aiLevel * 0.02f;
        dodgeRate = Math.min(dodgeRate, 0.99f);

        if (mob.getRandom().nextFloat() >= dodgeRate) return; // 回避失敗

        // === 回避成功 → ダメージキャンセル ===
        event.setCanceled(true);

        // 横にテレポート（攻撃者の攻撃方向に対して直角）
        LivingEntity attacker = null;
        if (event.getSource().getEntity() instanceof LivingEntity le) {
            attacker = le;
        }

        Vec3 dodgeDir;
        if (attacker != null) {
            // 攻撃者からの方向に対して直角に移動
            Vec3 attackDir = mob.position().subtract(attacker.position()).normalize();
            // 左右ランダム
            if (mob.getRandom().nextBoolean()) {
                dodgeDir = new Vec3(-attackDir.z, 0, attackDir.x); // 左
            } else {
                dodgeDir = new Vec3(attackDir.z, 0, -attackDir.x); // 右
            }
        } else {
            // 攻撃者不明 → ランダム方向
            double angle = mob.getRandom().nextDouble() * Math.PI * 2;
            dodgeDir = new Vec3(Math.cos(angle), 0, Math.sin(angle));
        }

        // テレポート距離（Lvが高いほど遠い）
        double teleportDist = 2.0 + (aiLevel - 8) * 1.5; // Lv8=2, Lv9=3.5, Lv10=5

        double newX = mob.getX() + dodgeDir.x * teleportDist;
        double newZ = mob.getZ() + dodgeDir.z * teleportDist;
        double newY = mob.getY();

        // 元の位置に残像パーティクル
        if (mob.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                mob.getX(), mob.getY() + 0.5, mob.getZ(),
                8, 0.3, 0.5, 0.3, 0.05);

            // テレポート先にもパーティクル
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                newX, newY + 0.5, newZ,
                5, 0.2, 0.4, 0.2, 0.02);
        }

        // テレポート
        mob.teleportTo(newX, newY, newZ);

        // 効果音
        mob.level().playSound(null, mob.blockPosition(),
            SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE,
            0.5f, 1.5f + mob.getRandom().nextFloat() * 0.5f);

        // 攻撃者にターゲットを向ける
        if (attacker != null) {
            mob.setTarget(attacker);
            mob.getLookControl().setLookAt(attacker, 180f, 30f);
        }
    }
}

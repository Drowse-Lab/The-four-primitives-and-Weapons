package minecraftarmorweapon.procedures;

import net.minecraft.world.level().LevelAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level().ServerLevel;
import net.minecraft.core.particles.ParticleTypes;

import minecraftarmorweapon.init.MinecraftArmorWeaponModItems;

import java.util.List;
import java.util.Comparator;

public class SwordOfNightChargingGlowProcedure {
    
    /**
     * Sword of Nightのチャージ中に視線上のエンティティを発光させる
     * @param world ワールド
     * @param x プレイヤーのX座標
     * @param y プレイヤーのY座標
     * @param z プレイヤーのZ座標
     * @param entity プレイヤーエンティティ
     * @param isCharging チャージ中かどうか
     */
    public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, boolean isCharging) {
        if (entity == null || world == null) return;
        if (!(entity instanceof Player player)) return;
        
        // Sword of Nightを持っているかチェック
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.getItem() != MinecraftArmorWeaponModItems.SWORD_OF_NIGHT.get()) {
            return;
        }
        
        if (isCharging) {
            // チャージ中：視線上のエンティティを検索して発光させる
            double range = 20; // 検索範囲
            Vec3 lookVec = entity.getLookAngle();
            Vec3 eyePos = entity.getEyePosition();
            
            // レイキャストで視線上のエンティティを検索
            for (double r = 1; r <= range; r += 0.5) {
                Vec3 checkPos = eyePos.add(lookVec.scale(r));
                
                // その位置周辺のエンティティを検索
                List<LivingEntity> nearbyEntities = world.getEntitiesOfClass(
                    LivingEntity.class,
                    AABB.ofSize(checkPos, 1.5, 1.5, 1.5),
                    e -> e != entity && e.isAlive()
                );
                
                if (!nearbyEntities.isEmpty()) {
                    // 最も近いエンティティを取得
                    LivingEntity target = nearbyEntities.stream()
                        .min(Comparator.comparingDouble(e -> e.distanceToSqr(checkPos)))
                        .orElse(null);
                    
                    if (target != null) {
                        // 発光エフェクトを付与（短時間）
                        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 4, 0, true, false));
                        
                        // チャージ中であることを示すタグを設定
                        target.getPersistentData().putBoolean("SwordOfNightTarget", true);
                        target.getPersistentData().putInt("SwordOfNightGlowTicks", 4);
                        
                        // パーティクルエフェクト（サーバー側のみ）
                        if (world instanceof ServerLevel serverLevel) {
                            serverLevel.sendParticles(
                                ParticleTypes.END_ROD,
                                target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                                3, 0.2, 0.2, 0.2, 0.01
                            );
                        }
                        
                        break; // 最初に見つかったエンティティのみ発光させる
                    }
                }
            }
        } else {
            // チャージ解除：周囲のエンティティの発光タグをクリア
            List<LivingEntity> glowingEntities = world.getEntitiesOfClass(
                LivingEntity.class,
                AABB.ofSize(new Vec3(x, y, z), 50, 50, 50),
                e -> e.getPersistentData().getBoolean("SwordOfNightTarget")
            );
            
            for (LivingEntity target : glowingEntities) {
                target.getPersistentData().remove("SwordOfNightTarget");
                target.getPersistentData().remove("SwordOfNightGlowTicks");
            }
        }
    }
    
    /**
     * エンティティの発光時間を管理（Tickごとに呼ばれる）
     */
    public static void managGlowTicks(LivingEntity entity) {
        if (entity.getPersistentData().contains("SwordOfNightGlowTicks")) {
            int ticks = entity.getPersistentData().getInt("SwordOfNightGlowTicks");
            if (ticks > 0) {
                entity.getPersistentData().putInt("SwordOfNightGlowTicks", ticks - 1);
            } else {
                // タイムアウト：タグを削除
                entity.getPersistentData().remove("SwordOfNightTarget");
                entity.getPersistentData().remove("SwordOfNightGlowTicks");
            }
        }
    }
}
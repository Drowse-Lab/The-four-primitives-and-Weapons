package minecraftarmorweapon.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.List;

public class ChargedAttackProcedure {
    
    public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, float chargePercent) {
        if (entity == null || !(entity instanceof Player)) return;
        
        Player player = (Player) entity;
        
        // ダメージと範囲の計算
        float baseDamage = 10.0f;
        float damage = baseDamage * (1.0f + chargePercent * 2.5f); // 最大3.5倍
        float range = 4.0f + chargePercent * 3.0f; // 最大7メートル
        
        // チャージレベルに応じた攻撃パターン
        if (chargePercent < 0.33f) {
            // レベル1: 前方扇形攻撃
            performFrontalAttack(world, player, damage, range * 0.7f);
        } else if (chargePercent < 0.66f) {
            // レベル2: 360度回転斬り
            performSpinAttack(world, player, damage, range * 0.85f);
        } else {
            // レベル3: 大技
            performUltimateAttack(world, player, damage, range);
        }
    }
    
    private static void performFrontalAttack(LevelAccessor world, Player player, float damage, float range) {
        Vec3 lookVec = player.getLookAngle();
        Vec3 playerPos = player.position();
        
        // 前方扇形範囲
        double angle = 60; // 60度の扇形
        
        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class,
            new AABB(playerPos.add(-range, -1, -range), playerPos.add(range, 2, range)),
            entity -> {
                if (entity == player) return false;
                Vec3 toEntity = entity.position().subtract(playerPos).normalize();
                double dot = lookVec.dot(toEntity);
                return dot > Math.cos(Math.toRadians(angle / 2)) && entity.distanceTo(player) <= range;
            }
        );
        
        // エフェクト
        if (world instanceof ServerLevel serverWorld) {
            for (int i = -30; i <= 30; i += 10) {
                double rad = Math.toRadians(player.getYRot() + i);
                for (double d = 0.5; d <= range; d += 0.5) {
                    serverWorld.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        playerPos.x - Math.sin(rad) * d,
                        playerPos.y + 1,
                        playerPos.z + Math.cos(rad) * d,
                        1, 0, 0, 0, 0);
                }
            }
        }
        
        // ダメージ処理
        for (LivingEntity target : targets) {
            target.hurt(DamageSource.playerAttack(player), damage);
            Vec3 knockback = target.position().subtract(playerPos).normalize().scale(0.5);
            target.setDeltaMovement(target.getDeltaMovement().add(knockback.x, 0.2, knockback.z));
        }
        
        if (world instanceof Level level) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.0f);
        }
    }
    
    private static void performSpinAttack(LevelAccessor world, Player player, float damage, float range) {
        Vec3 playerPos = player.position();
        
        // 360度範囲攻撃
        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class,
            new AABB(playerPos.add(-range, -1, -range), playerPos.add(range, 2, range)),
            entity -> entity != player && entity.distanceTo(player) <= range
        );
        
        // 回転エフェクト
        if (world instanceof ServerLevel serverWorld) {
            for (int i = 0; i < 360; i += 20) {
                double rad = Math.toRadians(i);
                for (double r = 1; r <= range; r += 1) {
                    serverWorld.sendParticles(ParticleTypes.CRIT,
                        playerPos.x + Math.cos(rad) * r,
                        playerPos.y + 1,
                        playerPos.z + Math.sin(rad) * r,
                        1, 0, 0.1, 0, 0);
                }
            }
            
            // 回転する刃のエフェクト
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 / 8) * i;
                serverWorld.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    playerPos.x + Math.cos(angle) * range * 0.7,
                    playerPos.y + 1,
                    playerPos.z + Math.sin(angle) * range * 0.7,
                    1, 0, 0, 0, 0);
            }
        }
        
        // ダメージとノックバック
        for (LivingEntity target : targets) {
            target.hurt(DamageSource.playerAttack(player), damage);
            Vec3 knockback = target.position().subtract(playerPos).normalize().scale(0.8);
            target.setDeltaMovement(target.getDeltaMovement().add(knockback.x, 0.3, knockback.z));
        }
        
        // プレイヤーの回転
        player.setYRot(player.getYRot() + 360);
        
        if (world instanceof Level level) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2f, 0.8f);
        }
    }
    
    private static void performUltimateAttack(LevelAccessor world, Player player, float damage, float range) {
        Vec3 playerPos = player.position();
        Vec3 lookVec = player.getLookAngle();
        
        // 前方直線と周囲の複合攻撃
        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class,
            new AABB(playerPos.add(-range, -2, -range), playerPos.add(range, 3, range)),
            entity -> entity != player && entity.distanceTo(player) <= range
        );
        
        // 壮大なエフェクト
        if (world instanceof ServerLevel serverWorld) {
            // 地面の衝撃波
            for (int ring = 1; ring <= 3; ring++) {
                final int currentRing = ring;
                for (int i = 0; i < 360; i += 10) {
                    double rad = Math.toRadians(i);
                    double r = range * (currentRing / 3.0);
                    serverWorld.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        playerPos.x + Math.cos(rad) * r,
                        playerPos.y + 0.1,
                        playerPos.z + Math.sin(rad) * r,
                        2, 0, 0, 0, 0.05);
                }
            }
            
            // 縦の斬撃エフェクト
            for (double d = 0; d <= range * 1.5; d += 0.3) {
                serverWorld.sendParticles(ParticleTypes.ENCHANTED_HIT,
                    playerPos.x + lookVec.x * d,
                    playerPos.y + 1 + d * 0.1,
                    playerPos.z + lookVec.z * d,
                    3, 0.1, 0.5, 0.1, 0.02);
            }
            
            // 雷撃エフェクト
            serverWorld.sendParticles(ParticleTypes.END_ROD,
                playerPos.x, playerPos.y + 2, playerPos.z,
                50, range * 0.5, 1, range * 0.5, 0.1);
        }
        
        // 強力なダメージと追加効果
        for (LivingEntity target : targets) {
            float finalDamage = damage;
            
            // 前方の敵には追加ダメージ
            Vec3 toTarget = target.position().subtract(playerPos).normalize();
            if (lookVec.dot(toTarget) > 0.5) {
                finalDamage *= 1.5f;
                target.setSecondsOnFire(5);
            }
            
            target.hurt(DamageSource.playerAttack(player), finalDamage);
            
            // 強力なノックバック
            Vec3 knockback = target.position().subtract(playerPos).normalize().scale(1.5);
            target.setDeltaMovement(knockback.x, 0.5, knockback.z);
            
            // スタン効果（動けなくする）
            target.setDeltaMovement(0, target.getDeltaMovement().y, 0);
        }
        
        // サウンド
        if (world instanceof Level level) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.8f, 1.0f);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.5f, 0.6f);
        }
    }
    
    public static void executeDashAttack(LevelAccessor world, double x, double y, double z, Entity entity) {
        if (entity == null || !(entity instanceof Player)) return;
        
        Player player = (Player) entity;
        Vec3 lookVec = player.getLookAngle();
        Vec3 playerPos = player.position();
        
        // ダッシュ移動
        player.setDeltaMovement(player.getDeltaMovement().add(lookVec.scale(2.0)));
        
        // 前方直線攻撃
        double distance = 6.0;
        Vec3 endPos = playerPos.add(lookVec.scale(distance));
        AABB searchArea = new AABB(playerPos, endPos).inflate(1.0);
        
        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, searchArea,
            entity2 -> {
                if (entity2 == player) return false;
                Vec3 toEntity = entity2.position().subtract(playerPos);
                double dot = lookVec.dot(toEntity.normalize());
                double dist = toEntity.length();
                return dot > 0.7 && dist <= distance;
            }
        );
        
        // エフェクト
        if (world instanceof ServerLevel serverWorld) {
            for (double d = 0; d <= distance; d += 0.5) {
                serverWorld.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    playerPos.x + lookVec.x * d,
                    playerPos.y + 1,
                    playerPos.z + lookVec.z * d,
                    1, 0.1, 0.1, 0.1, 0);
                
                serverWorld.sendParticles(ParticleTypes.CLOUD,
                    playerPos.x + lookVec.x * d,
                    playerPos.y + 0.1,
                    playerPos.z + lookVec.z * d,
                    2, 0.2, 0, 0.2, 0.01);
            }
        }
        
        // ダメージ
        for (LivingEntity target : targets) {
            target.hurt(DamageSource.playerAttack(player), 15.0f);
            target.setDeltaMovement(lookVec.scale(1.2).add(0, 0.3, 0));
        }
        
        if (world instanceof Level level) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2f, 1.3f);
        }
    }
}
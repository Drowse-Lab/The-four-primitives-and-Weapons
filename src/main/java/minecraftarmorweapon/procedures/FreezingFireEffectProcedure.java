package minecraftarmorweapon.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.DustParticleOptions;

public class FreezingFireEffectProcedure {
    
    // シンプルな零度の炎エフェクト適用
    public static void execute(LevelAccessor world, Entity target, Entity attacker, int duration, int level) {
        if (target == null || attacker == null)
            return;
            
        if (target instanceof LivingEntity livingTarget) {
            // 移動速度低下
            livingTarget.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, level));
            
            // 凍結効果
            livingTarget.setTicksFrozen(livingTarget.getTicksFrozen() + 40);
            
            // ダメージ
            float damage = 2.0f + (level * 0.5f);
            livingTarget.hurt(DamageSource.MAGIC, damage);
            
            // パーティクル
            if (world instanceof ServerLevel _level) {
                double x = target.getX();
                double y = target.getY() + target.getBbHeight() / 2;
                double z = target.getZ();
                
                // 青い炎のパーティクル
                DustParticleOptions blueFlame = new DustParticleOptions(
                    new com.mojang.math.Vector3f(0.5f, 0.8f, 1.0f), 1.0f
                );
                _level.sendParticles(blueFlame, x, y, z, 10, 0.3, 0.5, 0.3, 0.02);
                _level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 5, 0.2, 0.3, 0.2, 0.01);
                _level.sendParticles(ParticleTypes.SNOWFLAKE, x, y, z, 8, 0.4, 0.6, 0.4, 0.03);
            }
        }
    }
}
package minecraftarmorweapon.mixin;

import minecraftarmorweapon.difficulty.CustomDifficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    
    // ダメージ量の調整
    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true, index = 2)
    private float modifyDamageAmount(float amount, DamageSource source) {
        if (!CustomDifficulty.isCustomDifficultyActive()) {
            return amount;
        }
        
        CustomDifficulty.DifficultySettings settings = CustomDifficulty.getCurrentSettings();
        if (settings == null) {
            return amount;
        }
        
        LivingEntity entity = (LivingEntity)(Object)this;
        
        // プレイヤーが受けるダメージ
        if (entity instanceof Player) {
            return amount * settings.damageMultiplier;
        }
        // モブが与えるダメージ（プレイヤーへの攻撃時）
        else if (entity instanceof Mob && source.getEntity() instanceof Player) {
            return amount;
        }
        // モブが受けるダメージ
        else if (!(entity instanceof Player) && source.getEntity() instanceof Player) {
            return amount / settings.mobHealthMultiplier;
        }
        
        return amount;
    }
    
    // 回復量の調整
    @Inject(method = "heal", at = @At("HEAD"), cancellable = true)
    private void modifyHealing(float healAmount, CallbackInfo ci) {
        if (!CustomDifficulty.isCustomDifficultyActive()) {
            return;
        }
        
        CustomDifficulty.DifficultySettings settings = CustomDifficulty.getCurrentSettings();
        if (settings == null) {
            return;
        }
        
        LivingEntity entity = (LivingEntity)(Object)this;
        
        if (entity instanceof Player) {
            float modifiedHeal = healAmount * settings.healingMultiplier;
            entity.setHealth(entity.getHealth() + modifiedHeal);
            ci.cancel();
        }
    }
    
    // モブのスポーン時の体力調整
    @Inject(method = "finalizeSpawn", at = @At("RETURN"))
    private void adjustMobStats(CallbackInfo ci) {
        if (!CustomDifficulty.isCustomDifficultyActive()) {
            return;
        }
        
        CustomDifficulty.DifficultySettings settings = CustomDifficulty.getCurrentSettings();
        if (settings == null) {
            return;
        }
        
        LivingEntity entity = (LivingEntity)(Object)this;
        
        if (entity instanceof Mob && !(entity instanceof Player)) {
            // 体力の調整
            float maxHealth = entity.getMaxHealth() * settings.mobHealthMultiplier;
            entity.getAttribute(Attributes.MAX_HEALTH).setBaseValue(maxHealth);
            entity.setHealth(maxHealth);
            
            // 攻撃力の調整
            if (entity.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
                double attackDamage = entity.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue();
                entity.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(attackDamage * settings.mobDamageMultiplier);
            }
        }
    }
    
    // 暗闇でのダメージ処理
    @Inject(method = "tick", at = @At("TAIL"))
    private void handleDarknessHurt(CallbackInfo ci) {
        if (!CustomDifficulty.isCustomDifficultyActive()) {
            return;
        }
        
        CustomDifficulty.DifficultySettings settings = CustomDifficulty.getCurrentSettings();
        if (settings == null || !settings.darknessHurts) {
            return;
        }
        
        LivingEntity entity = (LivingEntity)(Object)this;
        
        if (entity instanceof Player && !((Player)entity).isCreative()) {
            // 光レベルをチェック
            int lightLevel = entity.level.getLightEmission(entity.blockPosition());
            if (lightLevel < 5 && entity.tickCount % 40 == 0) { // 2秒ごと
                entity.hurt(DamageSource.GENERIC, 1.0F);
            }
        }
    }
}
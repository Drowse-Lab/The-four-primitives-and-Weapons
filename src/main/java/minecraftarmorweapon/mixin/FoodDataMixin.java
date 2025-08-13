package minecraftarmorweapon.mixin;

import minecraftarmorweapon.difficulty.CustomDifficulty;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoodData.class)
public abstract class FoodDataMixin {
    
    @Shadow
    private float exhaustionLevel;
    
    @Shadow
    private int foodLevel;
    
    // 空腹度の減少速度を調整
    @Inject(method = "tick", at = @At("HEAD"))
    private void modifyHungerRate(Player player, CallbackInfo ci) {
        if (!CustomDifficulty.isCustomDifficultyActive()) {
            return;
        }
        
        CustomDifficulty.DifficultySettings settings = CustomDifficulty.getCurrentSettings();
        if (settings == null) {
            return;
        }
        
        // 空腹度減少速度の調整
        if (settings.hungerRateMultiplier > 1.0f) {
            float additionalExhaustion = 0.005f * (settings.hungerRateMultiplier - 1.0f);
            this.exhaustionLevel += additionalExhaustion;
        }
    }
}
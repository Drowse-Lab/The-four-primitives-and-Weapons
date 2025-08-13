package minecraftarmorweapon.mixin;

import minecraftarmorweapon.difficulty.CustomDifficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerMixin {
    
    @Shadow
    public abstract Inventory getInventory();
    
    // 死亡時のアイテムドロップ処理
    @Inject(method = "die", at = @At("HEAD"))
    private void handleDeathItemLoss(DamageSource source, CallbackInfo ci) {
        if (!CustomDifficulty.isCustomDifficultyActive()) {
            return;
        }
        
        CustomDifficulty.DifficultySettings settings = CustomDifficulty.getCurrentSettings();
        if (settings == null || !settings.loseItemsOnDeath) {
            return;
        }
        
        Player player = (Player)(Object)this;
        
        // クリエイティブモードは除外
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        
        // ナイトメアモードでアイテムを完全に失う
        if (CustomDifficulty.NIGHTMARE.equals(CustomDifficulty.getCurrentCustomDifficulty())) {
            Inventory inventory = this.getInventory();
            
            // インベントリをクリア（アイテムをドロップせずに消去）
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                if (!stack.isEmpty()) {
                    // ソウルバウンドなどの特殊エンチャントをチェック可能
                    if (!shouldKeepItem(stack)) {
                        inventory.setItem(i, ItemStack.EMPTY);
                    }
                }
            }
        }
    }
    
    // 特定のアイテムを保持するかチェック（将来の拡張用）
    private boolean shouldKeepItem(ItemStack stack) {
        // ここでソウルバウンドエンチャントなどをチェック可能
        return false;
    }
}
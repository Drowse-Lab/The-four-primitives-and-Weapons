package minecraftarmorweapon.mixin;

import minecraftarmorweapon.difficulty.CustomDifficulty;
import minecraftarmorweapon.ai.LunaticAI;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MobMixin {
    
    @Shadow
    protected GoalSelector goalSelector;
    
    @Shadow
    protected GoalSelector targetSelector;
    
    // モブがスポーンしたときにAIを強化（軽量実装）
    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void enhanceMobAI(CallbackInfo ci) {
        if (!CustomDifficulty.isCustomDifficultyActive()) {
            return;
        }
        
        CustomDifficulty.DifficultySettings settings = CustomDifficulty.getCurrentSettings();
        if (settings == null || settings.aiLevel <= 0) {
            return;
        }
        
        Mob mob = (Mob)(Object)this;
        
        // モンスターのみ強化
        if (!(mob instanceof Monster)) {
            return;
        }
        
        // 装備とAIを一括設定（軽量化）
        LunaticAI.setupMob(mob, settings.aiLevel);
        
        // AIレベル1: 戦術的行動
        if (settings.aiLevel >= 1) {
            this.goalSelector.addGoal(0, new LunaticAI.TacticalCombatGoal(mob));
            
            // ゾンビに剣と弓を持たせる
            if (mob instanceof Zombie) {
                mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
                mob.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.BOW));
            }
        }
        
        // AIレベル2: 高度な装備と戦術
        if (settings.aiLevel >= 2) {
            this.goalSelector.addGoal(0, new LunaticAI.FlankingGoal(mob));
            this.goalSelector.addGoal(1, new LunaticAI.ShieldDefenseGoal(mob));
            
            // 盾を装備
            mob.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        }
        
        // AIレベル3: 攻略不可能レベル
        if (settings.aiLevel >= 3) {
            this.goalSelector.addGoal(0, new LunaticAI.UnbeatableAI(mob));
            
            // エリトラと爆弾を装備
            mob.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.ELYTRA));
            
            // フルダイヤ装備
            mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
            mob.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
            mob.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
        }
    }
}
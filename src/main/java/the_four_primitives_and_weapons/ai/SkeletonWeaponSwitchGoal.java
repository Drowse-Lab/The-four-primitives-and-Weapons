package the_four_primitives_and_weapons.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.SwordItem;

import java.util.EnumSet;

/**
 * スケルトンの武器切り替えAIゴール
 */
public class SkeletonWeaponSwitchGoal extends Goal {
    private final Skeleton skeleton;
    private final ItemStack meleeWeapon;
    private final ItemStack rangedWeapon;
    private int switchCooldown = 0;
    private boolean wasUsingBow = true;
    
    public SkeletonWeaponSwitchGoal(Skeleton skeleton, ItemStack meleeWeapon, ItemStack rangedWeapon) {
        this.skeleton = skeleton;
        this.meleeWeapon = meleeWeapon;
        this.rangedWeapon = rangedWeapon;
        this.setFlags(EnumSet.noneOf(Goal.Flag.class)); // 他のAIと競合しない
    }
    
    @Override
    public boolean canUse() {
        return skeleton.getTarget() != null && skeleton.isAlive();
    }
    
    @Override
    public void tick() {
        if (switchCooldown > 0) {
            switchCooldown--;
            return;
        }
        
        LivingEntity target = skeleton.getTarget();
        if (target == null) {
            return;
        }
        
        double distance = skeleton.distanceToSqr(target);
        ItemStack currentWeapon = skeleton.getMainHandItem();
        
        // 近距離（3ブロック以内）で剣に切り替え
        if (distance < 9.0) { // 3ブロックの二乗
            if (!currentWeapon.is(meleeWeapon.getItem())) {
                skeleton.setItemSlot(EquipmentSlot.MAINHAND, meleeWeapon.copy());
                switchCooldown = 30; // 1.5秒のクールダウン
                wasUsingBow = false;
                
                // 攻撃的にする
                skeleton.setAggressive(true);
            }
        }
        // 中距離（3-6ブロック）は現在の武器を維持
        else if (distance < 36.0) { // 6ブロックの二乗
            // 何もしない（現在の武器を維持）
        }
        // 遠距離（6ブロック以上）で弓に切り替え
        else {
            if (!currentWeapon.is(rangedWeapon.getItem())) {
                skeleton.setItemSlot(EquipmentSlot.MAINHAND, rangedWeapon.copy());
                switchCooldown = 30;
                wasUsingBow = true;
                
                // 弓使用時は攻撃的でない
                skeleton.setAggressive(false);
            }
        }
    }
    
    @Override
    public void stop() {
        // デフォルトで弓に戻す
        if (!wasUsingBow) {
            skeleton.setItemSlot(EquipmentSlot.MAINHAND, rangedWeapon.copy());
            skeleton.setAggressive(false);
        }
    }
}
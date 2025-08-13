package minecraftarmorweapon.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.InteractionHand;

import java.util.*;

public class LunaticAI {
    
    // モブの初期設定（軽量化のため一括処理）
    public static void setupMob(Mob mob, int aiLevel) {
        // 基本ステータス強化
        if (mob.getAttribute(Attributes.MAX_HEALTH) != null) {
            mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40.0 * aiLevel);
            mob.setHealth(40.0f * aiLevel);
        }
        if (mob.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            mob.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3 + (0.1 * aiLevel));
        }
        if (mob.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            mob.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(5.0 * aiLevel);
        }
        
        // 装備ドロップ率を0に（ラグ軽減）
        mob.setDropChance(EquipmentSlot.MAINHAND, 0.0f);
        mob.setDropChance(EquipmentSlot.OFFHAND, 0.0f);
        mob.setDropChance(EquipmentSlot.HEAD, 0.0f);
        mob.setDropChance(EquipmentSlot.CHEST, 0.0f);
        mob.setDropChance(EquipmentSlot.LEGS, 0.0f);
        mob.setDropChance(EquipmentSlot.FEET, 0.0f);
    }
    
    // 戦術的戦闘AI（武器切り替え、回避、盾防御を統合）
    public static class TacticalCombatGoal extends Goal {
        private final Mob mob;
        private Player target;
        private int actionCooldown = 0;
        private int weaponSwitchCooldown = 0;
        private boolean useRanged = false;
        
        public TacticalCombatGoal(Mob mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }
        
        @Override
        public boolean canUse() {
            target = mob.level.getNearestPlayer(mob, 20);
            return target != null && !target.isCreative();
        }
        
        @Override
        public void tick() {
            if (actionCooldown > 0) {
                actionCooldown--;
                return;
            }
            
            double distance = mob.distanceToSqr(target);
            
            // 武器切り替えロジック（軽量）
            if (weaponSwitchCooldown <= 0) {
                if (distance > 36) { // 6ブロック以上なら弓
                    if (!useRanged) {
                        mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
                        mob.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.ARROW, 64));
                        useRanged = true;
                    }
                } else { // 近距離なら剣
                    if (useRanged) {
                        mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
                        mob.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
                        useRanged = false;
                    }
                }
                weaponSwitchCooldown = 20;
            } else {
                weaponSwitchCooldown--;
            }
            
            // 発射体回避（軽量チェック）
            if (mob.tickCount % 5 == 0) { // 5tickごとにチェック
                List<Projectile> projectiles = mob.level.getEntitiesOfClass(
                    Projectile.class, 
                    mob.getBoundingBox().inflate(5.0),
                    p -> p.getDeltaMovement().lengthSqr() > 0.01
                );
                
                if (!projectiles.isEmpty()) {
                    // 横に素早く回避
                    Vec3 dodge = new Vec3(
                        mob.getRandom().nextGaussian() * 2,
                        0.5,
                        mob.getRandom().nextGaussian() * 2
                    );
                    mob.setDeltaMovement(dodge);
                    actionCooldown = 10;
                }
            }
            
            // 盾で防御
            if (!useRanged && distance < 16 && mob.getRandom().nextFloat() < 0.3f) {
                mob.startUsingItem(InteractionHand.OFF_HAND);
                actionCooldown = 20;
            }
            
            // ターゲットを追跡
            mob.getLookControl().setLookAt(target);
            if (!useRanged) {
                mob.getNavigation().moveTo(target, 1.2);
            }
        }
        
        @Override
        public boolean canContinueToUse() {
            return target != null && target.isAlive() && !target.isCreative();
        }
    }
    
    // 挟み撃ちAI（軽量版）
    public static class FlankingGoal extends Goal {
        private final Mob mob;
        private Player target;
        private Vec3 flankPosition;
        private int recalculateCooldown = 0;
        
        public FlankingGoal(Mob mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }
        
        @Override
        public boolean canUse() {
            if (recalculateCooldown > 0) {
                recalculateCooldown--;
                return false;
            }
            
            target = mob.level.getNearestPlayer(mob, 30);
            if (target == null) return false;
            
            // 他のモブと連携して挟み撃ち位置を計算
            List<Mob> allies = mob.level.getEntitiesOfClass(
                Mob.class,
                mob.getBoundingBox().inflate(20),
                m -> m != mob && m instanceof Monster
            );
            
            if (!allies.isEmpty()) {
                // プレイヤーの背後を狙う
                Vec3 behindPlayer = target.position().add(
                    -target.getLookAngle().x * 5,
                    0,
                    -target.getLookAngle().z * 5
                );
                flankPosition = behindPlayer;
                return true;
            }
            
            return false;
        }
        
        @Override
        public void start() {
            if (flankPosition != null) {
                mob.getNavigation().moveTo(flankPosition.x, flankPosition.y, flankPosition.z, 1.5);
                recalculateCooldown = 60;
            }
        }
        
        @Override
        public boolean canContinueToUse() {
            return false;
        }
    }
    
    // 盾防御AI（軽量版）
    public static class ShieldDefenseGoal extends Goal {
        private final Mob mob;
        private int blockTime = 0;
        
        public ShieldDefenseGoal(Mob mob) {
            this.mob = mob;
        }
        
        @Override
        public boolean canUse() {
            // ダメージを受けそうなときに盾を構える
            Player player = mob.level.getNearestPlayer(mob, 5);
            if (player != null && player.swinging) {
                return mob.getItemInHand(InteractionHand.OFF_HAND).getItem() == Items.SHIELD;
            }
            return false;
        }
        
        @Override
        public void start() {
            mob.startUsingItem(InteractionHand.OFF_HAND);
            blockTime = 20;
        }
        
        @Override
        public void tick() {
            blockTime--;
            if (blockTime <= 0) {
                mob.stopUsingItem();
            }
        }
        
        @Override
        public boolean canContinueToUse() {
            return blockTime > 0;
        }
    }
    
    // 攻略不可能AI（全機能統合版・超軽量）
    public static class UnbeatableAI extends Goal {
        private final Mob mob;
        private Player target;
        private int phase = 0;
        private int phaseCooldown = 0;
        private int tntCooldown = 0;
        private boolean isFlying = false;
        
        public UnbeatableAI(Mob mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }
        
        @Override
        public boolean canUse() {
            target = mob.level.getNearestPlayer(mob, 50);
            return target != null && !target.isCreative();
        }
        
        @Override
        public void tick() {
            if (phaseCooldown > 0) {
                phaseCooldown--;
            } else {
                phase = mob.getRandom().nextInt(5);
                phaseCooldown = 40;
            }
            
            switch (phase) {
                case 0: // エリトラ飛行攻撃
                    if (!isFlying && mob.getItemBySlot(EquipmentSlot.CHEST).getItem() == Items.ELYTRA) {
                        mob.setDeltaMovement(0, 2, 0);
                        isFlying = true;
                    }
                    if (isFlying) {
                        Vec3 toTarget = target.position().subtract(mob.position()).normalize();
                        mob.setDeltaMovement(toTarget.scale(0.8));
                        
                        // 空中から爆弾投下
                        if (tntCooldown <= 0) {
                            mob.level.setBlock(mob.blockPosition().below(), Blocks.TNT.defaultBlockState(), 3);
                            mob.level.setBlock(mob.blockPosition().below(), Blocks.REDSTONE_TORCH.defaultBlockState(), 3);
                            tntCooldown = 60;
                        }
                    }
                    break;
                    
                case 1: // 超高速接近戦
                    isFlying = false;
                    mob.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.8);
                    mob.getNavigation().moveTo(target, 2.0);
                    
                    // 連続攻撃
                    if (mob.distanceToSqr(target) < 4) {
                        mob.swing(InteractionHand.MAIN_HAND);
                        target.hurt(DamageSource.mobAttack(mob), 10.0f);
                    }
                    break;
                    
                case 2: // 瞬間移動奇襲
                    isFlying = false;
                    if (mob.getRandom().nextFloat() < 0.3f) {
                        // プレイヤーの背後に瞬間移動
                        Vec3 behind = target.position().add(
                            -target.getLookAngle().x * 2,
                            0,
                            -target.getLookAngle().z * 2
                        );
                        mob.teleportTo(behind.x, behind.y, behind.z);
                        mob.swing(InteractionHand.MAIN_HAND);
                    }
                    break;
                    
                case 3: // 壁建築防御
                    isFlying = false;
                    BlockPos mobPos = mob.blockPosition();
                    for (int x = -1; x <= 1; x++) {
                        for (int y = 0; y <= 2; y++) {
                            for (int z = -1; z <= 1; z++) {
                                if (x == 0 && z == 0) continue;
                                BlockPos pos = mobPos.offset(x, y, z);
                                if (mob.level.isEmptyBlock(pos)) {
                                    mob.level.setBlock(pos, Blocks.OBSIDIAN.defaultBlockState(), 3);
                                }
                            }
                        }
                    }
                    break;
                    
                case 4: // 仲間召喚
                    isFlying = false;
                    if (mob.getRandom().nextFloat() < 0.1f) {
                        // 強化ゾンビを召喚
                        for (int i = 0; i < 3; i++) {
                            Entity zombie = EntityType.ZOMBIE.create(mob.level);
                            if (zombie instanceof Mob) {
                                Mob ally = (Mob) zombie;
                                ally.setPos(
                                    mob.getX() + mob.getRandom().nextGaussian() * 2,
                                    mob.getY(),
                                    mob.getZ() + mob.getRandom().nextGaussian() * 2
                                );
                                ally.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
                                ally.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
                                mob.level.addFreshEntity(ally);
                                ally.setTarget(target);
                            }
                        }
                    }
                    break;
            }
            
            if (tntCooldown > 0) tntCooldown--;
            
            // 常に回復
            if (mob.getHealth() < mob.getMaxHealth() && mob.tickCount % 20 == 0) {
                mob.heal(2.0f);
            }
        }
        
        @Override
        public boolean canContinueToUse() {
            return target != null && target.isAlive() && !target.isCreative();
        }
        
        @Override
        public void stop() {
            isFlying = false;
            mob.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3);
        }
    }
}
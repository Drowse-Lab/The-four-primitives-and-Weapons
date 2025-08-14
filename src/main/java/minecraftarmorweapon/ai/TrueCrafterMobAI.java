package minecraftarmorweapon.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import minecraftarmorweapon.command.CustomDifficultyCommand;

import java.util.EnumSet;

@Mod.EventBusSubscriber
public class TrueCrafterMobAI {
    
    @SubscribeEvent
    public static void onEntityJoinWorld(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof Monster monster)) {
            return;
        }
        
        // True Crafterモードが有効かチェック
        if (!CustomDifficultyCommand.isTrueCrafterEnabled()) {
            return;
        }
        
        // モンスターの種類に応じて強化
        if (monster instanceof Zombie zombie) {
            enhanceZombie(zombie);
        } else if (monster instanceof Skeleton skeleton) {
            enhanceSkeleton(skeleton);
        } else if (monster instanceof Creeper creeper) {
            enhanceCreeper(creeper);
        } else if (monster instanceof Spider spider) {
            enhanceSpider(spider);
        } else if (monster instanceof EnderMan enderman) {
            enhanceEnderman(enderman);
        }
    }
    
    // ゾンビの強化
    private static void enhanceZombie(Zombie zombie) {
        // 装備を追加
        zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        zombie.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        zombie.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
        zombie.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
        zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        
        // ドロップ率を0にする（装備品がドロップしないように）
        zombie.setDropChance(EquipmentSlot.HEAD, 0.0f);
        zombie.setDropChance(EquipmentSlot.CHEST, 0.0f);
        zombie.setDropChance(EquipmentSlot.LEGS, 0.0f);
        zombie.setDropChance(EquipmentSlot.FEET, 0.0f);
        zombie.setDropChance(EquipmentSlot.MAINHAND, 0.0f);
        
        // 能力強化
        zombie.getAttribute(Attributes.MAX_HEALTH).setBaseValue(30.0);
        zombie.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3);
        zombie.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(40.0);
        zombie.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.5);
        zombie.setHealth(30.0f);
        
        // ドアを破壊できるようにする
        zombie.setCanBreakDoors(true);
        
        // カスタムAIを追加
        zombie.goalSelector.addGoal(1, new ZombieLeapAttackGoal(zombie));
        zombie.goalSelector.addGoal(2, new ZombieBlockPlaceGoal(zombie));
    }
    
    // スケルトンの強化
    private static void enhanceSkeleton(Skeleton skeleton) {
        // 装備を強化
        ItemStack bow = new ItemStack(Items.BOW);
        bow.enchant(Enchantments.POWER_ARROWS, 2);
        bow.enchant(Enchantments.PUNCH_ARROWS, 1);
        skeleton.setItemSlot(EquipmentSlot.MAINHAND, bow);
        skeleton.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CHAINMAIL_HELMET));
        
        skeleton.setDropChance(EquipmentSlot.MAINHAND, 0.0f);
        skeleton.setDropChance(EquipmentSlot.HEAD, 0.0f);
        
        // 能力強化
        skeleton.getAttribute(Attributes.MAX_HEALTH).setBaseValue(25.0);
        skeleton.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.28);
        skeleton.setHealth(25.0f);
        
        // カスタムAIを追加
        skeleton.goalSelector.addGoal(1, new SkeletonDodgeGoal(skeleton));
        skeleton.goalSelector.addGoal(2, new SkeletonStrafeGoal(skeleton));
    }
    
    // クリーパーの強化
    private static void enhanceCreeper(Creeper creeper) {
        // 能力強化
        creeper.getAttribute(Attributes.MAX_HEALTH).setBaseValue(25.0);
        creeper.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3);
        creeper.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(20.0);
        creeper.setHealth(25.0f);
        
        // 爆発力を少し強化（explosionRadiusはprivateなので属性で対応）
        // デフォルトの爆発力は3なので、少し強くする効果をAIで実現
        
        // カスタムAIを追加
        creeper.goalSelector.addGoal(1, new CreeperSmartExplodeGoal(creeper));
    }
    
    // クモの強化
    private static void enhanceSpider(Spider spider) {
        // 能力強化
        spider.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20.0);
        spider.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.35);
        spider.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(3.0);
        spider.setHealth(20.0f);
        
        // カスタムAIを追加
        spider.goalSelector.addGoal(1, new SpiderPounceGoal(spider));
        spider.goalSelector.addGoal(2, new SpiderWebPlaceGoal(spider));
    }
    
    // エンダーマンの強化
    private static void enhanceEnderman(EnderMan enderman) {
        // 能力強化
        enderman.getAttribute(Attributes.MAX_HEALTH).setBaseValue(50.0);
        enderman.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(10.0);
        enderman.setHealth(50.0f);
        
        // カスタムAIを追加
        enderman.goalSelector.addGoal(1, new EndermanDodgeGoal(enderman));
    }
    
    // ゾンビの飛びかかり攻撃
    public static class ZombieLeapAttackGoal extends Goal {
        private final Zombie zombie;
        private int cooldown = 0;
        
        public ZombieLeapAttackGoal(Zombie zombie) {
            this.zombie = zombie;
            this.setFlags(EnumSet.of(Flag.JUMP, Flag.MOVE));
        }
        
        @Override
        public boolean canUse() {
            LivingEntity target = zombie.getTarget();
            if (target == null || cooldown > 0) return false;
            double distance = zombie.distanceToSqr(target);
            return distance > 4.0 && distance < 64.0 && zombie.isOnGround();
        }
        
        @Override
        public void start() {
            LivingEntity target = zombie.getTarget();
            if (target != null) {
                Vec3 direction = target.position().subtract(zombie.position()).normalize();
                zombie.setDeltaMovement(direction.x * 1.5, 0.6, direction.z * 1.5);
                zombie.hasImpulse = true;
                cooldown = 60;
            }
        }
        
        @Override
        public void tick() {
            if (cooldown > 0) cooldown--;
        }
    }
    
    // ゾンビのブロック設置
    public static class ZombieBlockPlaceGoal extends Goal {
        private final Zombie zombie;
        private int placeCooldown = 0;
        
        public ZombieBlockPlaceGoal(Zombie zombie) {
            this.zombie = zombie;
        }
        
        @Override
        public boolean canUse() {
            LivingEntity target = zombie.getTarget();
            if (target == null || placeCooldown > 0) return false;
            return target.getY() > zombie.getY() + 2;
        }
        
        @Override
        public void tick() {
            if (placeCooldown > 0) {
                placeCooldown--;
                return;
            }
            
            LivingEntity target = zombie.getTarget();
            if (target != null && target.getY() > zombie.getY() + 2) {
                BlockPos pos = zombie.blockPosition();
                if (zombie.level.getBlockState(pos).isAir()) {
                    zombie.level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 3);
                    placeCooldown = 20;
                }
            }
        }
    }
    
    // スケルトンの回避
    public static class SkeletonDodgeGoal extends Goal {
        private final Skeleton skeleton;
        private int dodgeCooldown = 0;
        
        public SkeletonDodgeGoal(Skeleton skeleton) {
            this.skeleton = skeleton;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }
        
        @Override
        public boolean canUse() {
            return skeleton.getTarget() != null && dodgeCooldown <= 0;
        }
        
        @Override
        public void tick() {
            if (dodgeCooldown > 0) {
                dodgeCooldown--;
                return;
            }
            
            LivingEntity target = skeleton.getTarget();
            if (target != null && skeleton.distanceToSqr(target) < 25) {
                // 近すぎる場合は後退
                Vec3 away = skeleton.position().subtract(target.position()).normalize();
                skeleton.setDeltaMovement(away.x * 0.5, skeleton.getDeltaMovement().y, away.z * 0.5);
                dodgeCooldown = 10;
            }
        }
    }
    
    // スケルトンのストレイフ移動
    public static class SkeletonStrafeGoal extends Goal {
        private final Skeleton skeleton;
        private int strafeTime = 0;
        private boolean strafeRight = false;
        
        public SkeletonStrafeGoal(Skeleton skeleton) {
            this.skeleton = skeleton;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }
        
        @Override
        public boolean canUse() {
            return skeleton.getTarget() != null;
        }
        
        @Override
        public void tick() {
            LivingEntity target = skeleton.getTarget();
            if (target == null) return;
            
            strafeTime--;
            if (strafeTime <= 0) {
                strafeRight = !strafeRight;
                strafeTime = 20 + skeleton.getRandom().nextInt(20);
            }
            
            // 横移動
            Vec3 strafe = target.position().subtract(skeleton.position()).normalize().cross(new Vec3(0, 1, 0));
            if (!strafeRight) strafe = strafe.scale(-1);
            
            skeleton.getMoveControl().strafe(0.0f, strafeRight ? 0.5f : -0.5f);
        }
    }
    
    // クリーパーのスマート爆発
    public static class CreeperSmartExplodeGoal extends Goal {
        private final Creeper creeper;
        
        public CreeperSmartExplodeGoal(Creeper creeper) {
            this.creeper = creeper;
        }
        
        @Override
        public boolean canUse() {
            LivingEntity target = creeper.getTarget();
            return target != null && creeper.distanceToSqr(target) < 9;
        }
        
        @Override
        public void tick() {
            LivingEntity target = creeper.getTarget();
            if (target != null) {
                // 壁越しでも爆発
                creeper.setSwellDir(1);
            }
        }
    }
    
    // クモの飛びかかり
    public static class SpiderPounceGoal extends Goal {
        private final Spider spider;
        private int cooldown = 0;
        
        public SpiderPounceGoal(Spider spider) {
            this.spider = spider;
            this.setFlags(EnumSet.of(Flag.JUMP, Flag.MOVE));
        }
        
        @Override
        public boolean canUse() {
            LivingEntity target = spider.getTarget();
            if (target == null || cooldown > 0) return false;
            double distance = spider.distanceToSqr(target);
            return distance > 4.0 && distance < 49.0;
        }
        
        @Override
        public void start() {
            LivingEntity target = spider.getTarget();
            if (target != null) {
                Vec3 direction = target.position().subtract(spider.position()).normalize();
                spider.setDeltaMovement(direction.x * 1.2, 0.5, direction.z * 1.2);
                spider.hasImpulse = true;
                cooldown = 40;
            }
        }
        
        @Override
        public void tick() {
            if (cooldown > 0) cooldown--;
        }
    }
    
    // クモの巣設置
    public static class SpiderWebPlaceGoal extends Goal {
        private final Spider spider;
        private int webCooldown = 0;
        
        public SpiderWebPlaceGoal(Spider spider) {
            this.spider = spider;
        }
        
        @Override
        public boolean canUse() {
            return spider.getTarget() != null && webCooldown <= 0;
        }
        
        @Override
        public void tick() {
            if (webCooldown > 0) {
                webCooldown--;
                return;
            }
            
            LivingEntity target = spider.getTarget();
            if (target != null && spider.distanceToSqr(target) < 16) {
                BlockPos targetPos = target.blockPosition();
                if (spider.level.getBlockState(targetPos).isAir()) {
                    spider.level.setBlock(targetPos, Blocks.COBWEB.defaultBlockState(), 3);
                    webCooldown = 100;
                }
            }
        }
    }
    
    // エンダーマンの回避
    public static class EndermanDodgeGoal extends Goal {
        private final EnderMan enderman;
        
        public EndermanDodgeGoal(EnderMan enderman) {
            this.enderman = enderman;
        }
        
        @Override
        public boolean canUse() {
            return enderman.getTarget() != null;
        }
        
        @Override
        public void tick() {
            // 矢を検知して回避
            enderman.level.getEntitiesOfClass(AbstractArrow.class, 
                enderman.getBoundingBox().inflate(4.0),
                arrow -> arrow.getDeltaMovement().length() > 0)
                .forEach(arrow -> {
                    // teleport()はprotectedなので、代わりに位置を変更
                    double x = enderman.getX() + (enderman.getRandom().nextDouble() - 0.5) * 16.0;
                    double y = enderman.getY();
                    double z = enderman.getZ() + (enderman.getRandom().nextDouble() - 0.5) * 16.0;
                    
                    // 安全な位置を探す
                    BlockPos pos = new BlockPos(x, y, z);
                    if (enderman.level.getBlockState(pos).isAir() && 
                        enderman.level.getBlockState(pos.below()).getMaterial().isSolid()) {
                        enderman.teleportTo(x, y, z);
                        enderman.level.playSound(null, enderman.xo, enderman.yo, enderman.zo, 
                            SoundEvents.ENDERMAN_TELEPORT, enderman.getSoundSource(), 1.0F, 1.0F);
                    }
                });
        }
    }
}
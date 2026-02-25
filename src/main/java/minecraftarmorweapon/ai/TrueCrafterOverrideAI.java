package minecraftarmorweapon.ai;

import minecraftarmorweapon.util.VersionHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import minecraftarmorweapon.command.CustomDifficultyCommand;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 既存のAIゴールを上書きする実装
 */
// 一時的に無効化 - ConcurrentModificationExceptionを防ぐため
//@Mod.EventBusSubscriber
public class TrueCrafterOverrideAI {
    
    // 一時ブロックの管理（時間と破壊段階を記録）
    private static final Map<BlockPos, TemporaryBlockData> temporaryBlocks = new ConcurrentHashMap<>();
    private static final long BLOCK_DECAY_TIME = 15000; // 15秒
    
    private static class TemporaryBlockData {
        final long placedTime;
        final UUID placedByEntity;
        int breakStage = -1;
        
        TemporaryBlockData(long placedTime, UUID placedByEntity) {
            this.placedTime = placedTime;
            this.placedByEntity = placedByEntity;
        }
    }
    
    @SubscribeEvent
    public static void onEntityJoinWorld(EntityJoinLevelEvent event) {
        if (!CustomDifficultyCommand.isTrueCrafterEnabled()) {
            return;
        }
        
        if (!(event.getEntity() instanceof Monster monster)) {
            return;
        }
        
        if (VersionHelper.getLevel(monster) == null || monster.level().isClientSide) {
            return;
        }
        
        // 次のティックで処理（エンティティが完全に初期化された後）
        monster.level().getServer().execute(() -> {
            try {
                if (monster instanceof Skeleton skeleton) {
                    overrideSkeleton(skeleton);
                } else if (monster instanceof Zombie zombie) {
                    overrideZombie(zombie);
                }
            } catch (Exception e) {
                // エラーを無視
            }
        });
    }
    
    private static void overrideSkeleton(Skeleton skeleton) {
        // 既存のRangedBowAttackGoalを削除
        removeGoals(skeleton.goalSelector, RangedBowAttackGoal.class);
        removeGoals(skeleton.goalSelector, MeleeAttackGoal.class);
        
        // 装備を設定
        ItemStack bow = new ItemStack(Items.BOW);
        bow.enchant(Enchantments.POWER_ARROWS, 2);
        skeleton.setItemSlot(EquipmentSlot.MAINHAND, bow);
        skeleton.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CHAINMAIL_HELMET));
        skeleton.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        
        skeleton.setDropChance(EquipmentSlot.MAINHAND, 0.0f);
        skeleton.setDropChance(EquipmentSlot.HEAD, 0.0f);
        skeleton.setDropChance(EquipmentSlot.OFFHAND, 0.0f);
        
        // ステータス強化
        if (skeleton.getAttribute(Attributes.MAX_HEALTH) != null) {
            skeleton.getAttribute(Attributes.MAX_HEALTH).setBaseValue(25.0);
            skeleton.setHealth(25.0f);
        }
        
        // 新しいAIゴールを追加（優先度を調整）
        skeleton.goalSelector.addGoal(2, new SkeletonSwitchingGoal(skeleton));
        skeleton.goalSelector.addGoal(3, new AvoidEntityGoal<>(skeleton, Player.class, 4.0F, 1.0D, 1.2D));
    }
    
    private static void overrideZombie(Zombie zombie) {
        // 既存の攻撃ゴールを削除
        removeGoals(zombie.goalSelector, ZombieAttackGoal.class);
        
        // 装備を設定
        zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        zombie.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        zombie.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
        zombie.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
        zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        zombie.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        
        zombie.setDropChance(EquipmentSlot.HEAD, 0.0f);
        zombie.setDropChance(EquipmentSlot.CHEST, 0.0f);
        zombie.setDropChance(EquipmentSlot.LEGS, 0.0f);
        zombie.setDropChance(EquipmentSlot.FEET, 0.0f);
        zombie.setDropChance(EquipmentSlot.MAINHAND, 0.0f);
        zombie.setDropChance(EquipmentSlot.OFFHAND, 0.0f);
        
        // ステータス強化
        if (zombie.getAttribute(Attributes.MAX_HEALTH) != null) {
            zombie.getAttribute(Attributes.MAX_HEALTH).setBaseValue(30.0);
            zombie.setHealth(30.0f);
        }
        if (zombie.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            zombie.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.28);
        }
        if (zombie.getAttribute(Attributes.KNOCKBACK_RESISTANCE) != null) {
            zombie.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.3);
        }
        
        zombie.setCanBreakDoors(true);
        
        // 新しいAIゴールを追加
        zombie.goalSelector.addGoal(2, new ZombieSwitchingGoal(zombie));
    }
    
    // 特定のクラスのゴールを削除
    private static void removeGoals(GoalSelector goalSelector, Class<?> goalClass) {
        try {
            // availableGoalsフィールドにアクセス
            var field = GoalSelector.class.getDeclaredField("availableGoals");
            field.setAccessible(true);
            Set<WrappedGoal> goals = (Set<WrappedGoal>) field.get(goalSelector);
            
            // 削除するゴールを特定
            Set<WrappedGoal> toRemove = goals.stream()
                .filter(wrappedGoal -> goalClass.isInstance(wrappedGoal.getGoal()))
                .collect(Collectors.toSet());
            
            // ゴールを削除
            goals.removeAll(toRemove);
        } catch (Exception e) {
            // リフレクションエラーを無視
        }
    }
    
    // スケルトンの武器切り替えゴール
    public static class SkeletonSwitchingGoal extends Goal {
        private final Skeleton skeleton;
        private final ItemStack bow;
        private final ItemStack sword;
        private int switchCooldown = 0;
        private int attackTime = -1;
        
        public SkeletonSwitchingGoal(Skeleton skeleton) {
            this.skeleton = skeleton;
            this.bow = new ItemStack(Items.BOW);
            this.bow.enchant(Enchantments.POWER_ARROWS, 2);
            this.sword = new ItemStack(Items.IRON_SWORD);
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }
        
        @Override
        public boolean canUse() {
            return skeleton.getTarget() != null;
        }
        
        @Override
        public void stop() {
            this.attackTime = -1;
            skeleton.setAggressive(false);
        }
        
        @Override
        public void tick() {
            LivingEntity target = skeleton.getTarget();
            if (target == null) return;
            
            double distance = skeleton.distanceToSqr(target);
            boolean canSee = skeleton.getSensing().hasLineOfSight(target);
            
            // クールダウン処理
            if (switchCooldown > 0) {
                switchCooldown--;
            }
            
            // 距離に応じて武器を切り替え
            if (switchCooldown <= 0) {
                ItemStack current = skeleton.getItemBySlot(EquipmentSlot.MAINHAND);
                
                if (distance > 64 && !current.is(Items.BOW)) { // 8ブロック以上
                    skeleton.setItemSlot(EquipmentSlot.MAINHAND, bow.copy());
                    switchCooldown = 40;
                    this.attackTime = -1;
                } else if (distance < 16 && !current.is(Items.IRON_SWORD)) { // 4ブロック以下
                    skeleton.setItemSlot(EquipmentSlot.MAINHAND, sword.copy());
                    switchCooldown = 40;
                    this.attackTime = -1;
                }
            }
            
            // 現在の武器に応じた攻撃処理
            ItemStack current = skeleton.getItemBySlot(EquipmentSlot.MAINHAND);
            
            if (current.is(Items.BOW)) {
                // 弓の攻撃
                skeleton.getLookControl().setLookAt(target, 30.0F, 30.0F);
                
                if (canSee) {
                    ++this.attackTime;
                    
                    if (this.attackTime >= 20) {
                        skeleton.performRangedAttack(target, 1.0F);
                        this.attackTime = -40;
                    }
                } else if (this.attackTime > 0) {
                    --this.attackTime;
                }
                
                skeleton.setAggressive(this.attackTime > 0);
                
            } else if (current.is(Items.IRON_SWORD)) {
                // 剣の攻撃
                skeleton.getLookControl().setLookAt(target, 30.0F, 30.0F);
                skeleton.getNavigation().moveTo(target, 1.2D);
                
                if (distance < 4 && canSee) {
                    skeleton.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                    skeleton.doHurtTarget(target);
                }
            }
        }
    }
    
    // ゾンビのブロック設置ゴール（武器切り替えなし）
    public static class ZombieSwitchingGoal extends Goal {
        private final Zombie zombie;
        private int blockPlaceCooldown = 0;
        private int leapCooldown = 0;
        
        public ZombieSwitchingGoal(Zombie zombie) {
            this.zombie = zombie;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }
        
        @Override
        public boolean canUse() {
            return zombie.getTarget() != null;
        }
        
        @Override
        public void tick() {
            LivingEntity target = zombie.getTarget();
            if (target == null) return;
            
            double distance = zombie.distanceToSqr(target);
            
            // クールダウン処理
            if (blockPlaceCooldown > 0) {
                blockPlaceCooldown--;
            }
            if (leapCooldown > 0) {
                leapCooldown--;
            }
            
            // ブロック設置（高所に登る）
            if (blockPlaceCooldown <= 0) {
                // ターゲットが高い位置にいる場合
                if (target.getY() > zombie.getY() + 1.5) {
                    BlockPos pos = zombie.blockPosition();
                    BlockPos placePos = pos.relative(zombie.getDirection());
                    
                    if (zombie.level().getBlockState(placePos).isAir() && 
                        zombie.level().getBlockState(placePos.below()).isSolid()) {
                        zombie.level().setBlock(placePos, Blocks.COBBLESTONE.defaultBlockState(), 3);
                        temporaryBlocks.put(placePos, new TemporaryBlockData(System.currentTimeMillis(), zombie.getUUID()));
                        blockPlaceCooldown = 20;
                    }
                }
                
                // 橋を作る（谷や水を渡る）
                BlockPos frontPos = zombie.blockPosition().relative(zombie.getDirection());
                BlockPos belowFront = frontPos.below();
                BlockState belowState = zombie.level().getBlockState(belowFront);
                
                if (belowState.isAir() || belowState.liquid()) {
                    zombie.level().setBlock(belowFront, Blocks.COBBLESTONE.defaultBlockState(), 3);
                    temporaryBlocks.put(belowFront, new TemporaryBlockData(System.currentTimeMillis(), zombie.getUUID()));
                    blockPlaceCooldown = 15;
                }
            }
            
            // 飛びかかり攻撃
            if (leapCooldown <= 0 && zombie.onGround()) {
                if (distance > 9 && distance < 49) { // 3-7ブロック
                    Vec3 direction = target.position().subtract(zombie.position()).normalize();
                    zombie.setDeltaMovement(direction.x * 1.0, 0.4, direction.z * 1.0);
                    zombie.hasImpulse = true;
                    leapCooldown = 60;
                }
            }
            
            // 通常の近接攻撃
            zombie.getNavigation().moveTo(target, 1.0D);
            
            if (distance < 4) {
                zombie.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                zombie.doHurtTarget(target);
            }
        }
    }
    
    // 一時ブロックの処理（ひび割れアニメーション付き）
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        
        // 10ティックごとに処理（0.5秒ごと）
        if (event.getServer().getTickCount() % 10 == 0) {
            long currentTime = System.currentTimeMillis();
            
            for (ServerLevel level : event.getServer().getAllLevels()) {
                Iterator<Map.Entry<BlockPos, TemporaryBlockData>> iterator = temporaryBlocks.entrySet().iterator();
                
                while (iterator.hasNext()) {
                    Map.Entry<BlockPos, TemporaryBlockData> entry = iterator.next();
                    BlockPos pos = entry.getKey();
                    TemporaryBlockData data = entry.getValue();
                    
                    // 経過時間を計算
                    long elapsed = currentTime - data.placedTime;
                    float progress = (float) elapsed / BLOCK_DECAY_TIME;
                    
                    if (progress >= 1.0f) {
                        // 時間経過でブロックを削除
                        if (level.getBlockState(pos).is(Blocks.COBBLESTONE) || 
                            level.getBlockState(pos).is(Blocks.COBWEB)) {
                            
                            // ブロック破壊パーティクルを生成（アイテムドロップなし）
                            level.destroyBlock(pos, false); // false = アイテムドロップなし
                        }
                        
                        // 破壊進行状況をリセット
                        level.destroyBlockProgress(data.placedByEntity.hashCode(), pos, -1);
                        iterator.remove();
                        
                    } else {
                        // ひび割れの進行状況を更新（0-9の10段階）
                        int breakStage = (int) (progress * 10);
                        
                        // ブロックがまだ存在する場合のみ更新
                        if (level.getBlockState(pos).is(Blocks.COBBLESTONE) || 
                            level.getBlockState(pos).is(Blocks.COBWEB)) {
                            
                            if (breakStage != data.breakStage) {
                                data.breakStage = breakStage;
                                
                                // 破壊進行状況を表示（全プレイヤーに送信）
                                level.destroyBlockProgress(data.placedByEntity.hashCode(), pos, breakStage);
                            }
                        } else {
                            // ブロックが既に破壊されている場合は削除
                            level.destroyBlockProgress(data.placedByEntity.hashCode(), pos, -1);
                            iterator.remove();
                        }
                    }
                }
            }
        }
        
        // メモリクリーンアップ（5分ごと）
        if (event.getServer().getTickCount() % 6000 == 0) {
            long currentTime = System.currentTimeMillis();
            temporaryBlocks.entrySet().removeIf(entry -> 
                currentTime - entry.getValue().placedTime > BLOCK_DECAY_TIME * 2
            );
        }
    }
    
    // モブ強化処理（スパイダー用）
    @SubscribeEvent
    public static void onLivingUpdate(LivingEvent.LivingTickEvent event) {
        if (!CustomDifficultyCommand.isTrueCrafterEnabled()) {
            return;
        }
        
        if (event.getEntity() instanceof Spider spider && !spider.level().isClientSide) {
            LivingEntity target = spider.getTarget();
            if (target != null) {
                // クモの巣設置
                if (spider.getRandom().nextInt(100) == 0) {
                    BlockPos targetPos = target.blockPosition();
                    if (spider.level().getBlockState(targetPos).isAir() && 
                        spider.distanceToSqr(target) < 16) {
                        spider.level().setBlock(targetPos, Blocks.COBWEB.defaultBlockState(), 3);
                        temporaryBlocks.put(targetPos, new TemporaryBlockData(System.currentTimeMillis(), spider.getUUID()));
                    }
                }
            }
        }
    }
}
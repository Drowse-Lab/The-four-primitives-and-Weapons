package minecraftarmorweapon.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import minecraftarmorweapon.command.CustomDifficultyCommand;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 安全なTrue Crafter Mode実装
 * AIゴールの変更を安全に行い、ConcurrentModificationExceptionを防ぐ
 */
@Mod.EventBusSubscriber
public class SafeTrueCrafterAI {
    
    // エンティティごとの強化状態を管理
    private static final Set<UUID> enhancedEntities = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, MobEnhancementData> enhancementData = new ConcurrentHashMap<>();
    
    private static class MobEnhancementData {
        boolean isEnhanced = false;
        int weaponSwitchCooldown = 0;
        int dodgeCooldown = 0;
        int blockPlaceCooldown = 0;
        ItemStack meleeWeapon = ItemStack.EMPTY;
        ItemStack rangedWeapon = ItemStack.EMPTY;
        long lastUpdateTick = 0;
    }
    
    @SubscribeEvent
    public static void onEntityJoinWorld(EntityJoinLevelEvent event) {
        if (!CustomDifficultyCommand.isTrueCrafterEnabled()) {
            return;
        }
        
        if (!(event.getEntity() instanceof Monster monster)) {
            return;
        }
        
        if (monster.level == null || monster.level.isClientSide) {
            return;
        }
        
        UUID entityId = monster.getUUID();
        
        // すでに強化済みならスキップ
        if (enhancedEntities.contains(entityId)) {
            return;
        }
        
        // サーバーの次のティックで処理（エンティティが完全に初期化された後）
        if (monster.level instanceof ServerLevel serverLevel) {
            serverLevel.getServer().execute(() -> {
                try {
                    enhanceMonster(monster);
                    enhancedEntities.add(entityId);
                } catch (Exception e) {
                    // エラーをログに記録するが、クラッシュは防ぐ
                    System.err.println("Failed to enhance monster: " + e.getMessage());
                }
            });
        }
    }
    
    private static void enhanceMonster(Monster monster) {
        if (monster instanceof Skeleton skeleton) {
            enhanceSkeleton(skeleton);
        } else if (monster instanceof Zombie zombie) {
            enhanceZombie(zombie);
        } else if (monster instanceof Spider spider) {
            enhanceSpider(spider);
        } else if (monster instanceof Creeper creeper) {
            enhanceCreeper(creeper);
        } else if (monster instanceof Witch witch) {
            enhanceWitch(witch);
        }
    }
    
    private static void enhanceSkeleton(Skeleton skeleton) {
        java.util.Random random = skeleton.getRandom();
        
        // ティアシステム（0: 通常, 1: 精鋭, 2: チャンピオン）
        int tier = random.nextFloat() < 0.7f ? 0 : (random.nextFloat() < 0.8f ? 1 : 2);
        
        // 弓の設定（ティアに応じて強化）
        ItemStack bow = new ItemStack(Items.BOW);
        if (tier == 0) {
            // 通常スケルトン
            bow.enchant(Enchantments.POWER_ARROWS, 1);
            if (random.nextBoolean()) {
                bow.enchant(Enchantments.PUNCH_ARROWS, 1);
            }
        } else if (tier == 1) {
            // 精鋭スケルトン
            bow.enchant(Enchantments.POWER_ARROWS, 2 + random.nextInt(2));
            bow.enchant(Enchantments.PUNCH_ARROWS, 1);
            if (random.nextFloat() < 0.3f) {
                bow.enchant(Enchantments.FLAMING_ARROWS, 1);
            }
        } else {
            // チャンピオンスケルトン
            bow.enchant(Enchantments.POWER_ARROWS, 3 + random.nextInt(2));
            bow.enchant(Enchantments.PUNCH_ARROWS, 2);
            bow.enchant(Enchantments.FLAMING_ARROWS, 1);
            if (random.nextFloat() < 0.5f) {
                bow.enchant(Enchantments.INFINITY_ARROWS, 1);
            }
        }
        skeleton.setItemSlot(EquipmentSlot.MAINHAND, bow);
        
        // 防具の設定（ランダム）
        if (tier == 0) {
            // 通常: 軽装
            if (random.nextFloat() < 0.6f) {
                ItemStack helmet = new ItemStack(random.nextBoolean() ? Items.LEATHER_HELMET : Items.CHAINMAIL_HELMET);
                skeleton.setItemSlot(EquipmentSlot.HEAD, helmet);
            }
            if (random.nextFloat() < 0.3f) {
                skeleton.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
            }
        } else if (tier == 1) {
            // 精鋭: 中装
            skeleton.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CHAINMAIL_HELMET));
            if (random.nextFloat() < 0.7f) {
                skeleton.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.CHAINMAIL_CHESTPLATE));
            }
            if (random.nextFloat() < 0.5f) {
                skeleton.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.CHAINMAIL_LEGGINGS));
            }
        } else {
            // チャンピオン: 重装
            ItemStack helmet = new ItemStack(Items.IRON_HELMET);
            helmet.enchant(Enchantments.ALL_DAMAGE_PROTECTION, 1 + random.nextInt(3));
            skeleton.setItemSlot(EquipmentSlot.HEAD, helmet);
            
            ItemStack chestplate = new ItemStack(Items.IRON_CHESTPLATE);
            if (random.nextFloat() < 0.5f) {
                chestplate.enchant(Enchantments.PROJECTILE_PROTECTION, 2);
            }
            skeleton.setItemSlot(EquipmentSlot.CHEST, chestplate);
            
            if (random.nextFloat() < 0.7f) {
                skeleton.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
            }
            if (random.nextFloat() < 0.5f) {
                skeleton.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
            }
        }
        
        // 盾の設定（ティアが高いほど確率が上がる）
        float shieldChance = tier == 0 ? 0.2f : (tier == 1 ? 0.5f : 0.8f);
        if (random.nextFloat() < shieldChance) {
            skeleton.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        }
        
        // 近接武器を準備（ティアに応じて）
        ItemStack sword;
        if (tier == 0) {
            sword = new ItemStack(random.nextBoolean() ? Items.STONE_SWORD : Items.IRON_SWORD);
        } else if (tier == 1) {
            sword = new ItemStack(Items.IRON_SWORD);
            sword.enchant(Enchantments.SHARPNESS, 1 + random.nextInt(2));
        } else {
            sword = new ItemStack(random.nextFloat() < 0.3f ? Items.DIAMOND_SWORD : Items.IRON_SWORD);
            sword.enchant(Enchantments.SHARPNESS, 2 + random.nextInt(2));
            if (random.nextFloat() < 0.3f) {
                sword.enchant(Enchantments.FIRE_ASPECT, 1);
            }
        }
        
        MobEnhancementData data = enhancementData.computeIfAbsent(skeleton.getUUID(), k -> new MobEnhancementData());
        data.meleeWeapon = sword;
        data.rangedWeapon = bow;
        
        // ドロップ率を0に
        skeleton.setDropChance(EquipmentSlot.MAINHAND, 0.0f);
        skeleton.setDropChance(EquipmentSlot.HEAD, 0.0f);
        skeleton.setDropChance(EquipmentSlot.CHEST, 0.0f);
        skeleton.setDropChance(EquipmentSlot.LEGS, 0.0f);
        skeleton.setDropChance(EquipmentSlot.FEET, 0.0f);
        skeleton.setDropChance(EquipmentSlot.OFFHAND, 0.0f);
        
        // ステータス強化（ティアに応じて）
        if (skeleton.getAttribute(Attributes.MAX_HEALTH) != null) {
            double health = tier == 0 ? 20.0 + random.nextInt(10) : (tier == 1 ? 30.0 + random.nextInt(10) : 40.0 + random.nextInt(20));
            skeleton.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
            skeleton.setHealth((float)health);
        }
        if (skeleton.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            double speed = 0.25 + (tier * 0.03) + (random.nextFloat() * 0.05);
            skeleton.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(speed);
        }
        if (skeleton.getAttribute(Attributes.FOLLOW_RANGE) != null) {
            skeleton.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(16.0 + tier * 4.0);
        }
        
        // 盾防御AIを追加（盾を持っている場合のみ）
        if (!skeleton.getOffhandItem().isEmpty() && skeleton.getOffhandItem().getItem() == Items.SHIELD) {
            skeleton.goalSelector.addGoal(6, new SkeletonShieldGoal(skeleton));
        }
    }
    
    private static void enhanceZombie(Zombie zombie) {
        java.util.Random random = zombie.getRandom();
        
        // ティアシステム（0: 通常, 1: 戦士, 2: バーサーカー）
        int tier = random.nextFloat() < 0.6f ? 0 : (random.nextFloat() < 0.75f ? 1 : 2);
        
        // 防具の設定（ティアとランダム性）
        if (tier == 0) {
            // 通常ゾンビ: 部分的な装備
            if (random.nextFloat() < 0.7f) {
                zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(
                    random.nextBoolean() ? Items.LEATHER_HELMET : Items.CHAINMAIL_HELMET
                ));
            }
            if (random.nextFloat() < 0.5f) {
                zombie.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
            }
            if (random.nextFloat() < 0.3f) {
                zombie.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.LEATHER_LEGGINGS));
            }
            if (random.nextFloat() < 0.4f) {
                zombie.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.LEATHER_BOOTS));
            }
        } else if (tier == 1) {
            // 戦士ゾンビ: チェインメイル主体
            zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(
                random.nextFloat() < 0.3f ? Items.IRON_HELMET : Items.CHAINMAIL_HELMET
            ));
            zombie.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.CHAINMAIL_CHESTPLATE));
            if (random.nextFloat() < 0.7f) {
                zombie.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.CHAINMAIL_LEGGINGS));
            }
            if (random.nextFloat() < 0.6f) {
                zombie.setItemSlot(EquipmentSlot.FEET, new ItemStack(
                    random.nextBoolean() ? Items.CHAINMAIL_BOOTS : Items.IRON_BOOTS
                ));
            }
        } else {
            // バーサーカーゾンビ: フル鉄装備
            ItemStack helmet = new ItemStack(random.nextFloat() < 0.2f ? Items.DIAMOND_HELMET : Items.IRON_HELMET);
            if (random.nextFloat() < 0.5f) {
                helmet.enchant(Enchantments.ALL_DAMAGE_PROTECTION, 1 + random.nextInt(2));
            }
            zombie.setItemSlot(EquipmentSlot.HEAD, helmet);
            
            ItemStack chestplate = new ItemStack(Items.IRON_CHESTPLATE);
            if (random.nextFloat() < 0.4f) {
                chestplate.enchant(Enchantments.THORNS, 1 + random.nextInt(2));
            }
            zombie.setItemSlot(EquipmentSlot.CHEST, chestplate);
            
            zombie.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
            zombie.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
        }
        
        // 武器の設定
        ItemStack weapon;
        if (tier == 0) {
            // 通常: 木〜鉄の武器
            if (random.nextFloat() < 0.3f) {
                weapon = ItemStack.EMPTY; // 素手
            } else if (random.nextFloat() < 0.5f) {
                weapon = new ItemStack(Items.WOODEN_SWORD);
            } else {
                weapon = new ItemStack(random.nextBoolean() ? Items.STONE_SWORD : Items.IRON_SWORD);
            }
        } else if (tier == 1) {
            // 戦士: 鉄武器中心
            if (random.nextFloat() < 0.3f) {
                // 斧使い
                weapon = new ItemStack(Items.IRON_AXE);
                weapon.enchant(Enchantments.SHARPNESS, 1);
            } else {
                weapon = new ItemStack(Items.IRON_SWORD);
                weapon.enchant(Enchantments.SHARPNESS, 1 + random.nextInt(2));
            }
        } else {
            // バーサーカー: 強力な武器
            if (random.nextFloat() < 0.2f) {
                // ダイヤ武器
                weapon = new ItemStack(random.nextBoolean() ? Items.DIAMOND_SWORD : Items.DIAMOND_AXE);
                weapon.enchant(Enchantments.SHARPNESS, 2 + random.nextInt(2));
            } else {
                // エンチャント付き鉄武器
                weapon = new ItemStack(random.nextFloat() < 0.4f ? Items.IRON_AXE : Items.IRON_SWORD);
                weapon.enchant(Enchantments.SHARPNESS, 2 + random.nextInt(2));
                if (random.nextFloat() < 0.3f) {
                    weapon.enchant(Enchantments.KNOCKBACK, 1);
                }
            }
        }
        
        if (!weapon.isEmpty()) {
            zombie.setItemSlot(EquipmentSlot.MAINHAND, weapon);
        }
        
        // 盾の設定（ティアが高いほど確率上昇）
        float shieldChance = tier == 0 ? 0.1f : (tier == 1 ? 0.4f : 0.7f);
        if (random.nextFloat() < shieldChance && !weapon.isEmpty()) {
            zombie.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        } else if (tier == 2 && random.nextFloat() < 0.2f) {
            // バーサーカーの一部は両手武器
            zombie.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.IRON_SWORD));
        }
        
        // ドロップ率を0に
        zombie.setDropChance(EquipmentSlot.HEAD, 0.0f);
        zombie.setDropChance(EquipmentSlot.CHEST, 0.0f);
        zombie.setDropChance(EquipmentSlot.LEGS, 0.0f);
        zombie.setDropChance(EquipmentSlot.FEET, 0.0f);
        zombie.setDropChance(EquipmentSlot.MAINHAND, 0.0f);
        zombie.setDropChance(EquipmentSlot.OFFHAND, 0.0f);
        
        // ステータス強化（ティアに応じて）
        if (zombie.getAttribute(Attributes.MAX_HEALTH) != null) {
            double health = tier == 0 ? 25.0 + random.nextInt(10) : (tier == 1 ? 35.0 + random.nextInt(15) : 50.0 + random.nextInt(20));
            zombie.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
            zombie.setHealth((float)health);
        }
        if (zombie.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            double speed = tier == 0 ? 0.23 : (tier == 1 ? 0.25 : 0.28);
            speed += random.nextFloat() * 0.03;
            zombie.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(speed);
        }
        if (zombie.getAttribute(Attributes.KNOCKBACK_RESISTANCE) != null) {
            zombie.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.2 + tier * 0.2);
        }
        if (zombie.getAttribute(Attributes.ARMOR) != null) {
            zombie.getAttribute(Attributes.ARMOR).setBaseValue(tier * 2.0);
        }
        if (zombie.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            zombie.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(3.0 + tier * 2.0);
        }
        
        // ドア破壊能力（ティアが高いほど確率上昇）
        zombie.setCanBreakDoors(tier > 0 || random.nextFloat() < 0.3f);
        
        // カスタムAIゴールを追加
        zombie.goalSelector.addGoal(1, new EnhancedZombieGoal(zombie));
        
        // 盾を持っている場合のみ盾AIを追加
        if (!zombie.getOffhandItem().isEmpty() && zombie.getOffhandItem().getItem() == Items.SHIELD) {
            zombie.goalSelector.addGoal(2, new ZombieShieldGoal(zombie));
        }
    }
    
    private static void enhanceSpider(Spider spider) {
        // ステータス強化
        if (spider.getAttribute(Attributes.MAX_HEALTH) != null) {
            spider.getAttribute(Attributes.MAX_HEALTH).setBaseValue(24.0);
            spider.setHealth(24.0f);
        }
        if (spider.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            spider.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.35);
        }
        if (spider.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            spider.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(5.0);
        }
        
        // カスタムAIゴールを追加
        spider.goalSelector.addGoal(1, new EnhancedSpiderGoal(spider));
    }
    
    private static void enhanceCreeper(Creeper creeper) {
        // ステータス強化
        if (creeper.getAttribute(Attributes.MAX_HEALTH) != null) {
            creeper.getAttribute(Attributes.MAX_HEALTH).setBaseValue(30.0);
            creeper.setHealth(30.0f);
        }
        if (creeper.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            creeper.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3);
        }
        if (creeper.getAttribute(Attributes.FOLLOW_RANGE) != null) {
            creeper.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(20.0);
        }
    }
    
    private static void enhanceWitch(Witch witch) {
        // ステータス強化
        if (witch.getAttribute(Attributes.MAX_HEALTH) != null) {
            witch.getAttribute(Attributes.MAX_HEALTH).setBaseValue(35.0);
            witch.setHealth(35.0f);
        }
        if (witch.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            witch.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3);
        }
    }
    
    // 毎ティック更新処理
    @SubscribeEvent
    public static void onLivingUpdate(LivingEvent.LivingTickEvent event) {
        if (!CustomDifficultyCommand.isTrueCrafterEnabled()) {
            return;
        }
        
        if (!(event.getEntity() instanceof Monster monster)) {
            return;
        }
        
        if (monster.level == null || monster.level.isClientSide) {
            return;
        }
        
        UUID entityId = monster.getUUID();
        if (!enhancedEntities.contains(entityId)) {
            return;
        }
        
        MobEnhancementData data = enhancementData.get(entityId);
        if (data == null) {
            return;
        }
        
        // クールダウンを減らす
        if (data.weaponSwitchCooldown > 0) {
            data.weaponSwitchCooldown--;
        }
        if (data.dodgeCooldown > 0) {
            data.dodgeCooldown--;
        }
        if (data.blockPlaceCooldown > 0) {
            data.blockPlaceCooldown--;
        }
        
        // スケルトンの武器切り替え処理（一時的に無効化 - デフォルトの弓攻撃を優先）
        /*
        if (monster instanceof Skeleton skeleton && data.weaponSwitchCooldown == 0) {
            LivingEntity target = skeleton.getTarget();
            if (target != null) {
                double distance = skeleton.distanceToSqr(target);
                ItemStack currentWeapon = skeleton.getMainHandItem();
                
                // 近距離なら剣に切り替え
                if (distance < 16.0 && !data.meleeWeapon.isEmpty() && !ItemStack.isSame(currentWeapon, data.meleeWeapon)) {
                    skeleton.setItemSlot(EquipmentSlot.MAINHAND, data.meleeWeapon.copy());
                    data.weaponSwitchCooldown = 40; // 2秒のクールダウン
                    
                    // 近接攻撃AIを追加
                    skeleton.goalSelector.addGoal(2, new MeleeAttackGoal(skeleton, 1.0D, false));
                }
                // 遠距離なら弓に切り替え
                else if (distance >= 16.0 && !data.rangedWeapon.isEmpty() && !ItemStack.isSame(currentWeapon, data.rangedWeapon)) {
                    skeleton.setItemSlot(EquipmentSlot.MAINHAND, data.rangedWeapon.copy());
                    data.weaponSwitchCooldown = 40;
                }
            }
        }
        */
        
        // 回避行動（全モンスター共通）
        if (data.dodgeCooldown == 0 && monster.getTarget() != null) {
            // 矢を検知して回避
            List<AbstractArrow> arrows = monster.level.getEntitiesOfClass(
                AbstractArrow.class, 
                monster.getBoundingBox().inflate(3.0),
                arrow -> arrow.getOwner() != monster && !arrow.isNoGravity()
            );
            
            if (!arrows.isEmpty()) {
                // 横に回避
                Vec3 dodgeVec = new Vec3(
                    monster.getRandom().nextGaussian() * 0.5,
                    0.2,
                    monster.getRandom().nextGaussian() * 0.5
                );
                monster.setDeltaMovement(monster.getDeltaMovement().add(dodgeVec));
                data.dodgeCooldown = 20; // 1秒のクールダウン
            }
        }
    }
    
    // カスタムAIゴール - スケルトンの盾防御（攻撃を妨げないように改良）
    private static class SkeletonShieldGoal extends Goal {
        private final Skeleton skeleton;
        private int blockingTime = 0;
        private int blockCooldown = 0;
        private boolean wasHurt = false;
        
        public SkeletonShieldGoal(Skeleton skeleton) {
            this.skeleton = skeleton;
            // 攻撃AIと競合しないようにフラグを設定しない
            this.setFlags(EnumSet.noneOf(Goal.Flag.class));
        }
        
        @Override
        public boolean canUse() {
            // 盾を持っていて、ターゲットがいて、弓を使用中でない場合のみ
            return !this.skeleton.getOffhandItem().isEmpty() 
                && this.skeleton.getOffhandItem().getItem() == Items.SHIELD 
                && this.skeleton.getTarget() != null
                && !this.skeleton.isUsingItem(); // 弓を使用中は盾を使わない
        }
        
        @Override
        public void start() {
            blockingTime = 0;
            wasHurt = false;
        }
        
        @Override
        public void tick() {
            // 弓を使用中なら盾は使わない
            if (this.skeleton.getMainHandItem().getItem() == Items.BOW && this.skeleton.isUsingItem()) {
                return;
            }
            
            if (blockCooldown > 0) {
                blockCooldown--;
                return;
            }
            
            LivingEntity target = this.skeleton.getTarget();
            if (target == null) {
                return;
            }
            
            // ダメージを受けたかチェック
            if (this.skeleton.getLastHurtByMob() != null && this.skeleton.getLastHurtByMobTimestamp() + 20 > this.skeleton.level.getGameTime()) {
                wasHurt = true;
            }
            
            double distance = this.skeleton.distanceToSqr(target);
            
            // 非常に近距離（3ブロック以内）またはダメージを受けた後のみ防御
            if ((distance < 9.0 || wasHurt) && distance < 25.0) {
                // ターゲットがこちらを向いている場合
                Vec3 targetLook = target.getLookAngle();
                Vec3 toSkeleton = this.skeleton.position().subtract(target.position()).normalize();
                double dot = targetLook.dot(toSkeleton);
                
                // 非常に近いか、ダメージを受けた後で相手がこちらを向いている
                if (distance < 4.0 || (wasHurt && dot > 0.3)) {
                    // 盾を構える（オフハンドのみ）
                    this.skeleton.startUsingItem(net.minecraft.world.InteractionHand.OFF_HAND);
                    blockingTime++;
                    
                    // 0.5秒（10tick）防御したら一旦解除
                    if (blockingTime > 10) {
                        this.skeleton.stopUsingItem();
                        blockCooldown = 60; // 3秒のクールダウン
                        blockingTime = 0;
                        wasHurt = false;
                    }
                } else {
                    // 条件を満たさない場合は盾を下げる
                    this.skeleton.stopUsingItem();
                    blockingTime = 0;
                }
            } else {
                // 遠距離では盾を使わない
                blockingTime = 0;
            }
        }
        
        @Override
        public void stop() {
            this.skeleton.stopUsingItem();
            blockingTime = 0;
            wasHurt = false;
        }
        
        @Override
        public boolean canContinueToUse() {
            // 弓を使用中なら継続しない
            if (this.skeleton.getMainHandItem().getItem() == Items.BOW && this.skeleton.isUsingItem()) {
                return false;
            }
            return this.skeleton.getTarget() != null;
        }
    }
    
    // カスタムAIゴール - ゾンビ用
    private static class EnhancedZombieGoal extends Goal {
        private final Zombie zombie;
        private int leapCooldown = 0;
        
        public EnhancedZombieGoal(Zombie zombie) {
            this.zombie = zombie;
            this.setFlags(EnumSet.of(Goal.Flag.JUMP));
        }
        
        @Override
        public boolean canUse() {
            return this.zombie.getTarget() != null;
        }
        
        @Override
        public void tick() {
            if (leapCooldown > 0) {
                leapCooldown--;
                return;
            }
            
            LivingEntity target = this.zombie.getTarget();
            if (target == null) {
                return;
            }
            
            double distance = this.zombie.distanceToSqr(target);
            
            // リープアタック（距離が離れている時）
            if (distance > 9.0 && distance < 36.0 && this.zombie.isOnGround()) {
                Vec3 leapVec = target.position().subtract(this.zombie.position()).normalize();
                this.zombie.setDeltaMovement(
                    leapVec.x * 0.8, 
                    0.4, 
                    leapVec.z * 0.8
                );
                leapCooldown = 60; // 3秒のクールダウン
            }
        }
    }
    
    // カスタムAIゴール - ゾンビの盾防御
    private static class ZombieShieldGoal extends Goal {
        private final Zombie zombie;
        private int blockingTime = 0;
        private int blockCooldown = 0;
        
        public ZombieShieldGoal(Zombie zombie) {
            this.zombie = zombie;
            this.setFlags(EnumSet.noneOf(Goal.Flag.class));
        }
        
        @Override
        public boolean canUse() {
            return !this.zombie.getOffhandItem().isEmpty() 
                && this.zombie.getOffhandItem().getItem() == Items.SHIELD 
                && this.zombie.getTarget() != null;
        }
        
        @Override
        public void tick() {
            if (blockCooldown > 0) {
                blockCooldown--;
                this.zombie.stopUsingItem();
                return;
            }
            
            LivingEntity target = this.zombie.getTarget();
            if (target == null) {
                this.zombie.stopUsingItem();
                return;
            }
            
            double distance = this.zombie.distanceToSqr(target);
            
            // 近距離で防御
            if (distance < 16.0) {
                Vec3 targetLook = target.getLookAngle();
                Vec3 toZombie = this.zombie.position().subtract(target.position()).normalize();
                double dot = targetLook.dot(toZombie);
                
                if (dot > 0.3 || distance < 4.0) {
                    this.zombie.startUsingItem(net.minecraft.world.InteractionHand.OFF_HAND);
                    blockingTime++;
                    
                    if (blockingTime > 30) {
                        this.zombie.stopUsingItem();
                        blockCooldown = 30;
                        blockingTime = 0;
                    }
                } else {
                    this.zombie.stopUsingItem();
                    blockingTime = 0;
                }
            } else {
                this.zombie.stopUsingItem();
                blockingTime = 0;
            }
        }
        
        @Override
        public void stop() {
            this.zombie.stopUsingItem();
            blockingTime = 0;
        }
    }
    
    // カスタムAIゴール - クモ用
    private static class EnhancedSpiderGoal extends Goal {
        private final Spider spider;
        private int webCooldown = 0;
        
        public EnhancedSpiderGoal(Spider spider) {
            this.spider = spider;
        }
        
        @Override
        public boolean canUse() {
            return this.spider.getTarget() != null;
        }
        
        @Override
        public void tick() {
            if (webCooldown > 0) {
                webCooldown--;
                return;
            }
            
            LivingEntity target = this.spider.getTarget();
            if (target == null) {
                return;
            }
            
            double distance = this.spider.distanceToSqr(target);
            
            // ウェブ設置（プレイヤーの足元）
            if (distance < 64.0 && this.spider.getRandom().nextInt(100) == 0) {
                BlockPos targetPos = target.blockPosition();
                if (this.spider.level.getBlockState(targetPos).isAir()) {
                    this.spider.level.setBlock(targetPos, Blocks.COBWEB.defaultBlockState(), 3);
                    webCooldown = 100; // 5秒のクールダウン
                }
            }
        }
    }
    
    // エンティティが死亡した時のクリーンアップ
    @SubscribeEvent
    public static void onEntityDeath(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        if (event.getEntity() instanceof Monster) {
            UUID entityId = event.getEntity().getUUID();
            enhancedEntities.remove(entityId);
            enhancementData.remove(entityId);
        }
    }
}
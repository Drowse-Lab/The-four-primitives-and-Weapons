package minecraftarmorweapon.ai;

import minecraftarmorweapon.util.VersionHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import minecraftarmorweapon.command.CustomDifficultyCommand;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 安全なTrue Crafter AI実装
 * AIゴールを追加せず、イベントベースですべて処理
 */
// TrueCrafterOverrideAIを使用（より安全な実装）
//@Mod.EventBusSubscriber
public class TrueCrafterSafeAI {
    
    // エンティティごとの状態管理
    private static final Map<UUID, MobState> mobStates = new ConcurrentHashMap<>();
    private static final Map<BlockPos, Long> temporaryBlocks = new ConcurrentHashMap<>();
    private static final long BLOCK_DECAY_TIME = 15000; // 15秒
    
    private static class MobState {
        ItemStack savedBow = ItemStack.EMPTY;
        ItemStack savedSword = ItemStack.EMPTY;
        int weaponSwitchCooldown = 0;
        int blockPlaceCooldown = 0;
        int dodgeCooldown = 0;
        int leapCooldown = 0;
        boolean initialized = false;
    }
    
    // スポーン時の強化
    @SubscribeEvent
    public static void onEntitySpawn(LivingSpawnEvent.SpecialSpawn event) {
        if (!CustomDifficultyCommand.isTrueCrafterEnabled()) {
            return;
        }
        
        if (!(event.getEntity() instanceof Monster monster)) {
            return;
        }
        
        if (VersionHelper.getLevel(monster) == null || monster.level.isClientSide) {
            return;
        }
        
        try {
            // 装備とステータスの強化
            if (monster instanceof Zombie zombie) {
                setupZombie(zombie);
            } else if (monster instanceof Skeleton skeleton) {
                setupSkeleton(skeleton);
            } else if (monster instanceof Spider spider) {
                setupSpider(spider);
            } else if (monster instanceof Creeper creeper) {
                setupCreeper(creeper);
            }
        } catch (Exception e) {
            // エラーを無視
        }
    }
    
    // 毎ティックの処理
    @SubscribeEvent
    public static void onLivingUpdate(LivingEvent.LivingTickEvent event) {
        if (!CustomDifficultyCommand.isTrueCrafterEnabled()) {
            return;
        }
        
        if (!(event.getEntity() instanceof Monster monster)) {
            return;
        }
        
        if (VersionHelper.getLevel(monster) == null || monster.level.isClientSide) {
            return;
        }
        
        // 状態を取得または作成
        UUID id = monster.getUUID();
        MobState state = mobStates.computeIfAbsent(id, k -> new MobState());
        
        try {
            // モンスタータイプ別の処理
            if (monster instanceof Zombie zombie) {
                processZombie(zombie, state);
            } else if (monster instanceof Skeleton skeleton) {
                processSkeleton(skeleton, state);
            } else if (monster instanceof Spider spider) {
                processSpider(spider, state);
            }
            
            // クールダウンを減らす
            if (state.weaponSwitchCooldown > 0) state.weaponSwitchCooldown--;
            if (state.blockPlaceCooldown > 0) state.blockPlaceCooldown--;
            if (state.dodgeCooldown > 0) state.dodgeCooldown--;
            if (state.leapCooldown > 0) state.leapCooldown--;
            
        } catch (Exception e) {
            // エラーを無視
        }
    }
    
    // ゾンビの初期設定
    private static void setupZombie(Zombie zombie) {
        // フル鉄装備
        zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        zombie.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        zombie.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
        zombie.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
        zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        zombie.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        
        // ドロップ率0
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
    }
    
    // スケルトンの初期設定
    private static void setupSkeleton(Skeleton skeleton) {
        // エンチャント弓
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
    }
    
    // クモの初期設定
    private static void setupSpider(Spider spider) {
        if (spider.getAttribute(Attributes.MAX_HEALTH) != null) {
            spider.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20.0);
            spider.setHealth(20.0f);
        }
        if (spider.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            spider.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.35);
        }
    }
    
    // クリーパーの初期設定
    private static void setupCreeper(Creeper creeper) {
        if (creeper.getAttribute(Attributes.MAX_HEALTH) != null) {
            creeper.getAttribute(Attributes.MAX_HEALTH).setBaseValue(25.0);
            creeper.setHealth(25.0f);
        }
        if (creeper.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            creeper.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.28);
        }
    }
    
    // ゾンビの処理
    private static void processZombie(Zombie zombie, MobState state) {
        LivingEntity target = zombie.getTarget();
        if (target == null) return;
        
        // 武器の初期化
        if (!state.initialized) {
            state.savedSword = new ItemStack(Items.IRON_SWORD);
            state.savedBow = new ItemStack(Items.BOW);
            state.initialized = true;
        }
        
        // 武器切り替え
        if (state.weaponSwitchCooldown <= 0) {
            double distance = zombie.distanceTo(target);
            ItemStack current = zombie.getItemBySlot(EquipmentSlot.MAINHAND);
            
            if (distance > 8 && !current.is(Items.BOW)) {
                zombie.setItemSlot(EquipmentSlot.MAINHAND, state.savedBow.copy());
                state.weaponSwitchCooldown = 40;
            } else if (distance < 5 && !current.is(Items.IRON_SWORD)) {
                zombie.setItemSlot(EquipmentSlot.MAINHAND, state.savedSword.copy());
                state.weaponSwitchCooldown = 40;
            }
        }
        
        // 飛びかかり攻撃
        if (state.leapCooldown <= 0 && zombie.isOnGround()) {
            double distance = zombie.distanceTo(target);
            if (distance > 3 && distance < 8) {
                Vec3 direction = target.position().subtract(zombie.position()).normalize();
                zombie.setDeltaMovement(direction.x * 1.2, 0.5, direction.z * 1.2);
                zombie.hasImpulse = true;
                state.leapCooldown = 60;
            }
        }
        
        // ブロック設置（登る）
        if (state.blockPlaceCooldown <= 0) {
            if (target.getY() > zombie.getY() + 2) {
                BlockPos pos = zombie.blockPosition();
                BlockPos placePos = pos.relative(zombie.getDirection());
                
                if (zombie.level.getBlockState(placePos).isAir() && 
                    zombie.level.getBlockState(placePos.below()).getMaterial().isSolid()) {
                    zombie.level.setBlock(placePos, Blocks.COBBLESTONE.defaultBlockState(), 3);
                    temporaryBlocks.put(placePos, System.currentTimeMillis());
                    state.blockPlaceCooldown = 30;
                }
            }
        }
    }
    
    // スケルトンの処理
    private static void processSkeleton(Skeleton skeleton, MobState state) {
        LivingEntity target = skeleton.getTarget();
        if (target == null) return;
        
        // 武器の初期化
        if (!state.initialized) {
            ItemStack bow = new ItemStack(Items.BOW);
            bow.enchant(Enchantments.POWER_ARROWS, 2);
            state.savedBow = bow;
            state.savedSword = new ItemStack(Items.IRON_SWORD);
            state.initialized = true;
        }
        
        // 武器切り替え（スケルトン用の特別な処理）
        if (state.weaponSwitchCooldown <= 0) {
            double distance = skeleton.distanceTo(target);
            ItemStack current = skeleton.getItemBySlot(EquipmentSlot.MAINHAND);
            
            if (distance > 6 && !current.is(Items.BOW)) {
                skeleton.setItemSlot(EquipmentSlot.MAINHAND, state.savedBow.copy());
                state.weaponSwitchCooldown = 40;
                
                // 近接攻撃をキャンセル
                skeleton.setAggressive(false);
                
            } else if (distance < 4 && !current.is(Items.IRON_SWORD)) {
                skeleton.setItemSlot(EquipmentSlot.MAINHAND, state.savedSword.copy());
                state.weaponSwitchCooldown = 40;
                
                // 攻撃的にする
                skeleton.setAggressive(true);
            }
        }
        
        // 回避移動
        if (state.dodgeCooldown <= 0 && target instanceof Player) {
            double distance = skeleton.distanceTo(target);
            if (distance < 5) {
                // 後退
                Vec3 away = skeleton.position().subtract(target.position()).normalize();
                skeleton.setDeltaMovement(away.x * 0.5, skeleton.getDeltaMovement().y, away.z * 0.5);
                state.dodgeCooldown = 20;
            }
        }
    }
    
    // クモの処理
    private static void processSpider(Spider spider, MobState state) {
        LivingEntity target = spider.getTarget();
        if (target == null) return;
        
        // 飛びかかり
        if (state.leapCooldown <= 0) {
            double distance = spider.distanceTo(target);
            if (distance > 3 && distance < 7 && spider.getRandom().nextInt(10) == 0) {
                Vec3 direction = target.position().subtract(spider.position()).normalize();
                spider.setDeltaMovement(direction.x * 1.0, 0.4, direction.z * 1.0);
                spider.hasImpulse = true;
                state.leapCooldown = 40;
            }
        }
        
        // クモの巣設置
        if (state.blockPlaceCooldown <= 0) {
            double distance = spider.distanceTo(target);
            if (distance < 4) {
                BlockPos targetPos = target.blockPosition();
                if (spider.level.getBlockState(targetPos).isAir()) {
                    spider.level.setBlock(targetPos, Blocks.COBWEB.defaultBlockState(), 3);
                    temporaryBlocks.put(targetPos, System.currentTimeMillis());
                    state.blockPlaceCooldown = 100;
                }
            }
        }
    }
    
    // 一時ブロックの削除
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        
        // 20ティックごとに処理
        if (event.getServer().getTickCount() % 20 == 0) {
            long currentTime = System.currentTimeMillis();
            Iterator<Map.Entry<BlockPos, Long>> iterator = temporaryBlocks.entrySet().iterator();
            
            while (iterator.hasNext()) {
                Map.Entry<BlockPos, Long> entry = iterator.next();
                
                // 時間経過でブロックを削除
                if (currentTime - entry.getValue() > BLOCK_DECAY_TIME) {
                    event.getServer().getAllLevels().forEach(level -> {
                        BlockState state = level.getBlockState(entry.getKey());
                        if (state.is(Blocks.COBBLESTONE) || state.is(Blocks.COBWEB)) {
                            level.setBlock(entry.getKey(), Blocks.AIR.defaultBlockState(), 3);
                        }
                    });
                    iterator.remove();
                }
            }
        }
        
        // メモリクリーンアップ（5分ごと）
        if (event.getServer().getTickCount() % 6000 == 0) {
            // 死んだエンティティの状態を削除
            mobStates.entrySet().removeIf(entry -> {
                // 古いエントリを削除（サイズが大きくなりすぎた場合）
                return mobStates.size() > 100;
            });
        }
    }
}
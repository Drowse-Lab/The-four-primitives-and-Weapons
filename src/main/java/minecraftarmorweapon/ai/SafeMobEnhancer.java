package minecraftarmorweapon.ai;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import minecraftarmorweapon.command.CustomDifficultyCommand;

/**
 * 最も安全なモブ強化システム
 * AIの変更は一切行わず、装備とステータスのみを変更
 */
// 無効化（TrueCrafterSafeAIを使用）
//@Mod.EventBusSubscriber
public class SafeMobEnhancer {
    
    // 強化済みのエンティティを記憶（重複強化を防ぐ）
    private static final java.util.Set<java.util.UUID> enhancedEntities = java.util.Collections.newSetFromMap(
        new java.util.WeakHashMap<>()
    );
    
    @SubscribeEvent
    public static void onEntitySpawn(LivingSpawnEvent.SpecialSpawn event) {
        // True Crafterモードが無効なら何もしない
        if (!CustomDifficultyCommand.isTrueCrafterEnabled()) {
            return;
        }
        
        // nullチェック
        if (event == null || event.getEntity() == null) {
            return;
        }
        
        // モンスターでなければ無視
        if (!(event.getEntity() instanceof Monster monster)) {
            return;
        }
        
        // すでに強化済みなら無視
        if (enhancedEntities.contains(monster.getUUID())) {
            return;
        }
        
        // レベルがnullなら無視
        if (monster.level() == null || monster.level().isClientSide) {
            return;
        }
        
        try {
            // 強化を実施
            if (monster instanceof Zombie zombie) {
                enhanceZombie(zombie);
                enhancedEntities.add(zombie.getUUID());
            } else if (monster instanceof Skeleton skeleton) {
                enhanceSkeleton(skeleton);
                enhancedEntities.add(skeleton.getUUID());
            } else if (monster instanceof Creeper creeper) {
                enhanceCreeper(creeper);
                enhancedEntities.add(creeper.getUUID());
            } else if (monster instanceof Spider spider) {
                enhanceSpider(spider);
                enhancedEntities.add(spider.getUUID());
            }
        } catch (Exception e) {
            // エラーが発生してもクラッシュしない
        }
    }
    
    private static void enhanceZombie(Zombie zombie) {
        try {
            // 装備を設定
            zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            zombie.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
            zombie.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
            zombie.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
            zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            
            // ドロップ率を0に
            zombie.setDropChance(EquipmentSlot.HEAD, 0.0f);
            zombie.setDropChance(EquipmentSlot.CHEST, 0.0f);
            zombie.setDropChance(EquipmentSlot.LEGS, 0.0f);
            zombie.setDropChance(EquipmentSlot.FEET, 0.0f);
            zombie.setDropChance(EquipmentSlot.MAINHAND, 0.0f);
            
            // ステータス強化（安全にチェック）
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
        } catch (Exception e) {
            // 無視
        }
    }
    
    private static void enhanceSkeleton(Skeleton skeleton) {
        try {
            // 弓にエンチャント
            ItemStack bow = new ItemStack(Items.BOW);
            bow.enchant(Enchantments.POWER_ARROWS, 2);
            skeleton.setItemSlot(EquipmentSlot.MAINHAND, bow);
            skeleton.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CHAINMAIL_HELMET));
            
            skeleton.setDropChance(EquipmentSlot.MAINHAND, 0.0f);
            skeleton.setDropChance(EquipmentSlot.HEAD, 0.0f);
            
            // ステータス強化
            if (skeleton.getAttribute(Attributes.MAX_HEALTH) != null) {
                skeleton.getAttribute(Attributes.MAX_HEALTH).setBaseValue(25.0);
                skeleton.setHealth(25.0f);
            }
        } catch (Exception e) {
            // 無視
        }
    }
    
    private static void enhanceCreeper(Creeper creeper) {
        try {
            // ステータス強化
            if (creeper.getAttribute(Attributes.MAX_HEALTH) != null) {
                creeper.getAttribute(Attributes.MAX_HEALTH).setBaseValue(25.0);
                creeper.setHealth(25.0f);
            }
            if (creeper.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
                creeper.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.28);
            }
        } catch (Exception e) {
            // 無視
        }
    }
    
    private static void enhanceSpider(Spider spider) {
        try {
            // ステータス強化
            if (spider.getAttribute(Attributes.MAX_HEALTH) != null) {
                spider.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20.0);
                spider.setHealth(20.0f);
            }
            if (spider.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
                spider.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.35);
            }
        } catch (Exception e) {
            // 無視
        }
    }
}
package minecraftarmorweapon.ai;

import minecraftarmorweapon.util.VersionHelper;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import minecraftarmorweapon.command.CustomDifficultyCommand;

/**
 * シンプルなモブ強化システム
 * AIゴールを追加せず、ステータスと装備のみを強化
 */
// 無効化（SafeMobEnhancerを使用）
//@Mod.EventBusSubscriber
public class SimpleMobEnhancer {
    
    @SubscribeEvent
    public static void onEntitySpawn(MobSpawnEvent.FinalizeSpawn event) {
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
        
        // レベルがnullなら無視
        if (VersionHelper.getLevel(monster) == null) {
            return;
        }
        
        try {
            // モンスターの種類に応じて装備とステータスを強化
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
        } catch (Exception e) {
            // エラーが発生してもクラッシュしない
            System.err.println("Error enhancing mob: " + e.getMessage());
        }
    }
    
    private static void enhanceZombie(Zombie zombie) {
        // 装備を設定（ドロップ率0）
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
    }
    
    private static void enhanceSkeleton(Skeleton skeleton) {
        // 弓にエンチャント（WeaponSwitchHandlerが武器を管理するので、初期装備のみ）
        ItemStack bow = new ItemStack(Items.BOW);
        bow.enchant(Enchantments.POWER_ARROWS, 2);
        skeleton.setItemSlot(EquipmentSlot.MAINHAND, bow);
        skeleton.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CHAINMAIL_HELMET));
        
        // オフハンドに盾を追加
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
    
    private static void enhanceCreeper(Creeper creeper) {
        // ステータス強化
        if (creeper.getAttribute(Attributes.MAX_HEALTH) != null) {
            creeper.getAttribute(Attributes.MAX_HEALTH).setBaseValue(25.0);
            creeper.setHealth(25.0f);
        }
        if (creeper.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            creeper.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.28);
        }
    }
    
    private static void enhanceSpider(Spider spider) {
        // ステータス強化
        if (spider.getAttribute(Attributes.MAX_HEALTH) != null) {
            spider.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20.0);
            spider.setHealth(20.0f);
        }
        if (spider.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            spider.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.35);
        }
        if (spider.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            spider.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(4.0);
        }
    }
    
    private static void enhanceEnderman(EnderMan enderman) {
        // ステータス強化
        if (enderman.getAttribute(Attributes.MAX_HEALTH) != null) {
            enderman.getAttribute(Attributes.MAX_HEALTH).setBaseValue(50.0);
            enderman.setHealth(50.0f);
        }
        if (enderman.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            enderman.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(10.0);
        }
    }
}
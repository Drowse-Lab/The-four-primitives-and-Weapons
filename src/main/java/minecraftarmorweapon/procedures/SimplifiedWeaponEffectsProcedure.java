package minecraftarmorweapon.procedures;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;

import minecraftarmorweapon.init.MinecraftArmorWeaponModMobEffects;
import minecraftarmorweapon.init.MinecraftArmorWeaponModItems;
import minecraftarmorweapon.init.MinecraftArmorWeaponModEnchantments;

/**
 * 武器の特殊効果を処理する簡略化されたクラス
 * Killエンチャントの処理は KillEnchantmentHandler に移動
 */
@Mod.EventBusSubscriber
public class SimplifiedWeaponEffectsProcedure {
    
    @SubscribeEvent
    public static void onEntityAttacked(LivingAttackEvent event) {
        if (event == null || event.getEntity() == null)
            return;
            
        LevelAccessor world = event.getEntity().level();
        Entity target = event.getEntity();
        Entity attacker = event.getSource().getEntity();
        
        if (world.isClientSide() || attacker == null)
            return;
            
        if (!(attacker instanceof LivingEntity livingAttacker))
            return;
            
        ItemStack weapon = livingAttacker.getMainHandItem();
        
        // Wither Katana効果
        if (weapon.getItem() == MinecraftArmorWeaponModItems.WITHER_KATANA.get()) {
            applyWitherEffect(target);
        }
        
        // Rivers of Blood効果
        if (weapon.getItem() == MinecraftArmorWeaponModItems.RIVERS_OF_BLOOD.get()) {
            applyBloodEffect(world, target, livingAttacker);
        }
        
        // Demonized エンチャント効果（体力回復）
        int demonizedLevel = EnchantmentHelper.getItemEnchantmentLevel(
            MinecraftArmorWeaponModEnchantments.DEMONIZED.get(), weapon);
        if (demonizedLevel > 0) {
            applyDemonizedEffect(livingAttacker, demonizedLevel);
        }
        
        // エフェクトベースの追加効果
        applyStatusEffects(world, target, livingAttacker);
    }
    
    private static void applyWitherEffect(Entity target) {
        if (target instanceof LivingEntity livingTarget) {
            livingTarget.addEffect(new MobEffectInstance(MobEffects.WITHER, 120, 2, false, true));
        }
    }
    
    private static void applyBloodEffect(LevelAccessor world, Entity target, LivingEntity attacker) {
        // 攻撃者にDevour Blood効果
        attacker.addEffect(new MobEffectInstance(
            MinecraftArmorWeaponModMobEffects.DEVOUR_BLOOD.get(), 1, 1, true, false));
        
        // 血のパーティクル
        if (world instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                ParticleTypes.FALLING_LAVA,
                target.getX(), target.getY() + 1, target.getZ(),
                20, 0.3, 0.5, 0.3, 0.1
            );
        }
    }
    
    private static void applyDemonizedEffect(LivingEntity attacker, int level) {
        // 体力回復
        attacker.heal(level);
        
        // プレイヤーの場合は満腹度も回復
        if (attacker instanceof Player player) {
            player.getFoodData().eat(level, level * 0.5f);
        }
    }
    
    private static void applyStatusEffects(LevelAccessor world, Entity target, LivingEntity attacker) {
        // Sword of Night効果
        if (attacker.hasEffect(MinecraftArmorWeaponModMobEffects.SWORD_OF_NIGHT_EFFECT.get())) {
            if (world instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                    ParticleTypes.CRIT,
                    target.getX(), target.getY() + 1, target.getZ(),
                    12, 0, 0, 0, 0.5
                );
                serverLevel.sendParticles(
                    ParticleTypes.END_ROD,
                    target.getX(), target.getY() + 1, target.getZ(),
                    12, 0, 0, 0, 0.1
                );
            }
        }
        
        // その他のステータス効果
        if (target instanceof LivingEntity livingTarget) {
            // Bubbleshot効果 → 窒息
            if (attacker.hasEffect(MinecraftArmorWeaponModMobEffects.BUBBLESHOT_EFFECT.get())) {
                livingTarget.addEffect(new MobEffectInstance(
                    MinecraftArmorWeaponModMobEffects.TISSOKU.get(), 120, 6, true, false));
            }
            
            // Fireball効果 → 炎上
            if (attacker.hasEffect(MinecraftArmorWeaponModMobEffects.FIREBALLEFFECT.get())) {
                target.setSecondsOnFire(15);
            }
            
            // Thunderbolt効果 → 雷撃
            if (attacker.hasEffect(MinecraftArmorWeaponModMobEffects.TUNDERBOLTEFFRCT.get())) {
                livingTarget.addEffect(new MobEffectInstance(
                    MinecraftArmorWeaponModMobEffects.THUNDER_HIT.get(), 120, 6, true, false));
            }
            
            // Storm効果 → 雷撃＋窒息
            if (attacker.hasEffect(MinecraftArmorWeaponModMobEffects.STORM_EFFECT.get())) {
                livingTarget.addEffect(new MobEffectInstance(
                    MinecraftArmorWeaponModMobEffects.THUNDER_HIT.get(), 120, 6, true, false));
                livingTarget.addEffect(new MobEffectInstance(
                    MinecraftArmorWeaponModMobEffects.TISSOKU.get(), 120, 6, true, false));
            }
        }
    }
}
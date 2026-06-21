package the_four_primitives_and_weapons.procedures;

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

import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModMobEffects;
import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModItems;
import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModEnchantments;

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
        if (weapon.getItem() == TheFourPrimitivesAndWeaponsModItems.WITHER_KATANA.get()) {
            applyWitherEffect(target);
        }
        
        // Rivers of Blood効果
        if (weapon.getItem() == TheFourPrimitivesAndWeaponsModItems.RIVERS_OF_BLOOD.get()) {
            applyBloodEffect(world, target, livingAttacker);
        }
        
        // Demonized エンチャント効果（体力回復）
        int demonizedLevel = EnchantmentHelper.getItemEnchantmentLevel(
            TheFourPrimitivesAndWeaponsModEnchantments.DEMONIZED.get(), weapon);
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
            TheFourPrimitivesAndWeaponsModMobEffects.DEVOUR_BLOOD.get(), 1, 1, true, false));
        
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
        if (attacker.hasEffect(TheFourPrimitivesAndWeaponsModMobEffects.SWORD_OF_NIGHT_EFFECT.get())) {
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
            if (attacker.hasEffect(TheFourPrimitivesAndWeaponsModMobEffects.BUBBLESHOT_EFFECT.get())) {
                applyIfNotFresh(livingTarget, TheFourPrimitivesAndWeaponsModMobEffects.TISSOKU.get(), 120, 6);
            }

            // Fireball効果 → 炎上
            if (attacker.hasEffect(TheFourPrimitivesAndWeaponsModMobEffects.FIREBALLEFFECT.get())) {
                target.setSecondsOnFire(15);
            }

            // Thunderbolt効果 → 雷撃 (+ kurikara 系装備時は通電 AOE)
            if (attacker.hasEffect(TheFourPrimitivesAndWeaponsModMobEffects.TUNDERBOLTEFFRCT.get())) {
                applyIfNotFresh(livingTarget, TheFourPrimitivesAndWeaponsModMobEffects.THUNDER_HIT.get(), 120, 6);
                // kurikara 系装備時: 周囲に通電 AOE (ダメージ + THUNDER_HIT 伝搬)
                if (hasKurikaraWeapon(attacker)) {
                    applyConductionAoe(world, livingTarget, attacker, 5.0, 4.0f);
                }
            }

            // Storm効果 → 雷撃＋窒息
            if (attacker.hasEffect(TheFourPrimitivesAndWeaponsModMobEffects.STORM_EFFECT.get())) {
                applyIfNotFresh(livingTarget, TheFourPrimitivesAndWeaponsModMobEffects.THUNDER_HIT.get(), 120, 6);
                applyIfNotFresh(livingTarget, TheFourPrimitivesAndWeaponsModMobEffects.TISSOKU.get(), 120, 6);
            }
        }
    }

    /** 攻撃者が kurikara 系武器を持っているか (main または off hand) */
    private static boolean hasKurikaraWeapon(LivingEntity attacker) {
        return isKurikara(attacker.getMainHandItem()) || isKurikara(attacker.getOffhandItem());
    }
    private static boolean isKurikara(ItemStack s) {
        if (s.isEmpty()) return false;
        return s.getItem() == TheFourPrimitivesAndWeaponsModItems.KURIKARAKEN.get()
            || s.getItem() == TheFourPrimitivesAndWeaponsModItems.KURIKARAKENSWORD.get()
            || s.getItem() == TheFourPrimitivesAndWeaponsModItems.KURIKARAKENUTIGATANA.get()
            || s.getItem() == TheFourPrimitivesAndWeaponsModItems.KAMINARI_KURIKARAKEN_SWORD.get()
            || s.getItem() == TheFourPrimitivesAndWeaponsModItems.KAMINARI_KURIKARAKEN_UTIGATANA.get()
            || s.getItem() == TheFourPrimitivesAndWeaponsModItems.KAMINARI_KURIKARAKEN_TYOKUTOU.get();
    }

    /**
     * 通電 AOE: target を中心に半径 r の範囲で全 LivingEntity に
     * lightning ダメージ + THUNDER_HIT を伝搬。attacker と target は除外。
     * 視覚効果として ELECTRIC_SPARK パーティクルも周囲に発射。
     */
    private static void applyConductionAoe(LevelAccessor world, LivingEntity target,
                                           LivingEntity attacker, double radius, float damage) {
        if (!(world instanceof ServerLevel serverLevel)) return;
        // 周囲 entity を集めて damage + effect を伝搬
        net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(
            target.getX() - radius, target.getY() - radius, target.getZ() - radius,
            target.getX() + radius, target.getY() + radius, target.getZ() + radius);
        for (LivingEntity le : serverLevel.getEntitiesOfClass(LivingEntity.class, box)) {
            if (le == attacker || le == target) continue;
            if (le.distanceToSqr(target) > radius * radius) continue;
            le.hurt(le.damageSources().lightningBolt(), damage);
            applyIfNotFresh(le, TheFourPrimitivesAndWeaponsModMobEffects.THUNDER_HIT.get(), 120, 6);
        }
        // 視覚: ELECTRIC_SPARK を周囲にばらまく
        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
            target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
            40, radius * 0.5, radius * 0.3, radius * 0.5, 0.3);
    }

    /**
     * 既に同じ effect が「十分な残り duration」で付与済みの場合は再付与しない。
     * 連続攻撃で duration が毎 hit リセットされて永続化する問題への対処。
     * 残り duration が REFRESH_THRESHOLD (40 tick = 2秒) 以下なら refresh する。
     */
    private static final int REFRESH_THRESHOLD = 40;
    private static void applyIfNotFresh(LivingEntity target,
                                        net.minecraft.world.effect.MobEffect effect,
                                        int duration, int amplifier) {
        MobEffectInstance existing = target.getEffect(effect);
        if (existing != null
                && existing.getAmplifier() >= amplifier
                && existing.getDuration() > REFRESH_THRESHOLD) {
            return; // 既に強力な効果が残っているので追加しない (永続化防止)
        }
        target.addEffect(new MobEffectInstance(effect, duration, amplifier, true, false));
    }
}
package minecraftarmorweapon.events;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import minecraftarmorweapon.init.MinecraftArmorWeaponModEnchantments;

/**
 * Demonizedエンチャントの効果を処理するハンドラー
 * - 攻撃した相手の体力を吸収
 * - エンチャントレベルによって吸収量が増加
 * - 攻撃者のスピードが一時的に上昇
 */
@Mod.EventBusSubscriber
public class DemonizedEnchantmentHandler {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        // ダメージソースが存在し、攻撃者がプレイヤーまたはLivingEntityの場合
        if (event.getSource().getEntity() instanceof LivingEntity attacker) {
            // 攻撃者が持っている武器を取得
            ItemStack weapon = attacker.getMainHandItem();

            // Demonizedエンチャントのレベルを取得
            int demonizedLevel = EnchantmentHelper.getItemEnchantmentLevel(
                MinecraftArmorWeaponModEnchantments.DEMONIZED.get(),
                weapon
            );

            // Demonizedエンチャントが付いている場合
            if (demonizedLevel > 0) {
                LivingEntity target = event.getEntity();
                float damage = event.getAmount();

                // 吸収する体力の計算（ダメージの20%～50%）
                // レベル1: 20%, レベル10: 50%
                float healPercent = 0.2f + (demonizedLevel - 1) * 0.033f; // 0.2 + 0.033*9 = 0.497
                float healAmount = damage * healPercent;

                // 体力を回復（上限を超えないように）
                if (attacker.getHealth() < attacker.getMaxHealth()) {
                    float newHealth = Math.min(
                        attacker.getHealth() + healAmount,
                        attacker.getMaxHealth()
                    );
                    attacker.setHealth(newHealth);
                }

                // スピード上昇効果（レベルに応じて持続時間が増加）
                // レベル1: 40tick(2秒), レベル10: 100tick(5秒)
                int speedDuration = 40 + (demonizedLevel - 1) * 7; // 40 + 7*9 = 103
                // スピードレベルは0（移動速度+20%）で固定
                attacker.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SPEED,
                    speedDuration,
                    0, // レベル0 (移動速度+20%)
                    false,
                    true // パーティクル表示
                ));

                // 吸収エフェクト（体力を吸い取る視覚効果）
                if (attacker.level instanceof ServerLevel serverLevel) {
                    // ターゲットから攻撃者に向かうパーティクル（血を吸収）
                    for (int i = 0; i < 5 + demonizedLevel; i++) {
                        double t = i / (5.0 + demonizedLevel);
                        double x = target.getX() + (attacker.getX() - target.getX()) * t;
                        double y = target.getY() + target.getBbHeight() / 2 +
                                  (attacker.getY() + attacker.getBbHeight() / 2 - target.getY() - target.getBbHeight() / 2) * t;
                        double z = target.getZ() + (attacker.getZ() - target.getZ()) * t;

                        // 赤いパーティクル（血）
                        serverLevel.sendParticles(
                            ParticleTypes.DAMAGE_INDICATOR,
                            x, y, z,
                            2, 0.1, 0.1, 0.1, 0.05
                        );
                    }

                    // 攻撃者の周りに回復エフェクト
                    serverLevel.sendParticles(
                        ParticleTypes.HEART,
                        attacker.getX(),
                        attacker.getY() + attacker.getBbHeight() / 2,
                        attacker.getZ(),
                        1 + demonizedLevel / 3, // レベルに応じてパーティクル数増加
                        0.3, 0.3, 0.3,
                        0.1
                    );

                    // スピード上昇のエフェクト
                    serverLevel.sendParticles(
                        ParticleTypes.SOUL,
                        attacker.getX(),
                        attacker.getY() + 0.1,
                        attacker.getZ(),
                        3 + demonizedLevel / 2,
                        0.3, 0.1, 0.3,
                        0.05
                    );
                }

                // 吸収サウンド（レベルに応じてピッチ変化）
                float pitch = 1.0f + (demonizedLevel * 0.05f); // レベル10で1.5
                attacker.level.playSound(
                    null,
                    attacker.getX(), attacker.getY(), attacker.getZ(),
                    SoundEvents.PLAYER_HURT_DROWN,
                    SoundSource.PLAYERS,
                    0.5f,
                    pitch
                );

                // 高レベル時は追加サウンド
                if (demonizedLevel >= 5) {
                    attacker.level.playSound(
                        null,
                        attacker.getX(), attacker.getY(), attacker.getZ(),
                        SoundEvents.SOUL_ESCAPE,
                        SoundSource.PLAYERS,
                        0.3f,
                        1.5f
                    );
                }
            }
        }
    }
}

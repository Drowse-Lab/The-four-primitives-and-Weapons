package minecraftarmorweapon.events;

import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import minecraftarmorweapon.init.MinecraftArmorWeaponModItems;
import minecraftarmorweapon.init.MinecraftArmorWeaponModMobEffects;

/**
 * Sword of Night: テレポート後のバックスタブ処理
 * SWORD_OF_NIGHT_EFFECT中の攻撃で追加ダメージ + ノックバック + スタン
 */
@Mod.EventBusSubscriber(modid = "minecraft_armor_weapon")
public class SwordOfNightBackstabHandler {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        // SWORD_OF_NIGHT_EFFECT中かチェック
        if (!player.hasEffect(MinecraftArmorWeaponModMobEffects.SWORD_OF_NIGHT_EFFECT.get())) return;

        // メインハンドにSword of Nightを持っているかチェック
        if (player.getMainHandItem().getItem() != MinecraftArmorWeaponModItems.SWORD_OF_NIGHT.get()) return;

        LivingEntity target = event.getEntity();

        // バックスタブ追加ダメージ: 元の5.5d → 9.5d (+4ダメージ追加)
        event.setAmount(event.getAmount() + 4.0f);

        // エフェクトを解除（1回限り）
        player.removeEffect(MinecraftArmorWeaponModMobEffects.SWORD_OF_NIGHT_EFFECT.get());

        // ノックバック III相当
        double knockX = -Math.sin(Math.toRadians(player.getYRot())) * 1.5;
        double knockZ = Math.cos(Math.toRadians(player.getYRot())) * 1.5;
        target.setDeltaMovement(target.getDeltaMovement().add(knockX, 0.3, knockZ));
        target.hurtMarked = true;

        // スタン: Slowness 127 + Mining Fatigue 127 (10tick = 0.5秒)
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 127, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 10, 127, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 10, 127, false, false));

        // エフェクト
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1, target.getZ(), 10, 0.5, 0.5, 0.5, 0.1);
            serverLevel.sendParticles(ParticleTypes.SMOKE, target.getX(), target.getY() + 1, target.getZ(), 5, 0.3, 0.3, 0.3, 0.02);
        }

        // サウンド
        player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 0.8f);

        // アクションバーにBACKSTAB表示
        player.displayClientMessage(net.minecraft.network.chat.Component.literal("§c§l[BACKSTAB!]"), true);
    }
}

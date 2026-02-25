package minecraftarmorweapon.events;

import minecraftarmorweapon.util.VersionHelper;

import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

import minecraftarmorweapon.util.DamageCalculator;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = "minecraft_armor_weapon")
public class FireballHitHandler {

    @SubscribeEvent
    public static void onFireballHit(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof LargeFireball fireball)) return;
        if (!(event.getRayTraceResult() instanceof EntityHitResult hitResult)) return;
        if (!(hitResult.getEntity() instanceof LivingEntity target)) return;

        // カスタムファイアボールかチェック
        CompoundTag tag = fireball.getPersistentData();
        if (!tag.contains("OwnerUUID")) return;

        // プレイヤーを取得
        if (!(VersionHelper.getLevel(fireball) instanceof ServerLevel serverLevel)) return;

        UUID ownerUUID = UUID.fromString(tag.getString("OwnerUUID"));
        Player player = serverLevel.getPlayerByUUID(ownerUUID);
        if (player == null) return;

        // 実際の攻撃イベントを発火（武器攻撃力 + エンチャント + エフェクト）
        player.attack(target);

        // 無敵時間をリセットしてボーナスダメージ（+3）を追加
        target.invulnerableTime = 0;
        float bonusDamage = tag.contains("BonusDamage") ? tag.getFloat("BonusDamage") : 3.0f;
        target.hurt(player.damageSources().playerAttack(player), bonusDamage);

        // 炎上効果を付与（6秒）
        target.setSecondsOnFire(6);
    }
}

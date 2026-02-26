package minecraftarmorweapon.events;

import minecraftarmorweapon.item.rarity.WeaponRarity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * 禁忌レアリティの特殊能力: 通常攻撃・ため攻撃時に周囲の飛び道具を跳ね返す
 */
@Mod.EventBusSubscriber(modid = "minecraft_armor_weapon")
public class ForbiddenRarityHandler {

    private static final double REFLECT_RADIUS = 5.0;
    private static final double REFLECT_SPEED = 1.5;

    /**
     * 通常攻撃時: 禁忌レアリティの武器で攻撃したら飛び道具を反射
     */
    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;

        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!(mainHand.getItem() instanceof SwordItem)) return;

        WeaponRarity rarity = WeaponRarity.getFromStack(mainHand);
        if (rarity != WeaponRarity.FORBIDDEN) return;

        reflectNearbyProjectiles(player);
    }

    /**
     * 周囲の飛び道具をプレイヤーの視線方向に跳ね返す
     */
    public static void reflectNearbyProjectiles(Player player) {
        if (player.level().isClientSide) return;

        AABB area = player.getBoundingBox().inflate(REFLECT_RADIUS);
        List<Entity> projectiles = player.level().getEntities(player, area,
                e -> e instanceof Projectile);

        if (projectiles.isEmpty()) return;

        Vec3 lookVec = player.getLookAngle().normalize().scale(REFLECT_SPEED);

        for (Entity entity : projectiles) {
            Projectile proj = (Projectile) entity;

            // プレイヤーの視線方向に飛ばす
            proj.setDeltaMovement(lookVec);
            proj.setOwner(player);
            proj.hurtMarked = true; // クライアント同期

            // パーティクル
            if (player.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        proj.getX(), proj.getY(), proj.getZ(),
                        5, 0.2, 0.2, 0.2, 0.02);
            }
        }

        // サウンド
        player.level().playSound(null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS,
                1.0f, 0.8f);
    }
}

package the_four_primitives_and_weapons.events;

import the_four_primitives_and_weapons.item.rarity.WeaponRarity;
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
@Mod.EventBusSubscriber(modid = "the_four_primitives_and_weapons")
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

    // TACZ MODの弾エンティティクラス名（実際のソース: com.tacz.guns.entity.EntityKineticBullet）
    // EntityKineticBulletはProjectileを継承しているのでinstanceofでも検出可能
    private static final String TACZ_BULLET_CLASS = "com.tacz.guns.entity.EntityKineticBullet";

    /**
     * エンティティが飛び道具かどうか判定（バニラ + TACZ + 他の銃MOD対応）
     * TACZ: EntityKineticBullet (extends Projectile, registry: tacz:bullet)
     */
    private static boolean isProjectileEntity(Entity entity) {
        // バニラProjectile（TACZ弾もProjectileを継承しているのでここで検出される）
        if (entity instanceof Projectile) return true;

        // TACZの弾を明示的にチェック（将来Projectile継承が変わった場合の保険）
        String className = entity.getClass().getName();
        if (className.equals(TACZ_BULLET_CLASS)) return true;

        // レジストリ名でTACZ弾をチェック
        String entityTypeKey = entity.getType().toShortString().toLowerCase();
        if (entityTypeKey.equals("tacz:bullet")) return true;

        // 他の銃MODの汎用チェック（Projectileを継承しない銃MOD対応）
        String lowerClassName = className.toLowerCase();
        if (lowerClassName.contains("bullet") || lowerClassName.contains("projectile")) {
            return true;
        }

        return false;
    }

    /**
     * 周囲の飛び道具をプレイヤーの視線方向に跳ね返す
     */
    public static void reflectNearbyProjectiles(Player player) {
        if (player.level().isClientSide) return;

        AABB area = player.getBoundingBox().inflate(REFLECT_RADIUS);
        List<Entity> projectiles = player.level().getEntities(player, area,
                ForbiddenRarityHandler::isProjectileEntity);

        if (projectiles.isEmpty()) return;

        Vec3 lookVec = player.getLookAngle().normalize().scale(REFLECT_SPEED);

        for (Entity entity : projectiles) {
            // プレイヤーの視線方向に飛ばす
            entity.setDeltaMovement(lookVec);
            entity.hurtMarked = true;

            // バニラProjectileの場合はオーナーも変更
            if (entity instanceof Projectile proj) {
                proj.setOwner(player);
            }

            // パーティクル
            if (player.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        entity.getX(), entity.getY(), entity.getZ(),
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

package the_four_primitives_and_weapons.entity;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages;

import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModCustomEntities;

/**
 * 種の飛び道具。 投げた種アイテムの見た目でほぼ直進し、 当たると小ダメージ。
 * 両手に種を持つと SeedShooterHandler が大量に発射して「弾幕」になる。
 */
public class SeedProjectileEntity extends ThrowableItemProjectile {

    private static final float DAMAGE = 0.5f; // 1発あたり

    public SeedProjectileEntity(EntityType<? extends SeedProjectileEntity> type, Level level) {
        super(type, level);
    }

    public SeedProjectileEntity(Level level, LivingEntity shooter) {
        super(TheFourPrimitivesAndWeaponsModCustomEntities.SEED_PROJECTILE.get(), shooter, level);
    }

    public SeedProjectileEntity(PlayMessages.SpawnEntity packet, Level level) {
        super(TheFourPrimitivesAndWeaponsModCustomEntities.SEED_PROJECTILE.get(), level);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.WHEAT_SEEDS;
    }

    @Override
    protected float getGravity() {
        return 0.012f; // ほぼ直進 ( 軽い落下のみ )
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (level().isClientSide) return;
        if (result.getEntity() instanceof LivingEntity target && target != getOwner()) {
            target.hurt(damageSources().thrown(this, getOwner()), DAMAGE);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            level().broadcastEntityEvent(this, (byte) 3); // 着弾: アイテム破片パーティクル
            discard();
        }
    }
}

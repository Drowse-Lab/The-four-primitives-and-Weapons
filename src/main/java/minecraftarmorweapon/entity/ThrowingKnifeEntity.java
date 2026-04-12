package minecraftarmorweapon.entity;

import minecraftarmorweapon.init.CustomEntityInit;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages;

/**
 * 投げナイフ飛翔体。
 * 右クリックで投擲、敵にヒット時はダメージを与えて消滅、
 * ブロックにヒット時はその場にアイテムとしてドロップする（再拾得可能）。
 */
@OnlyIn(value = Dist.CLIENT, _interface = ItemSupplier.class)
public class ThrowingKnifeEntity extends ThrowableItemProjectile implements ItemSupplier {

    private static final float DAMAGE = 6.0f;

    public ThrowingKnifeEntity(PlayMessages.SpawnEntity packet, Level world) {
        super(CustomEntityInit.THROWING_KNIFE_ENTITY.get(), world);
    }

    public ThrowingKnifeEntity(EntityType<? extends ThrowingKnifeEntity> type, Level world) {
        super(type, world);
    }

    public ThrowingKnifeEntity(Level world, LivingEntity thrower) {
        super(CustomEntityInit.THROWING_KNIFE_ENTITY.get(), thrower, world);
    }

    @Override
    protected Item getDefaultItem() {
        return CustomEntityInit.THROWING_KNIFE.get();
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity target = result.getEntity();
        if (target == this.getOwner()) return;
        target.hurt(this.damageSources().thrown(this, this.getOwner()), DAMAGE);
        if (!this.level().isClientSide) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.ARROW_HIT, SoundSource.PLAYERS, 1.0f, 1.2f);
            this.discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!this.level().isClientSide) {
            ItemEntity drop = new ItemEntity(this.level(),
                this.getX(), this.getY(), this.getZ(),
                new ItemStack(CustomEntityInit.THROWING_KNIFE.get()));
            drop.setDefaultPickUpDelay();
            this.level().addFreshEntity(drop);
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.6f, 1.4f);
            this.discard();
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public ItemStack getItem() {
        return new ItemStack(CustomEntityInit.THROWING_KNIFE.get());
    }
}

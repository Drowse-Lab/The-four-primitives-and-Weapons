package the_four_primitives_and_weapons.entity;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.joml.Vector3f;

import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModEntities;
import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModItems;
import the_four_primitives_and_weapons.item.GateFormula;

/**
 * Gateの飛び道具 - 金の直刀の見た目+金色パーティクル。
 * エンティティやブロックに当たると爆発する。
 */
public class GateProjectileEntity extends ThrowableProjectile implements ItemSupplier {

    private static final DustParticleOptions GOLD_PARTICLE =
            new DustParticleOptions(new Vector3f(1.0f, 0.816f, 0.0f), 1.0f);
    private int life = 0;

    public GateProjectileEntity(EntityType<? extends ThrowableProjectile> type, Level level) {
        super(type, level);
    }

    public GateProjectileEntity(Level level, Player player) {
        super(TheFourPrimitivesAndWeaponsModEntities.GATE_PROJECTILE.get(), player, level);
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    public ItemStack getItem() {
        return new ItemStack(TheFourPrimitivesAndWeaponsModItems.GOLD_TYOKUTO.get());
    }

    // 爆風で吹き飛ばされない（向きが変わらない）
    @Override
    public boolean ignoreExplosion() {
        return true;
    }

    // プッシュされない（他エンティティや爆風による速度変更を防止）
    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        life++;

        // 金色パーティクル (本数は lisp 設定)
        int particles = GateFormula.projParticlesPerTick();
        if (particles > 0 && level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(GOLD_PARTICLE,
                    getX(), getY(), getZ(), particles, 0.1, 0.1, 0.1, 0.0);
        }

        // 自動消滅: lifetime tick 後に爆発エフェクト付きで discard
        if (life > GateFormula.projLifetime()) {
            if (!level().isClientSide) {
                level().explode(this, getX(), getY(), getZ(),
                        GateFormula.projEndRadius(), Level.ExplosionInteraction.NONE);
            }
            discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (level().isClientSide) return;
        if (result.getEntity() == getOwner()) return;

        // ダメージ (lisp 設定)
        if (result.getEntity() instanceof LivingEntity target) {
            target.hurt(damageSources().explosion(this, getOwner()), GateFormula.projHitDamage());
        }

        // 爆発エフェクト（地形破壊なし）
        level().explode(this, getX(), getY(), getZ(),
                GateFormula.projHitRadius(), Level.ExplosionInteraction.NONE);
        discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        // 爆発エフェクト（地形破壊なし）
        if (!level().isClientSide) {
            level().explode(this, getX(), getY(), getZ(),
                    GateFormula.projHitRadius(), Level.ExplosionInteraction.NONE);
        }
        discard();
    }

    @Override
    protected float getGravity() {
        return GateFormula.projGravity();  // 0 = 完全直進
    }
}

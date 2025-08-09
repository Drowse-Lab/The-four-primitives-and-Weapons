

package minecraftarmorweapon.entity;

import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.network.PlayMessages;
import net.minecraftforge.network.NetworkHooks;

import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
//import net.minecraft.damagesource.DamageSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.SpectralArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import minecraftarmorweapon.init.MinecraftArmorWeaponModEntities;
import minecraftarmorweapon.entity.KatanaTobuEntity;
//package minecraftarmorweapon.entity.ai;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import minecraftarmorweapon.entity.ai.CustomMeleeAttackGoal;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.nbt.CompoundTag;
import java.util.UUID;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;

public class FlyingAttackerEntity extends Monster {
    private static final EntityDataAccessor<ItemStack> DATA_DISPLAY_ITEM = SynchedEntityData.defineId(FlyingAttackerEntity.class, EntityDataSerializers.ITEM_STACK);
    
    private LivingEntity owner;
    private UUID ownerUUID;
    private UUID targetUUID;
    private int projectileCheckCooldown = 0;
    private LivingEntity lastAttacker = null;
    private int arrowShootCooldown = 0;
    private int attackCooldown = 0;

    public FlyingAttackerEntity(PlayMessages.SpawnEntity packet, Level world) {
        this(MinecraftArmorWeaponModEntities.FLYING_ATTACKER.get(), world);
    }

    public FlyingAttackerEntity(EntityType<FlyingAttackerEntity> type, Level world) {
        super(type, world);
        maxUpStep = 0.6f;
        xpReward = 0;
        setNoAi(false);
        setPersistenceRequired();
    }
    
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_DISPLAY_ITEM, ItemStack.EMPTY);
    }

    public LivingEntity getOwner() {
        return this.owner;
    }

    public void setOwner(LivingEntity owner) {
        this.owner = owner;
        if (owner != null) {
            this.ownerUUID = owner.getUUID();
        }
    }

    public UUID getOwnerUUID() {
        return this.ownerUUID;
    }

    public void setOwnerUUID(UUID uuid) {
        this.ownerUUID = uuid;
    }

    public UUID getTargetUUID() {
        return this.targetUUID;
    }

    public void setTargetUUID(UUID uuid) {
        this.targetUUID = uuid;
    }
    
    public void setDisplayItem(ItemStack item) {
        this.entityData.set(DATA_DISPLAY_ITEM, item.copy());
    }
    
    public ItemStack getDisplayItem() {
        return this.entityData.get(DATA_DISPLAY_ITEM);
    }

    public LivingEntity getTargetEntity() {
        if (this.targetUUID != null && this.level instanceof ServerLevel) {
            Entity entity = ((ServerLevel) this.level).getEntity(this.targetUUID);
            if (entity instanceof LivingEntity) {
                return (LivingEntity) entity;
            }
        }
        return null;
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();

        // 攻撃者を最優先でターゲットに
        this.targetSelector.addGoal(0, new NearestAttackableTargetGoal<>(
            this,
            LivingEntity.class,
            true,
            entity -> {
                // 召喚者を攻撃した者を最優先
                if (this.owner != null && this.owner.getLastHurtByMob() == entity) {
                    return entity.isAlive();
                }
                return false;
            }
        ));
        
        // 指定されたターゲットを優先的に攻撃
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(
            this,
            LivingEntity.class,
            true,
            entity -> {
                // UUIDで指定されたターゲットがいる場合は、そのターゲットのみを攻撃
                if (this.targetUUID != null) {
                    return entity.getUUID().equals(this.targetUUID) && entity.isAlive();
                }
                // 指定されたターゲットがいない場合は、召喚者以外を攻撃
                if (this.ownerUUID != null) {
                    return !entity.getUUID().equals(this.ownerUUID) && entity.isAlive();
                }
                return entity != this.getOwner() && entity.isAlive();
            }
        ));

        // 攻撃行動を追加（近接攻撃）
        this.goalSelector.addGoal(1, new CustomMeleeAttackGoal(this, 1.2, false));

        // その他の行動
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(3, new FloatGoal(this));
        this.targetSelector.addGoal(4, new HurtByTargetGoal(this));
    }

    @Override
    public MobType getMobType() {
        return MobType.UNDEFINED;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }
    
    @Override
    public boolean fireImmune() {
        return true;
    }
    
    @Override
    public boolean canBeCollidedWith() {
        return false; // 他のエンティティとの衝突を無効化
    }
    
    @Override
    public boolean isPushable() {
        return false; // プッシュされない
    }
    
    @Override
    protected void pushEntities() {
        // 他のエンティティを押さない
    }
    
    @Override
    public boolean isPickable() {
        return false; // ピッキング（選択）不可にして発射体の判定から除外
    }
    
    @Override
    protected boolean canRide(Entity entity) {
        return false;
    }
    
    @Override
    public boolean skipAttackInteraction(Entity entity) {
        // すべての攻撃を無視（発射体含む）
        return true;
    }

    @Override
    public double getMyRidingOffset() {
        return -0.35D;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // /killコマンドとvoidダメージは受ける
        if (source.isCreativePlayer() || source == DamageSource.OUT_OF_WORLD || source.isBypassInvul()) {
            return super.hurt(source, amount);
        }
        
        // 火や溶岩ダメージを完全に無効化
        if (source == DamageSource.IN_FIRE || source == DamageSource.ON_FIRE || source == DamageSource.LAVA || source == DamageSource.HOT_FLOOR) {
            return false;
        }
        
        // 近接攻撃（剣など）は受ける
        if (source.getEntity() != null && source.getDirectEntity() == source.getEntity()) {
            // 召喚者からのダメージは無効
            if (source.getEntity() == this.owner) {
                return false;
            }
            return super.hurt(source, amount);
        }
        
        // 発射体ダメージは無効化
        if (source.isProjectile()) {
            return false;
        }
        
        // その他のダメージは無効化
        return false;
    }
    
    @Override
    public boolean isInvulnerable() {
        return false;
    }
    
    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        // /killコマンドとvoidダメージは受ける
        if (source.isCreativePlayer() || source == DamageSource.OUT_OF_WORLD || source.isBypassInvul()) {
            return false;
        }
        
        // 火や溶岩ダメージに対して完全に無敵
        if (source == DamageSource.IN_FIRE || source == DamageSource.ON_FIRE || source == DamageSource.LAVA || source == DamageSource.HOT_FLOOR) {
            return true;
        }
        
        // 近接攻撃（剣など）は受ける
        if (source.getEntity() != null && source.getDirectEntity() == source.getEntity()) {
            // 召喚者からのダメージは無敵
            if (source.getEntity() == this.owner) {
                return true;
            }
            return false;
        }
        
        // 発射体ダメージには無敵
        if (source.isProjectile()) {
            return true;
        }
        
        // その他のダメージは無効化
        return true;
    }

    @Override
    public SoundEvent getHurtSound(DamageSource ds) {
        return ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.generic.hurt"));
    }

    @Override
    public SoundEvent getDeathSound() {
        return ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.generic.death"));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        this.setNoGravity(true);
        
        // 火に対する免疫を保証
        this.clearFire();
        
        // マグマや溶岩から自動的に上昇
        if (this.isInLava()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0, 0.08, 0));
        }
        
        // 近くの発射体を検出して通過させる
        makeProjectilesPassThrough();
        
        // 攻撃クールダウンの処理
        if (attackCooldown > 0) {
            attackCooldown--;
        }
        
        // 剣モードの近接攻撃処理
        if (!this.getPersistentData().getBoolean("ArrowShootMode") && !this.level.isClientSide) {
            LivingEntity target = this.getTarget();
            if (target == null && this.targetUUID != null) {
                target = this.getTargetEntity();
            }
            
            if (target != null && target.isAlive() && attackCooldown == 0) {
                double distanceSq = this.distanceToSqr(target);
                
                // 近接攻撃範囲内（3ブロック以内）
                if (distanceSq < 9.0D) {
                    performMeleeAttack(target);
                    attackCooldown = 20; // 1秒のクールダウン
                } else if (distanceSq < 256.0D) {
                    // ターゲットに向かって移動
                    this.getLookControl().setLookAt(target);
                    Vec3 toTarget = new Vec3(
                        target.getX() - this.getX(),
                        target.getY() - this.getY(),
                        target.getZ() - this.getZ()
                    ).normalize().scale(0.3);
                    this.setDeltaMovement(this.getDeltaMovement().add(toTarget));
                }
            }
        }
        
        // 矢射撃モードのチェック
        if (this.getPersistentData().getBoolean("ArrowShootMode") && !this.level.isClientSide) {
            if (arrowShootCooldown > 0) {
                arrowShootCooldown--;
            }
            
            // ターゲットの検索
            LivingEntity target = this.getTarget();
            if (target == null && this.targetUUID != null) {
                target = this.getTargetEntity();
            }
            
            // ターゲットがいない場合は、召喚者の敵を探す
            if (target == null && this.owner != null) {
                // 召喚者の最後の攻撃者を優先
                if (this.owner.getLastHurtByMob() != null && this.owner.getLastHurtByMob().isAlive()) {
                    target = this.owner.getLastHurtByMob();
                    this.setTarget(target);
                    this.targetUUID = target.getUUID();
                } else {
                    // 周囲の敵を検索
                    List<LivingEntity> nearbyEntities = this.level.getEntitiesOfClass(
                        LivingEntity.class, 
                        this.getBoundingBox().inflate(16.0D),
                        e -> e != this && e != this.owner && e.isAlive() && 
                             !(e instanceof FlyingAttackerEntity) &&
                             (e instanceof Monster || (this.owner instanceof Player && e instanceof Player && e != this.owner))
                    );
                    
                    if (!nearbyEntities.isEmpty()) {
                        target = nearbyEntities.get(0);
                        this.setTarget(target);
                        this.targetUUID = target.getUUID();
                    }
                }
            }
            
            if (target != null && target.isAlive() && arrowShootCooldown == 0 && this.distanceToSqr(target) < 256.0D) {
                shootArrowAt(target);
                arrowShootCooldown = 20; // 1秒のクールダウン
            }
        }
        
        // 召喚者の最後の攻撃者をチェック
        if (this.owner != null) {
            LivingEntity ownerLastAttacker = this.owner.getLastHurtByMob();
            if (ownerLastAttacker != null && ownerLastAttacker.isAlive()) {
                this.lastAttacker = ownerLastAttacker;
                // 召喚者を攻撃した者を優先的にターゲットに
                this.setTarget(this.lastAttacker);
                this.targetUUID = this.lastAttacker.getUUID();
            }
        }

        // 指定されたターゲットが死んだり、離れすぎた場合は消滅
        if (this.targetUUID != null) {
            LivingEntity target = this.getTargetEntity();
            if (target == null || !target.isAlive() || this.distanceToSqr(target) > 256.0D) {
                this.discard();
                return;
            }
        }
        
        // 飛び道具の検知と防御
        if (this.projectileCheckCooldown <= 0) {
            checkAndDefendProjectiles();
            this.projectileCheckCooldown = 2; // 2tick毎にチェック
        } else {
            this.projectileCheckCooldown--;
        }

        // 溶岩内にいない場合のみ通常の浮遊動作
        if (!this.isInLava()) {
            if (this.tickCount % 20 < 10) {
                this.setDeltaMovement(this.getDeltaMovement().add(0, 0.01, 0));
            } else {
                this.setDeltaMovement(this.getDeltaMovement().add(0, -0.01, 0));
            }
        }
    }
    
    private void makeProjectilesPassThrough() {
        // このメソッドは不要になったため空実装
        // canBeHitByProjectile()とisPickable()で処理される
    }
    
    private void checkAndDefendProjectiles() {
        if (this.owner == null) return;
        
        // 半径10ブロック以内の飛び道具を検知
        double detectionRange = 10.0D;
        List<Entity> nearbyEntities = this.level.getEntities(this, 
            this.getBoundingBox().inflate(detectionRange));
        
        for (Entity entity : nearbyEntities) {
            if (entity instanceof Projectile) {
                Projectile projectile = (Projectile) entity;
                
                // 所有者に向かっている飛び道具かチェック
                if (isProjectileThreateningOwner(projectile)) {
                    // 飛び道具に向かって移動して迎撃
                    interceptProjectile(projectile);
                    break;
                }
            }
        }
    }
    
    private boolean isProjectileThreateningOwner(Projectile projectile) {
        if (this.owner == null || projectile.getOwner() == this.owner) {
            return false;
        }
        
        // 飛び道具の進行方向を確認
        Vec3 projectilePos = projectile.position();
        Vec3 projectileMotion = projectile.getDeltaMovement();
        Vec3 ownerPos = this.owner.position();
        
        // 飛び道具が所有者の方向に向かっているかチェック
        Vec3 toOwner = ownerPos.subtract(projectilePos).normalize();
        double dot = projectileMotion.normalize().dot(toOwner);
        
        // 0.7以上なら所有者に向かっていると判定（約45度以内）
        return dot > 0.7 && projectile.distanceToSqr(this.owner) < 100.0D;
    }
    
    private void interceptProjectile(Projectile projectile) {
        // 飛び道具に向かって高速移動
        Vec3 toProjectile = projectile.position().subtract(this.position()).normalize();
        this.setDeltaMovement(toProjectile.scale(2.0D));
        
        // 飛び道具に接触したら破壊
        if (this.distanceToSqr(projectile) < 4.0D) {
            projectile.discard();
            // パーティクルエフェクト
            if (this.level instanceof ServerLevel) {
                ServerLevel serverLevel = (ServerLevel) this.level;
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT,
                    projectile.getX(), projectile.getY(), projectile.getZ(),
                    10, 0.2, 0.2, 0.2, 0.1);
            }
        }
    }
    
    private void performMeleeAttack(LivingEntity target) {
        // 表示用アイテムから攻撃力を計算
        ItemStack displayItem = this.getDisplayItem();
        float baseDamage = 5.0F; // デフォルトダメージ
        
        if (!displayItem.isEmpty() && displayItem.getItem() instanceof SwordItem) {
            SwordItem sword = (SwordItem) displayItem.getItem();
            baseDamage = sword.getDamage() + 4.0F; // 剣のダメージ + ベースダメージ
        }
        
        // ダメージを与える
        DamageSource damageSource = DamageSource.mobAttack(this);
        if (this.owner != null) {
            // 召喚者の攻撃として扱う
            damageSource = DamageSource.mobAttack(this.owner instanceof Mob ? (Mob)this.owner : this);
        }
        
        boolean hit = target.hurt(damageSource, baseDamage);
        
        if (hit) {
            // ノックバック
            double knockbackX = target.getX() - this.getX();
            double knockbackZ = target.getZ() - this.getZ();
            double knockbackStrength = Math.sqrt(knockbackX * knockbackX + knockbackZ * knockbackZ);
            if (knockbackStrength > 0) {
                target.setDeltaMovement(
                    target.getDeltaMovement().add(
                        knockbackX / knockbackStrength * 0.5,
                        0.2,
                        knockbackZ / knockbackStrength * 0.5
                    )
                );
            }
            
            // 攻撃エフェクト
            this.level.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.0F, 1.0F);
        }
    }
    
    private void shootArrowAt(LivingEntity target) {
        // 表示用アイテム（矢）を取得
        ItemStack arrowItem = this.getDisplayItem();
        
        // 矢の生成（発射者はFlyingAttackerEntity自身）
        AbstractArrow arrow;
        
        if (arrowItem.getItem() == Items.SPECTRAL_ARROW) {
            arrow = new SpectralArrow(this.level, this);
        } else if (arrowItem.getItem() == Items.ARROW || arrowItem.getItem() == Items.TIPPED_ARROW) {
            arrow = new Arrow(this.level, this);
            if (arrowItem.getItem() == Items.TIPPED_ARROW) {
                ((Arrow)arrow).setEffectsFromItem(arrowItem);
            }
        } else {
            // カスタム矢の場合はKatanaTobuEntityを使用
            KatanaTobuEntity customArrow = new KatanaTobuEntity(MinecraftArmorWeaponModEntities.KATANA_TOBU.get(), 
                this, this.level);
            arrow = customArrow;
        }

        // 矢の位置をFlyingAttackerEntityの位置に設定
        arrow.setPos(this.getX(), this.getY() + this.getEyeHeight() - 0.1, this.getZ());
        
        // 射撃方向の計算
        double dx = target.getX() - this.getX();
        double dy = target.getY() + target.getEyeHeight() / 2 - (this.getY() + this.getEyeHeight());
        double dz = target.getZ() - this.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);

        arrow.shoot(dx, dy + distance * 0.2, dz, 1.6F, 1.0F);
        arrow.setBaseDamage(5.0);
        arrow.setPierceLevel((byte)1);
        
        // 矢の所有者を召喚者に設定（ダメージの帰属のため）
        if (this.owner != null) {
            arrow.setOwner(this.owner);
        }

        // サウンド再生
        this.level.playSound(null, this.getX(), this.getY(), this.getZ(), 
            SoundEvents.SKELETON_SHOOT, SoundSource.HOSTILE, 1.0F, 1.0F / (this.random.nextFloat() * 0.4F + 0.8F));

        this.level.addFreshEntity(arrow);
    }

    public static void init() {}

    public static AttributeSupplier.Builder createAttributes() {
        AttributeSupplier.Builder builder = Mob.createMobAttributes();
        builder = builder.add(Attributes.MOVEMENT_SPEED, 0.3);
        builder = builder.add(Attributes.MAX_HEALTH, 10);
        builder = builder.add(Attributes.ARMOR, 0);
        builder = builder.add(Attributes.ATTACK_DAMAGE, 3);
        builder = builder.add(Attributes.FOLLOW_RANGE, 16);
        return builder;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        if (this.ownerUUID != null) {
            compound.putUUID("OwnerUUID", this.ownerUUID);
        }
        if (this.targetUUID != null) {
            compound.putUUID("TargetUUID", this.targetUUID);
        }
        ItemStack displayItem = this.getDisplayItem();
        if (!displayItem.isEmpty()) {
            compound.put("DisplayItem", displayItem.save(new CompoundTag()));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.hasUUID("OwnerUUID")) {
            this.ownerUUID = compound.getUUID("OwnerUUID");
        }
        if (compound.hasUUID("TargetUUID")) {
            this.targetUUID = compound.getUUID("TargetUUID");
        }
        if (compound.contains("DisplayItem")) {
            this.setDisplayItem(ItemStack.of(compound.getCompound("DisplayItem")));
        }
    }
}
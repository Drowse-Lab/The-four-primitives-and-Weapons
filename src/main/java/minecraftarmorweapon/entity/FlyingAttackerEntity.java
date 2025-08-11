

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
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.entity.EquipmentSlot;
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
import java.util.HashSet;
import java.util.Set;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.entity.projectile.Fireball;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import minecraftarmorweapon.init.MinecraftArmorWeaponModEnchantments;
import net.minecraft.commands.arguments.EntityAnchorArgument;

public class FlyingAttackerEntity extends Monster {
    private static final EntityDataAccessor<ItemStack> DATA_DISPLAY_ITEM = SynchedEntityData.defineId(FlyingAttackerEntity.class, EntityDataSerializers.ITEM_STACK);
    
    private LivingEntity owner;
    private UUID ownerUUID;
    private UUID targetUUID;
    private int projectileCheckCooldown = 0;
    private LivingEntity lastAttacker = null;
    private int arrowShootCooldown = 0;
    private int attackCooldown = 0;
    private int deflectCooldown = 0; // 発射体を弾いた後のクールダウン（0.5秒 = 10tick）
    private Set<Integer> deflectedProjectiles = new HashSet<>(); // 既に弾いた発射体のIDを記録

    public FlyingAttackerEntity(PlayMessages.SpawnEntity packet, Level world) {
        this(MinecraftArmorWeaponModEntities.FLYING_ATTACKER.get(), world);
    }

    public FlyingAttackerEntity(EntityType<FlyingAttackerEntity> type, Level world) {
        super(type, world);
        maxUpStep = 0.6f;
        xpReward = 0;
        setNoAi(false);
        setPersistenceRequired();
        // バウンディングボックスのサイズを設定（幅0.6, 高さ1.8）
        this.refreshDimensions();
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

        // プレイヤーが攻撃したエンティティを最優先でターゲットに
        this.targetSelector.addGoal(0, new NearestAttackableTargetGoal<>(
            this,
            LivingEntity.class,
            true,
            entity -> {
                // FlyingAttackerEntity同士は攻撃しない
                if (entity instanceof FlyingAttackerEntity) {
                    return false;
                }
                // プレイヤーが最後に攻撃したエンティティを最優先
                if (this.owner != null && this.owner.getLastHurtMob() == entity) {
                    return entity.isAlive();
                }
                // 次に、プレイヤーを攻撃した者を優先
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
                // FlyingAttackerEntity同士は攻撃しない
                if (entity instanceof FlyingAttackerEntity) {
                    return false;
                }
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
        // FlyingAttackerEntityに攻撃されても反撃しない
        this.targetSelector.addGoal(4, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                LivingEntity attacker = FlyingAttackerEntity.this.getLastHurtByMob();
                // FlyingAttackerEntityからの攻撃は無視
                if (attacker instanceof FlyingAttackerEntity) {
                    return false;
                }
                return super.canUse();
            }
        });
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        // FlyingAttackerEntity同士は攻撃しない
        if (entity instanceof FlyingAttackerEntity) {
            return false;
        }
        
        // LivingEntityの場合はカスタム攻撃処理を使用
        if (entity instanceof LivingEntity) {
            performMeleeAttack((LivingEntity) entity);
            return true;
        }
        
        return super.doHurtTarget(entity);
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
        // スペクテイターモードの時のみtrue（選択可能）、それ以外はfalse
        return false;
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
        // スペクテイターモードでの選択を可能にする
        return true;
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
        // クリエイティブモードのプレイヤーからの攻撃は常に受ける
        if (source.getEntity() instanceof Player) {
            Player player = (Player) source.getEntity();
            if (player.isCreative()) {
                return super.hurt(source, amount);
            }
        }
        
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
            // 召喚者からのダメージは無効（クリエイティブ以外）
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
        
        // ownerがnullの場合、ownerUUIDから復元を試みる
        if (this.owner == null && this.ownerUUID != null && this.level instanceof ServerLevel) {
            Entity entity = ((ServerLevel) this.level).getEntity(this.ownerUUID);
            if (entity instanceof LivingEntity) {
                this.owner = (LivingEntity) entity;
            }
        }
        
        // ブロックにめり込んでいる場合の処理
        if (this.level.getBlockState(this.blockPosition()).getMaterial().isSolid() ||
            this.level.getBlockState(this.blockPosition().above()).getMaterial().isSolid()) {
            // 空いている方向を探して移動
            for (int i = 1; i <= 3; i++) {
                if (!this.level.getBlockState(this.blockPosition().above(i)).getMaterial().isSolid()) {
                    this.setPos(this.getX(), this.blockPosition().getY() + i, this.getZ());
                    break;
                }
            }
        }
        
        // 召喚者の近くに留まる処理
        if (this.owner != null && this.owner.isAlive()) {
            double distanceToOwner = this.distanceToSqr(this.owner);
            
            // 召喚者から離れすぎている場合（15ブロック以上）
            if (distanceToOwner > 225.0D) {
                // 召喚者の近くにテレポート
                this.teleportToOwner();
            } else if (distanceToOwner > 64.0D) {
                // 召喚者に向かって移動（8ブロック以上離れた場合）
                Vec3 toOwner = new Vec3(
                    this.owner.getX() - this.getX(),
                    this.owner.getY() + 2.0 - this.getY(), 
                    this.owner.getZ() - this.getZ()
                ).normalize().scale(0.3);
                this.setDeltaMovement(this.getDeltaMovement().add(toOwner));
            }
            
            // ターゲットがいる場合は、そちらに向かって移動
            if (this.getTarget() != null && this.getTarget().isAlive()) {
                // ターゲットの方を向く
                this.lookAt(EntityAnchorArgument.Anchor.EYES, this.getTarget().getEyePosition());
            } else {
                // ターゲットがいない場合は召喚者が向いている方向を向く
                this.setYRot(this.owner.getYRot());
                this.setXRot(this.owner.getXRot());
                this.setYHeadRot(this.owner.getYHeadRot());
            }
            
            // ターゲットがいない場合のみ、ゆるやかに召喚者の周りを浮遊
            // 頻繁な回転を避けるため、動きを大幅に減らす
            if (this.tickCount % 20 == 0 && this.getTarget() == null) { // 1秒に1回だけ位置を更新、かつターゲットがいない時
                double angle = this.random.nextFloat() * Math.PI * 2;
                double radius = 2.0 + this.random.nextFloat() * 2.0;
                double targetX = this.owner.getX() + Math.cos(angle) * radius;
                double targetY = this.owner.getY() + 2.0; // 召喚者の頭上2ブロック
                double targetZ = this.owner.getZ() + Math.sin(angle) * radius;
                
                // 目標位置への穏やかな移動
                Vec3 toTarget = new Vec3(
                    targetX - this.getX(),
                    targetY - this.getY(),
                    targetZ - this.getZ()
                ).scale(0.05);
                
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.8, 0.8, 0.8).add(toTarget));
            }
        }
        
        // 近くの発射体を検出して通過させる
        makeProjectilesPassThrough();
        
        // 攻撃クールダウンの処理
        if (attackCooldown > 0) {
            attackCooldown--;
        }
        
        // 発射体弾きクールダウンの処理
        if (deflectCooldown > 0) {
            deflectCooldown--;
        }
        
        // 剣モードの近接攻撃処理
        if (!this.getPersistentData().getBoolean("ArrowShootMode") && !this.level.isClientSide) {
            LivingEntity target = this.getTarget();
            if (target == null && this.targetUUID != null) {
                target = this.getTargetEntity();
                if (target != null) {
                    this.setTarget(target);  // ターゲットを再設定
                }
            }
            
            if (target != null && target.isAlive() && attackCooldown == 0) {
                double distanceSq = this.distanceToSqr(target);
                
                // 近接攻撃範囲内（4ブロック以内に拡大）
                if (distanceSq < 16.0D) {
                    System.out.println("DEBUG: Attempting melee attack on " + target.getClass().getSimpleName());
                    // 攻撃前にターゲットの方を向く
                    this.lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
                    this.doHurtTarget(target);  // performMeleeAttackの代わりにdoHurtTargetを使用
                    attackCooldown = 15; // 0.75秒のクールダウンに短縮
                } else if (distanceSq < 400.0D) {  // 追跡範囲を20ブロックに拡大
                    // ターゲットに向かって高速移動
                    this.getLookControl().setLookAt(target);
                    this.lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
                    
                    // プレイヤーが攻撃したエンティティの場合は移動速度を上げる
                    double speed = (this.owner != null && this.owner.getLastHurtMob() == target) ? 0.5 : 0.3;
                    
                    Vec3 toTarget = new Vec3(
                        target.getX() - this.getX(),
                        target.getY() + target.getBbHeight() * 0.5 - this.getY(),
                        target.getZ() - this.getZ()
                    ).normalize().scale(speed);
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
        
        // 召喚者の最後の攻撃者と、召喚者が最後に攻撃したエンティティをチェック
        if (this.owner != null) {
            // 優先度1: プレイヤーが最後に攻撃したエンティティ
            LivingEntity ownerLastHurt = this.owner.getLastHurtMob();
            if (ownerLastHurt != null && ownerLastHurt.isAlive() && ownerLastHurt != this) {
                // プレイヤーが攻撃したエンティティを最優先ターゲットに
                this.setTarget(ownerLastHurt);
                this.targetUUID = ownerLastHurt.getUUID();
            } 
            // 優先度2: プレイヤーを攻撃したエンティティ
            else {
                LivingEntity ownerLastAttacker = this.owner.getLastHurtByMob();
                if (ownerLastAttacker != null && ownerLastAttacker.isAlive()) {
                    this.lastAttacker = ownerLastAttacker;
                    // 召喚者を攻撃した者を優先的にターゲットに
                    this.setTarget(this.lastAttacker);
                    this.targetUUID = this.lastAttacker.getUUID();
                }
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
        
        // 剣モードの時のみ飛び道具の検知と防御
        if (!this.getPersistentData().getBoolean("ArrowShootMode")) {
            // 毎tickチェックして、より確実に発射体を検出
            checkAndDefendProjectiles();
        }

        // 溶岩内にいない場合のみ通常の浮遊動作（召喚者がいない場合のみ）
        if (!this.isInLava() && this.owner == null) {
            if (this.tickCount % 20 < 10) {
                this.setDeltaMovement(this.getDeltaMovement().add(0, 0.01, 0));
            } else {
                this.setDeltaMovement(this.getDeltaMovement().add(0, -0.01, 0));
            }
        }
    }
    
    private void teleportToOwner() {
        if (this.owner == null) return;
        
        // 召喚者の周りのランダムな位置にテレポート
        double angle = this.random.nextFloat() * Math.PI * 2;
        double distance = 2.0 + this.random.nextFloat() * 2.0;
        
        double targetX = this.owner.getX() + Math.cos(angle) * distance;
        double targetY = this.owner.getY() + 2.0;
        double targetZ = this.owner.getZ() + Math.sin(angle) * distance;
        
        this.teleportTo(targetX, targetY, targetZ);
        
        // 召喚者の向きに合わせる
        this.setYRot(this.owner.getYRot());
        this.setXRot(this.owner.getXRot());
        this.setYHeadRot(this.owner.getYHeadRot());
    }
    
    private void makeProjectilesPassThrough() {
        // このメソッドは不要になったため空実装
        // canBeHitByProjectile()とisPickable()で処理される
    }
    
    private void checkAndDefendProjectiles() {
        if (this.owner == null || this.deflectCooldown > 0) return;
        
        // 半径20ブロック以内の飛び道具を検知（検出範囲を拡大）
        double detectionRange = 20.0D;
        List<Entity> nearbyEntities = this.level.getEntities(this, 
            this.getBoundingBox().inflate(detectionRange));
        
        // 古い発射体のIDをクリーンアップ（既に存在しないエンティティのIDを削除）
        deflectedProjectiles.removeIf(id -> nearbyEntities.stream().noneMatch(e -> e.getId() == id));
        
        for (Entity entity : nearbyEntities) {
            // 既に弾いた発射体はスキップ
            if (deflectedProjectiles.contains(entity.getId())) {
                continue;
            }
            
            // すべての種類の発射体をチェック
            boolean isProjectile = false;
            
            // 具体的な発射体タイプを先にチェック
            if (entity instanceof ThrownPotion) {
                isProjectile = true;
                System.out.println("DEBUG: Found ThrownPotion at " + entity.position());
            }
            // トライデント
            else if (entity instanceof ThrownTrident) {
                isProjectile = true;
            }
            // ファイアボール系
            else if (entity instanceof Fireball) {
                isProjectile = true;
            }
            // AbstractArrow（矢など）
            else if (entity instanceof AbstractArrow) {
                isProjectile = true;
            }
            // ThrowableProjectile（雪玉、卵など）
            else if (entity instanceof ThrowableProjectile) {
                isProjectile = true;
            }
            // 基本的な発射体タイプ
            else if (entity instanceof Projectile) {
                isProjectile = true;
            }
            // その他の発射体（エンティティ名ベースのチェック）
            else {
                String entityName = entity.getClass().getSimpleName().toLowerCase();
                if (entityName.contains("projectile") || 
                    entityName.contains("arrow") || 
                    entityName.contains("bolt") ||
                    entityName.contains("fireball") ||
                    entityName.contains("throwable") ||
                    entityName.contains("bullet") ||
                    entityName.contains("meteor") ||
                    entityName.contains("potion")) {
                    isProjectile = true;
                }
            }
            
            if (isProjectile) {
                // 所有者に向かっている飛び道具かチェック
                boolean threatening = isProjectileThreateningOwner(entity);
                if (entity instanceof ThrownPotion) {
                    System.out.println("DEBUG: Checking if ThrownPotion is threatening: " + threatening);
                }
                if (threatening) {
                    // スプラッシュポーションは斬って破壊、それ以外は弾く
                    if (entity instanceof ThrownPotion) {
                        System.out.println("DEBUG: Destroying ThrownPotion!");
                        destroyPotion((ThrownPotion) entity);
                    } else {
                        deflectProjectile(entity);
                    }
                    deflectedProjectiles.add(entity.getId()); // 処理した発射体のIDを記録
                    this.deflectCooldown = 10; // 0.5秒のクールダウン（20tick/秒なので10tick）
                    break; // 一度に一つの発射体だけを処理
                }
            }
        }
    }
    
    private boolean isProjectileThreateningOwner(Entity projectile) {
        if (this.owner == null) return false;
        
        // 自分や召喚者が撃った飛び道具は除外
        Entity shooter = null;
        // スプラッシュポーション
        if (projectile instanceof ThrownPotion) {
            shooter = ((ThrownPotion) projectile).getOwner();
            System.out.println("DEBUG: ThrownPotion shooter: " + (shooter != null ? shooter.getName().getString() : "null"));
        }
        // トライデント
        else if (projectile instanceof ThrownTrident) {
            shooter = ((ThrownTrident) projectile).getOwner();
        }
        // AbstractArrow（矢など）
        else if (projectile instanceof AbstractArrow) {
            shooter = ((AbstractArrow) projectile).getOwner();
        }
        // ThrowableProjectile（雪玉、卵など）
        else if (projectile instanceof ThrowableProjectile) {
            shooter = ((ThrowableProjectile) projectile).getOwner();
        }
        // 基本Projectile
        else if (projectile instanceof Projectile) {
            shooter = ((Projectile) projectile).getOwner();
        } 
        else {
            // カスタム発射体の場合、NBTデータからOwnerをチェック
            if (projectile.getPersistentData().contains("Owner")) {
                UUID ownerUUID = projectile.getPersistentData().getUUID("Owner");
                if (this.level instanceof ServerLevel) {
                    shooter = ((ServerLevel) this.level).getEntity(ownerUUID);
                }
            }
        }
        
        if (shooter == this.owner || shooter == this) {
            return false;
        }
        
        // 飛び道具の位置と速度を取得
        Vec3 projectilePos = projectile.position();
        Vec3 projectileMotion = projectile.getDeltaMovement();
        
        // 速度が非常に小さい場合はスキップ（停止している飛び道具）
        // スプラッシュポーションは速度が遅いので閾値を下げる
        double motionThreshold = projectile instanceof ThrownPotion ? 0.001D : 0.01D;
        if (projectileMotion.lengthSqr() < motionThreshold) {
            if (projectile instanceof ThrownPotion) {
                System.out.println("DEBUG: ThrownPotion motion too small: " + projectileMotion.lengthSqr());
            }
            return false;
        }
        
        Vec3 ownerPos = this.owner.position().add(0, this.owner.getBbHeight() * 0.5, 0); // 中心位置で判定
        
        // 飛び道具から召喚者への方向ベクトル
        Vec3 toOwner = ownerPos.subtract(projectilePos);
        double distanceToOwner = toOwner.length();
        
        // 召喚者から20ブロック以内の飛び道具のみ検出
        if (distanceToOwner > 20.0D) {
            return false;
        }
        
        // 飛び道具の進行方向と召喚者への方向の角度をチェック
        Vec3 normalizedMotion = projectileMotion.normalize();
        Vec3 normalizedToOwner = toOwner.normalize();
        double dot = normalizedMotion.dot(normalizedToOwner);
        
        // より広い角度で検出
        // 召喚者に非常に近い飛び道具（3ブロック以内）は必ず脅威として扱う
        if (distanceToOwner < 3.0D) {
            return true; // 3ブロック以内なら無条件で脅威
        }
        
        // スプラッシュポーションはより広い角度で検出（弧を描いて飛ぶため）
        double angleThreshold;
        if (projectile instanceof ThrownPotion) {
            angleThreshold = distanceToOwner < 8.0D ? -0.5 : -0.2; // 非常に緩い判定
        } else {
            angleThreshold = distanceToOwner < 8.0D ? -0.1 : 0.2; // 矢も近距離では緩い判定
        }
        
        if (projectile instanceof ThrownPotion) {
            System.out.println("DEBUG: ThrownPotion dot product: " + dot + ", threshold: " + angleThreshold + ", distance: " + distanceToOwner);
        }
        
        return dot > angleThreshold;
    }
    
    private void deflectProjectile(Entity projectile) {
        // 飛び道具と召喚者の間に瞬間移動
        Vec3 projectilePos = projectile.position();
        
        if (this.owner != null) {
            // 召喚者と発射体の間の位置を計算
            Vec3 ownerPos = this.owner.position();
            Vec3 interceptPos = ownerPos.add(projectilePos.subtract(ownerPos).normalize().scale(2.0));
            this.teleportTo(interceptPos.x, interceptPos.y + 1.0, interceptPos.z);
        } else {
            // 飛び道具の近くに移動
            Vec3 toProjectile = projectilePos.subtract(this.position());
            if (toProjectile.length() > 2.0D) {
                Vec3 movePos = this.position().add(toProjectile.normalize().scale(toProjectile.length() - 1.0D));
                this.teleportTo(movePos.x, movePos.y, movePos.z);
            }
        }
        
        // 飛び道具の方向を反転
        Vec3 currentMotion = projectile.getDeltaMovement();
        Vec3 deflectedMotion = currentMotion.scale(-1.5); // 反転して速度を上げる
        projectile.setDeltaMovement(deflectedMotion);
        
        // 飛び道具の所有者を変更（召喚者の攻撃にする）
        if (this.owner != null) {
            // スプラッシュポーション
            if (projectile instanceof ThrownPotion) {
                ((ThrownPotion) projectile).setOwner(this.owner);
            }
            // トライデント
            else if (projectile instanceof ThrownTrident) {
                ((ThrownTrident) projectile).setOwner(this.owner);
            }
            // AbstractArrow（矢など）
            else if (projectile instanceof AbstractArrow) {
                ((AbstractArrow) projectile).setOwner(this.owner);
            }
            // ThrowableProjectile（雪玉、卵など）
            else if (projectile instanceof ThrowableProjectile) {
                ((ThrowableProjectile) projectile).setOwner(this.owner);
            }
            // 基本Projectile
            else if (projectile instanceof Projectile) {
                ((Projectile) projectile).setOwner(this.owner);
            }
            // カスタム発射体
            else {
                projectile.getPersistentData().putUUID("Owner", this.owner.getUUID());
            }
        }
        
        // エフェクトとサウンド
        if (this.level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel) this.level;
            
            // パーティクルエフェクト
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANTED_HIT,
                projectilePos.x, projectilePos.y, projectilePos.z,
                20, 0.3, 0.3, 0.3, 0.2);
            
            // 剣の弾き音
            this.level.playSound(null, projectilePos.x, projectilePos.y, projectilePos.z,
                SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 1.0F, 1.2F);
        }
        
        // 発射体の元の所有者を新しいターゲットに設定
        Entity originalOwner = null;
        // スプラッシュポーション
        if (projectile instanceof ThrownPotion) {
            originalOwner = ((ThrownPotion) projectile).getOwner();
        }
        // トライデント
        else if (projectile instanceof ThrownTrident) {
            originalOwner = ((ThrownTrident) projectile).getOwner();
        }
        // AbstractArrow（矢など）
        else if (projectile instanceof AbstractArrow) {
            originalOwner = ((AbstractArrow) projectile).getOwner();
        }
        // ThrowableProjectile（雪玉、卵など）
        else if (projectile instanceof ThrowableProjectile) {
            originalOwner = ((ThrowableProjectile) projectile).getOwner();
        }
        // 基本Projectile
        else if (projectile instanceof Projectile) {
            originalOwner = ((Projectile) projectile).getOwner();
        }
        else {
            // カスタム発射体の場合、NBTデータからOwnerを取得
            if (projectile.getPersistentData().contains("Owner")) {
                UUID ownerUUID = projectile.getPersistentData().getUUID("Owner");
                if (this.level instanceof ServerLevel) {
                    originalOwner = ((ServerLevel) this.level).getEntity(ownerUUID);
                }
            }
        }
        
        if (originalOwner instanceof LivingEntity && originalOwner != this.owner) {
            this.setTarget((LivingEntity) originalOwner);
            this.targetUUID = originalOwner.getUUID();
        }
    }
    
    private void destroyPotion(ThrownPotion potion) {
        // ポーションと召喚者の間に瞬間移動
        Vec3 potionPos = potion.position();
        
        if (this.owner != null) {
            // 召喚者とポーションの間の位置を計算
            Vec3 ownerPos = this.owner.position();
            Vec3 interceptPos = ownerPos.add(potionPos.subtract(ownerPos).normalize().scale(2.0));
            this.teleportTo(interceptPos.x, interceptPos.y + 1.0, interceptPos.z);
        } else {
            // ポーションの近くに移動
            Vec3 toPotion = potionPos.subtract(this.position());
            if (toPotion.length() > 2.0D) {
                Vec3 movePos = this.position().add(toPotion.normalize().scale(toPotion.length() - 1.0D));
                this.teleportTo(movePos.x, movePos.y, movePos.z);
            }
        }
        
        // エフェクトとサウンド
        if (this.level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel) this.level;
            
            // ガラスが割れるようなパーティクルエフェクト
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.ITEM_SNOWBALL,
                potionPos.x, potionPos.y, potionPos.z,
                30, 0.3, 0.3, 0.3, 0.2);
            
            // 斬撃エフェクト
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK,
                potionPos.x, potionPos.y, potionPos.z,
                1, 0, 0, 0, 0);
            
            // ガラスが割れる音と剣の音
            this.level.playSound(null, potionPos.x, potionPos.y, potionPos.z,
                SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 0.8F, 1.0F);
            this.level.playSound(null, potionPos.x, potionPos.y, potionPos.z,
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 0.8F, 1.2F);
        }
        
        // ポーションの投擲者を新しいターゲットに設定
        Entity thrower = potion.getOwner();
        if (thrower instanceof LivingEntity && thrower != this.owner) {
            this.setTarget((LivingEntity) thrower);
            this.targetUUID = thrower.getUUID();
        }
        
        // ポーションを破壊（削除）
        potion.discard();
    }
    
    private void interceptProjectile(Projectile projectile) {
        // このメソッドは使用しない（deflectProjectileに置き換え）
    }
    
    private void performMeleeAttack(LivingEntity target) {
        // FlyingAttackerEntity同士は攻撃しない
        if (target instanceof FlyingAttackerEntity) {
            return;
        }
        
        // 表示用アイテムから攻撃力を計算
        ItemStack displayItem = this.getDisplayItem();
        float baseDamage = 5.0F; // デフォルトダメージ
        
        // Killエンチャントのチェック
        boolean hasKillEnchant = false;
        if (!displayItem.isEmpty()) {
            int killLevel = EnchantmentHelper.getItemEnchantmentLevel(
                MinecraftArmorWeaponModEnchantments.KILL.get(), displayItem);
            if (killLevel > 0) {
                hasKillEnchant = true;
                // killエンチャントフラグをNBTに設定
                this.getPersistentData().putBoolean("minecraft_armor_weapon:killentity", true);
            }
            
            // 剣の基本ダメージ
            if (displayItem.getItem() instanceof SwordItem) {
                SwordItem sword = (SwordItem) displayItem.getItem();
                baseDamage = sword.getDamage() + 4.0F;
            }
            
            // EnchantmentHelperを使用してすべてのエンチャントダメージを計算
            // これにより他modのエンチャントも自動的に適用される
            float enchantmentDamage = EnchantmentHelper.getDamageBonus(displayItem, target.getMobType());
            baseDamage += enchantmentDamage;
        }
        
        // ダメージを与える
        DamageSource damageSource = DamageSource.mobAttack(this);
        if (this.owner != null) {
            // 召喚者の攻撃として扱う
            damageSource = DamageSource.mobAttack(this.owner instanceof Mob ? (Mob)this.owner : this);
        }
        
        // クリティカル判定（20%確率 + Looting/幸運レベル * 5%）
        boolean isCritical = false;
        if (!displayItem.isEmpty()) {
            int lootingLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MOB_LOOTING, displayItem);
            float critChance = 0.2F + (lootingLevel * 0.05F);
            isCritical = this.random.nextFloat() < critChance;
            if (isCritical) {
                baseDamage *= 1.5F;
            }
        }
        
        System.out.println("DEBUG: Attempting to deal " + baseDamage + " damage");
        boolean hit = target.hurt(damageSource, baseDamage);
        System.out.println("DEBUG: Hit successful? " + hit);
        
        if (hit) {
            // エンチャントのヒット時効果を適用（他modのエンチャントも含む）
            if (!displayItem.isEmpty()) {
                // doPostHurtEffectsはヒット後のすべてのエンチャント効果を適用
                EnchantmentHelper.doPostHurtEffects(this, target);
                
                // doPostDamageEffectsは攻撃後のすべてのエンチャント効果を適用
                EnchantmentHelper.doPostDamageEffects(target, this);
                
                // 火属性エンチャント（バニラ）
                int fireAspectLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_ASPECT, displayItem);
                if (fireAspectLevel > 0) {
                    target.setSecondsOnFire(fireAspectLevel * 4);
                }
                
                // ノックバック計算（エンチャント込み）
                float knockbackStrength = 0.5F;
                int knockbackLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.KNOCKBACK, displayItem);
                if (knockbackLevel > 0) {
                    knockbackStrength += knockbackLevel * 0.5F;
                }
                
                // スイープ攻撃エンチャント
                int sweepingLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SWEEPING_EDGE, displayItem);
                if (sweepingLevel > 0) {
                    // スイープダメージの計算（レベルに応じて増加）
                    float sweepRatio = sweepingLevel / 3.0F; // レベル1=33%, レベル2=66%, レベル3=100%
                    float sweepDamage = 1.0F + sweepRatio * baseDamage;
                    // 周囲の敵にダメージ
                    for (LivingEntity nearbyEntity : this.level.getEntitiesOfClass(LivingEntity.class, 
                            target.getBoundingBox().inflate(1.0D, 0.25D, 1.0D))) {
                        if (nearbyEntity != target && nearbyEntity != this && nearbyEntity != this.owner 
                                && !this.isAlliedTo(nearbyEntity) && this.distanceToSqr(nearbyEntity) < 9.0D) {
                            nearbyEntity.knockback(0.4F, this.getX() - nearbyEntity.getX(), this.getZ() - nearbyEntity.getZ());
                            nearbyEntity.hurt(damageSource, sweepDamage);
                        }
                    }
                    // スイープエフェクト
                    if (this.level instanceof ServerLevel) {
                        ServerLevel serverLevel = (ServerLevel) this.level;
                        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK,
                            target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                            1, 0, 0, 0, 0);
                    }
                }
                
                // ノックバック適用
                double knockbackX = target.getX() - this.getX();
                double knockbackZ = target.getZ() - this.getZ();
                double knockbackDistance = Math.sqrt(knockbackX * knockbackX + knockbackZ * knockbackZ);
                if (knockbackDistance > 0) {
                    target.setDeltaMovement(
                        target.getDeltaMovement().add(
                            knockbackX / knockbackDistance * knockbackStrength,
                            0.2,
                            knockbackZ / knockbackDistance * knockbackStrength
                        )
                    );
                }
            }
            
            // 攻撃エフェクト
            this.level.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.0F, 1.0F);
            
            // クリティカルエフェクト
            if (isCritical) {
                this.level.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 1.0F, 1.0F);
                if (this.level instanceof ServerLevel) {
                    ServerLevel serverLevel = (ServerLevel) this.level;
                    serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT,
                        target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                        15, 0.2, 0.2, 0.2, 0.1);
                }
            }
        }
        
        // 攻撃後にkillエンチャントフラグをクリア
        this.getPersistentData().remove("minecraft_armor_weapon:killentity");
    }
    
    private void shootArrowAt(LivingEntity target) {
        // FlyingAttackerEntity同士は攻撃しない
        if (target instanceof FlyingAttackerEntity) {
            return;
        }
        
        // 表示用アイテム（矢）を取得
        ItemStack arrowItem = this.getDisplayItem();
        
        // Killエンチャントのチェック（矢にkillエンチャントが付いている場合）
        if (!arrowItem.isEmpty()) {
            int killLevel = EnchantmentHelper.getItemEnchantmentLevel(
                MinecraftArmorWeaponModEnchantments.KILL.get(), arrowItem);
            if (killLevel > 0) {
                // killエンチャントフラグをNBTに設定
                this.getPersistentData().putBoolean("minecraft_armor_weapon:killentity", true);
            }
        }
        
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
        
        // 矢を撃った後にkillエンチャントフラグをクリア
        this.getPersistentData().remove("minecraft_armor_weapon:killentity");
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
        compound.putInt("DeflectCooldown", this.deflectCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.hasUUID("OwnerUUID")) {
            this.ownerUUID = compound.getUUID("OwnerUUID");
            // ワールド再参加時にownerを復元
            if (this.level instanceof ServerLevel) {
                Entity entity = ((ServerLevel) this.level).getEntity(this.ownerUUID);
                if (entity instanceof LivingEntity) {
                    this.owner = (LivingEntity) entity;
                }
            }
        }
        if (compound.hasUUID("TargetUUID")) {
            this.targetUUID = compound.getUUID("TargetUUID");
        }
        if (compound.contains("DisplayItem")) {
            this.setDisplayItem(ItemStack.of(compound.getCompound("DisplayItem")));
        }
        this.deflectCooldown = compound.getInt("DeflectCooldown");
    }
}
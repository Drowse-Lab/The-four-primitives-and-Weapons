package minecraftarmorweapon.entity;

import minecraftarmorweapon.util.VersionHelper;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

import minecraftarmorweapon.ai.PlayerLikeAIGoal;
import minecraftarmorweapon.init.MinecraftArmorWeaponModItems;
import minecraftarmorweapon.util.DamageCalculator;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Random;

/**
 * 一般兵（CommonSoldier）エンティティ
 *
 * ティア1のAIを持つ、プレイヤーのような動作をする敵対Mob
 *
 * 特徴:
 * - 基本的な回避能力（成功率30%）
 * - チャージ攻撃を使用
 * - 鉄の刀を装備
 * - プレイヤーと同じような戦闘スタイル
 */
public class CommonSoldierEntity extends PathfinderMob {

    private static final int AI_TIER = 1;
    private PlayerLikeAIGoal playerLikeAI;

    // プレイヤーへの反撃カウンター（素手攻撃の場合のみ1回だけ素手で反撃）
    private final Map<UUID, Boolean> playerCounterAttackUsed = new HashMap<>();
    // 戦闘モードに入ったプレイヤーのリスト（武器攻撃された場合）
    private final Map<UUID, Boolean> playerCombatMode = new HashMap<>();

    // NBTタグキー
    private static final String NBT_IS_SLIM = "IsSlim";
    private static final String NBT_SKIN_INDEX = "SkinIndex";

    // スキン設定（-1 = ランダム）
    private int isSlim = -1;  // -1: ランダム, 0: 通常, 1: スリム
    private int skinIndex = -1;  // -1: ランダム, 0以上: 指定されたインデックス

    public CommonSoldierEntity(EntityType<? extends PathfinderMob> type, Level world) {
        super(type, world);

        // メインハンドに鉄の刀
        this.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(MinecraftArmorWeaponModItems.IRON_KATANA.get()));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.05f);
        this.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(MinecraftArmorWeaponModItems.SAYA.get()));
        this.setDropChance(EquipmentSlot.OFFHAND, 0.05f);

        // 鉄のフル装備（ティア1）
        this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
        this.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));

        // 防具のドロップ率を設定
        this.setDropChance(EquipmentSlot.HEAD, 0.03f);
        this.setDropChance(EquipmentSlot.CHEST, 0.02f);
        this.setDropChance(EquipmentSlot.LEGS, 0.03f);
        this.setDropChance(EquipmentSlot.FEET, 0.03f);

        this.xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20.0)
            .add(Attributes.MOVEMENT_SPEED, 0.25)  // プレイヤーより速い移動速度
            .add(Attributes.ATTACK_DAMAGE, 5.0)
            .add(Attributes.ARMOR, 4.0)
            .add(Attributes.FOLLOW_RANGE, 64.0)    // 遠くの敵も追いかける
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));

        // PlayerLikeAIGoalが全ての移動と攻撃を制御
        this.playerLikeAI = new PlayerLikeAIGoal(this, AI_TIER);
        this.goalSelector.addGoal(1, this.playerLikeAI);

        // バニラのMeleeAttackGoalとWaterAvoidingRandomStrollGoalは削除（PlayerLikeAIGoalと競合するため）
        // this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, false));
        // this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));

        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        
        // 敵対Mobに対しては攻撃されたら反撃（A-Lifeエンティティは除外）
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this,
            CommonSoldierEntity.class,
            EliteSoldierEntity.class,
            SingularityEntity.class,
            HeroicTierEntity.class
        ).setAlertOthers());

        // 戦闘モードのプレイヤーを攻撃
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
            (player) -> player instanceof Player p && playerCombatMode.getOrDefault(p.getUUID(), false)));

        // 敵対モブを自発的に攻撃
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Zombie.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Skeleton.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Creeper.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Spider.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Pillager.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Vindicator.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Ravager.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Witch.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Evoker.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Vex.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, EnderMan.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Blaze.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, WitherSkeleton.class, true));

        // プレイヤーに対しては自発的に攻撃しない（攻撃されたらhurt()で処理）
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // 落下ダメージ無効化
        if (source == DamageSource.FALL && playerLikeAI != null && playerLikeAI.isFallDamageImmune()) {
            if (!this.level.isClientSide) {
                this.level.broadcastEntityEvent(this, (byte) 2);
            }
            return false;
        }

        // プレイヤーから攻撃された場合の特別処理
        if (source.getEntity() instanceof Player player && !this.level.isClientSide) {
            UUID playerUUID = player.getUUID();

            // プレイヤーが武器を持っているかチェック
            boolean isUnarmedAttack = isPlayerUnarmed(player);

            if (isUnarmedAttack) {
                // 素手攻撃の場合：1回だけ素手で反撃
                if (!playerCounterAttackUsed.getOrDefault(playerUUID, false)) {
                    counterAttackPlayer(player);
                    playerCounterAttackUsed.put(playerUUID, true);
                    player.displayClientMessage(Component.literal("§e" + this.getName().getString() + "§7は素手で反撃した！"), true);
                } else {
                    // 2回目以降は警告メッセージのみ
                    player.displayClientMessage(Component.literal("§7" + this.getName().getString() + "はこれ以上反撃しない..."), true);
                }
            } else {
                // 武器攻撃の場合：戦闘モードに入る
                if (!playerCombatMode.getOrDefault(playerUUID, false)) {
                    playerCombatMode.put(playerUUID, true);
                    player.displayClientMessage(Component.literal("§c" + this.getName().getString() + "§7が戦闘態勢に入った！"), true);

                    // このプレイヤーをターゲットに設定
                    this.setTarget(player);
                }
            }

            return super.hurt(source, amount);
        }

        return super.hurt(source, amount);
    }

    /**
     * プレイヤーが素手（武器なし）かどうか判定
     */
    private boolean isPlayerUnarmed(Player player) {
        ItemStack mainHand = player.getMainHandItem();

        // メインハンドが空
        if (mainHand.isEmpty()) {
            return true;
        }

        // 武器かどうかチェック
        return !isWeapon(mainHand);
    }

    /**
     * アイテムが武器かどうか判定
     */
    private boolean isWeapon(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        // バニラの武器
        if (stack.getItem() instanceof SwordItem ||
            stack.getItem() instanceof AxeItem ||
            stack.getItem() instanceof TridentItem ||
            stack.getItem() instanceof BowItem ||
            stack.getItem() instanceof CrossbowItem) {
            return true;
        }

        // カスタム武器（名前に"katana", "sword", "weapon"などが含まれる）
        String itemName = stack.getItem().toString().toLowerCase();
        return itemName.contains("katana") ||
               itemName.contains("sword") ||
               itemName.contains("weapon") ||
               itemName.contains("blade") ||
               itemName.contains("spear") ||
               itemName.contains("lance") ||
               itemName.contains("dagger") ||
               itemName.contains("scythe");
    }

    /**
     * プレイヤーに素手で1回だけ反撃
     */
    private void counterAttackPlayer(Player player) {
        // 距離チェック
        if (this.distanceTo(player) <= 3.0) {
            // 素手での弱いダメージ（2.0 = ハート1個分）
            player.hurt(DamageSource.mobAttack(this), 2.0f);

            // パンチ音
            this.level.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.PLAYER_ATTACK_WEAK, SoundSource.HOSTILE, 0.8f, 1.0f);

            // パーティクル
            if (VersionHelper.getLevel(this) instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                    ParticleTypes.CRIT,
                    player.getX(), player.getY() + 1, player.getZ(),
                    5, 0.3, 0.5, 0.3, 0
                );
            }
        }
    }

    @Override
    public void die(DamageSource cause) {
        super.die(cause);
        if (!this.level.isClientSide) {
            if (cause.getEntity() instanceof Player player) {
                player.displayClientMessage(Component.literal("§7一般兵を倒した"), true);
            }
        }
    }

    @Override
    public Component getName() {
        return Component.literal("§f一般兵");
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SKELETON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PLAYER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PLAYER_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 0.8f;
    }

    @Override
    public boolean canAttack(net.minecraft.world.entity.LivingEntity target) {
        // プレイヤーの場合
        if (target instanceof Player player) {
            // クリエイティブ/スペクテーターモードのプレイヤーは攻撃しない
            if (player.isCreative() || player.isSpectator()) {
                return false;
            }

            // サバイバル/アドベンチャーモードで戦闘モードに入っている場合のみ攻撃可能
            UUID playerUUID = player.getUUID();
            return playerCombatMode.getOrDefault(playerUUID, false);
        }

        // 敵対Mobは常に攻撃可能
        if (target instanceof Monster && !(target instanceof IronGolem)) {
            return true;
        }

        return super.canAttack(target);
    }

    public int getAITier() {
        return AI_TIER;
    }

    /**
     * エンティティが攻撃する際にカスタムダメージ計算とエフェクトを適用
     */
    @Override
    public boolean doHurtTarget(Entity target) {
        if (target instanceof LivingEntity livingTarget) {
            ItemStack weapon = this.getItemInHand(InteractionHand.MAIN_HAND);

            // DamageCalculatorを使用してダメージを計算
            float baseDamage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);

            // プレイヤーと同じダメージ計算システムを使用
            float actualDamage = DamageCalculator.calculateDamage(
                this, livingTarget, baseDamage, weapon
            );

            // ダメージを与える
            boolean result = livingTarget.hurt(DamageSource.mobAttack(this), actualDamage);

            if (result) {
                // 武器エフェクトを適用
                DamageCalculator.applyWeaponEffects(
                    this, livingTarget, actualDamage, weapon
                );

                // ノックバック
                Vec3 knockback = livingTarget.position().subtract(this.position()).normalize().scale(0.4);
                livingTarget.setDeltaMovement(livingTarget.getDeltaMovement().add(knockback.x, 0.1, knockback.z));

                // 攻撃エフェクト
                if (!this.level.isClientSide && VersionHelper.getLevel(this) instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(
                        ParticleTypes.SWEEP_ATTACK,
                        livingTarget.getX(), livingTarget.getY() + livingTarget.getBbHeight() / 2, livingTarget.getZ(),
                        1, 0, 0, 0, 0
                    );
                }

                // 攻撃音
                this.level.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.0f, 1.0f);
            }

            return result;
        }

        return super.doHurtTarget(target);
    }

    @Override
    protected boolean isSunBurnTick() {
        return false;
    }

    /**
     * NBTタグからデータを読み込み
     */
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(NBT_IS_SLIM, this.isSlim);
        tag.putInt(NBT_SKIN_INDEX, this.skinIndex);
    }

    /**
     * NBTタグにデータを保存
     */
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(NBT_IS_SLIM)) {
            this.isSlim = tag.getInt(NBT_IS_SLIM);
        }
        if (tag.contains(NBT_SKIN_INDEX)) {
            this.skinIndex = tag.getInt(NBT_SKIN_INDEX);
        }
    }

    /**
     * スリムモデルかどうかを取得
     * @return -1: ランダム（UUIDベース）, 0: 通常, 1: スリム
     */
    public int getIsSlim() {
        return this.isSlim;
    }

    /**
     * スリムモデルかどうかを設定
     * @param value -1: ランダム（UUIDベース）, 0: 通常, 1: スリム
     */
    public void setIsSlim(int value) {
        this.isSlim = value;
    }

    /**
     * スキンインデックスを取得
     * @return -1: ランダム（UUIDベース）, 0以上: 指定されたインデックス
     */
    public int getSkinIndex() {
        return this.skinIndex;
    }

    /**
     * スキンインデックスを設定
     * @param value -1: ランダム（UUIDベース）, 0以上: 指定されたインデックス
     */
    public void setSkinIndex(int value) {
        this.skinIndex = value;
    }

    public static void init() {
    }
}

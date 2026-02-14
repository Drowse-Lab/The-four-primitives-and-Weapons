package minecraftarmorweapon.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level().Level;
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
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level().ServerLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

import minecraftarmorweapon.ai.PlayerLikeAIGoal;
import minecraftarmorweapon.init.MinecraftArmorWeaponModItems;
import minecraftarmorweapon.util.DamageCalculator;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 精鋭兵（EliteSoldier）エンティティ
 *
 * ティア2のAIを持つ、プレイヤーのような動作をする強力な敵対Mob
 *
 * 特徴:
 * - 強化された回避能力（成功率50%）
 * - より積極的なチャージ攻撃使用
 * - ダイヤモンドフル装備
 * - プレイヤーと同じような高度な戦闘スタイル
 * - 属性攻撃（火・氷）を使用
 */
public class EliteSoldierEntity extends PathfinderMob {

    private static final int AI_TIER = 2;
    private PlayerLikeAIGoal playerLikeAI;

    // プレイヤーへの反撃カウンター（素手攻撃の場合のみ1回だけ素手で反撃）
    private final Map<UUID, Boolean> playerCounterAttackUsed = new HashMap<>();
    // 戦闘モードに入ったプレイヤーのリスト（武器攻撃された場合）
    private final Map<UUID, Boolean> playerCombatMode = new HashMap<>();

    // NBTタグキー
    private static final String NBT_IS_SLIM = "IsSlim";
    private static final String NBT_SKIN_INDEX = "SkinIndex";

    // スキン設定（-1 = ランダム）
    private int isSlim = -1;
    private int skinIndex = -1;

    public EliteSoldierEntity(EntityType<? extends PathfinderMob> type, Level world) {
        super(type, world);

        // メインハンドに鉄の刀（ティア2 - ティア1と同じ武器だが攻撃力が高い）
        this.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(MinecraftArmorWeaponModItems.IRON_KATANA.get()));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.08f);
        this.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(MinecraftArmorWeaponModItems.SAYA.get()));
        this.setDropChance(EquipmentSlot.OFFHAND, 0.08f);

        // ダイヤモンドのフル装備（ティア2）
        this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
        this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
        this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
        this.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));

        // 防具のドロップ率を設定
        this.setDropChance(EquipmentSlot.HEAD, 0.05f);
        this.setDropChance(EquipmentSlot.CHEST, 0.04f);
        this.setDropChance(EquipmentSlot.LEGS, 0.05f);
        this.setDropChance(EquipmentSlot.FEET, 0.05f);

        this.xpReward = 25;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 40.0)      // 一般兵20 → 40
            .add(Attributes.MOVEMENT_SPEED, 0.27)  // プレイヤーより速い移動速度
            .add(Attributes.ATTACK_DAMAGE, 7.0)    // 一般兵5.0 → 7.0
            .add(Attributes.ARMOR, 8.0)            // 一般兵4.0 → 8.0
            .add(Attributes.FOLLOW_RANGE, 64.0)    // 遠くの敵も追いかける
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.2);  // 一般兵0.0 → 0.2
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));

        // PlayerLikeAIGoalが全ての移動と攻撃を制御
        this.playerLikeAI = new PlayerLikeAIGoal(this, AI_TIER);
        this.goalSelector.addGoal(1, this.playerLikeAI);

        // バニラのMeleeAttackGoalとWaterAvoidingRandomStrollGoalは削除（PlayerLikeAIGoalと競合するため）
        // this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.1, false));
        // this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9));

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
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // 落下ダメージ無効化
        if (source == DamageSource.FALL && playerLikeAI != null && playerLikeAI.isFallDamageImmune()) {
            if (!this.level().isClientSide) {
                this.level().broadcastEntityEvent(this, (byte) 2);
            }
            return false;
        }

        // プレイヤーから攻撃された場合の特別処理
        if (source.getEntity() instanceof Player player && !this.level().isClientSide) {
            UUID playerUUID = player.getUUID();

            boolean isUnarmedAttack = isPlayerUnarmed(player);

            if (isUnarmedAttack) {
                if (!playerCounterAttackUsed.getOrDefault(playerUUID, false)) {
                    counterAttackPlayer(player);
                    playerCounterAttackUsed.put(playerUUID, true);
                    player.displayClientMessage(Component.literal("§e" + this.getName().getString() + "§7は素手で反撃した！"), true);
                } else {
                    player.displayClientMessage(Component.literal("§7" + this.getName().getString() + "はこれ以上反撃しない..."), true);
                }
            } else {
                if (!playerCombatMode.getOrDefault(playerUUID, false)) {
                    playerCombatMode.put(playerUUID, true);
                    player.displayClientMessage(Component.literal("§c" + this.getName().getString() + "§7が戦闘態勢に入った！"), true);
                    this.setTarget(player);
                }
            }

            return super.hurt(source, amount);
        }

        return super.hurt(source, amount);
    }

    private boolean isPlayerUnarmed(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.isEmpty()) {
            return true;
        }
        return !isWeapon(mainHand);
    }

    private boolean isWeapon(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        if (stack.getItem() instanceof SwordItem ||
            stack.getItem() instanceof AxeItem ||
            stack.getItem() instanceof TridentItem ||
            stack.getItem() instanceof BowItem ||
            stack.getItem() instanceof CrossbowItem) {
            return true;
        }

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

    private void counterAttackPlayer(Player player) {
        if (this.distanceTo(player) <= 3.5) {
            player.hurt(DamageSource.mobAttack(this), 3.0f);

            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.PLAYER_ATTACK_WEAK, SoundSource.HOSTILE, 0.8f, 1.0f);

            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                    ParticleTypes.CRIT,
                    player.getX(), player.getY() + 1, player.getZ(),
                    5, 0.3, 0.5, 0.3, 0
                );
            }
        }
    }

    /**
     * エンティティが攻撃する際にカスタムダメージ計算とエフェクトを適用
     */
    @Override
    public boolean doHurtTarget(Entity target) {
        if (target instanceof LivingEntity livingTarget) {
            ItemStack weapon = this.getItemInHand(InteractionHand.MAIN_HAND);

            float baseDamage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);

            // ティア2の強化ダメージ計算（1.3倍）
            float actualDamage = DamageCalculator.calculateDamage(
                this, livingTarget, baseDamage * 1.3f, weapon
            );

            boolean result = livingTarget.hurt(DamageSource.mobAttack(this), actualDamage);

            if (result) {
                DamageCalculator.applyWeaponEffects(
                    this, livingTarget, actualDamage, weapon
                );

                // より強いノックバック
                Vec3 knockback = livingTarget.position().subtract(this.position()).normalize().scale(0.5);
                livingTarget.setDeltaMovement(livingTarget.getDeltaMovement().add(knockback.x, 0.15, knockback.z));

                // 攻撃エフェクト（より派手）
                if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(
                        ParticleTypes.SWEEP_ATTACK,
                        livingTarget.getX(), livingTarget.getY() + livingTarget.getBbHeight() / 2, livingTarget.getZ(),
                        2, 0.1, 0.1, 0.1, 0
                    );

                    // 属性攻撃エフェクト（20%の確率）
                    if (this.random.nextFloat() < 0.2f) {
                        if (this.random.nextBoolean()) {
                            // 火属性
                            livingTarget.setSecondsOnFire(3);
                            serverLevel.sendParticles(
                                ParticleTypes.FLAME,
                                livingTarget.getX(), livingTarget.getY() + 1, livingTarget.getZ(),
                                10, 0.3, 0.3, 0.3, 0.05
                            );
                        } else {
                            // 氷属性（移動速度低下）
                            livingTarget.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 60, 1
                            ));
                            serverLevel.sendParticles(
                                ParticleTypes.SNOWFLAKE,
                                livingTarget.getX(), livingTarget.getY() + 1, livingTarget.getZ(),
                                10, 0.3, 0.3, 0.3, 0.05
                            );
                        }
                    }
                }

                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.0f, 0.9f);
            }

            return result;
        }

        return super.doHurtTarget(target);
    }

    @Override
    public void die(DamageSource cause) {
        super.die(cause);
        if (!this.level().isClientSide) {
            if (cause.getEntity() instanceof Player player) {
                player.displayClientMessage(Component.literal("§6精鋭兵を倒した"), true);
            }
        }
    }

    @Override
    public Component getName() {
        return Component.literal("§6精鋭兵");
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
        return 0.9f;
    }

    @Override
    public boolean canAttack(net.minecraft.world.entity.LivingEntity target) {
        if (target instanceof Player player) {
            if (player.isCreative() || player.isSpectator()) {
                return false;
            }

            UUID playerUUID = player.getUUID();
            return playerCombatMode.getOrDefault(playerUUID, false);
        }

        if (target instanceof Monster && !(target instanceof IronGolem)) {
            return true;
        }

        return super.canAttack(target);
    }

    public int getAITier() {
        return AI_TIER;
    }

    @Override
    protected boolean isSunBurnTick() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(NBT_IS_SLIM, this.isSlim);
        tag.putInt(NBT_SKIN_INDEX, this.skinIndex);
    }

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

    public int getIsSlim() {
        return this.isSlim;
    }

    public void setIsSlim(int value) {
        this.isSlim = value;
    }

    public int getSkinIndex() {
        return this.skinIndex;
    }

    public void setSkinIndex(int value) {
        this.skinIndex = value;
    }

    public static void init() {
    }
}

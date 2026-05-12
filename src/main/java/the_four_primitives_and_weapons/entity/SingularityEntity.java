package the_four_primitives_and_weapons.entity;

import the_four_primitives_and_weapons.util.VersionHelper;

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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import the_four_primitives_and_weapons.ai.PlayerLikeAIGoal;
import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModItems;
import the_four_primitives_and_weapons.util.DamageCalculator;
import the_four_primitives_and_weapons.damage.ElementType;
import the_four_primitives_and_weapons.damage.ElementalDamageUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Random;

/**
 * 特異点（Singularity）エンティティ - ティア3
 *
 * ティア3のAIを持つ、高度な戦闘能力を持つ敵対Mob
 *
 * 特徴:
 * - 高い回避能力（成功率70%）
 * - 6段コンボ攻撃
 * - 属性攻撃（火、氷）を30%確率で使用
 * - ダイヤモンド装備（強化エンチャント付き）
 * - カウンター攻撃（60%）
 * - ダメージ倍率: 1.6倍
 * - 移動速度: 2.6
 */
public class SingularityEntity extends PathfinderMob {

    private static final int AI_TIER = 3;
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

    public SingularityEntity(EntityType<? extends PathfinderMob> type, Level world) {
        super(type, world);

        // メインハンドに鉄の刀（ティア3） - 属性付き
        ItemStack weapon = new ItemStack(TheFourPrimitivesAndWeaponsModItems.IRON_KATANA.get());

        // ランダムに属性を付与（氷または電気）
        ElementType[] tier3Elements = {ElementType.ICE, ElementType.ELECTRIC};
        ElementType selectedElement = tier3Elements[this.random.nextInt(tier3Elements.length)];
        ElementalDamageUtils.setElement(weapon, selectedElement, 3); // レベル3

        this.setItemInHand(InteractionHand.MAIN_HAND, weapon);
        this.setDropChance(EquipmentSlot.MAINHAND, 0.1f);
        this.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(TheFourPrimitivesAndWeaponsModItems.SAYA.get()));
        this.setDropChance(EquipmentSlot.OFFHAND, 0.05f);

        // ダイヤモンドのフル装備（ティア3 - 強化エンチャント）
        this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
        this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
        this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
        this.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));

        this.setDropChance(EquipmentSlot.HEAD, 0.15f);
        this.setDropChance(EquipmentSlot.CHEST, 0.15f);
        this.setDropChance(EquipmentSlot.LEGS, 0.15f);
        this.setDropChance(EquipmentSlot.FEET, 0.15f);

        // スキン設定をランダムに決定
        if (this.isSlim == -1) {
            this.isSlim = this.random.nextInt(2);
        }
        if (this.skinIndex == -1) {
            this.skinIndex = this.random.nextInt(10);
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));

        // PlayerLikeAIGoalが全ての移動と攻撃を制御
        this.playerLikeAI = new PlayerLikeAIGoal(this, AI_TIER);
        this.goalSelector.addGoal(1, playerLikeAI);

        // バニラのMeleeAttackGoalとWaterAvoidingRandomStrollGoalは削除（PlayerLikeAIGoalと競合するため）
        // this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, false));
        // this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.8));

        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));

        // ターゲット選択（A-Lifeエンティティは除外）
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this,
            CommonSoldierEntity.class,
            EliteSoldierEntity.class,
            SingularityEntity.class,
            HeroicTierEntity.class
        ));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));

        // 敵対Mobをターゲット
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

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 60.0)        // HP: 60（ティア2: 40 → ティア3: 60）
                .add(Attributes.MOVEMENT_SPEED, 0.29)    // プレイヤーより速い移動速度
                .add(Attributes.ATTACK_DAMAGE, 10.0)     // 攻撃力: 10（ティア2: 7 → ティア3: 10、倍率1.6倍で16ダメージ）
                .add(Attributes.ARMOR, 12.0)             // 防御力: 12（ダイヤ装備+エンチャント）
                .add(Attributes.ATTACK_KNOCKBACK, 0.5)
                .add(Attributes.FOLLOW_RANGE, 64.0);     // 遠くの敵も追いかける
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // プレイヤーからの攻撃の場合
        if (source.getEntity() instanceof Player player) {
            ItemStack weapon = player.getItemInHand(InteractionHand.MAIN_HAND);
            boolean isUnarmed = weapon.isEmpty();
            UUID playerUUID = player.getUUID();

            // 武器を持っている場合は戦闘モードに入る
            if (!isUnarmed) {
                playerCombatMode.put(playerUUID, true);
                playerCounterAttackUsed.remove(playerUUID);  // 武器攻撃されたら素手カウンターをリセット
            }

            // 素手攻撃で、まだカウンター攻撃していない場合、1回だけ素手で反撃
            if (isUnarmed && !playerCounterAttackUsed.getOrDefault(playerUUID, false)) {
                playerCounterAttackUsed.put(playerUUID, true);
                // 素手で反撃（1ダメージ）
                player.hurt(this.damageSources().mobAttack(this), 1.0f);

                // メッセージ表示
                player.displayClientMessage(Component.literal("§c特異点: この程度か..."), false);

                return super.hurt(source, amount * 0.1f);  // 90%軽減
            }

            // 戦闘モードの場合は通常ダメージ
            if (playerCombatMode.getOrDefault(playerUUID, false)) {
                // ティア3なので、プレイヤーの攻撃を30%軽減
                return super.hurt(source, amount * 0.7f);
            }

            // 非戦闘状態での武器攻撃は90%軽減
            return super.hurt(source, amount * 0.1f);
        }

        return super.hurt(source, amount);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (target instanceof LivingEntity livingTarget) {
            ItemStack weapon = this.getItemInHand(InteractionHand.MAIN_HAND);

            // DamageCalculatorを使用してダメージを計算
            float baseDamage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);

            // ティア3のダメージ倍率: 1.6倍
            float actualDamage = DamageCalculator.calculateDamage(
                this, livingTarget, baseDamage * 1.6f, weapon
            );

            // ダメージを与える
            boolean result = livingTarget.hurt(this.damageSources().mobAttack(this), actualDamage);

            if (result) {
                // 武器エフェクトを適用
                DamageCalculator.applyWeaponEffects(
                    this, livingTarget, actualDamage, weapon
                );

                // 属性攻撃（30%の確率）
                if (this.random.nextFloat() < 0.3f) {
                    if (this.random.nextBoolean()) {
                        // 火属性攻撃（3秒炎上）
                        livingTarget.setSecondsOnFire(3);

                        // パーティクル
                        if (!this.level().isClientSide && VersionHelper.getLevel(this) instanceof ServerLevel serverLevel) {
                            serverLevel.sendParticles(
                                ParticleTypes.FLAME,
                                livingTarget.getX(), livingTarget.getY() + 1, livingTarget.getZ(),
                                10, 0.3, 0.5, 0.3, 0.02
                            );
                        }
                    } else {
                        // 氷属性攻撃（移動速度低下II、3秒）
                        livingTarget.addEffect(new MobEffectInstance(
                            MobEffects.MOVEMENT_SLOWDOWN, 60, 1
                        ));

                        // パーティクル
                        if (!this.level().isClientSide && VersionHelper.getLevel(this) instanceof ServerLevel serverLevel) {
                            serverLevel.sendParticles(
                                ParticleTypes.SNOWFLAKE,
                                livingTarget.getX(), livingTarget.getY() + 1, livingTarget.getZ(),
                                10, 0.3, 0.5, 0.3, 0.02
                            );
                        }
                    }
                }

                // ノックバック（より強力）
                Vec3 knockback = livingTarget.position().subtract(this.position()).normalize().scale(0.5);
                livingTarget.setDeltaMovement(livingTarget.getDeltaMovement().add(knockback.x, 0.15, knockback.z));

                // 攻撃エフェクト
                if (!this.level().isClientSide && VersionHelper.getLevel(this) instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(
                        ParticleTypes.SWEEP_ATTACK,
                        livingTarget.getX(), livingTarget.getY() + livingTarget.getBbHeight() / 2, livingTarget.getZ(),
                        2, 0, 0, 0, 0
                    );
                }

                // 攻撃音（より重い音）
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 1.2f, 0.9f);
            }

            return result;
        }

        return super.doHurtTarget(target);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt(NBT_IS_SLIM, this.isSlim);
        compound.putInt(NBT_SKIN_INDEX, this.skinIndex);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains(NBT_IS_SLIM)) {
            this.isSlim = compound.getInt(NBT_IS_SLIM);
        }
        if (compound.contains(NBT_SKIN_INDEX)) {
            this.skinIndex = compound.getInt(NBT_SKIN_INDEX);
        }
    }

    /**
     * スキンタイプを取得（0: 通常、1: スリム）
     */
    public int getSkinType() {
        return this.isSlim;
    }

    /**
     * スキンインデックスを取得
     */
    public int getSkinIndex() {
        return this.skinIndex;
    }

    /**
     * スキンタイプを設定
     */
    public void setSkinType(int type) {
        this.isSlim = type;
    }

    /**
     * スキンインデックスを設定
     */
    public void setSkinIndex(int index) {
        this.skinIndex = index;
    }
}

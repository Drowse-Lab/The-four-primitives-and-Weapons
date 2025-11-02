package minecraftarmorweapon.entity;

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
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import minecraftarmorweapon.ai.PlayerLikeAIGoal;
import minecraftarmorweapon.init.MinecraftArmorWeaponModItems;
import minecraftarmorweapon.util.DamageCalculator;
import minecraftarmorweapon.damage.ElementType;
import minecraftarmorweapon.damage.ElementalDamageUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 英雄級（HeroicTier）エンティティ - ティア4
 *
 * ティア4のAIを持つ、最強クラスの戦闘能力を持つ敵対Mob
 *
 * 特徴:
 * - 非常に高い回避能力（成功率80%）
 * - 8段コンボ攻撃
 * - 属性攻撃（火、氷、雷）を40%確率で使用
 * - ネザライト装備（最強エンチャント付き）
 * - カウンター攻撃（75%）
 * - パリィ（40%）
 * - ダメージ倍率: 2.0倍
 * - 移動速度: 3.0
 */
public class HeroicTierEntity extends PathfinderMob {

    private static final int AI_TIER = 4;
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

    public HeroicTierEntity(EntityType<? extends PathfinderMob> type, Level world) {
        super(type, world);

        // メインハンドに鉄の刀（ティア4 - 最強） - 属性付き
        ItemStack weapon = new ItemStack(MinecraftArmorWeaponModItems.IRON_KATANA.get());

        // ランダムに属性を付与（氷または電気） - ティア4は最高レベルの属性
        ElementType[] tier4Elements = {ElementType.ICE, ElementType.ELECTRIC};
        ElementType selectedElement = tier4Elements[this.random.nextInt(tier4Elements.length)];
        ElementalDamageUtils.setElement(weapon, selectedElement, 4); // レベル4

        this.setItemInHand(InteractionHand.MAIN_HAND, weapon);
        this.setDropChance(EquipmentSlot.MAINHAND, 0.2f);
        this.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(MinecraftArmorWeaponModItems.SAYA.get()));
        this.setDropChance(EquipmentSlot.OFFHAND, 0.1f);

        // ネザライトのフル装備（ティア4 - 最強エンチャント）
        this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
        this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
        this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.NETHERITE_LEGGINGS));
        this.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.NETHERITE_BOOTS));

        this.setDropChance(EquipmentSlot.HEAD, 0.3f);
        this.setDropChance(EquipmentSlot.CHEST, 0.3f);
        this.setDropChance(EquipmentSlot.LEGS, 0.3f);
        this.setDropChance(EquipmentSlot.FEET, 0.3f);

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
                .add(Attributes.MAX_HEALTH, 80.0)        // HP: 80（ティア3: 60 → ティア4: 80）
                .add(Attributes.MOVEMENT_SPEED, 0.3)     // プレイヤーより速い移動速度
                .add(Attributes.ATTACK_DAMAGE, 12.0)     // 攻撃力: 12（ティア3: 10 → ティア4: 12、倍率2.0倍で24ダメージ）
                .add(Attributes.ARMOR, 16.0)             // 防御力: 16（ネザライト装備+最強エンチャント）
                .add(Attributes.ARMOR_TOUGHNESS, 3.0)    // 防御強度: 3（ネザライト装備）
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.2)  // ノックバック耐性: 20%
                .add(Attributes.ATTACK_KNOCKBACK, 0.6)
                .add(Attributes.FOLLOW_RANGE, 40.0);
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
                player.hurt(DamageSource.mobAttack(this), 1.0f);

                // メッセージ表示
                player.displayClientMessage(Component.literal("§6§l英雄級: 愚かな..."), false);

                return super.hurt(source, amount * 0.05f);  // 95%軽減
            }

            // 戦闘モードの場合は通常ダメージ
            if (playerCombatMode.getOrDefault(playerUUID, false)) {
                // ティア4なので、プレイヤーの攻撃を50%軽減
                return super.hurt(source, amount * 0.5f);
            }

            // 非戦闘状態での武器攻撃は95%軽減
            return super.hurt(source, amount * 0.05f);
        }

        return super.hurt(source, amount);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (target instanceof LivingEntity livingTarget) {
            ItemStack weapon = this.getItemInHand(InteractionHand.MAIN_HAND);

            // DamageCalculatorを使用してダメージを計算
            float baseDamage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);

            // ティア4のダメージ倍率: 2.0倍
            float actualDamage = DamageCalculator.calculateDamage(
                this, livingTarget, baseDamage * 2.0f, weapon
            );

            // ダメージを与える
            boolean result = livingTarget.hurt(DamageSource.mobAttack(this), actualDamage);

            if (result) {
                // 武器エフェクトを適用
                DamageCalculator.applyWeaponEffects(
                    this, livingTarget, actualDamage, weapon
                );

                // 属性攻撃（40%の確率）- 火、氷、雷
                if (this.random.nextFloat() < 0.4f) {
                    int elementType = this.random.nextInt(3);

                    switch (elementType) {
                        case 0:
                            // 火属性攻撃（4秒炎上）
                            livingTarget.setSecondsOnFire(4);

                            // パーティクル
                            if (!this.level.isClientSide && this.level instanceof ServerLevel serverLevel) {
                                serverLevel.sendParticles(
                                    ParticleTypes.FLAME,
                                    livingTarget.getX(), livingTarget.getY() + 1, livingTarget.getZ(),
                                    15, 0.4, 0.6, 0.4, 0.03
                                );
                            }
                            break;

                        case 1:
                            // 氷属性攻撃（移動速度低下III、4秒）
                            livingTarget.addEffect(new MobEffectInstance(
                                MobEffects.MOVEMENT_SLOWDOWN, 80, 2
                            ));

                            // パーティクル
                            if (!this.level.isClientSide && this.level instanceof ServerLevel serverLevel) {
                                serverLevel.sendParticles(
                                    ParticleTypes.SNOWFLAKE,
                                    livingTarget.getX(), livingTarget.getY() + 1, livingTarget.getZ(),
                                    15, 0.4, 0.6, 0.4, 0.03
                                );
                            }
                            break;

                        case 2:
                            // 雷属性攻撃（弱体化II、3秒）
                            livingTarget.addEffect(new MobEffectInstance(
                                MobEffects.WEAKNESS, 60, 1
                            ));

                            // 追加ダメージ
                            livingTarget.hurt(DamageSource.mobAttack(this), actualDamage * 0.3f);

                            // パーティクル
                            if (!this.level.isClientSide && this.level instanceof ServerLevel serverLevel) {
                                serverLevel.sendParticles(
                                    ParticleTypes.ELECTRIC_SPARK,
                                    livingTarget.getX(), livingTarget.getY() + 1, livingTarget.getZ(),
                                    20, 0.4, 0.6, 0.4, 0.05
                                );
                            }
                            break;
                    }
                }

                // ノックバック（非常に強力）
                Vec3 knockback = livingTarget.position().subtract(this.position()).normalize().scale(0.7);
                livingTarget.setDeltaMovement(livingTarget.getDeltaMovement().add(knockback.x, 0.2, knockback.z));

                // 攻撃エフェクト（複数）
                if (!this.level.isClientSide && this.level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(
                        ParticleTypes.SWEEP_ATTACK,
                        livingTarget.getX(), livingTarget.getY() + livingTarget.getBbHeight() / 2, livingTarget.getZ(),
                        3, 0, 0, 0, 0
                    );

                    // クリティカルヒットエフェクト
                    serverLevel.sendParticles(
                        ParticleTypes.CRIT,
                        livingTarget.getX(), livingTarget.getY() + livingTarget.getBbHeight() / 2, livingTarget.getZ(),
                        10, 0.3, 0.5, 0.3, 0.1
                    );
                }

                // 攻撃音（非常に重い音）
                this.level.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 1.5f, 0.8f);
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

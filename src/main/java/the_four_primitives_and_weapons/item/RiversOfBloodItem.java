
package the_four_primitives_and_weapons.item;

import the_four_primitives_and_weapons.util.VersionHelper;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.UseAnim;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import org.joml.Vector3f;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.registries.ForgeRegistries;

import the_four_primitives_and_weapons.procedures.KatanaBloodYoukuritukusitatokiProcedure;
import the_four_primitives_and_weapons.skill.BloodSlashSkill;
import the_four_primitives_and_weapons.damage.SpecialDebuffHandler;
import the_four_primitives_and_weapons.procedures.IronKatanaturuwoShoudeChituteiruJiannoteitukuProcedure;

import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModTabs;

public class RiversOfBloodItem extends SwordItem {
	public RiversOfBloodItem() {
		super(new Tier() {
			public int getUses() {
				return 0;
			}

			public float getSpeed() {
				return 4f;
			}

			public float getAttackDamageBonus() {
				return 4f;
			}

			public int getLevel() {
				return 1;
			}

			public int getEnchantmentValue() {
				return 4;
			}

			public Ingredient getRepairIngredient() {
				return Ingredient.of();
			}
		}, 3, -2.4f, new Item.Properties());
	}

	/** 単押し ( tap ) / 長押し ( hold ) の閾値 — useDuration から残 tick を引いた値で判定 */
	public static final int HOLD_THRESHOLD_TICKS = 10; // 0.5 秒以上で長押し扱い
	public static final int USE_DURATION_TICKS   = 72000;

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return UseAnim.BLOCK;
	}

	@Override
	public int getUseDuration(ItemStack stack) {
		return USE_DURATION_TICKS;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player entity, InteractionHand hand) {
		// RIGHT_CLICK スロットが "rivers_of_blood_special" の時のみ使用開始 ( チャージ開始 )。
		// 単押し: 血斬撃波 / 長押し: TP 連続斬撃 → releaseUsing で振り分け。
		the_four_primitives_and_weapons.skill.PlayerSkillData.SkillStorage sd =
				the_four_primitives_and_weapons.skill.PlayerSkillData.getSkillData(entity);
		if (sd == null) return super.use(world, entity, hand);
		String motion = sd.getMotionForWeapon(
				the_four_primitives_and_weapons.skill.PlayerSkillData.AttackSlot.RIGHT_CLICK,
				entity.getMainHandItem());
		if (!"rivers_of_blood_special".equals(motion)) {
			return super.use(world, entity, hand);
		}
		entity.startUsingItem(hand);
		return InteractionResultHolder.consume(entity.getItemInHand(hand));
	}

	@Override
	public void onUseTick(Level world, LivingEntity user, ItemStack stack, int remainingUseDuration) {
		if (!(user instanceof Player player)) return;
		int held = USE_DURATION_TICKS - remainingUseDuration;
		// 視界の邪魔にならないように 「ため」 中は周囲パーティクルを出さない。
		//   進行状況は HUD ( BloodChargeOverlay ) でホットバー上に表示する。
		// 閾値到達 frame だけ、 短い音とごく控えめな flash で 「ため完了」 を通知。
		if (held == HOLD_THRESHOLD_TICKS && world instanceof ServerLevel sl) {
			sl.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 0.4f, 1.7f);
		}
	}

	@Override
	public void releaseUsing(ItemStack stack, Level world, LivingEntity user, int timeLeft) {
		if (!(user instanceof Player player)) return;
		int held = USE_DURATION_TICKS - timeLeft;
		if (held < HOLD_THRESHOLD_TICKS) {
			// 単押し → 前方扇形の血斬撃波
			BloodSlashSkill.fire(player);
		} else {
			// 長押し → TP 連続斬撃 ( Hemorrhagic Eclipse )
			KatanaBloodYoukuritukusitatokiProcedure.execute(world, player);
		}
	}

	@Override
	public void inventoryTick(ItemStack itemstack, Level world, Entity entity, int slot, boolean selected) {
		super.inventoryTick(itemstack, world, entity, slot, selected);
		if (selected)
			IronKatanaturuwoShoudeChituteiruJiannoteitukuProcedure.execute(world, entity.getX(), entity.getY(), entity.getZ(), entity);
	}
	
	@Override
	public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		if (!attacker.level().isClientSide && attacker instanceof Player player) {
			// 基本のライフスティール量（ダメージの20%）
			float damageDealt = this.getDamage();
			float healAmount = damageDealt * 0.2f;
			
			// ターゲットが呪われているかチェック（ウィザー効果またはカスタムNBTタグ）
			boolean isCursed = target.hasEffect(MobEffects.WITHER) || 
							   (target.getPersistentData().contains("Feyn") && 
							    "cursed".equals(target.getPersistentData().getString("Feyn")));
			
			if (isCursed) {
				// 呪われた敵からは追加でライフスティール（50%）
				healAmount = damageDealt * 0.5f;
				
				// 追加ダメージ
				target.hurt(target.damageSources().magic(), damageDealt * 0.3f);
				
				// 特殊エフェクト
				if (VersionHelper.getLevel(attacker) instanceof ServerLevel serverLevel) {
					// 血のパーティクル
					for (int i = 0; i < 20; i++) {
						double offsetX = (Math.random() - 0.5) * 2;
						double offsetY = Math.random() * 2;
						double offsetZ = (Math.random() - 0.5) * 2;
						serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
							target.getX() + offsetX,
							target.getY() + offsetY,
							target.getZ() + offsetZ,
							1, 0, 0, 0, 0.1);
					}
					
					// 暗黒のオーラ
					serverLevel.sendParticles(ParticleTypes.SOUL,
						target.getX(), target.getY() + 1, target.getZ(),
						10, 0.5, 0.5, 0.5, 0.05);
				}
				
				// 呪い強化：追加デバフ — Wither は DoT (カスタムダメージ)、 Weakness は attribute modifier
				SpecialDebuffHandler.applyWither(target, 100, 0.5f);
				SpecialDebuffHandler.applyWeakness(target, 200, 1);
				
				// 特殊サウンド
				attacker.level().playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
					SoundEvents.WITHER_HURT, SoundSource.PLAYERS, 0.5f, 0.8f);
			} else {
				// 通常の血のエフェクト ( 派手な dust + sweep_attack で「血しぶき」 を強調 )
				if (VersionHelper.getLevel(attacker) instanceof ServerLevel serverLevel) {
					double tx = target.getX();
					double ty = target.getY() + target.getBbHeight() / 2.0;
					double tz = target.getZ();
					serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
						tx, ty, tz, 12, 0.3, 0.3, 0.3, 0.1);
					DustParticleOptions deep   = new DustParticleOptions(new Vector3f(0.55f, 0.03f, 0.03f), 1.2f);
					DustParticleOptions bright = new DustParticleOptions(new Vector3f(0.85f, 0.10f, 0.10f), 1.0f);
					serverLevel.sendParticles(deep,   tx, ty, tz, 18, 0.35, 0.40, 0.35, 0.05);
					serverLevel.sendParticles(bright, tx, ty, tz, 10, 0.30, 0.35, 0.30, 0.08);
					serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
						tx, ty, tz, 2, 0.2, 0.2, 0.2, 0.0);
				}
			}
			
			// プレイヤーを回復
			player.heal(healAmount);
			
			// 回復エフェクト
			// if (VersionHelper.getLevel(attacker) instanceof ServerLevel serverLevel) {
			// 	serverLevel.sendParticles(ParticleTypes.HEART,
			// 		player.getX(), player.getY() + 1, player.getZ(),
			// 		3, 0.3, 0.3, 0.3, 0);
			// }
			
			// 吸血サウンド
			attacker.level().playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
				SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.5f, 1.2f);
		}
		
		return super.hurtEnemy(stack, target, attacker);
	}
}

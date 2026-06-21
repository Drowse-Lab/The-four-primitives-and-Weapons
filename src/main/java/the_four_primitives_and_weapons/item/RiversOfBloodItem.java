
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.registries.ForgeRegistries;

import the_four_primitives_and_weapons.procedures.KatanaBloodYoukuritukusitatokiProcedure;
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

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player entity, InteractionHand hand) {
		InteractionResultHolder<ItemStack> ar = super.use(world, entity, hand);
		// 特殊技「血の咆哮」は RIGHT_CLICK スロットが "rivers_of_blood_special" の時だけ発動。
		// (回避 / なし を選択している間は何もしない — 回避と被らないようにするため)
		the_four_primitives_and_weapons.skill.PlayerSkillData.SkillStorage sd =
				the_four_primitives_and_weapons.skill.PlayerSkillData.getSkillData(entity);
		if (sd != null) {
			String motion = sd.getMotionForWeapon(
					the_four_primitives_and_weapons.skill.PlayerSkillData.AttackSlot.RIGHT_CLICK,
					entity.getMainHandItem());
			if ("rivers_of_blood_special".equals(motion)) {
				KatanaBloodYoukuritukusitatokiProcedure.execute(world, entity);
			}
		}
		return ar;
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
				
				// 呪い強化：追加デバフ
				target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
				target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1));
				
				// 特殊サウンド
				attacker.level().playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
					SoundEvents.WITHER_HURT, SoundSource.PLAYERS, 0.5f, 0.8f);
			} else {
				// 通常の血のエフェクト
				if (VersionHelper.getLevel(attacker) instanceof ServerLevel serverLevel) {
					serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
						target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
						5, 0.3, 0.3, 0.3, 0.1);
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

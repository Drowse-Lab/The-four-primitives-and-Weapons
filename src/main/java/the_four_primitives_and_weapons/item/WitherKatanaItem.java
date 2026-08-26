
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import the_four_primitives_and_weapons.procedures.IronKatanaturuwoShoudeChituteiruJiannoteitukuProcedure;

import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModTabs;
import the_four_primitives_and_weapons.damage.SpecialDebuffHandler;

public class WitherKatanaItem extends SwordItem {
	public WitherKatanaItem() {
		super(new Tier() {
			public int getUses() {
				return 250;
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
				return 14;
			}

			public Ingredient getRepairIngredient() {
				return Ingredient.of();
			}
		}, 3, -2.4f, new Item.Properties());
	}

	@Override
	public void inventoryTick(ItemStack itemstack, Level world, Entity entity, int slot, boolean selected) {
		super.inventoryTick(itemstack, world, entity, slot, selected);
		if (selected)
			IronKatanaturuwoShoudeChituteiruJiannoteitukuProcedure.execute(world, entity.getX(), entity.getY(), entity.getZ(), entity);
	}
	
	@Override
	public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		if (!attacker.level().isClientSide) {
			// ターゲットが呪われているかチェック（既にウィザー効果があるかカスタムNBTタグ）
			boolean isCursed = target.hasEffect(MobEffects.WITHER) || 
							   (target.getPersistentData().contains("Feyn") && 
							    "cursed".equals(target.getPersistentData().getString("Feyn")));
			
			if (isCursed) {
				// 呪われた敵には強化ウィザー効果 — DoT (カスタムダメージ) + 移動低下/弱体化 (attribute modifier)
				SpecialDebuffHandler.applyWither(target, 300, 1.0f);
				SpecialDebuffHandler.applySlowness(target, 200, 1);
				SpecialDebuffHandler.applyWeakness(target, 200, 1);
				// 即座にウィザーダメージ
				target.hurt(target.damageSources().wither(), 6.0f);
				// 呪いをNBTタグに設定（他の武器でも認識できるように）
				target.getPersistentData().putString("Feyn", "cursed");
			} else {
				// 通常のウィザー効果 — DoT (カスタムダメージ) で
				SpecialDebuffHandler.applyWither(target, 140, 0.5f);
				
				// 初めて呪う場合はNBTタグを設定
				target.getPersistentData().putString("Feyn", "cursed");
			}
			
			// ウィザースケルトンのような暗黒パーティクル
			if (VersionHelper.getLevel(attacker) instanceof ServerLevel serverLevel) {
				// 呪われた敵にはより強力なエフェクト
				int particleCount = isCursed ? 30 : 15;
				
				// 黒い煙のエフェクト
				for (int i = 0; i < particleCount; i++) {
					double offsetX = (Math.random() - 0.5) * 1.5;
					double offsetY = Math.random() * 2;
					double offsetZ = (Math.random() - 0.5) * 1.5;
					serverLevel.sendParticles(ParticleTypes.SMOKE,
						target.getX() + offsetX,
						target.getY() + offsetY,
						target.getZ() + offsetZ,
						1, 0, 0, 0, 0.05);
				}
				
				// ウィザーの頭蓋骨パーティクル
				serverLevel.sendParticles(ParticleTypes.SOUL,
					target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
					8, 0.3, 0.3, 0.3, 0.02);
				
				// 暗黒のオーラ
				serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
					target.getX(), target.getY(), target.getZ(),
					5, 0.5, 0.5, 0.5, 0.01);
			}
			
			// ウィザーのサウンド効果
			attacker.level().playSound(null, target.getX(), target.getY(), target.getZ(),
				SoundEvents.WITHER_SKELETON_HURT, SoundSource.PLAYERS, 0.7f, 0.8f);
			
			// 呪われた敵には高確率（30%）、通常は低確率（10%）で追加のウィザー爆発
			double explosionChance = isCursed ? 0.3 : 0.1;
			if (Math.random() < explosionChance) {
				if (VersionHelper.getLevel(attacker) instanceof ServerLevel serverLevel) {
					// ウィザー爆発エフェクト
					serverLevel.sendParticles(ParticleTypes.EXPLOSION,
						target.getX(), target.getY() + 1, target.getZ(),
						1, 0, 0, 0, 0);
				}
				
				// 爆発ダメージ
				target.hurt(target.damageSources().wither(), 4.0f);
				
				// 爆発音
				attacker.level().playSound(null, target.getX(), target.getY(), target.getZ(),
					SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 0.5f, 1.0f);
			}
		}
		
		return super.hurtEnemy(stack, target, attacker);
	}
}

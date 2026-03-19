
package minecraftarmorweapon.item;

import minecraftarmorweapon.util.VersionHelper;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;

import minecraftarmorweapon.procedures.IronKatanaturuwoShoudeChituteiruJiannoteitukuProcedure;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 霊刀 - Spirit Blade
 * サイレントヒルFの霊刀にインスパイアされた呪いの刀
 * 狐の信仰の対象。怨念の力を宿す。
 *
 * ■特殊効果:
 * - 呪い(cursed)のFeyn NBTが付与される
 * - 黒いモヤ（パーティクル）が常に出る
 * - 怨念の力: 他の武器で敵を攻撃すると霊刀の耐久度が回復
 * - 攻撃時に怨念ダメージ追加 + 移動速度低下
 */
public class ReitouItem extends SwordItem {
	public ReitouItem() {
		super(new Tier() {
			public int getUses() {
				return 500; // 耐久度あり（怨念で回復する仕組み）
			}

			public float getSpeed() {
				return 4f;
			}

			public float getAttackDamageBonus() {
				return 5f;
			}

			public int getLevel() {
				return 3;
			}

			public int getEnchantmentValue() {
				return 3;
			}

			public Ingredient getRepairIngredient() {
				return Ingredient.of();
			}
		}, 3, -1.4f, new Item.Properties().rarity(Rarity.EPIC));
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return true; // 信仰の対象 - 常にエンチャントの光
	}

	@Override
	public void inventoryTick(ItemStack itemstack, Level world, Entity entity, int slot, boolean selected) {
		super.inventoryTick(itemstack, world, entity, slot, selected);

		// cursed Feyn NBTを常に付与
		if (!itemstack.hasTag()) {
			itemstack.setTag(new CompoundTag());
		}
		if (!"cursed".equals(itemstack.getTag().getString("Feyn"))) {
			itemstack.getTag().putString("Feyn", "cursed");
		}

		if (selected) {
			IronKatanaturuwoShoudeChituteiruJiannoteitukuProcedure.execute(world, entity.getX(), entity.getY(), entity.getZ(), entity);

			// 黒いモヤのパーティクル（手に持っている時）
			if (!world.isClientSide && world instanceof ServerLevel serverLevel) {
				// 刀の周囲に黒い煙を出す
				if (world.getGameTime() % 3 == 0) { // 3tick毎
					double offsetX = (Math.random() - 0.5) * 0.6;
					double offsetY = Math.random() * 0.8 + 0.5;
					double offsetZ = (Math.random() - 0.5) * 0.6;
					serverLevel.sendParticles(ParticleTypes.SMOKE,
						entity.getX() + offsetX,
						entity.getY() + offsetY,
						entity.getZ() + offsetZ,
						1, 0, 0.02, 0, 0.01);

					// ソウルパーティクル（怨念の可視化）
					if (world.getGameTime() % 9 == 0) {
						serverLevel.sendParticles(ParticleTypes.SOUL,
							entity.getX() + offsetX,
							entity.getY() + offsetY + 0.3,
							entity.getZ() + offsetZ,
							1, 0, 0.01, 0, 0.005);
					}
				}
			}
		}
	}

	@Override
	public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		if (!attacker.level().isClientSide) {
			// ターゲットが既に呪われているかチェック
			boolean isCursed = target.hasEffect(MobEffects.WITHER) ||
							   (target.getPersistentData().contains("Feyn") &&
							    "cursed".equals(target.getPersistentData().getString("Feyn")));

			// 怨念の呪いを付与
			target.getPersistentData().putString("Feyn", "cursed");

			if (isCursed) {
				// 呪われた敵には強化効果
				target.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 1));
				target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 300, 2));
				target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1));

				// 怨念の追加ダメージ
				target.hurt(target.damageSources().magic(), 4.0f);
			} else {
				// 初撃: 軽い呪い効果
				target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 0));
				target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 1));
			}

			// 黒いモヤのエフェクト（攻撃時）
			if (VersionHelper.getLevel(attacker) instanceof ServerLevel serverLevel) {
				int particleCount = isCursed ? 25 : 12;

				// 黒い煙のエフェクト
				for (int i = 0; i < particleCount; i++) {
					double offsetX = (Math.random() - 0.5) * 1.5;
					double offsetY = Math.random() * 2;
					double offsetZ = (Math.random() - 0.5) * 1.5;
					serverLevel.sendParticles(ParticleTypes.SMOKE,
						target.getX() + offsetX,
						target.getY() + offsetY,
						target.getZ() + offsetZ,
						1, 0, 0, 0, 0.03);
				}

				// 大きい煙（黒いモヤ）
				serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
					target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
					isCursed ? 10 : 5, 0.4, 0.4, 0.4, 0.02);

				// ソウルパーティクル（怨念の霊魂）
				serverLevel.sendParticles(ParticleTypes.SOUL,
					target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
					isCursed ? 8 : 3, 0.3, 0.3, 0.3, 0.02);
			}

			// 不気味なサウンド
			attacker.level().playSound(null, target.getX(), target.getY(), target.getZ(),
				SoundEvents.WITHER_SKELETON_HURT, SoundSource.PLAYERS, 0.6f, 0.5f);

			// 狐の鳴き声（低確率）
			if (Math.random() < 0.15) {
				attacker.level().playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
					SoundEvents.FOX_SCREECH, SoundSource.PLAYERS, 0.4f, 0.6f);
			}
		}

		return super.hurtEnemy(stack, target, attacker);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, world, tooltip, flag);
		tooltip.add(Component.translatable("tooltip.minecraft_armor_weapon.reitou.desc1")
				.setStyle(Style.EMPTY.withColor(TextColor.parseColor("#8844AA")).withItalic(true)));
		tooltip.add(Component.translatable("tooltip.minecraft_armor_weapon.reitou.desc2")
				.setStyle(Style.EMPTY.withColor(TextColor.parseColor("#666666")).withItalic(false)));
		tooltip.add(Component.translatable("tooltip.minecraft_armor_weapon.reitou.desc3")
				.setStyle(Style.EMPTY.withColor(TextColor.parseColor("#44AA44")).withItalic(false)));
	}
}

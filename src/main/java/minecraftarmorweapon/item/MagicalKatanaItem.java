
package minecraftarmorweapon.item;

import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.api.distmarker.Dist;

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

import minecraftarmorweapon.procedures.MagicalKatanamobugaturudeGongJisaretatokiProcedure;
import minecraftarmorweapon.procedures.IronKatanaturuwoShoudeChituteiruJiannoteitukuProcedure;
import minecraftarmorweapon.procedures.IronKatanaYoukuritukusitatokiProcedure;
import minecraftarmorweapon.procedures.MagicKatanaSpecialChargeProcedure;

import minecraftarmorweapon.init.MinecraftArmorWeaponModTabs;

import net.minecraft.world.item.UseAnim;

public class MagicalKatanaItem extends SwordItem {
	public MagicalKatanaItem() {
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
				return 9;
			}

			public Ingredient getRepairIngredient() {
				return Ingredient.of();
			}
		}, 3, -1.4f, new Item.Properties().tab(MinecraftArmorWeaponModTabs.TAB_WEAPON));
	}

	@Override
	public boolean hurtEnemy(ItemStack itemstack, LivingEntity entity, LivingEntity sourceentity) {
		boolean retval = super.hurtEnemy(itemstack, entity, sourceentity);
		MagicalKatanamobugaturudeGongJisaretatokiProcedure.execute(entity.level, entity, sourceentity);
		return retval;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player entity, InteractionHand hand) {
		ItemStack itemstack = entity.getItemInHand(hand);
		entity.startUsingItem(hand);
		return InteractionResultHolder.consume(itemstack);
	}

	@Override
	public void releaseUsing(ItemStack stack, Level world, LivingEntity entity, int timeLeft) {
		if (entity instanceof Player player) {
			int chargeDuration = this.getUseDuration(stack) - timeLeft;
			float chargePercent = Math.min(chargeDuration / 20.0f, 1.0f); // 最大1秒でフルチャージ

			if (chargePercent >= 0.5f) { // 50%以上チャージで発動
				MagicKatanaSpecialChargeProcedure.execute(world, player.getX(), player.getY(), player.getZ(), player, chargePercent);
			}
		}
	}

	@Override
	public int getUseDuration(ItemStack stack) {
		return 72000; // 最大使用時間（3600秒）
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return UseAnim.BOW; // 弓のような使用アニメーション
	}

	@Override
	public void inventoryTick(ItemStack itemstack, Level world, Entity entity, int slot, boolean selected) {
		super.inventoryTick(itemstack, world, entity, slot, selected);
		if (selected)
			IronKatanaturuwoShoudeChituteiruJiannoteitukuProcedure.execute(world, entity.getX(), entity.getY(), entity.getZ(), entity);
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public boolean isFoil(ItemStack itemstack) {
		return true;
	}
}

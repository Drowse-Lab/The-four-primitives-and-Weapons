
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

import minecraftarmorweapon.procedures.NomalwazaProcedure;
import minecraftarmorweapon.procedures.IronKatanaturuwoShoudeChituteiruJiannoteitukuProcedure;

import minecraftarmorweapon.init.MinecraftArmorWeaponModTabs;

public class ReplicaSwordOfLightItem extends SwordItem {
	// RPGish-HPDisplay データパック連携用タグ
	private static final String TAG_HOLY_DAMAGE = "minecraft_armor_weapon.mh_rpgish.holy_damage";

	public ReplicaSwordOfLightItem() {
		super(new Tier() {
			public int getUses() {
				return 250;
			}

			public float getSpeed() {
				return 4f;
			}

			public float getAttackDamageBonus() {
				return 5.5f;
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
		}, 0, -2.4f, new Item.Properties());
	}

	@Override
	public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		// RPGish-HPDisplay 用: 聖属性ダメージタグを付与（金色で表示される）
		target.addTag(TAG_HOLY_DAMAGE);
		return super.hurtEnemy(stack, target, attacker);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player entity, InteractionHand hand) {
		InteractionResultHolder<ItemStack> ar = super.use(world, entity, hand);
		NomalwazaProcedure.execute();
		return ar;
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

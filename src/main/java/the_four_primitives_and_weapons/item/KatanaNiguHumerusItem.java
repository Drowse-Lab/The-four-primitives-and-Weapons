
package the_four_primitives_and_weapons.item;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.network.chat.Component;
import org.joml.Vector3f;

import the_four_primitives_and_weapons.procedures.IronKatanaturuwoShoudeChituteiruJiannoteitukuProcedure;
import the_four_primitives_and_weapons.skill.GiantBoneArmSkill;
import the_four_primitives_and_weapons.skill.PlayerSkillData;

import java.util.List;

public class KatanaNiguHumerusItem extends SwordItem {

	/** 特殊技発動に必要な最小溜め時間 (tick)。River of Blood 風の右クリック溜め。 */
	private static final int MIN_CHARGE_TICKS = 20; // 1秒

	public KatanaNiguHumerusItem() {
		super(new Tier() {
			public int getUses() {
				return 0;
			}

			public float getSpeed() {
				return 4f;
			}

			public float getAttackDamageBonus() {
				return 7f;
			}

			public int getLevel() {
				return 1;
			}

			public int getEnchantmentValue() {
				return 10;
			}

			public Ingredient getRepairIngredient() {
				return Ingredient.of();
			}
		}, 3, -2.4f, new Item.Properties());
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player entity, InteractionHand hand) {
		ItemStack stack = entity.getItemInHand(hand);
		// River of Blood 方式: 右クリックに特殊技「巨骨の腕」が割り当てられている時だけ溜め開始。
		PlayerSkillData.SkillStorage sd = PlayerSkillData.getSkillData(entity);
		if (sd == null)
			return super.use(world, entity, hand);
		String motion = sd.getMotionForWeapon(PlayerSkillData.AttackSlot.RIGHT_CLICK, entity.getMainHandItem());
		if (!"giant_bone_arm_special".equals(motion))
			return super.use(world, entity, hand);
		// クールダウン中は溜め開始しない
		if (entity.getCooldowns().isOnCooldown(stack.getItem()))
			return InteractionResultHolder.fail(stack);
		entity.startUsingItem(hand);
		return InteractionResultHolder.consume(stack);
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		// CROSSBOW は一人称レンダラに case が無く溜め中に見えなくなるため使わない。
		// BOW は弓を引く動作で「溜め」感が出て、かつ一人称でちゃんと表示される。
		return UseAnim.BOW;
	}

	@Override
	public int getUseDuration(ItemStack stack) {
		return 72000;
	}

	/** 溜め中の演出 (骨色のチャージ — 閾値到達で合図音)。 */
	@Override
	public void onUseTick(Level world, LivingEntity entity, ItemStack stack, int remainingUseTicks) {
		if (!(entity instanceof Player player))
			return;
		int charged = getUseDuration(stack) - remainingUseTicks;
		float progress = Math.min(1.0f, (float) charged / (float) MIN_CHARGE_TICKS);
		boolean hitThreshold = (charged == MIN_CHARGE_TICKS);
		// 骨色: 灰白 → 純白
		the_four_primitives_and_weapons.client.event.ChargeParticleEmitter.emit(
				world, player, progress,
				new Vector3f(0.82f, 0.80f, 0.70f),
				new Vector3f(1.0f, 1.0f, 0.95f),
				charged, hitThreshold);
	}

	@Override
	public void releaseUsing(ItemStack stack, Level world, LivingEntity entity, int timeLeft) {
		int charged = getUseDuration(stack) - timeLeft;
		if (!world.isClientSide && charged >= MIN_CHARGE_TICKS && entity instanceof Player player) {
			GiantBoneArmSkill.fire(player);
		}
	}

	@Override
	public void appendHoverText(ItemStack itemstack, Level world, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(itemstack, world, list, flag);
		list.add(Component.literal("この刀実は……"));
		list.add(Component.literal("柄の部分が二グ様の上腕骨でできてるんだ"));
		list.add(Component.literal("§6特殊武器: §7スキル設定で右クリックに「巨骨の腕」を割り当て"));
		list.add(Component.literal("§7右クリック長押し: 巨骨の腕 — 薙ぎ払い→地叩き"));
	}

	@Override
	public void inventoryTick(ItemStack itemstack, Level world, Entity entity, int slot, boolean selected) {
		super.inventoryTick(itemstack, world, entity, slot, selected);
		if (selected)
			IronKatanaturuwoShoudeChituteiruJiannoteitukuProcedure.execute(world, entity.getX(), entity.getY(), entity.getZ(), entity);
	}
}

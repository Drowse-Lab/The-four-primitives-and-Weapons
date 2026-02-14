
package minecraftarmorweapon.item;

import net.minecraft.world.level().Level;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.util.RandomSource;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;

import minecraftarmorweapon.procedures.IronKatanaturuwoShoudeChituteiruJiannoteitukuProcedure;
import minecraftarmorweapon.entity.LokiDisarmEntity;
import minecraftarmorweapon.entity.LokiDecoydasuEntity;

import minecraftarmorweapon.init.MinecraftArmorWeaponModTabs;

public class LokiTheTricksterItem extends SwordItem {
	private static final String MODE_TAG = "LokiMode";
	private static final String DISARM_MODE = "disarm";
	private static final String DECOY_MODE = "decoy";
	private static final int CHARGE_TIME = 20; // 1秒(20tick)
	private static final int HUNGER_COST = 2; // 満腹度コスト(1つ分)

	public LokiTheTricksterItem() {
		super(new Tier() {
			public int getUses() {
				return 0;
			}

			public float getSpeed() {
				return 4f;
			}

			public float getAttackDamageBonus() {
				return 2f;
			}

			public int getLevel() {
				return 1;
			}

			public int getEnchantmentValue() {
				return 2;
			}

			public Ingredient getRepairIngredient() {
				return Ingredient.of();
			}
		}, 3, -1.4f, new Item.Properties().tab(MinecraftArmorWeaponModTabs.TAB_CHUZUME_TAB));
	}

	// モード取得（publicに変更してイベントハンドラーから使える様に）
	public static String getMode(ItemStack stack) {
		CompoundTag tag = stack.getOrCreateTag();
		if (!tag.contains(MODE_TAG)) {
			tag.putString(MODE_TAG, DISARM_MODE); // デフォルトはdisarm
		}
		return tag.getString(MODE_TAG);
	}

	// モード設定（publicメソッド追加）
	public static void setMode(ItemStack stack, String mode) {
		CompoundTag tag = stack.getOrCreateTag();
		tag.putString(MODE_TAG, mode);
	}

	// モード切り替え（外部から呼び出せるようにpublic staticに変更）
	public static void toggleMode(ItemStack stack) {
		String currentMode = getMode(stack);
		String newMode = currentMode.equals(DISARM_MODE) ? DECOY_MODE : DISARM_MODE;
		setMode(stack, newMode);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
		ItemStack itemstack = player.getItemInHand(hand);

		// スニーク中は右クリックでモード切り替え（ただし、オフハンドに鞘がある場合は無効）
		if (player.isShiftKeyDown()) {
			// オフハンドをチェック
			InteractionHand offHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
			ItemStack offHandItem = player.getItemInHand(offHand);

			// オフハンドに鞘がある場合はモード切り替えをスキップ（抜刀・納刀でモード切り替え）
			boolean hasSayaInOffHand = !offHandItem.isEmpty() &&
			                            offHandItem.getItem().getClass().getSimpleName().equals("SayaItem");

			if (!hasSayaInOffHand) {
				// 鞘を持っていない場合のみスニークでモード切り替え
				if (!world.isClientSide()) {
					toggleMode(itemstack);
					String newMode = getMode(itemstack);
					String modeName = newMode.equals(DISARM_MODE) ? "Disarm" : "Decoy";
					player.displayClientMessage(Component.literal("§e[スニーク] Loki Mode: " + modeName), true);
				}
				return InteractionResultHolder.success(itemstack);
			}
		}

		// 右クリックチャージは無効化 - 左クリック長押しシステムを使用
		return InteractionResultHolder.pass(itemstack);
	}

	// 右クリックチャージシステムは無効化 - ChargedAttackHandlerの左クリックシステムを使用
	/*
	@Override
	public void releaseUsing(ItemStack stack, Level world, LivingEntity entity, int timeLeft) {
		if (!(entity instanceof Player player)) {
			return;
		}

		int chargeTime = this.getUseDuration(stack) - timeLeft;

		// チャージ時間が足りない、または満腹度が足りない場合は不発
		if (chargeTime < CHARGE_TIME) {
			if (!world.isClientSide()) {
				player.displayClientMessage(Component.literal("チャージ不足!"), true);
			}
			return;
		}

		FoodData foodData = player.getFoodData();
		if (foodData.getFoodLevel() < HUNGER_COST) {
			if (!world.isClientSide()) {
				player.displayClientMessage(Component.literal("満腹度が足りません!"), true);
			}
			return;
		}

		// 満腹度を消費
		foodData.setFoodLevel(foodData.getFoodLevel() - HUNGER_COST);

		if (!world.isClientSide()) {
			String mode = getMode(stack);

			if (mode.equals(DISARM_MODE)) {
				// Disarmモード: ブーメラン発射
				LokiDisarmEntity.shoot(world, player, RandomSource.create(), 1.5f, 0, 0);
				player.displayClientMessage(Component.literal("Disarm発射!"), true);
			} else {
				// Decoyモード: デコイ召喚
				LokiDecoydasuEntity.shoot(world, player, RandomSource.create(), 0.3f, 0, 0);
				player.displayClientMessage(Component.literal("Decoy召喚!"), true);
			}
		}

		player.getCooldowns().addCooldown(this, 40); // 2秒のクールダウン
	}

	@Override
	public int getUseDuration(ItemStack stack) {
		return 72000; // 最大チャージ時間
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return UseAnim.BOW; // 弓のようなチャージアニメーション
	}
	*/

	@Override
	public void inventoryTick(ItemStack itemstack, Level world, Entity entity, int slot, boolean selected) {
		super.inventoryTick(itemstack, world, entity, slot, selected);
		if (selected) {
			IronKatanaturuwoShoudeChituteiruJiannoteitukuProcedure.execute(world, entity.getX(), entity.getY(), entity.getZ(), entity);
		}
	}

	// アイテムのツールチップにモードを追加
	@Override
	public net.minecraft.network.chat.Component getName(ItemStack stack) {
		String mode = getMode(stack);
		String modeName = mode.equals(DISARM_MODE) ? "§6[Disarm]" : "§b[Decoy]";
		return Component.literal(modeName + " ").append(super.getName(stack));
	}
}

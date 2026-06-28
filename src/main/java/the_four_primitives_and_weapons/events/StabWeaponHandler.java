package the_four_primitives_and_weapons.events;

import the_four_primitives_and_weapons.entity.StabbedWeaponEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 武器を地面に突き刺す ( 戦場の建築用 )。
 *   設置: メイン武器 + オフハンドに「戦地設営の杭」 でブロック上面を右クリック → 突き刺す ( 武器1本消費 )。
 *   回収: 突き刺さった武器を右クリック / 攻撃。
 *   プリセット: 杭をメインハンドに持って右クリック ( 空中 ) で「傾き・高さ」 プリセットを切替。
 */
@Mod.EventBusSubscriber(modid = "the_four_primitives_and_weapons")
public class StabWeaponHandler {

	private static final String TAG_PRESET = "StakePreset";

	/** プリセット: { 名前, 傾き(度), 高さオフセット(ブロック) }。 向きはプレイヤーの向きを使う。 */
	private static final String[] PRESET_NAME = {"直立", "やや傾け", "深く刺す", "高く掲げる", "横倒し"};
	private static final float[]  PRESET_TILT = { 5f,    22f,        8f,         5f,           80f };
	private static final double[] PRESET_HEIGHT = { 0.0,  0.0,       -0.35,       0.5,          0.0 };

	private static Item marker() {
		return the_four_primitives_and_weapons.init.KnifeExtrasRegistrar.BATTLE_STAKE.get();
	}

	private static int presetOf(ItemStack stake) {
		int i = stake.hasTag() ? stake.getTag().getInt(TAG_PRESET) : 0;
		return ((i % PRESET_NAME.length) + PRESET_NAME.length) % PRESET_NAME.length;
	}

	/** 杭をメインハンドに持って右クリック ( 空中など ) → プリセット切替。 */
	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
		ItemStack stake = event.getItemStack();
		if (stake.getItem() != marker()) return;
		Player player = event.getEntity();
		if (!player.level().isClientSide) {
			int next = (presetOf(stake) + 1) % PRESET_NAME.length;
			stake.getOrCreateTag().putInt(TAG_PRESET, next);
			player.displayClientMessage(Component.literal(
					"§e戦地設営の杭 プリセット: §f" + PRESET_NAME[next]
					+ " §7(傾き " + (int) PRESET_TILT[next] + "° / 高さ " + PRESET_HEIGHT[next] + ")"), true);
			player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.6f, 1.4f);
		}
		event.setCanceled(true);
		event.setCancellationResult(InteractionResult.sidedSuccess(player.level().isClientSide));
	}

	/** メインハンドに武器 + オフハンドに杭 でブロック上面を右クリック → その武器を地面に突き刺す ( 武器1本消費 )。 */
	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		if (event.getHand() != InteractionHand.MAIN_HAND) return;
		Player player = event.getEntity();

		ItemStack stake = player.getOffhandItem();          // オフハンド = 目印の杭
		if (stake.getItem() != marker()) return;            // 杭を持っている時だけ
		if (event.getFace() != Direction.UP) return;        // ブロック上面のみ

		ItemStack weapon = event.getItemStack();            // メインハンド = 刺す武器
		if (!DodgeAndBattouHandler.isWeapon(weapon)) return; // 武器 ( 刀/剣等 ) のみ

		Level level = event.getLevel();
		if (!level.isClientSide) {
			int p = presetOf(stake);                         // プリセットはオフハンドの杭に保存
			BlockPos pos = event.getPos();
			Vec3 hit = event.getHitVec() != null ? event.getHitVec().getLocation() : Vec3.atCenterOf(pos);
			double x = hit.x;
			double y = pos.getY() + 1.0 + PRESET_HEIGHT[p]; // 上面 + プリセット高さ
			double z = hit.z;

			StabbedWeaponEntity ent = new StabbedWeaponEntity(level);
			ent.setItem(weapon);                         // 刺さるのは武器
			ent.setStabYaw(player.getYRot() + 180f);     // 向き = プレイヤーの向き
			ent.setTilt(PRESET_TILT[p]);                  // 傾き = プリセット
			ent.moveTo(x, y, z, 0f, 0f);
			level.addFreshEntity(ent);

			level.playSound(null, x, y, z, SoundEvents.ARMOR_EQUIP_IRON, SoundSource.PLAYERS, 0.9f, 0.9f);
			level.playSound(null, x, y, z, SoundEvents.GRINDSTONE_USE, SoundSource.PLAYERS, 0.4f, 1.4f);

			if (!player.getAbilities().instabuild) weapon.shrink(1); // 武器を1本消費 ( 杭は消費しない )
		}
		event.setCanceled(true);
		event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
	}
}

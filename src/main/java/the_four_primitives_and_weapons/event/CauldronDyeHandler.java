package the_four_primitives_and_weapons.event;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.network.CauldronColorMessage;
import the_four_primitives_and_weapons.world.CauldronColorData;
import the_four_primitives_and_weapons.client.CauldronColorClient;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.Map;

/**
 * バニラの大釜（water_cauldron）に染料を入れると水が色付き、 複数入れると色が混ざる。
 * さらに 色付き大釜に染色可能防具（手袋付き上着など）を入れると その色に染められる。
 */
@Mod.EventBusSubscriber(modid = TheFourPrimitivesAndWeaponsMod.MODID)
public class CauldronDyeHandler {

	@SubscribeEvent
	public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
		if (event.getHand() != InteractionHand.MAIN_HAND) return;
		Level level = event.getLevel();
		BlockPos pos = event.getPos();
		BlockState state = level.getBlockState(pos);
		boolean isWaterCauldron = state.is(Blocks.WATER_CAULDRON);
		boolean isEmptyCauldron = state.is(Blocks.CAULDRON);
		if (!isWaterCauldron && !isEmptyCauldron) return;

		ItemStack held = event.getItemStack();

		// 水バケツで満たし直したら「新鮮な水」 → 保存色をリセット（普通のバニラ水色に戻す）。
		// バニラに満たさせるのでキャンセルしない。
		if (held.getItem() == net.minecraft.world.item.Items.WATER_BUCKET) {
			if (!level.isClientSide) {
				ServerLevel sl = (ServerLevel) level;
				CauldronColorData.get(sl).removeColor(pos);
				the_four_primitives_and_weapons.world.CauldronPotionData.get(sl).remove(pos); // ポーションも消す
				syncRemove(sl, pos);
			}
			return;
		}

		if (!isWaterCauldron) return; // 以降は水入り大釜のみ

		boolean isDye = held.getItem() instanceof DyeItem;
		boolean isDyeable = held.getItem() instanceof DyeableLeatherItem;
		boolean isSaya = the_four_primitives_and_weapons.util.SayaDesign.isSaya(held);
		if (!isDye && !isDyeable && !isSaya) return;

		// 現在色（両サイドで一致するよう、 サイドごとのキャッシュ/データから取得）
		Integer cur = level.isClientSide
				? CauldronColorClient.get(pos)
				: CauldronColorData.get((ServerLevel) level).getColor(pos);

		// 染色可能アイテムで、 まだ色が無い大釜 → バニラの「洗浄」に任せる（キャンセルしない）
		if (isDyeable && !isDye && cur == null) return;
		// 模様も地色も無い鞘を ただの水大釜に入れても何もしない（水を無駄に減らさない）
		if (isSaya && !isDye && cur == null
				&& !the_four_primitives_and_weapons.util.SayaDesign.hasBase(held)
				&& the_four_primitives_and_weapons.util.SayaDesign.patternCount(held) <= 0) return;

		event.setCanceled(true);
		event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
		if (level.isClientSide) return;

		ServerLevel sl = (ServerLevel) level;
		// ポーションが貯まっている大釜は染色に使わない ( 革防具等を右クリックしても何も起きない )。
		//   イベントは上で既にキャンセル済みなので、 バニラの洗浄も起きずに「無反応」 になる。
		if (the_four_primitives_and_weapons.world.CauldronPotionData.get(sl).has(pos)) return;
		Player player = event.getEntity();
		CauldronColorData data = CauldronColorData.get(sl);

		if (isDye) {
			int dye = dyeColor((DyeItem) held.getItem());
			int n = data.getCount(pos);
			// 投入数で重み付け平均: これまでの色を n、 新しい染料を 1 の重みで混ぜる。
			// 染料が多いほど 1 個の影響が小さくなり、 白を1個入れても一気に白くならない。
			int mixed = (cur == null || n <= 0) ? dye : mixWeighted(cur, n, dye);
			data.setColor(pos, mixed, n + 1);
			syncSet(sl, pos, mixed);
			if (!player.getAbilities().instabuild) held.shrink(1);
			sl.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.7f, 1.4f);
			sl.sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 0.85, pos.getZ() + 0.5,
					12, 0.25, 0.05, 0.25, 0.0);
		} else if (isSaya) {
			// 鞘を大釜に浸す。 色付き水なら地色を染める。
			// ただの水なら「洗う」: 旗のように 上に付けた模様を 1枚ずつ剥がし、
			// 模様が無くなったら地色を落とす ( = 一個前の染色に戻す )。
			if (cur != null) {
				the_four_primitives_and_weapons.util.SayaDesign.setBaseColorRgb(held, cur);
			} else if (!the_four_primitives_and_weapons.util.SayaDesign.removeLastPattern(held)) {
				if (held.hasTag()) held.getTag().remove(the_four_primitives_and_weapons.util.SayaDesign.BASE_KEY);
			}
			lowerLevel(sl, pos, state, data);
			sl.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.7f, 1.1f);
			sl.sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 0.85, pos.getZ() + 0.5,
					16, 0.25, 0.05, 0.25, 0.0);
		} else {
			// 色付き大釜に浸して染色。 水位を1減らし、 空になったら色リセット（バニラ準拠）
			DyeableLeatherItem dye = (DyeableLeatherItem) held.getItem();
			dye.setColor(held, cur);
			lowerLevel(sl, pos, state, data);
			sl.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.7f, 1.1f);
			sl.sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 0.85, pos.getZ() + 0.5,
					16, 0.25, 0.05, 0.25, 0.0);
		}
	}

	/** 水位を1減らす。 空になったら通常の大釜に戻し、 保存色も消す。 */
	private static void lowerLevel(ServerLevel sl, BlockPos pos, BlockState state, CauldronColorData data) {
		int lvl = state.getValue(LayeredCauldronBlock.LEVEL);
		if (lvl <= 1) {
			sl.setBlockAndUpdate(pos, Blocks.CAULDRON.defaultBlockState());
			data.removeColor(pos);
			syncRemove(sl, pos);
		} else {
			sl.setBlockAndUpdate(pos, state.setValue(LayeredCauldronBlock.LEVEL, lvl - 1));
		}
	}

	// 染料 → RGB int
	private static int dyeColor(DyeItem item) {
		float[] c = item.getDyeColor().getTextureDiffuseColors();
		return ((int) (c[0] * 255) << 16) | ((int) (c[1] * 255) << 8) | (int) (c[2] * 255);
	}

	// 2色を平均で混ぜる
	private static int mix(int a, int b) {
		int r = (((a >> 16) & 255) + ((b >> 16) & 255)) / 2;
		int g = (((a >> 8) & 255) + ((b >> 8) & 255)) / 2;
		int bl = ((a & 255) + (b & 255)) / 2;
		return (r << 16) | (g << 8) | bl;
	}

	/** これまでの色 a ( 重み wA ) に 新しい色 b ( 重み1 ) を足した加重平均。 */
	private static int mixWeighted(int a, int wA, int b) {
		int r = ((((a >> 16) & 255) * wA) + ((b >> 16) & 255)) / (wA + 1);
		int g = ((((a >> 8) & 255) * wA) + ((b >> 8) & 255)) / (wA + 1);
		int bl = (((a & 255) * wA) + (b & 255)) / (wA + 1);
		return (r << 16) | (g << 8) | bl;
	}

	private static void syncSet(ServerLevel sl, BlockPos pos, int color) {
		TheFourPrimitivesAndWeaponsMod.PACKET_HANDLER.send(
				PacketDistributor.TRACKING_CHUNK.with(() -> sl.getChunkAt(pos)),
				new CauldronColorMessage(false, new long[]{pos.asLong()}, new int[]{color}));
	}

	private static void syncRemove(ServerLevel sl, BlockPos pos) {
		TheFourPrimitivesAndWeaponsMod.PACKET_HANDLER.send(
				PacketDistributor.TRACKING_CHUNK.with(() -> sl.getChunkAt(pos)),
				new CauldronColorMessage(false, new long[]{pos.asLong()}, new int[]{CauldronColorMessage.REMOVE}));
	}

	private static void sendBulk(ServerPlayer sp) {
		if (!(sp.level() instanceof ServerLevel sl)) return;
		Map<Long, Integer> all = CauldronColorData.get(sl).all();
		long[] pos = new long[all.size()];
		int[] col = new int[all.size()];
		int i = 0;
		for (Map.Entry<Long, Integer> e : all.entrySet()) {
			pos[i] = e.getKey();
			col[i] = e.getValue();
			i++;
		}
		TheFourPrimitivesAndWeaponsMod.PACKET_HANDLER.send(
				PacketDistributor.PLAYER.with(() -> sp),
				new CauldronColorMessage(true, pos, col));
	}

	@SubscribeEvent
	public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer sp) sendBulk(sp);
	}

	@SubscribeEvent
	public static void onChangeDim(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (event.getEntity() instanceof ServerPlayer sp) sendBulk(sp);
	}
}

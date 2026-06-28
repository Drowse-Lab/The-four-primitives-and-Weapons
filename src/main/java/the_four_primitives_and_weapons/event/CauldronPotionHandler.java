package the_four_primitives_and_weapons.event;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.network.CauldronColorMessage;
import the_four_primitives_and_weapons.world.CauldronBloodData;
import the_four_primitives_and_weapons.world.CauldronColorData;
import the_four_primitives_and_weapons.world.CauldronPotionData;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * バニラの大釜にポーションを貯める / 混ぜる / 取り出す。
 *  - ポーション瓶を右クリック: 大釜に 1 回分（=水位1）注ぐ。 別のポーションを注ぐと中で混ざる。
 *  - 空き瓶を右クリック: 混ざったポーションを 1 回分取り出す（水位1減）。
 * 量は大釜の LEVEL（最大3=3回分）で管理、 混合エフェクトは {@link CauldronPotionData}、
 * 見た目の色は {@link CauldronColorData}（染色と共通）。
 */
@Mod.EventBusSubscriber(modid = TheFourPrimitivesAndWeaponsMod.MODID)
public class CauldronPotionHandler {

	private static final int MAX_LEVEL = 3; // 大釜の容量（=最大回数）

	/** ポーション大釜から色付きのモヤモヤ ( ENTITY_EFFECT 渦 ) を立ち上らせる。 */
	@SubscribeEvent
	public static void onLevelTick(net.minecraftforge.event.TickEvent.LevelTickEvent event) {
		if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
		if (!(event.level instanceof ServerLevel sl)) return;
		if ((sl.getGameTime() % 6) != 0) return; // 間引き
		CauldronPotionData pdata = CauldronPotionData.get(sl);
		if (pdata.positions().isEmpty()) return;
		CauldronColorData cdata = CauldronColorData.get(sl);
		for (long key : pdata.positions()) {
			BlockPos pos = BlockPos.of(key);
			if (!sl.isLoaded(pos)) continue;
			BlockState st = sl.getBlockState(pos);
			if (!st.is(Blocks.WATER_CAULDRON)) continue;
			Integer col = cdata.getColor(pos);
			if (col == null) continue;
			float r = ((col >> 16) & 0xFF) / 255.0f;
			float g = ((col >> 8) & 0xFF) / 255.0f;
			float b = (col & 0xFF) / 255.0f;
			int waterLevel = st.getValue(LayeredCauldronBlock.LEVEL);
			double surfaceY = pos.getY() + 0.3 + waterLevel * 0.18; // 水面付近
			for (int i = 0; i < 2; i++) {
				double px = pos.getX() + 0.3 + sl.random.nextDouble() * 0.4;
				double pz = pos.getZ() + 0.3 + sl.random.nextDouble() * 0.4;
				// count=0 + (r,g,b) で色付きの effect 粒子になる ( ポーションのモヤモヤ )
				sl.sendParticles(ParticleTypes.ENTITY_EFFECT, px, surfaceY, pz, 0, r, g, b, 1.0);
			}

			// 大釜に入っている生物に、 入っている間だけ そのポーションのエフェクトを付与
			//   ( 短時間で再付与し続けるので、 出ると間もなく切れる = 入っている瞬間だけ )
			List<MobEffectInstance> effs = pdata.getEffects(pos);
			if (effs != null && !effs.isEmpty()) {
				net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(pos);
				for (net.minecraft.world.entity.LivingEntity le :
						sl.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, box)) {
					for (MobEffectInstance e : effs) {
						le.addEffect(new MobEffectInstance(e.getEffect(), 40, e.getAmplifier(), false, true, true));
					}
				}
			}
		}
	}

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
		boolean isPotion = held.getItem() == Items.POTION && !PotionUtils.getMobEffects(held).isEmpty();
		boolean isEmptyBottle = held.getItem() == Items.GLASS_BOTTLE;
		if (!isPotion && !isEmptyBottle) return;

		// 空き瓶での取り出しは「ポーション大釜」 のみ介入（普通の水大釜はバニラの水汲みに任せる）
		if (isEmptyBottle) {
			if (level.isClientSide) {
				// クライアントは介入可否をサーバーに合わせるため、 水大釜以外は触らない
				if (!isWaterCauldron) return;
			} else {
				if (!isWaterCauldron || !CauldronPotionData.get((ServerLevel) level).has(pos)) return;
			}
		}

		// 血液が貯まっている大釜にはポーションを注がない（混在防止）
		if (isPotion && !level.isClientSide && CauldronBloodData.get((ServerLevel) level).has(pos)) return;

		event.setCanceled(true);
		event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
		if (level.isClientSide) return;

		ServerLevel sl = (ServerLevel) level;
		Player player = event.getEntity();
		CauldronPotionData pdata = CauldronPotionData.get(sl);
		CauldronColorData cdata = CauldronColorData.get(sl);

		if (isPotion) {
			pourIn(sl, pos, state, held, player, pdata, cdata, isWaterCauldron);
		} else {
			fillOut(sl, pos, state, held, player, pdata, cdata);
		}
	}

	/** ポーションを 1 回分（水位+1）注ぐ。 既存と混ぜる。 */
	private static void pourIn(ServerLevel sl, BlockPos pos, BlockState state, ItemStack held, Player player,
	                            CauldronPotionData pdata, CauldronColorData cdata, boolean isWaterCauldron) {
		int curLevel = isWaterCauldron ? state.getValue(LayeredCauldronBlock.LEVEL) : 0;
		if (curLevel >= MAX_LEVEL) {
			player.displayClientMessage(net.minecraft.network.chat.Component.literal("§c大釜がいっぱいです"), true);
			return;
		}

		List<MobEffectInstance> merged = merge(pdata.getEffects(pos), PotionUtils.getMobEffects(held));
		int newLevel = curLevel + 1;
		sl.setBlockAndUpdate(pos, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, newLevel));
		pdata.setEffects(pos, merged);

		int color = PotionUtils.getColor(merged);
		cdata.setColor(pos, color);
		syncColor(sl, pos, color);

		// ポーション瓶 → 空き瓶
		player.setItemInHand(InteractionHand.MAIN_HAND,
				ItemUtils.createFilledResult(held, player, new ItemStack(Items.GLASS_BOTTLE)));
		sl.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 0.8f, 1.0f);
		sl.sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 0.85, pos.getZ() + 0.5,
				14, 0.25, 0.05, 0.25, 0.0);
	}

	/** 混ざったポーションを 1 回分（水位-1）取り出す。 空になったら空の大釜へ。 */
	private static void fillOut(ServerLevel sl, BlockPos pos, BlockState state, ItemStack held, Player player,
	                             CauldronPotionData pdata, CauldronColorData cdata) {
		List<MobEffectInstance> effs = pdata.getEffects(pos);
		if (effs == null || effs.isEmpty()) return;

		ItemStack out = new ItemStack(Items.POTION);
		PotionUtils.setCustomEffects(out, copyOf(effs));
		// 混ざった色をガラス瓶 ( 液体 ) にも反映 ( 大釜の表示色をそのまま使う )
		Integer mixColor = cdata.getColor(pos);
		out.getOrCreateTag().putInt("CustomPotionColor",
				mixColor != null ? mixColor : PotionUtils.getColor(effs));

		int lvl = state.getValue(LayeredCauldronBlock.LEVEL);
		if (lvl <= 1) {
			sl.setBlockAndUpdate(pos, Blocks.CAULDRON.defaultBlockState());
			pdata.remove(pos);
			cdata.removeColor(pos);
			syncRemove(sl, pos);
		} else {
			sl.setBlockAndUpdate(pos, state.setValue(LayeredCauldronBlock.LEVEL, lvl - 1));
		}

		// 空き瓶 → ポーション
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemUtils.createFilledResult(held, player, out));
		sl.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.8f, 1.0f);
		sl.sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 0.85, pos.getZ() + 0.5,
				10, 0.25, 0.05, 0.25, 0.0);
	}

	/** 既存エフェクトに新規を混ぜる。 同じ効果は「強い方（amplifier→duration）」 を残す。 */
	private static List<MobEffectInstance> merge(List<MobEffectInstance> base, List<MobEffectInstance> add) {
		Map<MobEffect, MobEffectInstance> m = new LinkedHashMap<>();
		if (base != null) for (MobEffectInstance e : base) m.put(e.getEffect(), new MobEffectInstance(e));
		for (MobEffectInstance e : add) {
			MobEffectInstance ex = m.get(e.getEffect());
			if (ex == null
					|| e.getAmplifier() > ex.getAmplifier()
					|| (e.getAmplifier() == ex.getAmplifier() && e.getDuration() > ex.getDuration())) {
				m.put(e.getEffect(), new MobEffectInstance(e));
			}
		}
		return new ArrayList<>(m.values());
	}

	private static List<MobEffectInstance> copyOf(List<MobEffectInstance> src) {
		List<MobEffectInstance> out = new ArrayList<>(src.size());
		for (MobEffectInstance e : src) out.add(new MobEffectInstance(e));
		return out;
	}

	private static void syncColor(ServerLevel sl, BlockPos pos, int color) {
		TheFourPrimitivesAndWeaponsMod.PACKET_HANDLER.send(
				PacketDistributor.TRACKING_CHUNK.with(() -> sl.getChunkAt(pos)),
				new CauldronColorMessage(false, new long[]{pos.asLong()}, new int[]{color}));
	}

	private static void syncRemove(ServerLevel sl, BlockPos pos) {
		TheFourPrimitivesAndWeaponsMod.PACKET_HANDLER.send(
				PacketDistributor.TRACKING_CHUNK.with(() -> sl.getChunkAt(pos)),
				new CauldronColorMessage(false, new long[]{pos.asLong()}, new int[]{CauldronColorMessage.REMOVE}));
	}
}

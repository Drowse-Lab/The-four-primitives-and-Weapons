package the_four_primitives_and_weapons.event;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModItems;
import the_four_primitives_and_weapons.network.CauldronColorMessage;
import the_four_primitives_and_weapons.world.CauldronBloodData;
import the_four_primitives_and_weapons.world.CauldronColorData;
import the_four_primitives_and_weapons.world.CauldronPotionData;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/**
 * バニラの大釜に血液瓶（blood_bottle）を貯める / 取り出す。
 *  - 血液瓶を右クリック: 大釜に 1 回分（=水位+1）注ぐ。瓶のNBT（採血元UUID等）はそのまま保持される。
 *  - 空き瓶を右クリック: 最後に注いだ血液を 1 回分取り出す（水位-1）。NBTも復元される。
 * 量は大釜の LEVEL（最大3）で管理、中身は {@link CauldronBloodData}、見た目の色は {@link CauldronColorData}。
 */
@Mod.EventBusSubscriber(modid = TheFourPrimitivesAndWeaponsMod.MODID)
public class CauldronBloodHandler {

	private static final int MAX_LEVEL = 3;          // 大釜の容量（=最大回数）
	private static final int BLOOD_COLOR = 0x7A0E0E; // 血の色（水の着色用）

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
		boolean isBlood = held.getItem() == TheFourPrimitivesAndWeaponsModItems.BLOOD_BOTTLE.get();
		boolean isEmptyBottle = held.getItem() == Items.GLASS_BOTTLE;
		if (!isBlood && !isEmptyBottle) return;

		// ポーションが入っている大釜には干渉しない（混在防止）
		if (isBlood && !level.isClientSide && CauldronPotionData.get((ServerLevel) level).has(pos)) {
			return;
		}

		// 空き瓶での取り出しは「血液が貯まっている大釜」のみ介入
		if (isEmptyBottle) {
			if (level.isClientSide) {
				if (!isWaterCauldron) return;
			} else {
				if (!isWaterCauldron || !CauldronBloodData.get((ServerLevel) level).has(pos)) return;
			}
		}

		event.setCanceled(true);
		event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
		if (level.isClientSide) return;

		ServerLevel sl = (ServerLevel) level;
		Player player = event.getEntity();
		CauldronBloodData bdata = CauldronBloodData.get(sl);
		CauldronColorData cdata = CauldronColorData.get(sl);

		if (isBlood) {
			pourIn(sl, pos, state, held, player, bdata, cdata, isWaterCauldron);
		} else {
			fillOut(sl, pos, state, held, player, bdata, cdata);
		}
	}

	/** 血液瓶を 1 回分（水位+1）注ぐ。NBTを保持して貯める。 */
	private static void pourIn(ServerLevel sl, BlockPos pos, BlockState state, ItemStack held, Player player,
	                            CauldronBloodData bdata, CauldronColorData cdata, boolean isWaterCauldron) {
		int curLevel = isWaterCauldron ? state.getValue(LayeredCauldronBlock.LEVEL) : 0;
		if (curLevel >= MAX_LEVEL) {
			player.displayClientMessage(net.minecraft.network.chat.Component.literal("§c大釜がいっぱいです"), true);
			return;
		}

		// 血液瓶のNBT（採血元UUID等）をそのまま保存
		CompoundTag bottleTag = held.getTag();
		bdata.push(pos, bottleTag == null ? null : bottleTag.copy());

		int newLevel = curLevel + 1;
		sl.setBlockAndUpdate(pos, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, newLevel));

		cdata.setColor(pos, BLOOD_COLOR);
		syncColor(sl, pos, BLOOD_COLOR);

		// 血液瓶 → 空き瓶
		player.setItemInHand(InteractionHand.MAIN_HAND,
				ItemUtils.createFilledResult(held, player, new ItemStack(Items.GLASS_BOTTLE)));
		sl.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 0.8f, 1.0f);
		sl.sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 0.85, pos.getZ() + 0.5,
				14, 0.25, 0.05, 0.25, 0.0);
	}

	/** 貯めた血液を 1 回分（水位-1）取り出す。空になったら空の大釜へ。 */
	private static void fillOut(ServerLevel sl, BlockPos pos, BlockState state, ItemStack held, Player player,
	                             CauldronBloodData bdata, CauldronColorData cdata) {
		CompoundTag bottleTag = bdata.pop(pos);
		if (bottleTag == null) return;

		// 取り出した血液瓶（保存していたNBTを復元）
		ItemStack out = new ItemStack(TheFourPrimitivesAndWeaponsModItems.BLOOD_BOTTLE.get());
		if (!bottleTag.isEmpty()) {
			out.setTag(bottleTag);
		}

		int lvl = state.getValue(LayeredCauldronBlock.LEVEL);
		if (lvl <= 1 || !bdata.has(pos)) {
			sl.setBlockAndUpdate(pos, Blocks.CAULDRON.defaultBlockState());
			bdata.remove(pos);
			cdata.removeColor(pos);
			syncRemove(sl, pos);
		} else {
			sl.setBlockAndUpdate(pos, state.setValue(LayeredCauldronBlock.LEVEL, lvl - 1));
		}

		// 空き瓶 → 血液瓶
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemUtils.createFilledResult(held, player, out));
		sl.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.8f, 1.0f);
		sl.sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 0.85, pos.getZ() + 0.5,
				10, 0.25, 0.05, 0.25, 0.0);
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

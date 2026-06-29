package the_four_primitives_and_weapons.event;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * 既存の原木 ( minecraft:logs タグ ) から 生漆 ( raw_urushi ) を採る。 漆の木を新規追加せず、
 * どの木からでも採取できる。 採取方法は 2通り:
 *
 * <ul>
 *   <li><b>火打石 ( flint )</b> … 樹皮を掻いて採取。 消費なし、 連打防止のクールダウン付き ( 1個ずつ )。</li>
 *   <li><b>空きビン ( glass_bottle )</b> … 樹液を受けて採取。 ビンを1本消費して 生漆を得る ( クールダウンなし )。</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = TheFourPrimitivesAndWeaponsMod.MODID)
public class UrushiTapHandler {

	@SubscribeEvent
	public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
		if (event.getHand() != InteractionHand.MAIN_HAND) return;
		ItemStack held = event.getItemStack();
		boolean byFlint = held.getItem() == Items.FLINT;
		boolean byBottle = held.getItem() == Items.GLASS_BOTTLE;
		if (!byFlint && !byBottle) return;

		Level level = event.getLevel();
		BlockPos pos = event.getPos();
		BlockState state = level.getBlockState(pos);
		if (!state.is(BlockTags.LOGS)) return;

		event.setCanceled(true);
		event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
		if (level.isClientSide) return;

		Player player = event.getEntity();

		if (byFlint) {
			// 樹皮掻き: 消費なし・クールダウンで連打防止
			if (player.getCooldowns().isOnCooldown(Items.FLINT)) return;
			player.getCooldowns().addCooldown(Items.FLINT, 15);
		} else {
			// 樹液受け: ビンを1本消費
			if (!player.getAbilities().instabuild) held.shrink(1);
		}

		ItemHandlerHelper.giveItemToPlayer(player,
				new ItemStack(TheFourPrimitivesAndWeaponsModItems.RAW_URUSHI.get()));

		ServerLevel sl = (ServerLevel) level;
		sl.playSound(null, pos, SoundEvents.HONEY_BLOCK_SLIDE, SoundSource.BLOCKS, 0.6f, 1.1f);
		sl.sendParticles(ParticleTypes.FALLING_HONEY, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5,
				6, 0.25, 0.2, 0.25, 0.0);
	}
}

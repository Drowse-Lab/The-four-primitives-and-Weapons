package the_four_primitives_and_weapons.procedures;

import net.minecraftforge.items.ItemHandlerHelper;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.Advancement;

import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModItems;

import java.util.Iterator;

public class RpgBookGuiMagicSwordsmanTapProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		if (Mth.nextInt(RandomSource.create(), 0, 3) == 1) {
			if (entity instanceof Player _player) {
				ItemStack _setstack = new ItemStack(TheFourPrimitivesAndWeaponsModItems.MAGISCHES_FEEN_KATANA.get());
				_setstack.setCount(1);
				ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
			}
			if (entity instanceof Player _player) {
				ItemStack _setstack = new ItemStack(TheFourPrimitivesAndWeaponsModItems.FIREBALL.get());
				_setstack.setCount(1);
				ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
			}
			if (entity instanceof Player _player) {
				ItemStack _setstack = new ItemStack(TheFourPrimitivesAndWeaponsModItems.BUBBLESHOT.get());
				_setstack.setCount(1);
				ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
			}
			if (entity instanceof Player _player) {
				ItemStack _setstack = new ItemStack(TheFourPrimitivesAndWeaponsModItems.THUNDERBOLT.get());
				_setstack.setCount(1);
				ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
			}
			if (entity instanceof Player _player) {
				ItemStack _setstack = new ItemStack(TheFourPrimitivesAndWeaponsModItems.WIND_STEP.get());
				_setstack.setCount(1);
				ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
			}
			if (entity instanceof ServerPlayer _player) {
				Advancement _adv = _player.server.getAdvancements().getAdvancement(new ResourceLocation("the_four_primitives_and_weapons:sword_progress"));
				AdvancementProgress _ap = _player.getAdvancements().getOrStartProgress(_adv);
				if (!_ap.isDone()) {
					Iterator _iterator = _ap.getRemainingCriteria().iterator();
					while (_iterator.hasNext())
						_player.getAdvancements().award(_adv, (String) _iterator.next());
				}
			}
			if (entity instanceof Player _player)
				_player.closeContainer();
			if (entity instanceof Player _player) {
				ItemStack _stktoremove = new ItemStack(TheFourPrimitivesAndWeaponsModItems.RPG_BOOK.get());
				_player.getInventory().clearOrCountMatchingItems(p -> _stktoremove.getItem() == p.getItem(), 1, _player.inventoryMenu.getCraftSlots());
			}
		} else {
			if (entity instanceof Player _player) {
				ItemStack _setstack = new ItemStack(TheFourPrimitivesAndWeaponsModItems.DARKNESS_KATANA.get());
				_setstack.setCount(1);
				ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
			}
			if (entity instanceof Player _player) {
				ItemStack _setstack = new ItemStack(TheFourPrimitivesAndWeaponsModItems.DARKNESS.get());
				_setstack.setCount(1);
				ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
			}
			if (entity instanceof ServerPlayer _player) {
				Advancement _adv = _player.server.getAdvancements().getAdvancement(new ResourceLocation("the_four_primitives_and_weapons:sword_progress"));
				AdvancementProgress _ap = _player.getAdvancements().getOrStartProgress(_adv);
				if (!_ap.isDone()) {
					Iterator _iterator = _ap.getRemainingCriteria().iterator();
					while (_iterator.hasNext())
						_player.getAdvancements().award(_adv, (String) _iterator.next());
				}
			}
			if (entity instanceof Player _player)
				_player.closeContainer();
			if (entity instanceof Player _player) {
				ItemStack _stktoremove = new ItemStack(TheFourPrimitivesAndWeaponsModItems.RPG_BOOK.get());
				_player.getInventory().clearOrCountMatchingItems(p -> _stktoremove.getItem() == p.getItem(), 1, _player.inventoryMenu.getCraftSlots());
			}
		}
	}
}

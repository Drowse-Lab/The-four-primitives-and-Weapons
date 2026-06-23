
package the_four_primitives_and_weapons.network;

import the_four_primitives_and_weapons.util.VersionHelper;

import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.network.FriendlyByteBuf;

import the_four_primitives_and_weapons.procedures.RkigaYasaretatokiProcedure;
import the_four_primitives_and_weapons.events.DodgeAndBattouHandler;
import the_four_primitives_and_weapons.util.CuriosScabbardHelper;
import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;

import java.util.function.Supplier;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class RMessage {
	int type, pressedms;

	public RMessage(int type, int pressedms) {
		this.type = type;
		this.pressedms = pressedms;
	}

	public RMessage(FriendlyByteBuf buffer) {
		this.type = buffer.readInt();
		this.pressedms = buffer.readInt();
	}

	public static void buffer(RMessage message, FriendlyByteBuf buffer) {
		buffer.writeInt(message.type);
		buffer.writeInt(message.pressedms);
	}

	public static void handler(RMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			pressAction(context.getSender(), message.type, message.pressedms);
		});
		context.setPacketHandled(true);
	}

	public static void pressAction(Player entity, int type, int pressedms) {
		Level world = VersionHelper.getLevel(entity);
		// security measure to prevent arbitrary chunk generation
		if (!world.hasChunkAt(entity.blockPosition()))
			return;
		if (type == 0) {
			ItemStack mainHand = entity.getItemInHand(InteractionHand.MAIN_HAND);
			ItemStack offHand = entity.getItemInHand(InteractionHand.OFF_HAND);

			// 具現化 Magical Katana の R 短押し動作:
			//   抜刀は最優先でインベントリにある鞘から。 鞘がインベに 1 個も無ければ shatter。
			boolean hasSayaInInv = hasAnyScabbardInInventory(entity);
			if (the_four_primitives_and_weapons.events.MagicalKatanaCrystalHandler.isMaterialized(mainHand)
					&& !hasSayaInInv) {
				the_four_primitives_and_weapons.events.MagicalKatanaCrystalHandler.shatterOnSheathe(
						entity, mainHand, InteractionHand.MAIN_HAND);
				return;
			}
			if (the_four_primitives_and_weapons.events.MagicalKatanaCrystalHandler.isMaterialized(offHand)
					&& !hasSayaInInv) {
				the_four_primitives_and_weapons.events.MagicalKatanaCrystalHandler.shatterOnSheathe(
						entity, offHand, InteractionHand.OFF_HAND);
				return;
			}

			boolean mainIsBluepurge = isBluepurge(mainHand);
			boolean offIsBluepurge = isBluepurge(offHand);

			boolean mainIsWeapon = !mainIsBluepurge && DodgeAndBattouHandler.isWeapon(mainHand)
					&& !DodgeAndBattouHandler.isSaya(mainHand);
			boolean offIsWeapon = !offIsBluepurge && DodgeAndBattouHandler.isWeapon(offHand)
					&& !DodgeAndBattouHandler.isSaya(offHand);

			// 納刀チェック1: 武器+空の鞘を両手で持っていれば納刀
			if (mainIsWeapon && DodgeAndBattouHandler.isSaya(offHand)
					&& !CuriosScabbardHelper.hasStoredWeapon(offHand)) {
				DodgeAndBattouHandler.performSheathing(entity, mainHand, offHand,
						InteractionHand.MAIN_HAND, InteractionHand.OFF_HAND);
				return;
			} else if (offIsWeapon && DodgeAndBattouHandler.isSaya(mainHand)
					&& !CuriosScabbardHelper.hasStoredWeapon(mainHand)) {
				DodgeAndBattouHandler.performSheathing(entity, offHand, mainHand,
						InteractionHand.OFF_HAND, InteractionHand.MAIN_HAND);
				return;
			}

			// 納刀チェック2: Curiosスロットの空の鞘
			// メインハンドの納刀に失敗 (互換鞘なし等) してもオフハンドを試すよう、
			// else if ではなく独立した if にする。
			if (mainIsWeapon) {
				if (CuriosScabbardHelper.sheathIntoCurioSlot(entity, mainHand, InteractionHand.MAIN_HAND)) {
					return;
				}
			}
			if (offIsWeapon) {
				if (CuriosScabbardHelper.sheathIntoCurioSlot(entity, offHand, InteractionHand.OFF_HAND)) {
					return;
				}
			}

			// 納刀チェック3: インベントリスロットの空の鞘
			// 同上: メインハンド納刀失敗時もオフハンドを試す
			if (mainIsWeapon) {
				if (sheathIntoInventory(entity, mainHand, InteractionHand.MAIN_HAND)) {
					return;
				}
			}
			if (offIsWeapon) {
				if (sheathIntoInventory(entity, offHand, InteractionHand.OFF_HAND)) {
					return;
				}
			}

			// 抜刀チェック A: 利き手 (メインハンド) の満杯鞘 + オフハンド空 → 利き手の鞘から抜刀
			if (CuriosScabbardHelper.isScabbard(mainHand)
					&& CuriosScabbardHelper.hasStoredWeapon(mainHand)
					&& offHand.isEmpty()) {
				if (drawFromMainHandSaya(entity, mainHand)) return;
			}

			// 抜刀チェック B: 手が空で各所の満杯鞘があれば抜刀
			if (mainHand.isEmpty()) {
				// オフハンドの満杯鞘 → メインハンドへ抜刀
				if (CuriosScabbardHelper.isScabbard(offHand)
						&& CuriosScabbardHelper.hasStoredWeapon(offHand)) {
					if (drawFromOffHandSaya(entity, offHand)) return;
				}
				// どっちの手にも納刀済み鞘がない場合: Curios (belt → back) → インベントリ手前の順で抜刀
				if (drawFromCuriosFirst(entity)) return;
				if (drawFromInventoryFirst(entity)) return;
			}

			RkigaYasaretatokiProcedure.execute(entity);

			// R キー押下のタイミングで、 オーナーが自分の具現化 Magical Katana を一覧で chat に出す
			// ( = 納刀 UI の代替。 他人のインベントリにあるものも含める )
			if (entity instanceof net.minecraft.server.level.ServerPlayer sp) {
				the_four_primitives_and_weapons.events.MagicalKatanaCrystalHandler.listOwnedToChat(sp);
			}
		}
	}

	/**
	 * インベントリスロットの空の鞘に納刀を試みる
	 */
	private static boolean sheathIntoInventory(Player player, ItemStack weaponStack, InteractionHand weaponHand) {
		if (!DodgeAndBattouHandler.isWeapon(weaponStack)) return false;
		int selectedSlot = player.getInventory().selected;
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			if (i == selectedSlot || i == 40) continue;
			ItemStack scabbard = player.getInventory().getItem(i);
			if (!CuriosScabbardHelper.isScabbard(scabbard)) continue;
			if (CuriosScabbardHelper.hasStoredWeapon(scabbard)) continue;
			if (!CuriosScabbardHelper.isCompatible(weaponStack, scabbard)) continue;

			// 互換のある最初の空鞘に納刀
			net.minecraft.nbt.CompoundTag sheathTag = scabbard.getOrCreateTag();
			String storageKey = CuriosScabbardHelper.storageKeyFor(scabbard);
			net.minecraft.nbt.CompoundTag weaponData = weaponStack.save(new net.minecraft.nbt.CompoundTag());
			sheathTag.put(storageKey, weaponData);

			// 鞘の見た目は SayaModelWrapper が NBT (storageKey) を読んで動的に解決する。
			scabbard.setTag(sheathTag);

			player.setItemInHand(weaponHand, ItemStack.EMPTY);
			player.getInventory().setItem(i, scabbard);
			player.playSound(net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_IRON, 1.0F, 0.8F);
			player.displayClientMessage(net.minecraft.network.chat.Component.literal("§7納刀"), true);
			return true;
		}
		return false;
	}

	/**
	 * オフハンドの満杯鞘からメインハンドへ抜刀。空鞘はオフハンドに残す。
	 */
	private static boolean drawFromOffHandSaya(Player player, ItemStack scabbard) {
		ItemStack weapon = CuriosScabbardHelper.extractWeaponFromScabbard(scabbard);
		if (weapon.isEmpty()) return false;

		CuriosScabbardHelper.clearWeaponFromScabbard(scabbard);
		player.setItemInHand(InteractionHand.MAIN_HAND, weapon);
		player.setItemInHand(InteractionHand.OFF_HAND, scabbard);
		player.playSound(net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_IRON, 1.0F, 1.0F);
		return true;
	}

	/**
	 * メインハンドの満杯鞘から抜刀。武器はメインハンドへ、空鞘はオフハンドへ。
	 * (利き手の鞘抜刀; 呼び出し側は offHand が空であることを保証する)
	 */
	private static boolean drawFromMainHandSaya(Player player, ItemStack scabbard) {
		ItemStack weapon = CuriosScabbardHelper.extractWeaponFromScabbard(scabbard);
		if (weapon.isEmpty()) return false;

		CuriosScabbardHelper.clearWeaponFromScabbard(scabbard);
		player.setItemInHand(InteractionHand.MAIN_HAND, weapon);
		player.setItemInHand(InteractionHand.OFF_HAND, scabbard);
		player.playSound(net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_IRON, 1.0F, 1.0F);
		return true;
	}

	/**
	 * Curiosスロットの満杯鞘から抜刀
	 */
	private static boolean drawFromCuriosFirst(Player player) {
		CuriosScabbardHelper.ScabbardSlotInfo info = CuriosScabbardHelper.findLoadedScabbardInCurios(player);
		if (info == null) return false;
		return CuriosScabbardHelper.battouFromCurioSlot(player, InteractionHand.MAIN_HAND);
	}

	/**
	 * インベントリスロットの満杯鞘から抜刀
	 */
	private static boolean drawFromInventoryFirst(Player player) {
		int selectedSlot = player.getInventory().selected;
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			if (i == selectedSlot || i == 40) continue;
			ItemStack scabbard = player.getInventory().getItem(i);
			if (!CuriosScabbardHelper.isScabbard(scabbard)) continue;
			if (!CuriosScabbardHelper.hasStoredWeapon(scabbard)) continue;

			ItemStack weapon = CuriosScabbardHelper.extractWeaponFromScabbard(scabbard);
			if (weapon.isEmpty()) continue;

			player.setItemInHand(InteractionHand.MAIN_HAND, weapon);
			CuriosScabbardHelper.clearWeaponFromScabbard(scabbard);
			player.getInventory().setItem(i, scabbard);
			player.playSound(net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_IRON, 1.0F, 1.0F);
			return true;
		}
		return false;
	}

	/**
	 * 互換性ある空鞘 ( 手 / Curios / インベントリ ) が 1 個でも見つかれば true。
	 * Magical Katana 具現化版でも、 ベース Magical Katana の Saya ( KATANA ) には納刀可。
	 */
	private static boolean hasCompatibleEmptyScabbard(Player player, ItemStack weaponStack) {
		for (CuriosScabbardHelper.DrawableWeaponInfo info :
				CuriosScabbardHelper.findAllEmptyScabbards(player)) {
			if (CuriosScabbardHelper.isCompatible(weaponStack, info.scabbardStack)) {
				return true;
			}
		}
		return false;
	}

	/** メインインベントリ ( + ホットバー ) に saya ( SayaItem 系 ) が 1 個でもあれば true。 */
	private static boolean hasAnyScabbardInInventory(Player player) {
		net.minecraft.world.entity.player.Inventory inv = player.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack s = inv.getItem(i);
			if (CuriosScabbardHelper.isScabbard(s)) return true;
		}
		return false;
	}

	private static boolean isBluepurge(ItemStack stack) {
		if (stack.isEmpty()) return false;
		String name = stack.getItem().getClass().getSimpleName();
		return name.contains("Bluepurge");
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		TheFourPrimitivesAndWeaponsMod.addNetworkMessage(RMessage.class, RMessage::buffer, RMessage::new, RMessage::handler);
	}
}

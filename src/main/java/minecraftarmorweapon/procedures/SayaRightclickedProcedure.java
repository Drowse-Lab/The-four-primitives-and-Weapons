package minecraftarmorweapon.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionHand;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;

public class SayaRightclickedProcedure {
	public static void execute(LevelAccessor world, Entity entity, ItemStack sheathStack, InteractionHand hand) {
		if (entity == null || sheathStack == null)
			return;
			
		if (!(entity instanceof Player player))
			return;
		
		// 鞘を持っていない場合は処理しない
		if (!isSaya(sheathStack))
			return;
			
		CompoundTag tag = sheathStack.getOrCreateTag();
		
		// 右クリックで納刀（鞘が空の場合のみ）
		if (!tag.contains("StoredKatana")) {
			// 反対の手から刀を取得
			InteractionHand otherHand = (hand == InteractionHand.MAIN_HAND) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
			ItemStack katanaItem = player.getItemInHand(otherHand);
			
			// 反対の手に刀を持っている場合
			if (isKatana(katanaItem)) {
				// 刀の情報を鞘に保存
				CompoundTag katanaTag = new CompoundTag();
				katanaItem.save(katanaTag);
				tag.put("StoredKatana", katanaTag);
				
				// カスタムモデルデータを設定（刀が入った鞘の見た目）
				int modelData = getModelDataForKatana(katanaItem);
				tag.putInt("CustomModelData", modelData);
				
				// タグを確実にItemStackに適用
				sheathStack.setTag(tag);
				
				// 反対の手から刀を削除
				player.setItemInHand(otherHand, ItemStack.EMPTY);
				
				// 納刀音を再生
				player.playSound(SoundEvents.ITEM_ARMOR_EQUIP_LEATHER, 1.0F, 0.8F);
			}
		}
	}
	
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity instanceof Player player) {
			ItemStack sheathStack = player.getItemInHand(InteractionHand.MAIN_HAND);
			if (isSaya(sheathStack)) {
				execute(world, entity, sheathStack, InteractionHand.MAIN_HAND);
			} else {
				sheathStack = player.getItemInHand(InteractionHand.OFF_HAND);
				if (isSaya(sheathStack)) {
					execute(world, entity, sheathStack, InteractionHand.OFF_HAND);
				}
			}
		}
	}
	
	private static boolean isSaya(ItemStack stack) {
		if (stack.isEmpty()) return false;
		String itemName = stack.getItem().getClass().getSimpleName();
		return itemName.equals("SayaItem");
	}
	
	private static boolean isKatana(ItemStack stack) {
		if (stack.isEmpty()) return false;
		String itemName = stack.getItem().getClass().getSimpleName();
		return itemName.contains("Katana") || itemName.contains("katana");
	}
	
	private static int getModelDataForKatana(ItemStack katanaStack) {
		String itemName = katanaStack.getItem().getClass().getSimpleName();
		
		// Map each katana type to a specific custom model data
		// These will correspond to different model files with proper handle textures
		if (itemName.equals("IronKatanaItem")) return 1;
		if (itemName.equals("GoldKatanaItem")) return 2;
		if (itemName.equals("StoneKatanaItem")) return 3;
		if (itemName.equals("NetheriteKatanaItem")) return 4;
		if (itemName.equals("WitherKatanaItem")) return 5;
		if (itemName.equals("MotoWitherKatanaItem")) return 6;
		if (itemName.equals("DarknessKatanaItem")) return 7;
		if (itemName.equals("MagicalKatanaItem")) return 8;
		if (itemName.equals("MagischesFeenKatanaItem")) return 9;
		if (itemName.equals("PrototypeKatanaItem")) return 10;
		if (itemName.equals("OldKatanaItem")) return 11;
		if (itemName.equals("MyTestIronKatanaItem")) return 12;
		
		return 0; // Default (empty saya)
	}
}

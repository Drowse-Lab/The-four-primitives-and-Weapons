package minecraftarmorweapon.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionHand;
import net.minecraft.nbt.CompoundTag;

public class SayaRightclickedProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
			
		if (!(entity instanceof Player player))
			return;
			
		ItemStack sheathStack = player.getItemInHand(InteractionHand.OFF_HAND);
		
		// 鞘を持っていない場合は処理しない
		if (!isSaya(sheathStack))
			return;
			
		CompoundTag tag = sheathStack.getOrCreateTag();
		
		// Shift+右クリックで納刀
		if (player.isShiftKeyDown()) {
			ItemStack mainHandItem = player.getItemInHand(InteractionHand.MAIN_HAND);
			
			// 利き手に刀を持っていて、鞘が空の場合
			if (isKatana(mainHandItem) && !tag.contains("StoredKatana")) {
				// 刀の情報を鞘に保存
				CompoundTag katanaTag = new CompoundTag();
				mainHandItem.save(katanaTag);
				tag.put("StoredKatana", katanaTag);
				
				// カスタムモデルデータを設定（刀が入った鞘の見た目）
				int modelData = getModelDataForKatana(mainHandItem);
				tag.putInt("CustomModelData", modelData);
				
				// 利き手から刀を削除
				player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			}
		} 
		// 通常の右クリックで抜刀
		else {
			// 鞘に刀が入っている場合
			if (tag.contains("StoredKatana")) {
				// 利き手が空の場合のみ抜刀
				ItemStack mainHandItem = player.getItemInHand(InteractionHand.MAIN_HAND);
				if (mainHandItem.isEmpty()) {
					// 保存された刀の情報から刀を生成
					ItemStack katanaStack = ItemStack.of(tag.getCompound("StoredKatana"));
					
					// 利き手に刀を配置
					player.setItemInHand(InteractionHand.MAIN_HAND, katanaStack);
					
					// 鞘から刀の情報を削除（空の鞘にする）
					tag.remove("StoredKatana");
					tag.putInt("CustomModelData", 0); // 空の鞘のモデル
				}
			}
		}
	}
	
	public static void execute(Entity entity) {
		execute(null, entity);
	}
	
	public static void execute() {
		execute(null, null);
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
		
		if (itemName.contains("Iron")) return 1;
		if (itemName.contains("Gold")) return 2;
		if (itemName.contains("Stone")) return 3;
		if (itemName.contains("Netherite")) return 4;
		if (itemName.contains("Wither")) return 5;
		if (itemName.contains("Darkness")) return 6;
		if (itemName.contains("Magical") || itemName.contains("Magic")) return 7;
		
		return 0; // Default
	}
}

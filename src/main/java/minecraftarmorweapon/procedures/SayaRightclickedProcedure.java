package minecraftarmorweapon.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionHand;

public class SayaRightclickedProcedure {
	public static void execute(LevelAccessor world, Entity entity, ItemStack sheathStack, InteractionHand hand) {
		// 納刀はRキーのみで行う（RMessage → DodgeAndBattouHandler.performSheathing経由）
		// 鞘の右クリックでは納刀しない
	}

	public static void execute(LevelAccessor world, Entity entity) {
		// 納刀はRキーのみ
	}
}

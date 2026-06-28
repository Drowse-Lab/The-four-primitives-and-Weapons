package the_four_primitives_and_weapons.mixin;

import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import the_four_primitives_and_weapons.util.SayaDesign;

/**
 * 機織り機GUI のプレビュー更新 ( LoomScreen#containerChanged ) は、 結果スロットの
 * アイテムを ((BannerItem)stack.getItem()).getColor() でキャストして地色を取る。
 * 鞘 ( saya ) は BannerItem ではないのでここで ClassCastException → クラッシュする。
 *
 * 結果スロットが鞘のときだけ、 getItem() の戻り値を「鞘の地色に対応するバナーアイテム」に
 * すり替えて、 キャストと地色取得を成立させる ( プレビューの地色も鞘の染色色になる )。
 * 模様レイヤーは元の鞘スタックの NBT から読まれるので正しく表示される。
 */
@Mixin(LoomScreen.class)
public class LoomScreenSayaMixin {

	private static final Item[] TFP_BANNERS = {
			Items.WHITE_BANNER, Items.ORANGE_BANNER, Items.MAGENTA_BANNER, Items.LIGHT_BLUE_BANNER,
			Items.YELLOW_BANNER, Items.LIME_BANNER, Items.PINK_BANNER, Items.GRAY_BANNER,
			Items.LIGHT_GRAY_BANNER, Items.CYAN_BANNER, Items.PURPLE_BANNER, Items.BLUE_BANNER,
			Items.BROWN_BANNER, Items.GREEN_BANNER, Items.RED_BANNER, Items.BLACK_BANNER
	};

	@Redirect(method = "containerChanged", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/item/ItemStack;getItem()Lnet/minecraft/world/item/Item;", ordinal = 0))
	private Item tfp$resultItem(ItemStack stack) {
		if (SayaDesign.isSaya(stack)) {
			int id = SayaDesign.getBaseColor(stack).getId();
			if (id < 0 || id >= TFP_BANNERS.length) id = 0;
			return TFP_BANNERS[id];
		}
		return stack.getItem();
	}
}

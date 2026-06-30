package the_four_primitives_and_weapons.mixin;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import the_four_primitives_and_weapons.util.SayaDesign;

import java.util.List;

/**
 * 機織り機GUI の鞘対応:
 * <ul>
 *   <li>{@code containerChanged}: 結果スロットの鞘を BannerItem へキャストする箇所で
 *       ClassCastException を防ぐ ( 地色に対応するバナーをすり替え )。</li>
 *   <li>{@code renderBg}: 結果プレビューの「旗の形」描画を鞘のときはスキップする
 *       ( 旗の見た目を出さない )。 鞘そのものは結果スロットにアイテムとして表示される。</li>
 * </ul>
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

	/** 結果プレビューの旗描画: 鞘のときはスキップ ( 旗の見た目を出さない )。 */
	@Redirect(method = "renderBg", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/blockentity/BannerRenderer;renderPatterns(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/model/geom/ModelPart;Lnet/minecraft/client/resources/model/Material;ZLjava/util/List;)V"))
	private void tfp$skipBannerForSaya(PoseStack pose, MultiBufferSource buf, int light, int overlay,
									   ModelPart flag, Material base, boolean banner, List patterns) {
		ItemStack result = ((LoomScreen) (Object) this).getMenu().getResultSlot().getItem();
		if (!SayaDesign.isSaya(result)) {
			BannerRenderer.renderPatterns(pose, buf, light, overlay, flag, base, banner, patterns);
		}
		// 鞘なら旗を描かない ( 鞘は結果スロットにアイテム表示される )
	}
}

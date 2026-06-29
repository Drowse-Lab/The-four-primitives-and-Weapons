package the_four_primitives_and_weapons.client;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModItems;
import the_four_primitives_and_weapons.util.SayaDesign;

import net.minecraft.world.item.ItemStack;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 鞘 ( saya ) の地色 ( 染色 ) を、 鞘本体 ( wrap ) の面の tintindex 0 に反映する。
 * 染色は作業台 ( {@link the_four_primitives_and_weapons.item.SayaDyeRecipe} ) で設定する。
 *
 * <p>鞘本体テクスチャは乗算tintで色が乗るよう明るいグレーに調整済み。 未染色のときは
 * 暗いグレーを返して元の色合い ( ほぼ黒 ) に戻し、 染色されたら染料色を鮮やかに乗せる。</p>
 */
@Mod.EventBusSubscriber(modid = TheFourPrimitivesAndWeaponsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class SayaColorClient {

	/** 未染色時の地色 ( 明るくしたテクスチャを元の暗さに戻すための乗算グレー )。 */
	private static final int UNDYED = 0xFF4A4A4A;

	@SubscribeEvent
	public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
		event.register((stack, tintIndex) -> {
			// tintindex 0 = 鞘本体の地色、 1以上 = 機織り模様レイヤー ( layer = tintindex-1 ) の染料色
			if (tintIndex <= 0) return baseRgb(stack);
			return patternRgb(stack, tintIndex - 1);
		},
				TheFourPrimitivesAndWeaponsModItems.SAYA.get(),
				TheFourPrimitivesAndWeaponsModItems.TYOKUTO_SAYA.get(),
				TheFourPrimitivesAndWeaponsModItems.SWORD_SAYA.get(),
				TheFourPrimitivesAndWeaponsModItems.RAPIER_SAYA.get());
	}

	/** layer 番目の機織り模様レイヤーの染料色を 0xFFRRGGBB で返す ( 無ければ白 )。 */
	private static int patternRgb(ItemStack stack, int layer) {
		net.minecraft.nbt.ListTag patterns =
				net.minecraft.world.level.block.entity.BannerBlockEntity.getItemPatterns(stack);
		if (layer < 0 || layer >= patterns.size()) return 0xFFFFFFFF;
		int id = patterns.getCompound(layer).getInt("Color");
		net.minecraft.world.item.DyeColor color = net.minecraft.world.item.DyeColor.byId(id);
		float[] c = color.getTextureDiffuseColors();
		return 0xFF000000 | ((int) (c[0] * 255f) << 16) | ((int) (c[1] * 255f) << 8) | (int) (c[2] * 255f);
	}

	/** 鞘本体の地色を 0xFFRRGGBB で返す。 未染色は暗いグレー。 */
	private static int baseRgb(ItemStack stack) {
		int rgb = SayaDesign.getBaseRgb(stack);
		if (rgb >= 0) return 0xFF000000 | rgb;          // 染色済み
		return isThemedTexture(stack) ? 0xFFFFFFFF : UNDYED; // 未染色
	}

	/**
	 * 専用テクスチャ ( 暗いグレー既定tintを掛けてはいけない鞘 ) かどうか。
	 * 霊刀 ( reitou ) 系、 または スタイル ( 木目/着せ/刻 ) が設定された鞘。
	 * これらは未染色のとき テクスチャそのままを見せたいので 白tint を返す。
	 */
	private static boolean isThemedTexture(ItemStack stack) {
		if (SayaDesign.hasStyle(stack)) return true;               // 木目/着せ/刻 などのスタイル
		net.minecraft.nbt.CompoundTag t = stack.getTag();
		if (t == null) return false;
		if (t.getInt("SayaNBT") == 1) return true;                 // SayaVisualUpdateHandler が立てる霊刀フラグ
		if ("reitou".equals(t.getString("SayaStyle"))) return true;
		return t.contains("StoredKatana")
				&& t.getCompound("StoredKatana").getString("id").contains("reitou");
	}
}

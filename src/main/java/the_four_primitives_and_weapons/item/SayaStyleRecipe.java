package the_four_primitives_and_weapons.item;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;

import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.util.SayaDesign;

/**
 * 作業台で 鞘 ( saya ) + 素材1個 → 鞘の「スタイル」を変える ( 地色とは別 )。
 *
 * <ul>
 *   <li>板材 ( minecraft:planks ) … 木目鞘 ( その木材 )。 style = "wood_&lt;木材&gt;"</li>
 *   <li>革 ( leather ) … 着せ鞘。 style = "kise"</li>
 *   <li>火打石 ( flint ) … 刻鞘。 style = "kizami"</li>
 *   <li>ハニカム ( honeycomb ) … 塗鞘に戻す ( スタイル解除 )。 style = なし</li>
 * </ul>
 */
public class SayaStyleRecipe extends CustomRecipe {

	public SayaStyleRecipe(ResourceLocation id, CraftingBookCategory cat) {
		super(id, cat);
	}

	/** 鮫皮アイテムを受けるタグ ( 前提modの鮫/エイ皮をここに入れると鮫鞘が作れる )。 */
	public static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> RAYSKIN =
			ItemTags.create(new ResourceLocation(TheFourPrimitivesAndWeaponsMod.MODID, "rayskin"));

	private static boolean is(ItemStack s, RegistryObject<net.minecraft.world.item.Item> ro) {
		return s.getItem() == ro.get();
	}

	/**
	 * 鞘以外の素材から 付与する仕立て style を返す。 "" は塗鞘に戻す。 認識できなければ null。
	 * <ul>
	 *   <li>板材 → 木目 / 革 → 着せ / 火打石 → 刻 / 砂利 → ( 漆と併用で ) 石目 / 鮫皮 → 鮫</li>
	 *   <li>漆黒の漆 → 黒呂塗、 ＋グロウストーンダスト → 呂色塗 ( 磨き )</li>
	 *   <li>朱の漆 → 朱漆塗 / 生漆 → 溜塗、 ＋砂利 → 石目塗</li>
	 *   <li>ハニカム → 塗鞘に戻す ( "" )</li>
	 * </ul>
	 */
	private static String styleForMods(java.util.List<ItemStack> mods) {
		if (mods.isEmpty() || mods.size() > 2) return null;
		if (mods.size() == 1) {
			ItemStack m = mods.get(0);
			if (m.is(ItemTags.PLANKS)) {
				// 板材なら何でも ( 他mod含む ) 木目鞘に。 完全ID を保存し その板材テクスチャを使う。
				ResourceLocation id = ForgeRegistries.ITEMS.getKey(m.getItem());
				return (id == null) ? null : ("wood:" + id);
			}
			if (m.getItem() == Items.LEATHER) return "kise";
			if (m.getItem() == Items.FLINT) return "kizami";
			if (m.getItem() == Items.IRON_INGOT) return "gunto"; // 軍刀拵えの鞘 ( 特殊な鞘 )
			if (m.getItem() == Items.HONEYCOMB) return ""; // 塗鞘に戻す
			if (m.is(RAYSKIN)) return "same";
			if (is(m, the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModItems.URUSHI_BLACK)) return "kuroro";
			if (is(m, the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModItems.URUSHI_RED)) return "shunuri";
			if (is(m, the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModItems.RAW_URUSHI)) return "tame";
			return null;
		}
		// 2個: 漆 + 追加素材
		boolean black = false, red = false, raw = false, glow = false, gravel = false;
		for (ItemStack m : mods) {
			if (is(m, the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModItems.URUSHI_BLACK)) black = true;
			else if (is(m, the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModItems.URUSHI_RED)) red = true;
			else if (is(m, the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModItems.RAW_URUSHI)) raw = true;
			else if (m.getItem() == Items.GLOWSTONE_DUST) glow = true;
			else if (m.getItem() == Items.GRAVEL) gravel = true;
			else return null;
		}
		if (black && glow) return "roiro";   // 呂色塗 ( 磨き黒 )
		if (raw && gravel) return "ishime";  // 石目塗
		if (red && gravel) return "ishime";  // 朱でも石目可
		return null;
	}

	/** 鞘以外の素材を集める ( 鞘が0/2個以上なら null )。 */
	private static java.util.List<ItemStack> collectMods(CraftingContainer inv) {
		java.util.List<ItemStack> mods = new java.util.ArrayList<>();
		int saya = 0;
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack s = inv.getItem(i);
			if (s.isEmpty()) continue;
			if (SayaDesign.isSaya(s)) saya++;
			else mods.add(s);
		}
		return (saya == 1) ? mods : null;
	}

	@Override
	public boolean matches(CraftingContainer inv, Level level) {
		java.util.List<ItemStack> mods = collectMods(inv);
		return mods != null && styleForMods(mods) != null;
	}

	@Override
	public ItemStack assemble(CraftingContainer inv, RegistryAccess access) {
		java.util.List<ItemStack> mods = collectMods(inv);
		if (mods == null) return ItemStack.EMPTY;
		String style = styleForMods(mods);
		if (style == null) return ItemStack.EMPTY;

		ItemStack saya = ItemStack.EMPTY;
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack s = inv.getItem(i);
			if (!s.isEmpty() && SayaDesign.isSaya(s)) { saya = s; break; }
		}
		if (saya.isEmpty()) return ItemStack.EMPTY;

		ItemStack out = saya.copy();
		out.setCount(1);
		SayaDesign.setStyle(out, style); // "" は塗鞘に戻す ( タグ削除 )
		return out;
	}

	@Override
	public boolean canCraftInDimensions(int w, int h) {
		return w * h >= 2;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return Registrar.SERIALIZER.get();
	}

	public static final class Registrar {
		private Registrar() {}
		public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
				DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS,
						TheFourPrimitivesAndWeaponsMod.MODID);
		public static final RegistryObject<RecipeSerializer<SayaStyleRecipe>> SERIALIZER =
				SERIALIZERS.register("saya_style",
						() -> new SimpleCraftingRecipeSerializer<>(SayaStyleRecipe::new));
	}
}

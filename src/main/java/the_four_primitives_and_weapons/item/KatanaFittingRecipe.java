package the_four_primitives_and_weapons.item;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.DyeItem;
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
import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModItems;
import the_four_primitives_and_weapons.util.KatanaFittings;

/**
 * 作業台で 刀 ( IRON_KATANA ) の 柄/鍔 を染色する。
 * <ul>
 *   <li>刀 + 染料 → <b>柄巻き</b>の色</li>
 *   <li>刀 + 染料 + 金塊 ( gold_nugget ) → <b>鍔</b>の色 ( 金塊が「鍔」の目印 )</li>
 * </ul>
 */
public class KatanaFittingRecipe extends CustomRecipe {

	public KatanaFittingRecipe(ResourceLocation id, CraftingBookCategory cat) {
		super(id, cat);
	}

	private static boolean isKatana(ItemStack s) {
		return KatanaFittings.isFittingWeapon(s);
	}

	private static boolean isMarker(ItemStack s) {
		return s.getItem() == Items.GOLD_NUGGET || s.getItem() == Items.IRON_NUGGET
				|| s.getItem() == Items.COPPER_INGOT;
	}

	@Override
	public boolean matches(CraftingContainer inv, Level level) {
		int katana = 0, dye = 0, marker = 0, string = 0, ingot = 0;
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack s = inv.getItem(i);
			if (s.isEmpty()) continue;
			if (isKatana(s)) katana++;
			else if (s.getItem() instanceof DyeItem) dye++;
			else if (isMarker(s)) marker++;
			else if (s.getItem() == Items.STRING) string++;
			else if (s.getItem() == Items.IRON_INGOT) ingot++;
			else return false;
		}
		if (katana != 1 || marker > 1 || string > 1 || dye > 1 || ingot > 1) return false;
		// 「染料で色(+部位)」/「糸で柄巻き切替」/「鉄インゴットで鍔デザイン切替」のどれか一方
		if (dye == 1) return string == 0 && ingot == 0;
		if (string == 1) return marker == 0 && ingot == 0;
		if (ingot == 1) return marker == 0;
		return false;
	}

	@Override
	public ItemStack assemble(CraftingContainer inv, RegistryAccess access) {
		ItemStack katana = ItemStack.EMPTY;
		DyeItem dye = null;
		String part = "tsuka"; // 既定 = 柄巻き
		boolean string = false, ingot = false;
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack s = inv.getItem(i);
			if (s.isEmpty()) continue;
			if (isKatana(s)) {
				if (!katana.isEmpty()) return ItemStack.EMPTY;
				katana = s;
			} else if (s.getItem() instanceof DyeItem d) {
				if (dye != null) return ItemStack.EMPTY;
				dye = d;
			} else if (s.getItem() == Items.GOLD_NUGGET) {
				part = "tsuba";   // 金塊 = 鍔
			} else if (s.getItem() == Items.IRON_NUGGET) {
				part = "fuchi";   // 鉄塊 = 縁
			} else if (s.getItem() == Items.COPPER_INGOT) {
				part = "kashira"; // 銅 = 頭
			} else if (s.getItem() == Items.STRING) {
				string = true;
			} else if (s.getItem() == Items.IRON_INGOT) {
				ingot = true;     // 鉄インゴット = 鍔デザイン切替
			} else {
				return ItemStack.EMPTY;
			}
		}
		if (katana.isEmpty()) return ItemStack.EMPTY;

		ItemStack out = katana.copy();
		out.setCount(1);
		if (ingot) {
			KatanaFittings.setTsubaStyle(out, KatanaFittings.nextTsuba(KatanaFittings.getTsubaStyle(out)));
		} else if (string) {
			KatanaFittings.setTsukaWrap(out, KatanaFittings.nextWrap(KatanaFittings.getTsukaWrap(out)));
		} else if (dye != null) {
			int rgb = KatanaFittings.dyeRgb(dye.getDyeColor());
			switch (part) {
				case "tsuba":   KatanaFittings.setTsuba(out, rgb); break;
				case "fuchi":   KatanaFittings.setFuchi(out, rgb); break;
				case "kashira": KatanaFittings.setKashira(out, rgb); break;
				default:        KatanaFittings.setTsuka(out, rgb); break;
			}
		} else {
			return ItemStack.EMPTY;
		}
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
				DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, TheFourPrimitivesAndWeaponsMod.MODID);
		public static final RegistryObject<RecipeSerializer<KatanaFittingRecipe>> SERIALIZER =
				SERIALIZERS.register("katana_fitting",
						() -> new SimpleCraftingRecipeSerializer<>(KatanaFittingRecipe::new));
	}
}

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
		return s.getItem() == TheFourPrimitivesAndWeaponsModItems.IRON_KATANA.get();
	}

	@Override
	public boolean matches(CraftingContainer inv, Level level) {
		int katana = 0, dye = 0, marker = 0, string = 0;
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack s = inv.getItem(i);
			if (s.isEmpty()) continue;
			if (isKatana(s)) katana++;
			else if (s.getItem() instanceof DyeItem) dye++;
			else if (s.getItem() == Items.GOLD_NUGGET) marker++;
			else if (s.getItem() == Items.STRING) string++;
			else return false;
		}
		if (katana != 1 || marker > 1 || string > 1 || dye > 1) return false;
		// 「染料で色」か「糸で柄巻き切替」のどちらか一方
		return (dye == 1 && string == 0) || (string == 1 && dye == 0);
	}

	@Override
	public ItemStack assemble(CraftingContainer inv, RegistryAccess access) {
		ItemStack katana = ItemStack.EMPTY;
		DyeItem dye = null;
		boolean tsuba = false, string = false;
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
				tsuba = true;
			} else if (s.getItem() == Items.STRING) {
				string = true;
			} else {
				return ItemStack.EMPTY;
			}
		}
		if (katana.isEmpty()) return ItemStack.EMPTY;

		ItemStack out = katana.copy();
		out.setCount(1);
		if (string) {
			// 糸: 柄巻きデザインを次へ切り替え
			KatanaFittings.setTsukaWrap(out, KatanaFittings.nextWrap(KatanaFittings.getTsukaWrap(out)));
		} else if (dye != null) {
			int rgb = KatanaFittings.dyeRgb(dye.getDyeColor());
			if (tsuba) KatanaFittings.setTsuba(out, rgb);
			else KatanaFittings.setTsuka(out, rgb);
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

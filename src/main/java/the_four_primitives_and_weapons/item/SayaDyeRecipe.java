package the_four_primitives_and_weapons.item;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
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
 * 作業台で 鞘 ( saya ) + 染料1個 → 鞘の「地の色」を染める ( 旗の地色と同じく単色 )。
 * 模様は機織り機 ( {@link the_four_primitives_and_weapons.mixin.LoomBannerSlotMixin} ) で重ねる。
 */
public class SayaDyeRecipe extends CustomRecipe {

	public SayaDyeRecipe(ResourceLocation id, CraftingBookCategory cat) {
		super(id, cat);
	}

	@Override
	public boolean matches(CraftingContainer inv, Level level) {
		boolean foundSaya = false;
		boolean foundDye = false;
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack s = inv.getItem(i);
			if (s.isEmpty()) continue;
			if (SayaDesign.isSaya(s)) {
				if (foundSaya) return false; // 鞘は1本だけ
				foundSaya = true;
			} else if (s.getItem() instanceof DyeItem) {
				if (foundDye) return false; // 地色は単色なので染料も1個
				foundDye = true;
			} else {
				return false; // 鞘・染料以外が混ざっていたらマッチしない
			}
		}
		return foundSaya && foundDye;
	}

	@Override
	public ItemStack assemble(CraftingContainer inv, RegistryAccess access) {
		ItemStack saya = ItemStack.EMPTY;
		DyeColor color = null;
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack s = inv.getItem(i);
			if (s.isEmpty()) continue;
			if (SayaDesign.isSaya(s)) {
				if (!saya.isEmpty()) return ItemStack.EMPTY;
				saya = s;
			} else if (s.getItem() instanceof DyeItem d) {
				if (color != null) return ItemStack.EMPTY;
				color = d.getDyeColor();
			} else {
				return ItemStack.EMPTY;
			}
		}
		if (saya.isEmpty() || color == null) return ItemStack.EMPTY;

		ItemStack out = saya.copy();
		out.setCount(1);
		SayaDesign.setBaseColor(out, color);
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
		public static final RegistryObject<RecipeSerializer<SayaDyeRecipe>> SERIALIZER =
				SERIALIZERS.register("saya_dye",
						() -> new SimpleCraftingRecipeSerializer<>(SayaDyeRecipe::new));
	}
}

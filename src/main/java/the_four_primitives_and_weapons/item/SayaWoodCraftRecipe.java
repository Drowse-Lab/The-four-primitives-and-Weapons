package the_four_primitives_and_weapons.item;

import com.google.gson.JsonObject;

import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;

import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.util.SayaDesign;

/**
 * 鞘のクラフトレシピ ( 定形 ) の拡張。 通常の {@link ShapedRecipe} と同じく形でマッチするが、
 * クラフトに使った <b>板材の木材</b> を読み取り、 出来上がった鞘に その木の <b>木目鞘スタイル</b>
 * ( {@code wood_<木材>} ) を付ける。 JSON では板材を {@code "tag":"minecraft:planks"} にしておけば
 * どの木でも使え、 使った木がそのまま見た目になる。
 */
public class SayaWoodCraftRecipe extends ShapedRecipe {

	public SayaWoodCraftRecipe(ShapedRecipe r) {
		super(r.getId(), r.getGroup(), r.category(), r.getWidth(), r.getHeight(),
				r.getIngredients(), r.getResultItem(RegistryAccess.EMPTY), r.showNotification());
	}

	@Override
	public ItemStack assemble(CraftingContainer inv, RegistryAccess access) {
		ItemStack out = super.assemble(inv, access);
		String wood = firstPlanksWood(inv);
		if (wood != null) SayaDesign.setStyle(out, "wood_" + wood);
		return out;
	}

	/** クラフト枠の中の最初の板材から木材名 ( "oak" 等 ) を返す。 無ければ null。 */
	private static String firstPlanksWood(CraftingContainer inv) {
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack s = inv.getItem(i);
			if (s.isEmpty() || !s.is(ItemTags.PLANKS)) continue;
			ResourceLocation id = ForgeRegistries.ITEMS.getKey(s.getItem());
			if (id == null) continue;
			String path = id.getPath();
			if (path.endsWith("_planks")) {
				return path.substring(0, path.length() - "_planks".length());
			}
		}
		return null;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return Registrar.SERIALIZER.get();
	}

	/** 定形レシピのシリアライザに委譲し、 結果を本クラスでラップする。 */
	public static final class Serializer implements RecipeSerializer<SayaWoodCraftRecipe> {
		private final ShapedRecipe.Serializer shaped = new ShapedRecipe.Serializer();

		@Override
		public SayaWoodCraftRecipe fromJson(ResourceLocation id, JsonObject json) {
			return new SayaWoodCraftRecipe(shaped.fromJson(id, json));
		}

		@Override
		public SayaWoodCraftRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
			ShapedRecipe r = shaped.fromNetwork(id, buf);
			return (r == null) ? null : new SayaWoodCraftRecipe(r);
		}

		@Override
		public void toNetwork(FriendlyByteBuf buf, SayaWoodCraftRecipe recipe) {
			shaped.toNetwork(buf, recipe);
		}
	}

	public static final class Registrar {
		private Registrar() {}
		public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
				DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS,
						TheFourPrimitivesAndWeaponsMod.MODID);
		public static final RegistryObject<RecipeSerializer<SayaWoodCraftRecipe>> SERIALIZER =
				SERIALIZERS.register("saya_wood_craft", Serializer::new);
	}
}

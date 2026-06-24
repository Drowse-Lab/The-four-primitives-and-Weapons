package the_four_primitives_and_weapons.item;

import com.google.gson.JsonObject;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.inventory.CraftingContainer;

import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.events.MagicalKatanaCrystalHandler;
import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModItems;

/**
 * Magical Katana 解放クラフト。
 *
 * レシピ:
 *   - 入力 ( シェイプレス ):
 *       1. Magical Katana ( ロック中、 isUnlocked == false )
 *       2. Corrosion Book ( 侵食魔導書 )
 *   - 出力: 入力の Magical Katana の NBT を全部引き継いだ上で UNLOCKED_KEY=true をセット
 *   - エンチャント / レアリティ / その他 NBT は維持される
 *
 * Custom にして CraftingContainer から元 stack の NBT を読み取りつつ
 * 出力に転送する ( バニラ shapeless だと assemble 時に NBT は失われる )。
 */
public class MagicalKatanaUnlockRecipe extends CustomRecipe {

    public MagicalKatanaUnlockRecipe(ResourceLocation id, CraftingBookCategory cat) {
        super(id, cat);
    }

    @Override
    public boolean matches(CraftingContainer inv, Level level) {
        boolean foundLockedKatana = false;
        boolean foundBook         = false;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) continue;
            if (isLockedMagicalKatana(s)) {
                if (foundLockedKatana) return false;
                foundLockedKatana = true;
            } else if (isCorrosionBook(s)) {
                if (foundBook) return false;
                foundBook = true;
            } else {
                // それ以外のアイテムが混ざっていたらマッチしない
                return false;
            }
        }
        return foundLockedKatana && foundBook;
    }

    @Override
    public ItemStack assemble(CraftingContainer inv, RegistryAccess access) {
        // 入力の Magical Katana を探して NBT を引き継ぎ unlock flag を追加
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (isLockedMagicalKatana(s)) {
                ItemStack out = s.copy();
                out.setCount(1);
                CompoundTag tag = out.getOrCreateTag();
                tag.putBoolean("MagicalKatanaUnlocked", true);
                return out;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int w, int h) {
        return w * h >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Registrar.SERIALIZER.get();
    }

    // ─── 判定ヘルパー ────────────────────────────────────────────────
    private static boolean isLockedMagicalKatana(ItemStack s) {
        if (s.isEmpty()) return false;
        if (s.getItem() != TheFourPrimitivesAndWeaponsModItems.MAGICAL_KATANA.get()) return false;
        // 既に解放済みのものはマッチさせない ( 無駄消費防止 )
        return !MagicalKatanaCrystalHandler.isUnlocked(s);
    }

    private static boolean isCorrosionBook(ItemStack s) {
        if (s.isEmpty()) return false;
        return s.getItem() == TheFourPrimitivesAndWeaponsModItems.CORROSION_BOOK.get();
    }

    // ─── 登録 ───────────────────────────────────────────────────────
    public static final class Registrar {
        private Registrar() {}
        public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
                DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS,
                        TheFourPrimitivesAndWeaponsMod.MODID);
        public static final RegistryObject<RecipeSerializer<MagicalKatanaUnlockRecipe>> SERIALIZER =
                SERIALIZERS.register("magical_katana_unlock",
                        () -> new SimpleCraftingRecipeSerializer<>(MagicalKatanaUnlockRecipe::new));
    }
}

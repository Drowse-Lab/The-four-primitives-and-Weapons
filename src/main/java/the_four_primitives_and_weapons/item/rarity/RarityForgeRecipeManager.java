package the_four_primitives_and_weapons.item.rarity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * data/the_four_primitives_and_weapons/rarity_forge_recipes/ 以下のJSONを読み込み、
 * RarityForgeRecipes にセットするリロードリスナー。
 *
 * JSONフォーマット:
 * {
 *   "pattern": ["M", "M", "S"],
 *   "key": {
 *     "M": "minecraft:iron_ingot",
 *     "S": "minecraft:stick"
 *   },
 *   "result": "the_four_primitives_and_weapons:iron_katana"
 * }
 */
@Mod.EventBusSubscriber(modid = TheFourPrimitivesAndWeaponsMod.MODID)
public class RarityForgeRecipeManager extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(RarityForgeRecipeManager.class);
    private static final Gson GSON = new GsonBuilder().create();
    private static final RarityForgeRecipeManager INSTANCE = new RarityForgeRecipeManager();

    public RarityForgeRecipeManager() {
        super(GSON, "rarity_forge_recipes");
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> recipes,
                         ResourceManager resourceManager, ProfilerFiller profiler) {
        List<RarityForgeRecipe> loaded = new ArrayList<>();

        for (Map.Entry<ResourceLocation, JsonElement> entry : recipes.entrySet()) {
            try {
                RarityForgeRecipe recipe = parseRecipe(entry.getKey(), entry.getValue().getAsJsonObject());
                if (recipe != null) {
                    loaded.add(recipe);
                }
            } catch (Exception e) {
                LOGGER.error("Error loading rarity forge recipe {}: {}", entry.getKey(), e.getMessage());
            }
        }

        RarityForgeRecipes.setRecipes(loaded);
        LOGGER.info("Loaded {} rarity forge recipes", loaded.size());
    }

    private RarityForgeRecipe parseRecipe(ResourceLocation id, JsonObject json) {
        boolean isUnbreakable = json.has("unbreakable") && json.get("unbreakable").getAsBoolean();

        // result（#input = 入力アイテムをそのまま返す）
        String resultId = json.get("result").getAsString();
        Item resultItem = null;
        boolean resultIsInput = "#input".equals(resultId);
        if (!resultIsInput) {
            resultItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation(resultId));
            if (resultItem == null) {
                LOGGER.warn("Unknown result item '{}' in recipe {}", resultId, id);
                return null;
            }
        }

        // key（#input = 任意のSwordItem受け入れ）
        JsonObject keyObj = json.getAsJsonObject("key");
        Map<Character, Item> keyMap = new HashMap<>();
        char inputKeyChar = 0;
        for (Map.Entry<String, JsonElement> keyEntry : keyObj.entrySet()) {
            String itemId = keyEntry.getValue().getAsString();
            if ("#input".equals(itemId)) {
                inputKeyChar = keyEntry.getKey().charAt(0);
                keyMap.put(inputKeyChar, null); // null = 任意の武器アイテム
            } else {
                Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemId));
                if (item == null) {
                    LOGGER.warn("Unknown ingredient '{}' in recipe {}", itemId, id);
                    return null;
                }
                keyMap.put(keyEntry.getKey().charAt(0), item);
            }
        }

        // pattern
        JsonArray patternArray = json.getAsJsonArray("pattern");
        String[] rows = new String[patternArray.size()];
        for (int i = 0; i < patternArray.size(); i++) {
            rows[i] = patternArray.get(i).getAsString();
        }

        int height = rows.length;
        int width = 0;
        for (String row : rows) width = Math.max(width, row.length());

        Item[][] grid = new Item[height][width];
        int inputSlotIdx = -1;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < rows[y].length(); x++) {
                char c = rows[y].charAt(x);
                if (c != ' ') {
                    grid[y][x] = keyMap.get(c);
                    if (inputKeyChar != 0 && c == inputKeyChar) {
                        inputSlotIdx = y * width + x;
                    }
                }
            }
        }

        int elementLevel = 0;
        if (json.has("element_level")) {
            elementLevel = json.get("element_level").getAsInt();
        }

        // Unbreakableレシピ: resultがnullの場合はダミーアイテムを使用（実際はクラフト時に入力を複製）
        if (resultItem == null) {
            resultItem = net.minecraft.world.item.Items.IRON_SWORD; // ダミー（JEI表示用）
        }

        RarityForgeRecipe recipe = new RarityForgeRecipe(resultItem, grid, width, height, elementLevel, isUnbreakable);
        recipe.setInputSlotIndex(inputSlotIdx);
        return recipe;
    }
}

package the_four_primitives_and_weapons.item.rarity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * data/<ns>/rarity_forge_unique_recipes/*.json を読み込んで
 * RarityForgeUniqueRecipes にセットする。
 *
 * JSON フォーマット ( バニラの shaped crafting recipe 風 ):
 * {
 *   "pattern": ["XYX", "Y Y", "XYX"],
 *   "key": {
 *     "X": "minecraft:diamond",
 *     "Y": "minecraft:stick"
 *   },
 *   "result": "minecraft:diamond_sword",
 *   "count": 1
 * }
 */
@Mod.EventBusSubscriber(modid = TheFourPrimitivesAndWeaponsMod.MODID)
public class RarityForgeUniqueRecipeManager extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(RarityForgeUniqueRecipeManager.class);
    private static final Gson GSON = new GsonBuilder().create();
    private static final RarityForgeUniqueRecipeManager INSTANCE = new RarityForgeUniqueRecipeManager();

    public RarityForgeUniqueRecipeManager() {
        super(GSON, "rarity_forge_unique_recipes");
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> entries,
                         ResourceManager resourceManager, ProfilerFiller profiler) {
        List<RarityForgeUniqueRecipe> loaded = new ArrayList<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : entries.entrySet()) {
            try {
                RarityForgeUniqueRecipe r = parseRecipe(entry.getKey(), entry.getValue().getAsJsonObject());
                if (r != null) loaded.add(r);
            } catch (Exception e) {
                LOGGER.error("Error loading unique recipe {}: {}", entry.getKey(), e.getMessage());
            }
        }
        RarityForgeUniqueRecipes.setRecipes(loaded);
        LOGGER.info("Loaded {} rarity-forge unique recipes", loaded.size());
    }

    private RarityForgeUniqueRecipe parseRecipe(ResourceLocation id, JsonObject json) {
        if (!json.has("pattern") || !json.has("key") || !json.has("result")) {
            LOGGER.warn("Unique recipe {} missing required fields", id);
            return null;
        }
        // key map
        JsonObject keyObj = json.getAsJsonObject("key");
        Map<Character, Item> keyMap = new HashMap<>();
        for (Map.Entry<String, JsonElement> e : keyObj.entrySet()) {
            if (e.getKey().length() != 1) continue;
            String itemId = e.getValue().getAsString();
            Item it = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemId));
            if (it == null) {
                LOGGER.warn("Unique recipe {} references unknown item: {}", id, itemId);
                continue;
            }
            keyMap.put(e.getKey().charAt(0), it);
        }
        // pattern
        JsonArray patternArr = json.getAsJsonArray("pattern");
        int height = patternArr.size();
        int width = 0;
        String[] rows = new String[height];
        for (int i = 0; i < height; i++) {
            rows[i] = patternArr.get(i).getAsString();
            width = Math.max(width, rows[i].length());
        }
        Item[][] grid = new Item[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < rows[y].length(); x++) {
                char c = rows[y].charAt(x);
                if (c != ' ') {
                    grid[y][x] = keyMap.get(c);
                }
            }
        }
        // result
        String resultId = json.get("result").getAsString();
        Item resultItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation(resultId));
        if (resultItem == null) {
            LOGGER.warn("Unique recipe {} has unknown result item: {}", id, resultId);
            return null;
        }
        int count = json.has("count") ? json.get("count").getAsInt() : 1;
        return new RarityForgeUniqueRecipe(resultItem, count, grid, width, height);
    }
}

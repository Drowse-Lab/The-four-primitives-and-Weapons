package minecraftarmorweapon.item.rarity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import minecraftarmorweapon.MinecraftArmorWeaponMod;
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
 * data/minecraft_armor_weapon/rarity_forge_recipes/ 以下のJSONを読み込み、
 * RarityForgeRecipes にセットするリロードリスナー。
 *
 * JSONフォーマット:
 * {
 *   "pattern": ["M", "M", "S"],
 *   "key": {
 *     "M": "minecraft:iron_ingot",
 *     "S": "minecraft:stick"
 *   },
 *   "result": "minecraft_armor_weapon:iron_katana"
 * }
 */
@Mod.EventBusSubscriber(modid = MinecraftArmorWeaponMod.MODID)
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
        // result
        String resultId = json.get("result").getAsString();
        Item resultItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation(resultId));
        if (resultItem == null) {
            LOGGER.warn("Unknown result item '{}' in recipe {}", resultId, id);
            return null;
        }

        // key
        JsonObject keyObj = json.getAsJsonObject("key");
        Map<Character, Item> keyMap = new HashMap<>();
        for (Map.Entry<String, JsonElement> keyEntry : keyObj.entrySet()) {
            String itemId = keyEntry.getValue().getAsString();
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemId));
            if (item == null) {
                LOGGER.warn("Unknown ingredient '{}' in recipe {}", itemId, id);
                return null;
            }
            keyMap.put(keyEntry.getKey().charAt(0), item);
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
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < rows[y].length(); x++) {
                char c = rows[y].charAt(x);
                if (c != ' ') {
                    grid[y][x] = keyMap.get(c);
                }
            }
        }

        return new RarityForgeRecipe(resultItem, grid, width, height);
    }
}

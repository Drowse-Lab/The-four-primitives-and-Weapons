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
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * data/the_four_primitives_and_weapons/rarity_config/ 以下のJSONを読み込む。
 *
 * - material_tiers.json: 素材ティア分類
 * - weights.json:        確率テーブル＋ティアボーナス
 */
@Mod.EventBusSubscriber(modid = TheFourPrimitivesAndWeaponsMod.MODID)
public class RarityCraftingConfigManager extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(RarityCraftingConfigManager.class);
    private static final Gson GSON = new GsonBuilder().create();
    private static final RarityCraftingConfigManager INSTANCE = new RarityCraftingConfigManager();

    public RarityCraftingConfigManager() {
        super(GSON, "rarity_config");
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> entries,
                         ResourceManager resourceManager, ProfilerFiller profiler) {
        for (Map.Entry<ResourceLocation, JsonElement> entry : entries.entrySet()) {
            String path = entry.getKey().getPath(); // e.g. "material_tiers" or "weights"
            try {
                JsonObject json = entry.getValue().getAsJsonObject();
                if (path.equals("material_tiers")) {
                    loadMaterialTiers(json);
                } else if (path.equals("weights")) {
                    loadWeights(json);
                }
            } catch (Exception e) {
                LOGGER.error("Error loading rarity config {}: {}", entry.getKey(), e.getMessage());
            }
        }
    }

    private void loadMaterialTiers(JsonObject json) {
        Set<String> cursed = toStringSet(json, "cursed");
        Set<String> netherStar = toStringSet(json, "nether_star");
        Set<String> diamond = toStringSet(json, "diamond");
        Set<String> iron = toStringSet(json, "iron");

        RarityCraftingLogic.setMaterialTiers(cursed, netherStar, diamond, iron);
        LOGGER.info("Loaded material tiers: cursed={}, nether_star={}, diamond={}, iron={}",
                cursed.size(), netherStar.size(), diamond.size(), iron.size());
    }

    private void loadWeights(JsonObject json) {
        // weights_by_level
        JsonArray weightsArray = json.getAsJsonArray("weights_by_level");
        int[][] weights = new int[weightsArray.size()][];
        for (int i = 0; i < weightsArray.size(); i++) {
            JsonArray row = weightsArray.get(i).getAsJsonArray();
            weights[i] = new int[row.size()];
            for (int j = 0; j < row.size(); j++) {
                weights[i][j] = row.get(j).getAsInt();
            }
        }

        // tier_bonuses
        JsonObject bonusObj = json.getAsJsonObject("tier_bonuses");
        Map<String, Double> tierBonuses = new HashMap<>();
        for (Map.Entry<String, JsonElement> e : bonusObj.entrySet()) {
            tierBonuses.put(e.getKey(), e.getValue().getAsDouble());
        }

        RarityCraftingLogic.setWeightsAndBonuses(weights, tierBonuses);
        LOGGER.info("Loaded rarity weights ({} levels) and tier bonuses", weights.length);
    }

    private static Set<String> toStringSet(JsonObject json, String key) {
        Set<String> set = new HashSet<>();
        if (!json.has(key)) return set;
        JsonArray arr = json.getAsJsonArray(key);
        for (JsonElement e : arr) {
            set.add(e.getAsString());
        }
        return set;
    }
}

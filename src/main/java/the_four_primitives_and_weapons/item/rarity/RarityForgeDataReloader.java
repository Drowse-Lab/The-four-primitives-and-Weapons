package the_four_primitives_and_weapons.item.rarity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.damage.ElementType;
import the_four_primitives_and_weapons.network.RarityForgeDataSyncPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * data/&lt;namespace&gt;/rarity_forge/ 以下の JSON を読み込んで
 * RarityForgeCenterLogic の各 table を上書きする。
 *
 * 認識するファイル ( ファイル名は固定 ):
 *   - catalyst_levels.json : 触媒 ID → element Lv マップ
 *   - book_elements.json   : 魔導書 ID → ElementType マップ
 *   - unbreakable.json     : Unbreakable 化用 触媒組み合わせ list
 *
 * 同名ファイルを別 datapack で上書きすると最後勝ち ( datapack の優先順位通り )。
 * 複数ファイルが揃わない場合は揃った分のみ反映、 残りはデフォルトのまま。
 */
@Mod.EventBusSubscriber(modid = TheFourPrimitivesAndWeaponsMod.MODID)
public class RarityForgeDataReloader extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(RarityForgeDataReloader.class);
    private static final Gson GSON = new GsonBuilder().create();
    private static final RarityForgeDataReloader INSTANCE = new RarityForgeDataReloader();

    public RarityForgeDataReloader() {
        super(GSON, "rarity_forge");
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
    }

    /**
     * datapack reload / プレイヤーログイン時に、 server 側の table を
     * client へ同期する ( 単一プレイヤー or 全プレイヤー )。
     */
    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        RarityForgeDataSyncPacket packet = new RarityForgeDataSyncPacket(
                RarityForgeCenterLogic.getCatalystLevelTable(),
                RarityForgeCenterLogic.getBookElementTable(),
                RarityForgeCenterLogic.getUnbreakablePairs());
        ServerPlayer player = event.getPlayer();
        if (player != null) {
            TheFourPrimitivesAndWeaponsMod.PACKET_HANDLER.send(
                    PacketDistributor.PLAYER.with(() -> player), packet);
        } else {
            TheFourPrimitivesAndWeaponsMod.PACKET_HANDLER.send(
                    PacketDistributor.ALL.noArg(), packet);
        }
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> entries,
                         ResourceManager resourceManager, ProfilerFiller profiler) {
        // 一旦 default に戻す ( 任意のファイルが欠けた datapack でも残骸が残らないように )
        // ※ datapack 側で明示的に空 map を渡したい場合は object に "levels": {} 等を書く
        boolean sawCat = false, sawBook = false, sawUnbreak = false;

        for (Map.Entry<ResourceLocation, JsonElement> entry : entries.entrySet()) {
            String path = entry.getKey().getPath();
            try {
                JsonObject json = entry.getValue().getAsJsonObject();
                switch (path) {
                    case "catalyst_levels": loadCatalystLevels(json); sawCat = true; break;
                    case "book_elements":   loadBookElements(json);   sawBook = true; break;
                    case "unbreakable":     loadUnbreakable(json);    sawUnbreak = true; break;
                    default:
                        LOGGER.warn("Unknown rarity_forge data file: {}", entry.getKey());
                }
            } catch (Exception e) {
                LOGGER.error("Error loading rarity_forge data {}: {}", entry.getKey(), e.getMessage());
            }
        }

        if (!sawCat)    RarityForgeCenterLogic.setCatalystLevels(null);
        if (!sawBook)   RarityForgeCenterLogic.setBookElements(null);
        if (!sawUnbreak) RarityForgeCenterLogic.setUnbreakablePairs(null);
    }

    private void loadCatalystLevels(JsonObject json) {
        if (!json.has("levels")) {
            LOGGER.warn("catalyst_levels.json: missing 'levels' object — using defaults");
            RarityForgeCenterLogic.setCatalystLevels(null);
            return;
        }
        Map<String, Integer> table = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : json.getAsJsonObject("levels").entrySet()) {
            table.put(e.getKey(), e.getValue().getAsInt());
        }
        RarityForgeCenterLogic.setCatalystLevels(table);
        LOGGER.info("Loaded rarity_forge catalyst_levels: {} entries", table.size());
    }

    private void loadBookElements(JsonObject json) {
        if (!json.has("elements")) {
            LOGGER.warn("book_elements.json: missing 'elements' object — using defaults");
            RarityForgeCenterLogic.setBookElements(null);
            return;
        }
        Map<String, ElementType> table = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : json.getAsJsonObject("elements").entrySet()) {
            String value = e.getValue().getAsString();
            try {
                table.put(e.getKey(), ElementType.valueOf(value));
            } catch (IllegalArgumentException ex) {
                LOGGER.warn("Unknown ElementType '{}' for {} in book_elements.json — skipped", value, e.getKey());
            }
        }
        RarityForgeCenterLogic.setBookElements(table);
        LOGGER.info("Loaded rarity_forge book_elements: {} entries", table.size());
    }

    private void loadUnbreakable(JsonObject json) {
        if (!json.has("pairs")) {
            LOGGER.warn("unbreakable.json: missing 'pairs' array — using defaults");
            RarityForgeCenterLogic.setUnbreakablePairs(null);
            return;
        }
        List<RarityForgeCenterLogic.UnbreakablePair> list = new ArrayList<>();
        JsonArray arr = json.getAsJsonArray("pairs");
        for (int i = 0; i < arr.size(); i++) {
            JsonObject obj = arr.get(i).getAsJsonObject();
            String cat0 = obj.get("cat0").getAsString();
            String cat1 = obj.get("cat1").getAsString();
            list.add(new RarityForgeCenterLogic.UnbreakablePair(cat0, cat1));
        }
        RarityForgeCenterLogic.setUnbreakablePairs(list);
        LOGGER.info("Loaded rarity_forge unbreakable pairs: {} entries", list.size());
    }
}

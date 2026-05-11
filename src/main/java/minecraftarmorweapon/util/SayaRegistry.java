package minecraftarmorweapon.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * JSON駆動の鞘(saya)登録レジストリ。
 * data/&lt;namespace&gt;/maw_saya/ 以下のJSONを読み込み、
 * 各サヤタイプ（katana / tyokuto / sword）に納刀可能なアイテムを管理する。
 * アドオンMODはこのJSONを置くだけで自分のアイテムを納刀対象にできる。
 *
 * JSONフォーマット:
 * <pre>
 * {
 *   "katana":  { "your_mod:item_id": 1, ... },
 *   "tyokuto": { "your_mod:item_id": 4, ... },
 *   "sword":   { "your_mod:item_id": 1, ... }
 * }
 * </pre>
 * 値はサヤ本体の models/item/saya.json (tyokuto_saya.json / sword_saya.json) で
 * 定義された custom_model_data を指定する。既存のスロット(1..N)を流用するか、
 * 独自のスロットを使う場合はリソースパックで saya.json の overrides を拡張する。
 */
@Mod.EventBusSubscriber(modid = "minecraft_armor_weapon")
public class SayaRegistry extends SimplePreparableReloadListener<Map<SayaRegistry.SayaType, Map<ResourceLocation, Integer>>> {

    public enum SayaType {
        KATANA("katana"),
        TYOKUTO("tyokuto"),
        SWORD("sword");

        private final String key;

        SayaType(String key) { this.key = key; }

        public String getKey() { return key; }
    }

    private static final Gson GSON = new GsonBuilder().create();
    private static final String DIRECTORY = "maw_saya";
    private static final SayaRegistry INSTANCE = new SayaRegistry();

    private static final Map<SayaType, Map<ResourceLocation, Integer>> ENTRIES = new EnumMap<>(SayaType.class);
    static {
        for (SayaType t : SayaType.values()) ENTRIES.put(t, new HashMap<>());
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
    }

    @Nonnull
    @Override
    protected Map<SayaType, Map<ResourceLocation, Integer>> prepare(@Nonnull ResourceManager manager,
                                                                     @Nonnull ProfilerFiller profiler) {
        Map<SayaType, Map<ResourceLocation, Integer>> result = new EnumMap<>(SayaType.class);
        for (SayaType t : SayaType.values()) result.put(t, new HashMap<>());

        manager.listResources(DIRECTORY, loc -> {
            String path = loc.getPath();
            int slash = path.lastIndexOf('/');
            String fileName = slash >= 0 ? path.substring(slash + 1) : path;
            return path.endsWith(".json") && !fileName.startsWith("_");
        }).forEach((location, resource) -> {
            try (Reader reader = new InputStreamReader(resource.open())) {
                JsonObject root = GSON.fromJson(reader, JsonObject.class);
                if (root == null) return;
                for (SayaType type : SayaType.values()) {
                    if (!root.has(type.getKey())) continue;
                    JsonElement el = root.get(type.getKey());
                    if (!el.isJsonObject()) continue;
                    JsonObject obj = el.getAsJsonObject();
                    Map<ResourceLocation, Integer> map = result.get(type);
                    for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                        try {
                            ResourceLocation itemId = new ResourceLocation(entry.getKey());
                            int modelData = entry.getValue().getAsInt();
                            if (modelData > 0) map.put(itemId, modelData);
                        } catch (Exception ignored) {
                            // 不正なエントリはスキップ
                        }
                    }
                }
            } catch (Exception ignored) {
                // JSONパースエラーは無視
            }
        });

        return result;
    }

    @Override
    protected void apply(@Nonnull Map<SayaType, Map<ResourceLocation, Integer>> prepared,
                         @Nonnull ResourceManager manager, @Nonnull ProfilerFiller profiler) {
        for (SayaType t : SayaType.values()) {
            Map<ResourceLocation, Integer> dest = ENTRIES.get(t);
            dest.clear();
            dest.putAll(prepared.get(t));
        }
    }

    /**
     * 指定タイプのサヤに対応するモデルデータを取得（未登録なら 0）。
     */
    public static int getModelData(SayaType type, ItemStack stack) {
        if (stack.isEmpty()) return 0;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) return 0;
        return ENTRIES.get(type).getOrDefault(id, 0);
    }

    /**
     * 指定タイプのサヤに登録されているか判定。
     */
    public static boolean isRegistered(SayaType type, ItemStack stack) {
        return getModelData(type, stack) != 0;
    }

    /**
     * Java側からの追加登録用（デバッグ・移行用）。通常はJSONで宣言する。
     */
    public static void register(SayaType type, ResourceLocation itemId, int modelData) {
        if (modelData <= 0) return;
        ENTRIES.get(type).put(itemId, modelData);
    }
}

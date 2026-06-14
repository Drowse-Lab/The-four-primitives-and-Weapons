package the_four_primitives_and_weapons.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.network.SayaRegistrySyncPacket;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * JSON駆動の鞘(saya)登録レジストリ。
 * data/&lt;namespace&gt;/maw_saya/*.json (.jsonc) を自動収集し、
 * 各サヤタイプ (katana / tyokuto / sword) に納刀可能なアイテムを管理する。
 *
 * <h2>JSONフォーマット</h2>
 *
 * <p>各エントリは <b>整数</b> または <b>文字列</b> のいずれかを取れる:</p>
 *
 * <pre>
 * {
 *   "katana": {
 *     "your_mod:item_id_a": 1,                                    // ← 整数: 本体内蔵モデルのスロット番号
 *     "your_mod:item_id_b": "your_mod:custom/saya/katana/saya_x"  // ← 文字列: アドオン独自モデルパス
 *   }
 * }
 * </pre>
 *
 * <ul>
 *   <li>整数を指定すると本体MOD の saya.json overrides の対応 custom_model_data
 *       スロットの内蔵モデルが使われる (従来の挙動)。</li>
 *   <li>文字列を指定するとそのモデル ResourceLocation を SayaDynamicModelEvents
 *       が動的にロードして saya アイテム表示時に差し替える。<b>custom_model_data
 *       を経由しない</b>ため、いくらでも追加できて他アドオンと衝突しない。</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = "the_four_primitives_and_weapons")
public class SayaRegistry extends SimplePreparableReloadListener<Map<SayaRegistry.SayaType, Map<ResourceLocation, SayaRegistry.Entry>>> {

    public enum SayaType {
        KATANA("katana"),
        TYOKUTO("tyokuto"),
        SWORD("sword"),
        RAPIER("rapier");

        private final String key;

        SayaType(String key) { this.key = key; }

        public String getKey() { return key; }
    }

    /** 鞘の表示エントリ。modelData (>0) と modelLocation のどちらか一方が有効。 */
    public static final class Entry {
        private final int modelData;
        @Nullable
        private final ResourceLocation modelLocation;

        private Entry(int modelData, @Nullable ResourceLocation modelLocation) {
            this.modelData = modelData;
            this.modelLocation = modelLocation;
        }

        public static Entry ofSlot(int slot) {
            return new Entry(slot, null);
        }

        public static Entry ofModel(ResourceLocation modelLocation) {
            return new Entry(0, modelLocation);
        }

        public int modelData() { return modelData; }

        @Nullable
        public ResourceLocation modelLocation() { return modelLocation; }

        public boolean hasCustomModel() { return modelLocation != null; }
    }

    private static final Gson GSON = new GsonBuilder().create();
    private static final String DIRECTORY = "maw_saya";
    private static final SayaRegistry INSTANCE = new SayaRegistry();

    private static final Map<SayaType, Map<ResourceLocation, Entry>> ENTRIES = new EnumMap<>(SayaType.class);
    static {
        for (SayaType t : SayaType.values()) ENTRIES.put(t, new HashMap<>());
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
    }

    /**
     * データパックリロード時 / プレイヤーログイン時に呼ばれる。
     * サーバー側の ENTRIES をクライアントに同期する。
     */
    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        Map<SayaType, Map<ResourceLocation, Entry>> snapshot = snapshotEntries();
        SayaRegistrySyncPacket packet = new SayaRegistrySyncPacket(snapshot);
        ServerPlayer player = event.getPlayer();
        if (player != null) {
            TheFourPrimitivesAndWeaponsMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player), packet);
        } else {
            TheFourPrimitivesAndWeaponsMod.PACKET_HANDLER.send(
                PacketDistributor.ALL.noArg(), packet);
        }
    }

    /** 現在の ENTRIES の防御的コピーを返す (パケット送信用)。 */
    private static Map<SayaType, Map<ResourceLocation, Entry>> snapshotEntries() {
        Map<SayaType, Map<ResourceLocation, Entry>> copy = new EnumMap<>(SayaType.class);
        for (SayaType t : SayaType.values()) {
            copy.put(t, new HashMap<>(ENTRIES.get(t)));
        }
        return copy;
    }

    /**
     * クライアント側: 受信したエントリで ENTRIES を置き換える。
     * 呼び出しはネットハンドラから enqueueWork 経由なのでクライアントメインスレッド上で動く。
     */
    public static void replaceEntriesClient(Map<SayaType, Map<ResourceLocation, Entry>> incoming) {
        for (SayaType t : SayaType.values()) {
            Map<ResourceLocation, Entry> dest = ENTRIES.get(t);
            dest.clear();
            Map<ResourceLocation, Entry> src = incoming.get(t);
            if (src != null) dest.putAll(src);
        }
    }

    @Nonnull
    @Override
    protected Map<SayaType, Map<ResourceLocation, Entry>> prepare(@Nonnull ResourceManager manager,
                                                                     @Nonnull ProfilerFiller profiler) {
        Map<SayaType, Map<ResourceLocation, Entry>> result = new EnumMap<>(SayaType.class);
        for (SayaType t : SayaType.values()) result.put(t, new HashMap<>());

        manager.listResources(DIRECTORY, loc -> {
            String path = loc.getPath();
            int slash = path.lastIndexOf('/');
            String fileName = slash >= 0 ? path.substring(slash + 1) : path;
            return (path.endsWith(".json") || path.endsWith(".jsonc"))
                    && !fileName.startsWith("_");
        }).forEach((location, resource) -> {
            try (Reader reader = new InputStreamReader(resource.open())) {
                // .jsonc 対応: // と /* */ コメントを除去してから Gson に渡す
                String raw = readAll(reader);
                String stripped = stripJsonComments(raw);
                JsonObject root = GSON.fromJson(stripped, JsonObject.class);
                if (root == null) return;
                for (SayaType type : SayaType.values()) {
                    if (!root.has(type.getKey())) continue;
                    JsonElement el = root.get(type.getKey());
                    if (!el.isJsonObject()) continue;
                    JsonObject obj = el.getAsJsonObject();
                    Map<ResourceLocation, Entry> map = result.get(type);
                    for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                        try {
                            ResourceLocation itemId = new ResourceLocation(entry.getKey());
                            Entry parsed = parseEntry(entry.getValue());
                            if (parsed != null) map.put(itemId, parsed);
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

    /** JsonElement を Entry に変換。数値ならスロット、文字列ならモデルパス、その他は null。 */
    @Nullable
    private static Entry parseEntry(JsonElement el) {
        if (el == null || el.isJsonNull()) return null;
        if (!el.isJsonPrimitive()) return null;
        JsonPrimitive p = el.getAsJsonPrimitive();
        if (p.isNumber()) {
            int n = p.getAsInt();
            return n > 0 ? Entry.ofSlot(n) : null;
        }
        if (p.isString()) {
            String s = p.getAsString();
            if (s == null || s.isEmpty()) return null;
            try {
                return Entry.ofModel(new ResourceLocation(s));
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    /** Reader の全内容を文字列に読み込む。 */
    private static String readAll(Reader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[4096];
        int n;
        while ((n = reader.read(buf)) != -1) sb.append(buf, 0, n);
        return sb.toString();
    }

    /**
     * JSON 文字列から // と /* ... *\/ のコメントを除去。
     * 文字列リテラル内の // はそのまま残す。
     */
    private static String stripJsonComments(String src) {
        StringBuilder out = new StringBuilder(src.length());
        boolean inString = false;
        boolean escape = false;
        int i = 0;
        int len = src.length();
        while (i < len) {
            char c = src.charAt(i);
            if (inString) {
                out.append(c);
                if (escape) escape = false;
                else if (c == '\\') escape = true;
                else if (c == '"') inString = false;
                i++;
            } else {
                if (c == '"') {
                    inString = true;
                    out.append(c);
                    i++;
                } else if (c == '/' && i + 1 < len && src.charAt(i + 1) == '/') {
                    // 行コメント → 改行まで飛ばす
                    i += 2;
                    while (i < len && src.charAt(i) != '\n') i++;
                } else if (c == '/' && i + 1 < len && src.charAt(i + 1) == '*') {
                    // ブロックコメント → */ まで飛ばす
                    i += 2;
                    while (i + 1 < len && !(src.charAt(i) == '*' && src.charAt(i + 1) == '/')) i++;
                    i += 2;
                } else {
                    out.append(c);
                    i++;
                }
            }
        }
        return out.toString();
    }

    @Override
    protected void apply(@Nonnull Map<SayaType, Map<ResourceLocation, Entry>> prepared,
                         @Nonnull ResourceManager manager, @Nonnull ProfilerFiller profiler) {
        for (SayaType t : SayaType.values()) {
            Map<ResourceLocation, Entry> dest = ENTRIES.get(t);
            dest.clear();
            dest.putAll(prepared.get(t));
        }
    }

    // ==================================================
    // 公開 API
    // ==================================================

    /**
     * 指定タイプのサヤに対応するモデルデータを取得 (未登録なら 0)。
     * カスタムモデル登録の場合も 0 を返すため、模写識別は
     * {@link #getEntry(SayaType, ItemStack)} を併用すること。
     */
    public static int getModelData(SayaType type, ItemStack stack) {
        Entry e = getEntry(type, stack);
        return e == null ? 0 : e.modelData;
    }

    /** 指定タイプのサヤに登録された Entry を取得 (未登録なら null)。 */
    @Nullable
    public static Entry getEntry(SayaType type, ItemStack stack) {
        if (stack.isEmpty()) return null;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) return null;
        return ENTRIES.get(type).get(id);
    }

    /** 指定タイプのサヤに登録されているか判定 (slot/model どちらでも true)。 */
    public static boolean isRegistered(SayaType type, ItemStack stack) {
        return getEntry(type, stack) != null;
    }

    /** 指定タイプの全エントリを返す (クライアント側の事前ベイク用)。 */
    public static Map<ResourceLocation, Entry> getAllEntries(SayaType type) {
        return Collections.unmodifiableMap(ENTRIES.get(type));
    }

    /** 全タイプを通して、登録されている独自モデル ResourceLocation の集合を返す。 */
    public static Set<ResourceLocation> getAllCustomModelLocations() {
        Set<ResourceLocation> result = new HashSet<>();
        for (SayaType t : SayaType.values()) {
            for (Entry e : ENTRIES.get(t).values()) {
                if (e.modelLocation != null) result.add(e.modelLocation);
            }
        }
        return result;
    }

    /**
     * Java側からの追加登録用 (デバッグ・移行用)。通常はJSONで宣言する。
     */
    public static void register(SayaType type, ResourceLocation itemId, int modelData) {
        if (modelData <= 0) return;
        ENTRIES.get(type).put(itemId, Entry.ofSlot(modelData));
    }

    /** Java側からのカスタムモデル追加登録用。 */
    public static void registerModel(SayaType type, ResourceLocation itemId, ResourceLocation modelLocation) {
        if (modelLocation == null) return;
        ENTRIES.get(type).put(itemId, Entry.ofModel(modelLocation));
    }
}

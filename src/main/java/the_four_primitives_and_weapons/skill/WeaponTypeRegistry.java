package the_four_primitives_and_weapons.skill;

import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import the_four_primitives_and_weapons.skill.PlayerSkillData.AttackSlot;

import javax.annotation.Nonnull;
import java.io.*;
import java.util.*;

/**
 * JSON駆動の武器タイプレジストリ。
 * data/<namespace>/weapon_types/ 以下のJSONを読み込み、
 * 武器ごとの使用可能技と特殊技を管理する。
 * 他MODのデータパックからも追加可能。
 */
@Mod.EventBusSubscriber(modid = "the_four_primitives_and_weapons")
public class WeaponTypeRegistry extends SimplePreparableReloadListener<Map<String, WeaponTypeRegistry.WeaponTypeData>> {

    private static final Gson GSON = new GsonBuilder().create();
    private static final String DIRECTORY = "weapon_types";
    private static final WeaponTypeRegistry INSTANCE = new WeaponTypeRegistry();

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
    }

    // アイテムID → 武器タイプ
    private static final Map<String, WeaponTypeData> ITEM_TO_TYPE = new HashMap<>();
    // タイプID → 武器タイプ
    private static final Map<String, WeaponTypeData> TYPES = new LinkedHashMap<>();
    // アイテムID → 特殊武器定義
    private static final Map<String, SpecialWeaponData> SPECIAL_WEAPONS = new HashMap<>();

    // === データクラス ===

    public static class WeaponTypeData {
        private final String id;
        private final String displayName;
        private final List<String> items;
        private final Map<String, List<String>> motions;
        private final Map<String, String> defaultMotions; // slot_id → motion_id
        private final Set<String> preferredMotions; // 得意技（攻撃ゲージ+50%）
        private final Set<String> normalMotions;    // 通常技（ドキュメント目的、挙動変化なし）
        private final Set<String> dislikedMotions;  // 不得意技（攻撃力-40%/ゲージ-50%）

        public WeaponTypeData(String id, String displayName, List<String> items,
                              Map<String, List<String>> motions, Map<String, String> defaultMotions,
                              Set<String> preferredMotions, Set<String> normalMotions, Set<String> dislikedMotions) {
            this.id = id;
            this.displayName = displayName;
            this.items = items;
            this.motions = motions;
            this.defaultMotions = defaultMotions;
            this.preferredMotions = preferredMotions;
            this.normalMotions = normalMotions;
            this.dislikedMotions = dislikedMotions;
        }

        public String getId() { return id; }
        public String getDisplayName() { return displayName; }
        public List<String> getItems() { return items; }
        public Set<String> getPreferredMotions() { return preferredMotions; }
        public Set<String> getNormalMotions() { return normalMotions; }
        public Set<String> getDislikedMotions() { return dislikedMotions; }
        /** 翻訳キー: weapon_type.the_four_primitives_and_weapons.&lt;id&gt; (未翻訳の場合は displayName にフォールバック) */
        public String translationKey() { return "weapon_type.the_four_primitives_and_weapons." + id; }

        /**
         * 得意技か判定（combatスロット技のみ対象）。
         */
        public boolean isPreferredCombatMotion(String motionId) {
            if (preferredMotions == null || preferredMotions.isEmpty()) return false;
            if (motionId == null || motionId.isEmpty()) return false;
            List<String> combat = motions.getOrDefault("combat", Collections.emptyList());
            if (!combat.contains(motionId)) return false;
            return preferredMotions.contains(motionId);
        }

        /**
         * 不得意技か判定（combatスロット技のみ対象）。
         */
        public boolean isDislikedCombatMotion(String motionId) {
            if (dislikedMotions == null || dislikedMotions.isEmpty()) return false;
            if (motionId == null || motionId.isEmpty()) return false;
            List<String> combat = motions.getOrDefault("combat", Collections.emptyList());
            if (!combat.contains(motionId)) return false;
            return dislikedMotions.contains(motionId);
        }

        /**
         * 指定AttackSlotで使用可能なモーションIDリストを返す
         */
        public List<String> getMotionsForSlot(AttackSlot slot) {
            switch (slot) {
                case FIRST_HIT:
                case SECOND_HIT:
                case THIRD_HIT:
                case CHARGED:
                    return motions.getOrDefault("combat", Collections.emptyList());
                case DASH:
                    return motions.getOrDefault("dash", Collections.emptyList());
                case RIGHT_CLICK:
                    return motions.getOrDefault("right_click", Collections.emptyList());
                case SHIFT_RIGHT_CLICK:
                    return motions.getOrDefault("shift_right_click", Collections.emptyList());
                default:
                    return Collections.emptyList();
            }
        }

        /**
         * 指定AttackSlotのデフォルトモーションIDを返す（未定義ならnull）
         */
        public String getDefaultMotion(AttackSlot slot) {
            return defaultMotions.get(slot.getId());
        }
    }

    public static class SpecialWeaponData {
        private final boolean enabled;
        private final Map<String, List<String>> specialMotions;

        public SpecialWeaponData(boolean enabled, Map<String, List<String>> specialMotions) {
            this.enabled = enabled;
            this.specialMotions = specialMotions;
        }

        public boolean isEnabled() { return enabled; }

        public List<String> getSpecialMotionsForSlot(AttackSlot slot) {
            if (!enabled) return Collections.emptyList();
            switch (slot) {
                case FIRST_HIT:
                case SECOND_HIT:
                case THIRD_HIT:
                case CHARGED:
                    return specialMotions.getOrDefault("combat", Collections.emptyList());
                case DASH:
                    return specialMotions.getOrDefault("dash", Collections.emptyList());
                case RIGHT_CLICK:
                    return specialMotions.getOrDefault("right_click", Collections.emptyList());
                case SHIFT_RIGHT_CLICK:
                    return specialMotions.getOrDefault("shift_right_click", Collections.emptyList());
                default:
                    return Collections.emptyList();
            }
        }
    }

    // === リソースリロード ===

    @Nonnull
    @Override
    protected Map<String, WeaponTypeData> prepare(@Nonnull ResourceManager manager, @Nonnull ProfilerFiller profiler) {
        Map<String, WeaponTypeData> result = new LinkedHashMap<>();
        Map<String, SpecialWeaponData> specials = new HashMap<>();
        Map<String, Set<String>> preferredMap = new HashMap<>();
        Map<String, Set<String>> normalMap = new HashMap<>();
        Map<String, Set<String>> dislikedMap = new HashMap<>();

        manager.listResources(DIRECTORY, loc -> {
                    String path = loc.getPath();
                    String fileName = path.substring(path.lastIndexOf('/') + 1);
                    return (path.endsWith(".json") || path.endsWith(".jsonc"))
                            && !fileName.startsWith("_");
                }).forEach((location, resource) -> {
            try (Reader reader = new InputStreamReader(resource.open())) {
                // .jsonc 対応: // と /* */ コメントを除去してから Gson に渡す
                String raw = readAll(reader);
                String stripped = stripJsonComments(raw);
                JsonObject root = GSON.fromJson(stripped, JsonObject.class);
                if (root == null) return;

                // types の読み込み
                if (root.has("types")) {
                    JsonObject types = root.getAsJsonObject("types");
                    for (Map.Entry<String, JsonElement> entry : types.entrySet()) {
                        String typeId = entry.getKey();
                        JsonObject typeObj = entry.getValue().getAsJsonObject();
                        WeaponTypeData data = parseWeaponType(typeId, typeObj);
                        // 既存のタイプがあればアイテムリストをマージ
                        if (result.containsKey(typeId)) {
                            WeaponTypeData existing = result.get(typeId);
                            existing.items.addAll(data.items);
                            // motions はオーバーライド
                            existing.motions.putAll(data.motions);
                            // preferred/normal/disliked もマージ
                            existing.preferredMotions.addAll(data.preferredMotions);
                            existing.normalMotions.addAll(data.normalMotions);
                            existing.dislikedMotions.addAll(data.dislikedMotions);
                        } else {
                            result.put(typeId, data);
                        }
                    }
                }

                // special_weapons の読み込み
                if (root.has("special_weapons")) {
                    JsonObject sw = root.getAsJsonObject("special_weapons");
                    for (Map.Entry<String, JsonElement> entry : sw.entrySet()) {
                        String itemId = entry.getKey();
                        JsonObject swObj = entry.getValue().getAsJsonObject();
                        specials.put(itemId, parseSpecialWeapon(swObj));
                    }
                }

                // 専用ファイル preferred_motions.json 等から得意技マップを読み込み
                // (type ID → preferred motion ID list)
                if (root.has("preferred_motions") && root.get("preferred_motions").isJsonObject()) {
                    JsonObject pm = root.getAsJsonObject("preferred_motions");
                    for (Map.Entry<String, JsonElement> entry : pm.entrySet()) {
                        String typeId = entry.getKey();
                        Set<String> set = preferredMap.computeIfAbsent(typeId, k -> new HashSet<>());
                        for (JsonElement e : entry.getValue().getAsJsonArray()) {
                            String motionId = e.getAsString();
                            if (motionId != null && !motionId.isEmpty()) set.add(motionId);
                        }
                    }
                }

                // normal_motions マップを読み込み（ドキュメント目的、挙動変化なし）
                if (root.has("normal_motions") && root.get("normal_motions").isJsonObject()) {
                    JsonObject nm = root.getAsJsonObject("normal_motions");
                    for (Map.Entry<String, JsonElement> entry : nm.entrySet()) {
                        String typeId = entry.getKey();
                        Set<String> set = normalMap.computeIfAbsent(typeId, k -> new HashSet<>());
                        for (JsonElement e : entry.getValue().getAsJsonArray()) {
                            String motionId = e.getAsString();
                            if (motionId != null && !motionId.isEmpty()) set.add(motionId);
                        }
                    }
                }

                // 同様に disliked_motions マップを読み込み
                if (root.has("disliked_motions") && root.get("disliked_motions").isJsonObject()) {
                    JsonObject dm = root.getAsJsonObject("disliked_motions");
                    for (Map.Entry<String, JsonElement> entry : dm.entrySet()) {
                        String typeId = entry.getKey();
                        Set<String> set = dislikedMap.computeIfAbsent(typeId, k -> new HashSet<>());
                        for (JsonElement e : entry.getValue().getAsJsonArray()) {
                            String motionId = e.getAsString();
                            if (motionId != null && !motionId.isEmpty()) set.add(motionId);
                        }
                    }
                }
            } catch (Exception e) {
                // JSONパースエラーは無視（ログのみ）
            }
        });

        // preferred_motions マップを該当タイプに適用（types より後に読まれても反映される）
        for (Map.Entry<String, Set<String>> entry : preferredMap.entrySet()) {
            WeaponTypeData type = result.get(entry.getKey());
            if (type != null) {
                type.preferredMotions.addAll(entry.getValue());
            }
        }
        // normal_motions も同様
        for (Map.Entry<String, Set<String>> entry : normalMap.entrySet()) {
            WeaponTypeData type = result.get(entry.getKey());
            if (type != null) {
                type.normalMotions.addAll(entry.getValue());
            }
        }
        // disliked_motions も同様
        for (Map.Entry<String, Set<String>> entry : dislikedMap.entrySet()) {
            WeaponTypeData type = result.get(entry.getKey());
            if (type != null) {
                type.dislikedMotions.addAll(entry.getValue());
            }
        }

        // 一時保存（applyで使う）
        tempSpecials = specials;
        return result;
    }

    private Map<String, SpecialWeaponData> tempSpecials = new HashMap<>();

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
    protected void apply(@Nonnull Map<String, WeaponTypeData> prepared, @Nonnull ResourceManager manager, @Nonnull ProfilerFiller profiler) {
        TYPES.clear();
        ITEM_TO_TYPE.clear();
        SPECIAL_WEAPONS.clear();

        TYPES.putAll(prepared);

        // アイテムID → タイプのマッピング構築
        for (WeaponTypeData type : TYPES.values()) {
            for (String itemId : type.items) {
                ITEM_TO_TYPE.put(itemId, type);
            }
        }

        SPECIAL_WEAPONS.putAll(tempSpecials);
        tempSpecials.clear();

        // weapon_stats の「item別×type別」マージ結果はこのタイプ表に依存している。
        // ここで捨てないと、 リロード順によっては タイプ表が空だった瞬間の
        // 結果 ( types セクションが効いていない状態 ) を掴んだままになる。
        WeaponStatsRegistry.invalidateCache();
    }

    // === パーサー ===

    private static WeaponTypeData parseWeaponType(String id, JsonObject obj) {
        String displayName = obj.has("display_name") ? obj.get("display_name").getAsString() : id;

        List<String> items = new ArrayList<>();
        if (obj.has("items")) {
            for (JsonElement e : obj.getAsJsonArray("items")) {
                items.add(e.getAsString());
            }
        }

        Map<String, List<String>> motions = new HashMap<>();
        if (obj.has("motions")) {
            JsonObject motionsObj = obj.getAsJsonObject("motions");
            for (Map.Entry<String, JsonElement> entry : motionsObj.entrySet()) {
                List<String> motionList = new ArrayList<>();
                for (JsonElement e : entry.getValue().getAsJsonArray()) {
                    motionList.add(e.getAsString());
                }
                motions.put(entry.getKey(), motionList);
            }
        }

        Map<String, String> defaultMotions = new HashMap<>();
        if (obj.has("default_motions")) {
            JsonObject defObj = obj.getAsJsonObject("default_motions");
            for (Map.Entry<String, JsonElement> entry : defObj.entrySet()) {
                defaultMotions.put(entry.getKey(), entry.getValue().getAsString());
            }
        }

        Set<String> preferredMotions = new HashSet<>();
        if (obj.has("preferred_motions")) {
            for (JsonElement e : obj.getAsJsonArray("preferred_motions")) {
                preferredMotions.add(e.getAsString());
            }
        }

        Set<String> normalMotions = new HashSet<>();
        if (obj.has("normal_motions")) {
            for (JsonElement e : obj.getAsJsonArray("normal_motions")) {
                normalMotions.add(e.getAsString());
            }
        }

        Set<String> dislikedMotions = new HashSet<>();
        if (obj.has("disliked_motions")) {
            for (JsonElement e : obj.getAsJsonArray("disliked_motions")) {
                dislikedMotions.add(e.getAsString());
            }
        }

        return new WeaponTypeData(id, displayName, items, motions, defaultMotions,
                preferredMotions, normalMotions, dislikedMotions);
    }

    private static SpecialWeaponData parseSpecialWeapon(JsonObject obj) {
        boolean enabled = obj.has("enabled") && obj.get("enabled").getAsBoolean();

        Map<String, List<String>> specialMotions = new HashMap<>();
        if (obj.has("special_motions")) {
            JsonObject sm = obj.getAsJsonObject("special_motions");
            for (Map.Entry<String, JsonElement> entry : sm.entrySet()) {
                List<String> motionList = new ArrayList<>();
                for (JsonElement e : entry.getValue().getAsJsonArray()) {
                    motionList.add(e.getAsString());
                }
                specialMotions.put(entry.getKey(), motionList);
            }
        }

        return new SpecialWeaponData(enabled, specialMotions);
    }

    // === 公開API ===

    /**
     * アイテムの武器タイプを取得（未登録ならnull）
     * 他MODの弓/クロスボウ/投げ系も instanceof でフォールバック判定する。
     * トライデントは除外。
     */
    public static WeaponTypeData getTypeForItem(ItemStack stack) {
        if (stack.isEmpty()) return null;
        ResourceLocation regName = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (regName != null) {
            WeaponTypeData registered = ITEM_TO_TYPE.get(regName.toString());
            if (registered != null) return registered;
        }

        // 他MODの遠距離武器を instanceof でフォールバック判定
        net.minecraft.world.item.Item item = stack.getItem();
        if (item instanceof net.minecraft.world.item.TridentItem) return null;
        if (item instanceof net.minecraft.world.item.BowItem) return TYPES.get("bow");
        if (item instanceof net.minecraft.world.item.CrossbowItem) return TYPES.get("crossbow");
        if (item instanceof the_four_primitives_and_weapons.item.ThrowingKnifeItem) return TYPES.get("throwing");
        return null;
    }

    /**
     * 回転技 (spin_slash / hookshot 縦回転) の範囲倍率を武器タイプから取得。
     * 1.0 を基準として、リーチが長い武器ほど大きい値を返す。
     */
    public static double getSpinRangeScale(ItemStack stack) {
        WeaponTypeData type = getTypeForItem(stack);
        if (type == null) return 1.0;
        switch (type.getId()) {
            case "dagger":         return 0.7;
            case "straight_sword": return 0.95;
            case "katana":         return 1.0;
            case "sword":          return 1.05;
            case "rapier":         return 1.2;
            case "greatsword":     return 1.6;
            case "trident":        return 1.5; // 槍
            default:               return 1.0;
        }
    }

    /**
     * アイテムの特殊武器定義を取得（未登録ならnull）
     */
    public static SpecialWeaponData getSpecialForItem(ItemStack stack) {
        if (stack.isEmpty()) return null;
        ResourceLocation regName = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (regName == null) return null;
        return SPECIAL_WEAPONS.get(regName.toString());
    }

    /**
     * 指定アイテム・スロットで使用可能なモーションIDリストを返す
     * （武器タイプの基本モーション + 特殊技）
     */
    public static List<String> getAvailableMotionIds(ItemStack stack, AttackSlot slot) {
        List<String> result = new ArrayList<>();

        WeaponTypeData type = getTypeForItem(stack);
        if (type != null) {
            result.addAll(type.getMotionsForSlot(slot));
        }

        SpecialWeaponData special = getSpecialForItem(stack);
        if (special != null && special.isEnabled()) {
            result.addAll(special.getSpecialMotionsForSlot(slot));
        }

        return result;
    }

    /**
     * 全武器タイプを返す
     */
    public static Collection<WeaponTypeData> getAllTypes() {
        return TYPES.values();
    }

    /**
     * 特定タイプを取得
     */
    public static WeaponTypeData getType(String typeId) {
        return TYPES.get(typeId);
    }

    /**
     * データが読み込み済みかどうか
     */
    public static boolean isLoaded() {
        return !TYPES.isEmpty();
    }
}

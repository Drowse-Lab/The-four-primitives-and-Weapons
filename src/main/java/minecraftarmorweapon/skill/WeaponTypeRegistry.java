package minecraftarmorweapon.skill;

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

import minecraftarmorweapon.skill.PlayerSkillData.AttackSlot;

import javax.annotation.Nonnull;
import java.io.*;
import java.util.*;

/**
 * JSON駆動の武器タイプレジストリ。
 * data/<namespace>/weapon_types/ 以下のJSONを読み込み、
 * 武器ごとの使用可能技と特殊技を管理する。
 * 他MODのデータパックからも追加可能。
 */
@Mod.EventBusSubscriber(modid = "minecraft_armor_weapon")
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

        public WeaponTypeData(String id, String displayName, List<String> items,
                              Map<String, List<String>> motions, Map<String, String> defaultMotions) {
            this.id = id;
            this.displayName = displayName;
            this.items = items;
            this.motions = motions;
            this.defaultMotions = defaultMotions;
        }

        public String getId() { return id; }
        public String getDisplayName() { return displayName; }
        public List<String> getItems() { return items; }

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

        manager.listResources(DIRECTORY, loc -> loc.getPath().endsWith(".json")
                && !loc.getPath().substring(loc.getPath().lastIndexOf('/') + 1).startsWith("_")).forEach((location, resource) -> {
            try (Reader reader = new InputStreamReader(resource.open())) {
                JsonObject root = GSON.fromJson(reader, JsonObject.class);
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
            } catch (Exception e) {
                // JSONパースエラーは無視（ログのみ）
            }
        });

        // 一時保存（applyで使う）
        tempSpecials = specials;
        return result;
    }

    private Map<String, SpecialWeaponData> tempSpecials = new HashMap<>();

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

        return new WeaponTypeData(id, displayName, items, motions, defaultMotions);
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
     */
    public static WeaponTypeData getTypeForItem(ItemStack stack) {
        if (stack.isEmpty()) return null;
        ResourceLocation regName = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (regName == null) return null;
        return ITEM_TO_TYPE.get(regName.toString());
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

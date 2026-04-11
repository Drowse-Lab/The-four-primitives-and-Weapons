package minecraftarmorweapon.config;

import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.util.*;

/**
 * JSONで難易度別Mob装備を一括管理。
 * data/<namespace>/equipment_tiers/ 以下のJSONを読み込む。
 * 他MODのアイテムもレジストリ名で指定可能。
 */
@Mod.EventBusSubscriber(modid = "minecraft_armor_weapon")
public class EquipmentTierRegistry extends SimplePreparableReloadListener<Map<Integer, Map<String, EquipmentTierRegistry.MobEquipment>>> {

    private static final Gson GSON = new GsonBuilder().create();
    private static final EquipmentTierRegistry INSTANCE = new EquipmentTierRegistry();

    // tier -> (entityTypeId -> MobEquipment)
    private static final Map<Integer, Map<String, MobEquipment>> TIERS = new HashMap<>();

    /**
     * 1スロットの装備情報
     */
    public static class SlotEntry {
        public final String itemId;
        public final List<EnchantEntry> enchantments;
        public final float dropChance;
        /** 装備が出現する確率 (0.0-1.0)。1.0 = 確定出現 */
        public final float chance;

        public SlotEntry(String itemId, List<EnchantEntry> enchantments, float dropChance, float chance) {
            this.itemId = itemId;
            this.enchantments = enchantments;
            this.dropChance = dropChance;
            this.chance = chance;
        }

        /**
         * ItemStackを生成する。アイテムが見つからない場合はnullを返す。
         */
        @Nullable
        public ItemStack createStack() {
            var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemId));
            if (item == null || item == net.minecraft.world.item.Items.AIR) {
                return null;
            }
            ItemStack stack = new ItemStack(item);
            for (EnchantEntry ench : enchantments) {
                Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(new ResourceLocation(ench.id));
                if (enchantment != null) {
                    stack.enchant(enchantment, ench.level);
                }
            }
            return stack;
        }
    }

    public static class EnchantEntry {
        public final String id;
        public final int level;

        public EnchantEntry(String id, int level) {
            this.id = id;
            this.level = level;
        }
    }

    /**
     * あるMobタイプの全スロット装備情報
     */
    public static class MobEquipment {
        private final Map<EquipmentSlot, SlotEntry> slots = new EnumMap<>(EquipmentSlot.class);

        public void setSlot(EquipmentSlot slot, SlotEntry entry) {
            slots.put(slot, entry);
        }

        @Nullable
        public SlotEntry getSlot(EquipmentSlot slot) {
            return slots.get(slot);
        }

        public Set<Map.Entry<EquipmentSlot, SlotEntry>> getSlots() {
            return slots.entrySet();
        }

        public boolean isEmpty() {
            return slots.isEmpty();
        }
    }

    // === リソースリスナー登録 ===

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
    }

    @Nonnull
    @Override
    protected Map<Integer, Map<String, MobEquipment>> prepare(@Nonnull ResourceManager manager, @Nonnull ProfilerFiller profiler) {
        Map<Integer, Map<String, MobEquipment>> result = new HashMap<>();

        manager.listResources("equipment_tiers", loc -> loc.getPath().endsWith(".json")).forEach((location, resource) -> {
            try (Reader reader = new InputStreamReader(resource.open())) {
                JsonObject root = GSON.fromJson(reader, JsonObject.class);
                if (root == null || !root.has("tiers")) return;

                JsonObject tiers = root.getAsJsonObject("tiers");
                for (Map.Entry<String, JsonElement> tierEntry : tiers.entrySet()) {
                    String tierKey = tierEntry.getKey();
                    // _commentキーをスキップ
                    if (tierKey.startsWith("_")) continue;

                    int tier;
                    try {
                        tier = Integer.parseInt(tierKey);
                    } catch (NumberFormatException e) {
                        continue;
                    }

                    JsonObject tierObj = tierEntry.getValue().getAsJsonObject();
                    if (!tierObj.has("mob_types")) continue;

                    JsonObject mobTypes = tierObj.getAsJsonObject("mob_types");
                    Map<String, MobEquipment> mobMap = result.computeIfAbsent(tier, k -> new HashMap<>());

                    for (Map.Entry<String, JsonElement> mobEntry : mobTypes.entrySet()) {
                        String mobTypeId = mobEntry.getKey();
                        JsonObject slotsObj = mobEntry.getValue().getAsJsonObject();
                        MobEquipment equipment = parseMobEquipment(slotsObj);
                        if (!equipment.isEmpty()) {
                            mobMap.put(mobTypeId, equipment);
                        }
                    }
                }
            } catch (Exception e) {
                // パースエラーは無視
            }
        });

        return result;
    }

    @Override
    protected void apply(@Nonnull Map<Integer, Map<String, MobEquipment>> prepared,
                         @Nonnull ResourceManager manager, @Nonnull ProfilerFiller profiler) {
        TIERS.clear();
        TIERS.putAll(prepared);
    }

    // === JSON解析 ===

    private static MobEquipment parseMobEquipment(JsonObject slotsObj) {
        MobEquipment equipment = new MobEquipment();

        for (Map.Entry<String, JsonElement> slotEntry : slotsObj.entrySet()) {
            String slotName = slotEntry.getKey();
            // _commentキーをスキップ
            if (slotName.startsWith("_")) continue;

            EquipmentSlot slot = parseSlotName(slotName);
            if (slot == null) continue;

            JsonObject slotObj = slotEntry.getValue().getAsJsonObject();
            String itemId = slotObj.has("item") ? slotObj.get("item").getAsString() : null;
            if (itemId == null || itemId.isEmpty()) continue;

            float dropChance = slotObj.has("drop_chance") ? slotObj.get("drop_chance").getAsFloat() : 0.0f;
            float chance = slotObj.has("chance") ? slotObj.get("chance").getAsFloat() : 1.0f;

            List<EnchantEntry> enchantments = new ArrayList<>();
            if (slotObj.has("enchantments")) {
                JsonArray enchArray = slotObj.getAsJsonArray("enchantments");
                for (JsonElement enchElem : enchArray) {
                    JsonObject enchObj = enchElem.getAsJsonObject();
                    String enchId = enchObj.has("id") ? enchObj.get("id").getAsString() : null;
                    int enchLevel = enchObj.has("level") ? enchObj.get("level").getAsInt() : 1;
                    if (enchId != null) {
                        enchantments.add(new EnchantEntry(enchId, enchLevel));
                    }
                }
            }

            equipment.setSlot(slot, new SlotEntry(itemId, enchantments, dropChance, chance));
        }

        return equipment;
    }

    @Nullable
    private static EquipmentSlot parseSlotName(String name) {
        return switch (name.toLowerCase()) {
            case "head" -> EquipmentSlot.HEAD;
            case "chest" -> EquipmentSlot.CHEST;
            case "legs" -> EquipmentSlot.LEGS;
            case "feet" -> EquipmentSlot.FEET;
            case "mainhand" -> EquipmentSlot.MAINHAND;
            case "offhand" -> EquipmentSlot.OFFHAND;
            default -> null;
        };
    }

    // === 公開API ===

    /**
     * 指定されたtierとエンティティタイプに対応する装備を取得する。
     * エンティティタイプが見つからない場合はnullを返す。
     */
    @Nullable
    public static MobEquipment getEquipment(int tier, String entityTypeId) {
        Map<String, MobEquipment> mobMap = TIERS.get(tier);
        if (mobMap == null) return null;
        return mobMap.get(entityTypeId);
    }

    /**
     * Mobに装備を適用する。
     * @return 装備が適用された場合true
     */
    public static boolean applyEquipment(Mob mob, int tier) {
        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
        if (entityId == null) return false;

        MobEquipment equipment = getEquipment(tier, entityId.toString());
        if (equipment == null) return false;

        for (var entry : equipment.getSlots()) {
            EquipmentSlot slot = entry.getKey();
            SlotEntry slotEntry = entry.getValue();

            // 出現率チェック（chance未指定 or 1.0 なら確定）
            if (slotEntry.chance < 1.0f && mob.getRandom().nextFloat() >= slotEntry.chance) {
                continue;
            }

            ItemStack stack = slotEntry.createStack();
            if (stack != null) {
                mob.setItemSlot(slot, stack);
                mob.setDropChance(slot, slotEntry.dropChance);
            }
        }

        return true;
    }

    /**
     * レジストリにデータがロードされているか
     */
    public static boolean isLoaded() {
        return !TIERS.isEmpty();
    }

    /**
     * ロード済みのtier一覧を取得
     */
    public static Set<Integer> getLoadedTiers() {
        return Collections.unmodifiableSet(TIERS.keySet());
    }
}

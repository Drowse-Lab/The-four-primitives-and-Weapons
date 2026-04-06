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

import javax.annotation.Nonnull;
import java.io.*;
import java.util.*;

/**
 * JSONで武器ステータス(耐久値、エンチャント適性、攻撃力、攻撃速度)を一括管理。
 * data/<namespace>/weapon_stats/ 以下のJSONを読み込む。
 *
 * 攻撃力と攻撃速度はAttributeModifierで上書きする。
 * 耐久値とエンチャント適性はMixinで上書きする。
 */
@Mod.EventBusSubscriber(modid = "minecraft_armor_weapon")
public class WeaponStatsRegistry extends SimplePreparableReloadListener<Map<String, WeaponStatsRegistry.WeaponStats>> {

    private static final Gson GSON = new GsonBuilder().create();
    private static final WeaponStatsRegistry INSTANCE = new WeaponStatsRegistry();

    // アイテムID → ステータス
    private static final Map<String, WeaponStats> STATS = new HashMap<>();

    public static class WeaponStats {
        public final int durability;
        public final int enchantability;
        public final float damageBonus;
        public final float attackSpeed;

        public WeaponStats(int durability, int enchantability, float damageBonus, float attackSpeed) {
            this.durability = durability;
            this.enchantability = enchantability;
            this.damageBonus = damageBonus;
            this.attackSpeed = attackSpeed;
        }
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
    }

    @Nonnull
    @Override
    protected Map<String, WeaponStats> prepare(@Nonnull ResourceManager manager, @Nonnull ProfilerFiller profiler) {
        Map<String, WeaponStats> result = new HashMap<>();

        manager.listResources("weapon_stats", loc -> loc.getPath().endsWith(".json")).forEach((location, resource) -> {
            try (Reader reader = new InputStreamReader(resource.open())) {
                JsonObject root = GSON.fromJson(reader, JsonObject.class);
                if (root == null || !root.has("weapons")) return;

                JsonObject weapons = root.getAsJsonObject("weapons");
                for (Map.Entry<String, JsonElement> entry : weapons.entrySet()) {
                    String itemId = entry.getKey();
                    JsonObject stats = entry.getValue().getAsJsonObject();

                    int durability = stats.has("durability") ? stats.get("durability").getAsInt() : -1;
                    int enchant = stats.has("enchantability") ? stats.get("enchantability").getAsInt() : -1;
                    float damage = stats.has("damage_bonus") ? stats.get("damage_bonus").getAsFloat() : Float.NaN;
                    float speed = stats.has("attack_speed") ? stats.get("attack_speed").getAsFloat() : Float.NaN;

                    result.put(itemId, new WeaponStats(durability, enchant, damage, speed));
                }
            } catch (Exception e) {
                // パースエラーは無視
            }
        });

        return result;
    }

    @Override
    protected void apply(@Nonnull Map<String, WeaponStats> prepared, @Nonnull ResourceManager manager, @Nonnull ProfilerFiller profiler) {
        STATS.clear();
        STATS.putAll(prepared);
    }

    // === 公開API ===

    public static WeaponStats getStats(ItemStack stack) {
        if (stack.isEmpty()) return null;
        ResourceLocation regName = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (regName == null) return null;
        return STATS.get(regName.toString());
    }

    public static WeaponStats getStats(String itemId) {
        return STATS.get(itemId);
    }

    /**
     * エンチャント適性を取得（JSON定義があれば上書き、なければ-1）
     */
    public static int getEnchantability(ItemStack stack) {
        WeaponStats stats = getStats(stack);
        return stats != null ? stats.enchantability : -1;
    }

    /**
     * 耐久値を取得（JSON定義があれば上書き、なければ-1）
     */
    public static int getDurability(ItemStack stack) {
        WeaponStats stats = getStats(stack);
        return stats != null ? stats.durability : -1;
    }

    public static boolean isLoaded() {
        return !STATS.isEmpty();
    }
}

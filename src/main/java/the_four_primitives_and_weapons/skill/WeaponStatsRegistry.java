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
@Mod.EventBusSubscriber(modid = "the_four_primitives_and_weapons")
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
        /** 総攻撃力の絶対値上書き ( NaN=未設定 )。 */
        public final float attackDamage;
        /** 近接リーチ ( ENTITY_REACH ) の加算値 ( NaN=未設定 )。 マイナスで短く。 */
        public final float attackRange;
        /** チャージ突きの設定 ( null=なし )。 */
        public final ThrustConfig thrust;

        public WeaponStats(int durability, int enchantability, float damageBonus, float attackSpeed) {
            this(durability, enchantability, damageBonus, attackSpeed, Float.NaN, Float.NaN, null);
        }

        public WeaponStats(int durability, int enchantability, float damageBonus, float attackSpeed,
                           float attackDamage, float attackRange, ThrustConfig thrust) {
            this.durability = durability;
            this.enchantability = enchantability;
            this.damageBonus = damageBonus;
            this.attackSpeed = attackSpeed;
            this.attackDamage = attackDamage;
            this.attackRange = attackRange;
            this.thrust = thrust;
        }
    }

    /** チャージ突き連撃の設定 ( JSON: "thrust": { range, hits, knockback, dash, damage } )。 */
    public static class ThrustConfig {
        public final double range;      // 突きの到達距離 ( 短い=奥行きが短い )
        public final int hits;          // 連撃数
        public final double knockback;  // ノックバック
        public final double dash;       // 前方への踏み込み量
        public final float damage;      // 1ヒットのダメージ ( <=0 なら武器の攻撃力を使用 )

        public ThrustConfig(double range, int hits, double knockback, double dash, float damage) {
            this.range = range;
            this.hits = hits;
            this.knockback = knockback;
            this.dash = dash;
            this.damage = damage;
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
                    // _comment_* などオブジェクトでないエントリはスキップ ( コメント行 )。
                    if (!entry.getValue().isJsonObject()) continue;
                    JsonObject stats = entry.getValue().getAsJsonObject();

                    int durability = stats.has("durability") ? stats.get("durability").getAsInt() : -1;
                    int enchant = stats.has("enchantability") ? stats.get("enchantability").getAsInt() : -1;
                    float damage = stats.has("damage_bonus") ? stats.get("damage_bonus").getAsFloat() : Float.NaN;
                    float speed = stats.has("attack_speed") ? stats.get("attack_speed").getAsFloat() : Float.NaN;
                    float atkDamage = stats.has("attack_damage") ? stats.get("attack_damage").getAsFloat() : Float.NaN;
                    float atkRange = stats.has("attack_range") ? stats.get("attack_range").getAsFloat() : Float.NaN;

                    ThrustConfig thrust = null;
                    if (stats.has("thrust") && stats.get("thrust").isJsonObject()) {
                        JsonObject th = stats.getAsJsonObject("thrust");
                        boolean enabled = !th.has("enabled") || th.get("enabled").getAsBoolean();
                        if (enabled) {
                            double range = th.has("range") ? th.get("range").getAsDouble() : 3.0;
                            int hits = th.has("hits") ? th.get("hits").getAsInt() : 3;
                            double kb = th.has("knockback") ? th.get("knockback").getAsDouble() : 0.3;
                            double dash = th.has("dash") ? th.get("dash").getAsDouble() : 0.5;
                            float dmg = th.has("damage") ? th.get("damage").getAsFloat() : 0f;
                            thrust = new ThrustConfig(range, hits, kb, dash, dmg);
                        }
                    }

                    result.put(itemId, new WeaponStats(durability, enchant, damage, speed, atkDamage, atkRange, thrust));
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

    /** 攻撃範囲ボーナス ( JSONの attack_range。 未設定=0 )。 各技の range に加算する用。 */
    public static double attackRangeBonus(ItemStack stack) {
        WeaponStats s = getStats(stack);
        return (s != null && !Float.isNaN(s.attackRange)) ? s.attackRange : 0.0;
    }

    public static boolean isLoaded() {
        return !STATS.isEmpty();
    }
}

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
public class WeaponStatsRegistry extends SimplePreparableReloadListener<WeaponStatsRegistry.Prepared> {

    private static final Gson GSON = new GsonBuilder().create();
    private static final WeaponStatsRegistry INSTANCE = new WeaponStatsRegistry();

    // アイテムID → ステータス ( "weapons" セクション )
    private static final Map<String, WeaponStats> STATS = new HashMap<>();
    // 武器タイプID → 既定ステータス ( "types" セクション )。 item に値が無いフィールドはこちらで補完。
    private static final Map<String, WeaponStats> TYPE_STATS = new HashMap<>();
    // item別×type別のマージ結果キャッシュ ( getStats は高頻度呼び出しなので毎回 merge しない )。
    // reload の apply でクリア。 "ステータス無し" は NONE_STATS を入れて null 再計算も防ぐ。
    private static final Map<String, WeaponStats> MERGED_CACHE = new java.util.concurrent.ConcurrentHashMap<>();
    private static final WeaponStats NONE_STATS = new WeaponStats(-1, -1, Float.NaN, Float.NaN);

    /** reload の prepare→apply 間で item別 / type別 の両マップを受け渡す。 */
    public static class Prepared {
        final Map<String, WeaponStats> items;
        final Map<String, WeaponStats> types;
        Prepared(Map<String, WeaponStats> items, Map<String, WeaponStats> types) {
            this.items = items;
            this.types = types;
        }
    }

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
        /** 投擲の設定 ( null=なし )。 */
        public final ThrowConfig throwCfg;

        public WeaponStats(int durability, int enchantability, float damageBonus, float attackSpeed) {
            this(durability, enchantability, damageBonus, attackSpeed, Float.NaN, Float.NaN, null, null);
        }

        public WeaponStats(int durability, int enchantability, float damageBonus, float attackSpeed,
                           float attackDamage, float attackRange, ThrustConfig thrust) {
            this(durability, enchantability, damageBonus, attackSpeed, attackDamage, attackRange, thrust, null);
        }

        public WeaponStats(int durability, int enchantability, float damageBonus, float attackSpeed,
                           float attackDamage, float attackRange, ThrustConfig thrust, ThrowConfig throwCfg) {
            this.durability = durability;
            this.enchantability = enchantability;
            this.damageBonus = damageBonus;
            this.attackSpeed = attackSpeed;
            this.attackDamage = attackDamage;
            this.attackRange = attackRange;
            this.thrust = thrust;
            this.throwCfg = throwCfg;
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

    /**
     * 投擲の設定 ( JSON: {@code "throw": { damage, cooldown, velocity, hunger, stuck_lifetime }} )。
     *
     * <p>投げナイフと、 右クリックで投げられる武器 ( ダガー ) の両方が参照する。
     * 各値は「未設定なら Java 側の既定を使う」ため、 書いた項目だけが効く。</p>
     */
    public static class ThrowConfig {
        /** 命中ダメージ ( NaN=未設定 → 武器の攻撃力 / ナイフ既定 )。 */
        public final float damage;
        /** 投擲後のクールダウン tick ( -1=未設定 )。 */
        public final int cooldown;
        /** 射出初速 ( NaN=未設定 )。 大きいほど速く遠くへ飛ぶ。 */
        public final float velocity;
        /** 1投あたりの食料ゲージ消費 ( NaN=未設定 )。 肉アイコン半分=1.0。 */
        public final float hunger;
        /** 刺さってから消えるまでの tick ( -1=未設定 )。 */
        public final int stuckLifetime;
        /** 刺さる深さ ( 1 = 1/100 ブロック、 NaN=未設定 )。 大きいほど深く突き刺さる。
         *  刀身の長さは武器ごとに違うので、 同じ値だと埋まり具合がそろわない。 */
        public final float stickForward;
        /** 衝突面の法線方向オフセット ( 1 = 1/100 ブロック、 NaN=未設定 )。 マイナスで壁にめり込む。 */
        public final float stickNormal;
        /**
         * エンティティ原点から刃先までの距離 ( ブロック、 NaN=未設定 → Java 既定の 0.5 )。
         *
         * <p>飛翔体は ground display で描かれるので、 モデルの
         * {@code display.ground} の translation / scale を変えると刃先の位置も動く。
         * この値がずれていると、 同じ {@code stick_forward} でも刺さり具合が武器ごとに変わる。</p>
         */
        public final float stickTip;

        public ThrowConfig(float damage, int cooldown, float velocity, float hunger, int stuckLifetime,
                           float stickForward, float stickNormal, float stickTip) {
            this.damage = damage;
            this.cooldown = cooldown;
            this.velocity = velocity;
            this.hunger = hunger;
            this.stuckLifetime = stuckLifetime;
            this.stickForward = stickForward;
            this.stickNormal = stickNormal;
            this.stickTip = stickTip;
        }
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
    }

    @Nonnull
    @Override
    protected Prepared prepare(@Nonnull ResourceManager manager, @Nonnull ProfilerFiller profiler) {
        Map<String, WeaponStats> items = new HashMap<>();
        Map<String, WeaponStats> types = new HashMap<>();

        manager.listResources("weapon_stats", loc -> loc.getPath().endsWith(".json")).forEach((location, resource) -> {
            try (Reader reader = new InputStreamReader(resource.open())) {
                JsonObject root = GSON.fromJson(reader, JsonObject.class);
                if (root == null) return;
                // "weapons": item別 ( 最優先 ) / "types": 武器タイプ別の既定値。
                parseSection(root, "weapons", items);
                parseSection(root, "types", types);
            } catch (Exception e) {
                // パースエラーは無視
            }
        });

        return new Prepared(items, types);
    }

    /** root の {@code section} オブジェクトを走査し、key→WeaponStats を {@code out} に格納する。 */
    private static void parseSection(JsonObject root, String section, Map<String, WeaponStats> out) {
        if (root == null || !root.has(section) || !root.get(section).isJsonObject()) return;
        JsonObject obj = root.getAsJsonObject(section);
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            // _comment_* などオブジェクトでないエントリはスキップ ( コメント行 )。
            if (!entry.getValue().isJsonObject()) continue;
            out.put(entry.getKey(), parseStats(entry.getValue().getAsJsonObject()));
        }
    }

    /** JSON 1 エントリを WeaponStats へ。 未設定フィールドは番兵値 ( -1 / NaN / null )。 */
    private static WeaponStats parseStats(JsonObject stats) {
        int durability = stats.has("durability") ? stats.get("durability").getAsInt() : -1;
        int enchant = stats.has("enchantability") ? stats.get("enchantability").getAsInt() : -1;
        float damage = stats.has("damage_bonus") ? stats.get("damage_bonus").getAsFloat() : Float.NaN;
        float speed = stats.has("attack_speed") ? stats.get("attack_speed").getAsFloat() : Float.NaN;
        float atkDamage = stats.has("attack_damage") ? stats.get("attack_damage").getAsFloat() : Float.NaN;
        float atkRange = stats.has("attack_range") ? stats.get("attack_range").getAsFloat() : Float.NaN;
        ThrowConfig throwCfg = null;
        if (stats.has("throw") && stats.get("throw").isJsonObject()) {
            JsonObject tw = stats.getAsJsonObject("throw");
            throwCfg = new ThrowConfig(
                    tw.has("damage")         ? tw.get("damage").getAsFloat()        : Float.NaN,
                    tw.has("cooldown")       ? tw.get("cooldown").getAsInt()        : -1,
                    tw.has("velocity")       ? tw.get("velocity").getAsFloat()      : Float.NaN,
                    tw.has("hunger")         ? tw.get("hunger").getAsFloat()        : Float.NaN,
                    tw.has("stuck_lifetime") ? tw.get("stuck_lifetime").getAsInt()  : -1,
                    tw.has("stick_forward")  ? tw.get("stick_forward").getAsFloat()  : Float.NaN,
                    tw.has("stick_normal")   ? tw.get("stick_normal").getAsFloat()   : Float.NaN,
                    tw.has("stick_tip")      ? tw.get("stick_tip").getAsFloat()      : Float.NaN);
        }

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
        return new WeaponStats(durability, enchant, damage, speed, atkDamage, atkRange, thrust, throwCfg);
    }

    @Override
    protected void apply(@Nonnull Prepared prepared, @Nonnull ResourceManager manager, @Nonnull ProfilerFiller profiler) {
        STATS.clear();
        STATS.putAll(prepared.items);
        TYPE_STATS.clear();
        TYPE_STATS.putAll(prepared.types);
        MERGED_CACHE.clear();
    }

    /** item別ステータスを type別既定で補完する ( item に値のあるフィールドが優先 )。 */
    private static WeaponStats merge(WeaponStats item, WeaponStats type) {
        if (type == null) return item;
        if (item == null) return type;
        return new WeaponStats(
                item.durability != -1 ? item.durability : type.durability,
                item.enchantability != -1 ? item.enchantability : type.enchantability,
                !Float.isNaN(item.damageBonus) ? item.damageBonus : type.damageBonus,
                !Float.isNaN(item.attackSpeed) ? item.attackSpeed : type.attackSpeed,
                !Float.isNaN(item.attackDamage) ? item.attackDamage : type.attackDamage,
                !Float.isNaN(item.attackRange) ? item.attackRange : type.attackRange,
                item.thrust != null ? item.thrust : type.thrust,
                item.throwCfg != null ? item.throwCfg : type.throwCfg);
    }

    // === 公開API ===

    public static WeaponStats getStats(ItemStack stack) {
        if (stack.isEmpty()) return null;
        ResourceLocation regName = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (regName == null) return null;
        String id = regName.toString();

        WeaponStats cached = MERGED_CACHE.get(id);
        if (cached != null) return cached == NONE_STATS ? null : cached;

        WeaponStats item = STATS.get(id);
        WeaponStats type = null;
        // types セクションの既定は WeaponTypeRegistry でタイプを引いてから当てる。
        // タイプ表がまだ読み込まれていない間に引くと type=null の結果を
        // 掴んでしまうので、 その場合はキャッシュに載せない
        // ( 載せると types の設定が永久に効かなくなる )。
        boolean typesReady = WeaponTypeRegistry.isLoaded();
        if (typesReady && !TYPE_STATS.isEmpty()) {
            WeaponTypeRegistry.WeaponTypeData td = WeaponTypeRegistry.getTypeForItem(stack);
            if (td != null) type = TYPE_STATS.get(td.getId());
        }
        WeaponStats merged = merge(item, type);
        if (typesReady) {
            MERGED_CACHE.put(id, merged == null ? NONE_STATS : merged);
        }
        return merged;
    }

    /**
     * item別×type別 のマージ結果キャッシュを捨てる。
     *
     * <p>マージ結果は {@link WeaponTypeRegistry} のタイプ表に依存しているので、
     * そちらが再読み込みされたら必ず呼ぶこと。 呼ばないと リロードの順序次第で
     * 「タイプ表が空の瞬間に引いた type=null の結果」が残り続け、
     * types セクションの設定が効かなくなる。</p>
     */
    public static void invalidateCache() {
        MERGED_CACHE.clear();
    }

    /** item別のみ ( type別既定は ItemStack が要るため補完しない )。 */
    public static WeaponStats getStats(String itemId) {
        return STATS.get(itemId);
    }

    /** 武器タイプID直指定で type別既定を取得 ( 無ければ null )。 */
    public static WeaponStats getTypeStats(String typeId) {
        return TYPE_STATS.get(typeId);
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

    /** 投擲設定 ( JSONの "throw" )。 未設定なら null。 */
    public static ThrowConfig throwConfig(ItemStack stack) {
        WeaponStats s = getStats(stack);
        return (s != null) ? s.throwCfg : null;
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

package the_four_primitives_and_weapons.damage;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 属性専用の持続ダメージ（DoT）システム。
 * Wither effectもwither DamageSourceも使わず、
 * 炎ダメージと同じtickベースで独自に直接ダメージを与える。
 *
 * 使い方:
 *   ElementalDoTHandler.apply(target, duration, dmgPerTick, ElementType.CORROSION);
 */
public class ElementalDoTHandler {

    /**
     * 持続ダメージのデータ。
     * duration と dmgPerTick はレベルに応じて呼び出し元で計算済みの値を受け取る。
     */
    private static class DoTEntry {
        int       remainingTick;
        float     dmgPerTick;
        ElementType element;

        DoTEntry(int remainingTick, float dmgPerTick, ElementType element) {
            this.remainingTick = remainingTick;
            this.dmgPerTick    = dmgPerTick;
            this.element       = element;
        }
    }

    // entityUUID → DoTEntry
    private static final Map<UUID, DoTEntry> dotMap = new ConcurrentHashMap<>();

    // ────────────────────────────────────────────────────────────────
    // 独自 DamageSource キー
    // ────────────────────────────────────────────────────────────────

    /**
     * 属性ごとの独自 DamageSource を生成する。
     * vanilla の DamageType を流用しつつ、メッセージIDで属性を区別する。
     *
     * ※ 1.20.1 では DamageType は data-driven なので、
     *   resources/data/the_four_primitives_and_weapons/damage_type/ に
     *   corrosion_dot.json / dark_dot.json を用意してください。
     *   （fallback として magic を使います）
     */
    public static DamageSource createDoTSource(LivingEntity target, ElementType element) {
        // 継続ダメージ専用の DamageType がある element ( 例: DARK → dark_dot ) なら
        // 直接ヒットと区別された tag/死亡メッセージで処理する。 それ以外は属性そのものの key を使う。
        // バニラの damageSources().magic() は使わない (解決失敗時のフォールバックだけ magic)。
        net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> dotKey =
                dotKeyFor(element);
        if (dotKey != null) {
            return ModDamageSources.of(target.level(), dotKey, null);
        }
        return ModDamageSources.ofElement(target.level(), element, null);
    }

    /** 継続ダメージ専用の DamageType key を返す。 無ければ null。 */
    private static net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType>
            dotKeyFor(ElementType element) {
        if (element == null) return null;
        switch (element) {
            case DARK:
                return the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModDamageTypes.DARK_DOT;
            case BLOOD:
                return the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModDamageTypes.BLOOD_DOT;
            default:
                return null;
        }
    }

    // ────────────────────────────────────────────────────────────────
    // 公開API
    // ────────────────────────────────────────────────────────────────

    /**
     * 持続ダメージを付与する。
     * 既存のDoTがある場合は残り時間が長い方を採用し、ダメージは加算する。
     *
     * @param target     対象エンティティ
     * @param duration   持続tick数  (例: 60 = 3秒)
     * @param dmgPerTick 1tickあたりのダメージ (例: 0.5f)
     * @param element    属性タイプ（DamageSource の識別に使用）
     */
    public static void apply(LivingEntity target, int duration,
                             float dmgPerTick, ElementType element) {
        UUID id = target.getUUID();
        DoTEntry existing = dotMap.get(id);

        if (existing != null && existing.element == element) {
            // 残り時間は長い方を優先、ダメージは加算
            existing.remainingTick = Math.max(existing.remainingTick, duration);
            existing.dmgPerTick   += dmgPerTick;
        } else {
            dotMap.put(id, new DoTEntry(duration, dmgPerTick, element));
        }
    }

    /**
     * 上限付きで持続ダメージを付与する。
     * 加算後の1tickあたりダメージを {@code maxDmgPerTick} で打ち止めにするので、
     * 刀のような連撃武器でもDoTが無限に積み上がらない。
     *
     * @param maxDmgPerTick 加算後の1tickあたりダメージの上限
     */
    public static void applyCapped(LivingEntity target, int duration,
                                   float dmgPerTick, ElementType element, float maxDmgPerTick) {
        UUID id = target.getUUID();
        DoTEntry existing = dotMap.get(id);

        if (existing != null && existing.element == element) {
            existing.remainingTick = Math.max(existing.remainingTick, duration);
            existing.dmgPerTick = Math.min(maxDmgPerTick, existing.dmgPerTick + dmgPerTick);
        } else {
            dotMap.put(id, new DoTEntry(duration, Math.min(maxDmgPerTick, dmgPerTick), element));
        }
    }

    // ── 蓄積 ( ElementDamageKind.BUILDUP ) 用 ─────────────────────

    /** 蓄積の基礎持続tick ( 3秒 )。 */
    private static final int   BUILDUP_BASE_DURATION      = 60;
    /** 属性レベル1ごとの持続延長 ( +0.5秒/Lv )。 */
    private static final int   BUILDUP_DURATION_PER_LEVEL = 10;
    /** 蓄積の持続上限 ( 10秒 )。 */
    private static final int   BUILDUP_DURATION_MAX       = 200;
    /** 一発分の属性ダメージのうち、1tickに乗せる割合。 */
    private static final float BUILDUP_TICK_RATIO         = 0.10f;
    /** 連撃 ( 刀など ) で無限に積み上がらないための 1tick あたり上限。 */
    private static final float BUILDUP_DAMAGE_CAP         = 3.0f;

    /**
     * {@link ElementDamageKind#BUILDUP} の属性ダメージを、時間をかけて削る DoT に変換する。
     *
     * <p>一発分として計算済みの「属性が足した分」を受け取り、
     * 衰弱のようにじわじわ入る形へ置き換える。持続はレベルで伸びる。</p>
     *
     * @param elementBonus 属性が足した分 ( 一発で与えるはずだったダメージ )
     * @param element      属性タイプ ( DamageSource の識別に使用 )
     * @param level        属性レベル
     */
    public static void applyBuildup(LivingEntity target, float elementBonus,
                                    ElementType element, int level) {
        if (target == null || elementBonus <= 0.0F) return;
        int duration = Math.min(BUILDUP_DURATION_MAX,
                BUILDUP_BASE_DURATION + BUILDUP_DURATION_PER_LEVEL * Math.max(0, level - 1));

        // 闇や血のように属性側が独自の DoT を掛けている場合、
        // 蓄積の上限で既存の DoT を切り下げてしまわないよう上限を引き上げる。
        float cap = BUILDUP_DAMAGE_CAP;
        DoTEntry existing = dotMap.get(target.getUUID());
        if (existing != null && existing.element == element) {
            cap = Math.max(cap, existing.dmgPerTick);
        }

        applyCapped(target, duration, elementBonus * BUILDUP_TICK_RATIO, element, cap);
    }

    /**
     * 対象エンティティのDoTを即時解除する。
     */
    public static void clear(LivingEntity target) {
        dotMap.remove(target.getUUID());
    }

    /**
     * 対象がDoT中かどうかを返す。
     */
    public static boolean isActive(LivingEntity target) {
        return dotMap.containsKey(target.getUUID());
    }

    /**
     * 対象が指定属性のDoT中かどうかを返す。
     */
    public static boolean isActive(LivingEntity target, ElementType element) {
        DoTEntry entry = dotMap.get(target.getUUID());
        return entry != null && entry.element == element;
    }

    /** 対象にかかっているDoTの属性。 かかっていなければ {@link ElementType#NONE}。 */
    public static ElementType getActiveElement(LivingEntity target) {
        if (target == null) return ElementType.NONE;
        DoTEntry entry = dotMap.get(target.getUUID());
        return entry != null ? entry.element : ElementType.NONE;
    }

    /** 対象にかかっているDoTの1tickあたりダメージ。 かかっていなければ 0。 */
    public static float getDamagePerTick(LivingEntity target) {
        if (target == null) return 0.0f;
        DoTEntry entry = dotMap.get(target.getUUID());
        return entry != null ? entry.dmgPerTick : 0.0f;
    }

    /** 対象にかかっているDoTの残りtick。 かかっていなければ 0。 */
    public static int getRemainingTick(LivingEntity target) {
        if (target == null) return 0;
        DoTEntry entry = dotMap.get(target.getUUID());
        return entry != null ? entry.remainingTick : 0;
    }

    // ────────────────────────────────────────────────────────────────
    // TickHandler
    // ────────────────────────────────────────────────────────────────

    @Mod.EventBusSubscriber(modid = "the_four_primitives_and_weapons")
    public static class DoTTickHandler {

        @SubscribeEvent
        public static void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            if (event.getServer() == null) return;

            dotMap.entrySet().removeIf(entry -> {
                DoTEntry dot = entry.getValue();

                // tick消費
                dot.remainingTick--;
                if (dot.remainingTick <= 0) return true; // 終了 → 削除

                // エンティティを検索
                LivingEntity target = null;
                for (ServerLevel level : event.getServer().getAllLevels()) {
                    net.minecraft.world.entity.Entity e = level.getEntity(entry.getKey());
                    if (e instanceof LivingEntity living) {
                        target = living;
                        break;
                    }
                }
                if (target == null || !target.isAlive()) return true;

                // 独自 DamageSource で直接ダメージ
                DamageSource source = createDoTSource(target, dot.element);
                target.hurt(source, dot.dmgPerTick);

                return false;
            });
        }
    }
}

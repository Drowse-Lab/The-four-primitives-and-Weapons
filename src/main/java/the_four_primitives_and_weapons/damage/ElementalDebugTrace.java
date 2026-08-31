package the_four_primitives_and_weapons.damage;

import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 「この一撃で属性が実際に何ダメージ足したか」をデバッグ表示用に記録する。
 *
 * <p>デバッグMobの表示はもともと {@code 実ダメージ − 入力} を「属性分」として出していたが、
 * この差には属性以外の補正 ( 熟練度のチャージ不足ペナルティ、特性による軽減、防具 … ) が
 * 全部混ざるため、属性が加算されていてもマイナスで表示されることがあった。</p>
 *
 * <p>そこで属性処理そのもの ( {@link the_four_primitives_and_weapons.ElementalDamageEvent} と
 * {@code LivingEntityDamageMixin} ) が算出した増減をここに記録し、デバッグMob側はその値を読む。
 * 記録対象はデバッグMobだけなので、通常の戦闘では何も溜まらない。</p>
 */
public final class ElementalDebugTrace {

    /** 1 tick 分の記録。 同じ tick 中の複数経路 ( mixin + event ) は加算する。 */
    public static final class Entry {
        public final ElementType type;
        public final int level;
        /** 属性ダメージの与え方 ( 物理 / 魔法 / 蓄積 )。 */
        public final ElementDamageKind kind;
        public float delta;
        public long gameTime;

        Entry(ElementType type, int level, ElementDamageKind kind, float delta, long gameTime) {
            this.type = type;
            this.level = level;
            this.kind = kind;
            this.delta = delta;
            this.gameTime = gameTime;
        }
    }

    private static final Map<UUID, Entry> ENTRIES = new ConcurrentHashMap<>();

    /**
     * アドオンが追加した計測対象の判定。
     * 他MODのターゲットダミー等を、属性の内訳を記録する対象にできる。
     */
    private static final java.util.List<java.util.function.Predicate<LivingEntity>> EXTRA_PROBES =
            new java.util.concurrent.CopyOnWriteArrayList<>();

    private ElementalDebugTrace() {}

    /**
     * 計測対象を追加する ( アドオン向け )。
     *
     * <p>デフォルトではデバッグMobとターゲットダミーだけが記録対象だが、
     * ここに判定を足すと他MODのエンティティでも属性の内訳を取れるようになる。
     * 通常の戦闘で常時 true を返すような判定を登録しないこと ( 記録が溜まり続ける )。</p>
     *
     * <p>登録は {@code FMLCommonSetupEvent} 等の初期化時に 1 回だけ行う。</p>
     */
    public static void addProbe(java.util.function.Predicate<LivingEntity> predicate) {
        if (predicate != null) EXTRA_PROBES.add(predicate);
    }

    /**
     * 属性による増減を記録する ( デバッグMob以外は無視 )。
     *
     * @param delta 属性処理の前後の差 ( 加算なら正 )
     */
    public static void record(LivingEntity target, ElementType type, int level, float delta) {
        record(target, type, level, ElementDamageKind.PHYSICAL, delta);
    }

    /**
     * 属性による増減を、与え方 ( 物理 / 魔法 / 蓄積 ) 付きで記録する。
     */
    public static void record(LivingEntity target, ElementType type, int level,
                              ElementDamageKind kind, float delta) {
        if (target == null || type == null || type == ElementType.NONE) return;
        if (!isProbe(target)) return;

        long now = target.level().getGameTime();
        Entry existing = ENTRIES.get(target.getUUID());
        if (existing != null && existing.gameTime == now && existing.type == type) {
            existing.delta += delta;   // 同じ一撃で mixin と event の両方が動いた場合
            return;
        }
        ENTRIES.put(target.getUUID(), new Entry(type, level,
                kind != null ? kind : ElementDamageKind.PHYSICAL, delta, now));
    }

    /** 記録対象 ( 計測用のMob ) かどうか。 通常の戦闘では何も溜めない。 */
    private static boolean isProbe(LivingEntity target) {
        if (target instanceof the_four_primitives_and_weapons.entity.DebugMobEntity
                || target instanceof the_four_primitives_and_weapons.entity.TargetDummyEntity) {
            return true;
        }
        // アドオンが登録した判定 ( 他MODのダミー等 )。 1 つでも壊れていても本体は止めない。
        for (java.util.function.Predicate<LivingEntity> probe : EXTRA_PROBES) {
            try {
                if (probe.test(target)) return true;
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    /** この tick に記録された増減を取り出して消す。 無ければ null。 */
    public static Entry consume(LivingEntity target) {
        if (target == null) return null;
        Entry entry = ENTRIES.remove(target.getUUID());
        if (entry == null) return null;
        return entry.gameTime == target.level().getGameTime() ? entry : null;
    }
}

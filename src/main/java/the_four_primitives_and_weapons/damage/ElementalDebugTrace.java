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
        public float delta;
        public long gameTime;

        Entry(ElementType type, int level, float delta, long gameTime) {
            this.type = type;
            this.level = level;
            this.delta = delta;
            this.gameTime = gameTime;
        }
    }

    private static final Map<UUID, Entry> ENTRIES = new ConcurrentHashMap<>();

    private ElementalDebugTrace() {}

    /**
     * 属性による増減を記録する ( デバッグMob以外は無視 )。
     *
     * @param delta 属性処理の前後の差 ( 加算なら正 )
     */
    public static void record(LivingEntity target, ElementType type, int level, float delta) {
        if (target == null || type == null || type == ElementType.NONE) return;
        if (!(target instanceof the_four_primitives_and_weapons.entity.DebugMobEntity)) return;

        long now = target.level().getGameTime();
        Entry existing = ENTRIES.get(target.getUUID());
        if (existing != null && existing.gameTime == now && existing.type == type) {
            existing.delta += delta;   // 同じ一撃で mixin と event の両方が動いた場合
            return;
        }
        ENTRIES.put(target.getUUID(), new Entry(type, level, delta, now));
    }

    /** この tick に記録された増減を取り出して消す。 無ければ null。 */
    public static Entry consume(LivingEntity target) {
        if (target == null) return null;
        Entry entry = ENTRIES.remove(target.getUUID());
        if (entry == null) return null;
        return entry.gameTime == target.level().getGameTime() ? entry : null;
    }
}

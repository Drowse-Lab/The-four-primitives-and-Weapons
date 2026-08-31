package the_four_primitives_and_weapons.damage;

/**
 * ターゲットダミー 1 体分のダメージ計測。
 *
 * <p>「実際に HP が減った量」と「属性が足した分」を分けて集計する。
 * 蓄積 ( {@link ElementDamageKind#BUILDUP} ) は一発では入らず DoT として後から入るので、
 * 内訳の蓄積は<b>予定値</b>、実際に入った分は DoT 側に計上される。</p>
 *
 * <p>最後のヒットから {@link #SESSION_IDLE_TICKS} 以上空くと次のヒットで自動的に
 * 新しい計測セッションを始めるので、殴り直すたびに DPS が取り直せる。</p>
 */
public class DummyDamageStats {

    /** これ以上ヒットが空いたら次のヒットで計測をやり直す ( 5秒 )。 */
    public static final int SESSION_IDLE_TICKS = 100;

    // ── 実ダメージ ────────────────────────────────────────────────
    private long  firstHitTime = -1;
    private long  lastHitTime  = -1;
    private float total;
    private int   hits;
    private float maxHit;
    private float minHit;

    // ── 属性が足した分の内訳 ──────────────────────────────────────
    private final float[] byKind    = new float[ElementDamageKind.values().length];
    private final float[] byElement = new float[ElementType.values().length];

    // ── DoT ( 蓄積 / 出血 / 闇の継続 ) ────────────────────────────
    private float dotTotal;
    private int   dotTicks;
    private final float[] dotByElement = new float[ElementType.values().length];

    /**
     * 直接の一撃を記録する。
     *
     * @param actualDamage 実際に HP が減った量 ( 防具軽減・魔法貫通の反映後 )
     * @param trace        属性処理が記録した増減 ( 属性が乗らなかった一撃なら null )
     */
    public void recordHit(long gameTime, float actualDamage, ElementalDebugTrace.Entry trace) {
        beginSessionIfIdle(gameTime);

        if (hits == 0) {
            maxHit = actualDamage;
            minHit = actualDamage;
        } else {
            maxHit = Math.max(maxHit, actualDamage);
            minHit = Math.min(minHit, actualDamage);
        }
        total += actualDamage;
        hits++;
        lastHitTime = gameTime;

        if (trace != null && trace.type != ElementType.NONE) {
            byKind[trace.kind.ordinal()]    += trace.delta;
            byElement[trace.type.ordinal()] += trace.delta;
        }
    }

    /**
     * DoT ( 蓄積・出血・闇の継続 ) の 1tick 分を記録する。
     * ヒット数には数えないが、総ダメージと計測ウィンドウには含める。
     */
    public void recordDot(long gameTime, ElementType element, float damage) {
        if (damage <= 0.0f) return;
        beginSessionIfIdle(gameTime);

        total    += damage;
        dotTotal += damage;
        dotTicks++;
        lastHitTime = gameTime;
        if (element != null) {
            dotByElement[element.ordinal()] += damage;
        }
    }

    private void beginSessionIfIdle(long gameTime) {
        if (lastHitTime >= 0 && gameTime - lastHitTime > SESSION_IDLE_TICKS) {
            reset();
        }
        if (firstHitTime < 0) {
            firstHitTime = gameTime;
        }
    }

    public void reset() {
        firstHitTime = -1;
        lastHitTime  = -1;
        total = 0.0f;
        hits = 0;
        maxHit = 0.0f;
        minHit = 0.0f;
        dotTotal = 0.0f;
        dotTicks = 0;
        java.util.Arrays.fill(byKind, 0.0f);
        java.util.Arrays.fill(byElement, 0.0f);
        java.util.Arrays.fill(dotByElement, 0.0f);
    }

    public boolean isEmpty() {
        return hits == 0 && dotTicks == 0;
    }

    public float getTotal()      { return total; }
    public int   getHits()       { return hits; }
    public float getMaxHit()     { return maxHit; }
    public float getMinHit()     { return minHit; }
    public float getDotTotal()   { return dotTotal; }
    public int   getDotTicks()   { return dotTicks; }
    public long  getLastHitTime(){ return lastHitTime; }

    public float getAverageHit() {
        return hits > 0 ? total / hits : 0.0f;
    }

    public float getByKind(ElementDamageKind kind) {
        return byKind[kind.ordinal()];
    }

    public float getByElement(ElementType element) {
        return byElement[element.ordinal()];
    }

    public float getDotByElement(ElementType element) {
        return dotByElement[element.ordinal()];
    }

    /** 計測開始からの経過秒。 */
    public float getElapsedSeconds() {
        if (firstHitTime < 0 || lastHitTime < 0) return 0.0f;
        return (lastHitTime - firstHitTime) / 20.0f;
    }

    /**
     * 秒あたりダメージ。 初弾から最後の一撃までを計測区間とする。
     * 1 発しか当たっていない ( 区間 0 ) 間は算出できないので負値を返す。
     */
    public float getDps() {
        float seconds = getElapsedSeconds();
        if (seconds <= 0.0f) return -1.0f;
        return total / seconds;
    }
}

package minecraftarmorweapon.item;

import minecraftarmorweapon.ai.lisp.LispInterpreter;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Gate / ConvergentGate / GateProjectile の数値パラメータを Lisp スクリプトから取得するブリッジ。
 *
 * 起動時に一度だけ {@code data/minecraft_armor_weapon/gate/formula.lisp} を読み込み、
 * {@link LispInterpreter} で評価してグローバル変数を登録する。以降は
 * {@link #getInt(String, int)} / {@link #getDouble(String, double)} で
 * 値を取り出すだけなので毎回のパース/評価コストは発生しない。
 *
 * 構造は {@link KnifeLauncherFormula} と対称。lisp ファイルが存在しない / 評価に失敗した
 * 場合はデフォルト値にフォールバック。
 */
public final class GateFormula {

    private static final ResourceLocation FORMULA_PATH =
        new ResourceLocation("minecraft_armor_weapon", "gate/formula.lisp");

    private static final LispInterpreter INTERPRETER = new LispInterpreter();
    private static boolean loaded = false;

    private GateFormula() {}

    /**
     * ResourceManager (データパック再読み込みで呼ばれる) 経由で
     * formula.lisp を読み込む。LispInterpreter の globals に define を登録。
     */
    public static void reload(ResourceManager rm) {
        loaded = false;
        try {
            Optional<Resource> res = rm.getResource(FORMULA_PATH);
            if (res.isEmpty()) return;
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(res.get().open(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line).append('\n');
                String wrapped = "(seq " + sb + ")";
                Object ast = INTERPRETER.parse(wrapped);
                if (ast != null) {
                    INTERPRETER.eval(ast);
                    loaded = true;
                }
            }
        } catch (Throwable t) {
            loaded = false;
        }
    }

    /** 起動時にクラスロード直読みで 1 回だけ試行 (データパック未導入でも default が動く) */
    static {
        try {
            java.io.InputStream in = GateFormula.class.getClassLoader()
                .getResourceAsStream("data/minecraft_armor_weapon/gate/formula.lisp");
            if (in != null) {
                StringBuilder sb = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line).append('\n');
                }
                Object ast = INTERPRETER.parse("(seq " + sb + ")");
                if (ast != null) {
                    INTERPRETER.eval(ast);
                    loaded = true;
                }
            }
        } catch (Throwable ignored) {}
    }

    // --- 基本アクセサ -------------------------------------------------

    public static int getInt(String symbol, int fallback) {
        if (!loaded) return fallback;
        Object v = INTERPRETER.eval(new LispInterpreter.Symbol(symbol));
        if (v instanceof Number n) return n.intValue();
        return fallback;
    }

    public static double getDouble(String symbol, double fallback) {
        if (!loaded) return fallback;
        Object v = INTERPRETER.eval(new LispInterpreter.Symbol(symbol));
        if (v instanceof Number n) return n.doubleValue();
        return fallback;
    }

    // --- 高レベル API: GateItem --------------------------------------
    public static int    gateProjectileCount() { return Math.max(1, getInt("gate-projectile-count", 3)); }
    public static double gateSideSpread()       { return getDouble("gate-side-spread",       3.5); }
    public static double gateForwardOffset()    { return getDouble("gate-forward-offset",   -2.0); }
    public static double gateVerticalOffset()   { return getDouble("gate-vertical-offset",   0.5); }
    public static double gateShootVelocity()    { return getDouble("gate-shoot-velocity",    2.0); }
    public static int    gateCooldown()         { return getInt   ("gate-cooldown",          20); }
    public static int    gateSoundReps()        { return Math.max(1, getInt("gate-sound-reps", 4)); }
    public static int    gateResistAmp()        { return getInt   ("gate-resist-amp",        4); }
    public static int    gateResistDur()        { return getInt   ("gate-resist-dur",        20); }

    // --- 高レベル API: ConvergentGateItem ----------------------------
    public static double convergeDistance()         { return getDouble("converge-distance",         20.0); }
    public static double convergeSpread()           { return getDouble("converge-spread",            5.0); }
    public static int    convergeProjectileCount() { return Math.max(2, getInt("converge-projectile-count", 5)); }
    public static double convergeShootVelocity()    { return getDouble("converge-shoot-velocity",    2.0); }
    public static int    convergeCooldown()         { return getInt   ("converge-cooldown",          20); }

    // --- 高レベル API: GateProjectileEntity --------------------------
    public static int    projLifetime()    { return getInt   ("gate-proj-lifetime",             100); }
    public static float  projEndRadius()   { return (float) getDouble("gate-proj-end-radius",    1.5); }
    public static float  projHitDamage()   { return (float) getDouble("gate-proj-hit-damage",    15.0); }
    public static float  projHitRadius()   { return (float) getDouble("gate-proj-hit-radius",     2.0); }
    public static int    projParticlesPerTick() { return Math.max(0, getInt("gate-proj-particles-per-tick", 3)); }
    public static float  projGravity()     { return (float) getDouble("gate-proj-gravity",        0.0); }
}

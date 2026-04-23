package minecraftarmorweapon.item;

import minecraftarmorweapon.ai.lisp.LispInterpreter;
import minecraftarmorweapon.entity.ThrowingKnifeEntity.KnifeType;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * KnifeLauncher の数値パラメータを Lisp スクリプトから取得するブリッジ。
 *
 * 起動時に一度だけ {@code data/minecraft_armor_weapon/knife_launcher/formula.lisp} を読み込み、
 * {@link LispInterpreter} で評価してグローバル変数を登録する。以降は
 * {@link #getInt(String, int)} / {@link #getDouble(String, double)} で
 * 値を取り出すだけなので毎回のパース/評価コストは発生しない。
 *
 * formula.lisp が存在しない / 評価に失敗した場合はデフォルト値にフォールバック。
 */
public final class KnifeLauncherFormula {

    private static final ResourceLocation FORMULA_PATH =
        new ResourceLocation("minecraft_armor_weapon", "knife_launcher/formula.lisp");

    private static final LispInterpreter INTERPRETER = new LispInterpreter();
    private static boolean loaded = false;

    private KnifeLauncherFormula() {}

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
                // ファイル全体を (seq ...) で包んで一度にパース/評価する
                String wrapped = "(seq " + sb + ")";
                Object ast = INTERPRETER.parse(wrapped);
                if (ast != null) {
                    INTERPRETER.eval(ast);
                    loaded = true;
                }
            }
        } catch (Throwable t) {
            // 読み込み失敗時はデフォルトへフォールバック
            loaded = false;
        }
    }

    /** formula.lisp 読み込みなしでもクラスロード時に 1 回だけ試行する (リソース直読み) */
    static {
        try {
            java.io.InputStream in = KnifeLauncherFormula.class.getClassLoader()
                .getResourceAsStream("data/minecraft_armor_weapon/knife_launcher/formula.lisp");
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
        Object v = INTERPRETER.eval(new minecraftarmorweapon.ai.lisp.LispInterpreter.Symbol(symbol));
        if (v instanceof Number n) return n.intValue();
        return fallback;
    }

    public static double getDouble(String symbol, double fallback) {
        if (!loaded) return fallback;
        Object v = INTERPRETER.eval(new minecraftarmorweapon.ai.lisp.LispInterpreter.Symbol(symbol));
        if (v instanceof Number n) return n.doubleValue();
        return fallback;
    }

    // --- 高レベル API (モード別値) -----------------------------------

    public static int maxCountFor(KnifeType mode) {
        return switch (mode) {
            case STUN   -> getInt("max-stun",   3);
            case SCREW  -> getInt("max-screw",  5);
            case HOMING -> getInt("max-homing", 3);
            default     -> getInt("max-normal", 10);
        };
    }

    public static int cooldownFor(KnifeType mode) {
        return switch (mode) {
            case STUN   -> getInt("cooldown-stun",   16);
            case SCREW  -> getInt("cooldown-screw",  10);
            case HOMING -> getInt("cooldown-homing", 12);
            default     -> getInt("cooldown-normal", 8);
        };
    }

    public static double manaCostFor(KnifeType mode) {
        return switch (mode) {
            case STUN   -> getDouble("mana-stun",   25.0);
            case SCREW  -> getDouble("mana-screw",  10.0);
            case HOMING -> getDouble("mana-homing", 30.0);
            default     -> getDouble("mana-normal", 0.0);
        };
    }

    public static float spawnOffsetH() { return (float) getDouble("spawn-offset-h", 1.2); }
    public static float spawnOffsetV() { return (float) getDouble("spawn-offset-v", 0.6); }
    public static float shootVelocity() { return (float) getDouble("shoot-velocity", 1.6); }
}

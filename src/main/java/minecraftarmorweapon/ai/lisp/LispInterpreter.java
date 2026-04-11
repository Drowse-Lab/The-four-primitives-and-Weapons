package minecraftarmorweapon.ai.lisp;

import java.util.*;
import java.util.function.Function;

/**
 * Mob AI行動木用の軽量Lisp S式インタプリタ。
 *
 * サポートする構文:
 *   (if condition then-expr else-expr)
 *   (seq expr1 expr2 ...)          ;; 順次実行
 *   (and expr1 expr2 ...)          ;; 全てtrueなら最後の値
 *   (or  expr1 expr2 ...)          ;; 最初のtrueの値
 *   (not expr)
 *   (< a b)  (> a b)  (<= a b)  (>= a b)  (= a b)
 *   (+ a b)  (- a b)  (* a b)  (/ a b)
 *   (random min max)               ;; min〜maxの乱数
 *   (define name expr)             ;; 変数定義
 *   シンボル / 数値リテラル / 文字列リテラル
 *
 * 行動プリミティブは外部から関数として登録する。
 */
public class LispInterpreter {

    private final Map<String, Object> globals = new HashMap<>();
    private final Map<String, Function<List<Object>, Object>> builtins = new HashMap<>();
    private final Random random = new Random();

    public LispInterpreter() {
        registerBuiltins();
    }

    // === パーサー ===

    /**
     * S式文字列をパースしてASTに変換する。
     */
    public Object parse(String source) {
        List<String> tokens = tokenize(source);
        if (tokens.isEmpty()) return null;
        int[] pos = {0};
        return readForm(tokens, pos);
    }

    private List<String> tokenize(String source) {
        List<String> tokens = new ArrayList<>();
        int i = 0;
        while (i < source.length()) {
            char c = source.charAt(i);

            // 空白スキップ
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }

            // コメント（;から行末まで）
            if (c == ';') {
                while (i < source.length() && source.charAt(i) != '\n') i++;
                continue;
            }

            // 括弧
            if (c == '(' || c == ')') {
                tokens.add(String.valueOf(c));
                i++;
                continue;
            }

            // 文字列リテラル
            if (c == '"') {
                StringBuilder sb = new StringBuilder();
                i++; // 開き引用符をスキップ
                while (i < source.length() && source.charAt(i) != '"') {
                    if (source.charAt(i) == '\\' && i + 1 < source.length()) {
                        i++;
                    }
                    sb.append(source.charAt(i));
                    i++;
                }
                i++; // 閉じ引用符をスキップ
                tokens.add("\"" + sb.toString() + "\"");
                continue;
            }

            // アトム（シンボル/数値）
            StringBuilder sb = new StringBuilder();
            while (i < source.length() && !Character.isWhitespace(source.charAt(i))
                   && source.charAt(i) != '(' && source.charAt(i) != ')') {
                sb.append(source.charAt(i));
                i++;
            }
            tokens.add(sb.toString());
        }
        return tokens;
    }

    private Object readForm(List<String> tokens, int[] pos) {
        if (pos[0] >= tokens.size()) return null;

        String token = tokens.get(pos[0]);

        if ("(".equals(token)) {
            pos[0]++;
            List<Object> list = new ArrayList<>();
            while (pos[0] < tokens.size() && !")".equals(tokens.get(pos[0]))) {
                list.add(readForm(tokens, pos));
            }
            pos[0]++; // 閉じ括弧をスキップ
            return list;
        } else if (")".equals(token)) {
            pos[0]++;
            return null;
        } else {
            pos[0]++;
            return parseAtom(token);
        }
    }

    private Object parseAtom(String token) {
        // 文字列リテラル
        if (token.startsWith("\"") && token.endsWith("\"")) {
            return token.substring(1, token.length() - 1);
        }

        // 数値
        try {
            if (token.contains(".")) {
                return Double.parseDouble(token);
            }
            return Integer.parseInt(token);
        } catch (NumberFormatException e) {
            // シンボル
        }

        // ブーリアン
        if ("true".equals(token)) return true;
        if ("false".equals(token)) return false;
        if ("nil".equals(token)) return null;

        return new Symbol(token);
    }

    // === 評価器 ===

    /**
     * ASTを環境内で評価する。
     */
    public Object eval(Object ast) {
        return eval(ast, globals);
    }

    public Object eval(Object ast, Map<String, Object> env) {
        if (ast == null) return null;

        // アトム
        if (ast instanceof Symbol sym) {
            String name = sym.name;
            if (env.containsKey(name)) return env.get(name);
            if (globals.containsKey(name)) return globals.get(name);
            return sym; // 未定義シンボルはそのまま返す（アクション名として使う）
        }
        if (ast instanceof Integer || ast instanceof Double || ast instanceof String || ast instanceof Boolean) {
            return ast;
        }

        // リスト（関数呼び出し）
        if (ast instanceof List<?> list) {
            if (list.isEmpty()) return null;

            Object head = list.get(0);
            String op = head instanceof Symbol ? ((Symbol) head).name : head.toString();

            // 特殊形式
            switch (op) {
                case "if":
                    return evalIf(list, env);
                case "seq":
                    return evalSeq(list, env);
                case "and":
                    return evalAnd(list, env);
                case "or":
                    return evalOr(list, env);
                case "not":
                    return !isTruthy(eval(list.get(1), env));
                case "define":
                    return evalDefine(list, env);
                case "let":
                    return evalLet(list, env);
                case "random":
                    return evalRandom(list, env);
                default:
                    // ビルトイン関数
                    if (builtins.containsKey(op)) {
                        List<Object> args = new ArrayList<>();
                        for (int i = 1; i < list.size(); i++) {
                            args.add(eval(list.get(i), env));
                        }
                        return builtins.get(op).apply(args);
                    }
                    // 未知の関数 → アクション名としてリストを返す
                    List<Object> evaledArgs = new ArrayList<>();
                    evaledArgs.add(op);
                    for (int i = 1; i < list.size(); i++) {
                        evaledArgs.add(eval(list.get(i), env));
                    }
                    return evaledArgs;
            }
        }

        return ast;
    }

    private Object evalIf(List<?> list, Map<String, Object> env) {
        Object cond = eval(list.get(1), env);
        if (isTruthy(cond)) {
            return eval(list.get(2), env);
        } else if (list.size() > 3) {
            return eval(list.get(3), env);
        }
        return null;
    }

    private Object evalSeq(List<?> list, Map<String, Object> env) {
        Object result = null;
        for (int i = 1; i < list.size(); i++) {
            result = eval(list.get(i), env);
        }
        return result;
    }

    private Object evalAnd(List<?> list, Map<String, Object> env) {
        Object result = true;
        for (int i = 1; i < list.size(); i++) {
            result = eval(list.get(i), env);
            if (!isTruthy(result)) return false;
        }
        return result;
    }

    private Object evalOr(List<?> list, Map<String, Object> env) {
        for (int i = 1; i < list.size(); i++) {
            Object result = eval(list.get(i), env);
            if (isTruthy(result)) return result;
        }
        return false;
    }

    private Object evalDefine(List<?> list, Map<String, Object> env) {
        String name = ((Symbol) list.get(1)).name;
        Object value = eval(list.get(2), env);
        env.put(name, value);
        return value;
    }

    private Object evalLet(List<?> list, Map<String, Object> env) {
        Map<String, Object> localEnv = new HashMap<>(env);
        List<?> bindings = (List<?>) list.get(1);
        for (int i = 0; i < bindings.size(); i += 2) {
            String name = ((Symbol) bindings.get(i)).name;
            Object value = eval(bindings.get(i + 1), localEnv);
            localEnv.put(name, value);
        }
        Object result = null;
        for (int i = 2; i < list.size(); i++) {
            result = eval(list.get(i), localEnv);
        }
        return result;
    }

    private Object evalRandom(List<?> list, Map<String, Object> env) {
        double min = toDouble(eval(list.get(1), env));
        double max = toDouble(eval(list.get(2), env));
        return min + random.nextDouble() * (max - min);
    }

    // === ビルトイン登録 ===

    private void registerBuiltins() {
        // 比較演算子
        builtins.put("<", args -> toDouble(args.get(0)) < toDouble(args.get(1)));
        builtins.put(">", args -> toDouble(args.get(0)) > toDouble(args.get(1)));
        builtins.put("<=", args -> toDouble(args.get(0)) <= toDouble(args.get(1)));
        builtins.put(">=", args -> toDouble(args.get(0)) >= toDouble(args.get(1)));
        builtins.put("=", args -> Math.abs(toDouble(args.get(0)) - toDouble(args.get(1))) < 0.001);

        // 算術演算子
        builtins.put("+", args -> toDouble(args.get(0)) + toDouble(args.get(1)));
        builtins.put("-", args -> toDouble(args.get(0)) - toDouble(args.get(1)));
        builtins.put("*", args -> toDouble(args.get(0)) * toDouble(args.get(1)));
        builtins.put("/", args -> {
            double divisor = toDouble(args.get(1));
            return divisor == 0 ? 0.0 : toDouble(args.get(0)) / divisor;
        });
        builtins.put("min", args -> Math.min(toDouble(args.get(0)), toDouble(args.get(1))));
        builtins.put("max", args -> Math.max(toDouble(args.get(0)), toDouble(args.get(1))));
        builtins.put("abs", args -> Math.abs(toDouble(args.get(0))));
    }

    /**
     * 外部から関数を登録する（AIプリミティブ用）。
     */
    public void registerFunction(String name, Function<List<Object>, Object> fn) {
        builtins.put(name, fn);
    }

    /**
     * 変数を設定する（センサーデータ注入用）。
     */
    public void setVariable(String name, Object value) {
        globals.put(name, value);
    }

    // === ユーティリティ ===

    public static boolean isTruthy(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean b) return b;
        if (value instanceof Number n) return n.doubleValue() != 0;
        if (value instanceof String s) return !s.isEmpty();
        return true;
    }

    public static double toDouble(Object value) {
        if (value instanceof Number n) return n.doubleValue();
        if (value instanceof Boolean b) return b ? 1.0 : 0.0;
        if (value instanceof String s) {
            try { return Double.parseDouble(s); } catch (NumberFormatException e) { return 0; }
        }
        return 0;
    }

    public static int toInt(Object value) {
        return (int) toDouble(value);
    }

    /**
     * シンボル（変数名/関数名）
     */
    public static class Symbol {
        public final String name;

        public Symbol(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Symbol s && name.equals(s.name);
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }
    }

    // === S式の文字列変換（ゲノム保存用） ===

    /**
     * ASTをS式文字列に変換する。
     */
    public static String toSExpression(Object ast) {
        if (ast == null) return "nil";
        if (ast instanceof Symbol s) return s.name;
        if (ast instanceof String s) return "\"" + s + "\"";
        if (ast instanceof Integer || ast instanceof Double || ast instanceof Boolean) return ast.toString();
        if (ast instanceof List<?> list) {
            StringBuilder sb = new StringBuilder("(");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(" ");
                sb.append(toSExpression(list.get(i)));
            }
            sb.append(")");
            return sb.toString();
        }
        return ast.toString();
    }
}

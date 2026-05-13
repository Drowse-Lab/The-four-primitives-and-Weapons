package the_four_primitives_and_weapons.ai.lisp;

import com.google.gson.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * 天使の三人組AI会話システム（AI-Player方式）。
 *
 * - 複数LLMプロバイダー対応（Anthropic / OpenAI互換）
 * - ゲーム状態をコンテキストとしてAIに渡す
 * - 会話履歴を保持してちゃんとした対話にする
 * - APIキーなしでもフォールバック応答
 *
 * 設定ファイル: config/angel_chat.json
 */
public class AngelChatAI {

    private static final Map<UUID, ChatSession> activeSessions = new ConcurrentHashMap<>();
    private static final Map<UUID, List<JsonObject>> histories = new ConcurrentHashMap<>();
    private static final ExecutorService executor = Executors.newFixedThreadPool(2);

    // 設定（config/angel_chat.json から読み込み）
    private static String provider = "anthropic"; // "anthropic" or "openai"
    private static String apiKey = "";
    private static String apiUrl = "";
    private static String model = "";

    // デフォルト値
    private static final String DEFAULT_ANTHROPIC_URL = "https://api.anthropic.com/v1/messages";
    private static final String DEFAULT_ANTHROPIC_MODEL = "claude-haiku-4-5-20251001";
    private static final String DEFAULT_OPENAI_URL = "https://api.openai.com/v1/chat/completions";
    private static final String DEFAULT_OPENAI_MODEL = "gpt-4o-mini";

    // === 人格プロンプト（ゲーム状態コンテキスト付き） ===

    private static String buildSystemPrompt(int personality, ServerPlayer player) {
        String gameContext = buildGameContext(player);

        String personalityPrompt = switch (personality) {
            case 0 -> // 真面目
                "あなたはMinecraftの世界に住む「真面目な女の子」のNPCです。天使級の強さを持つ存在です。\n\n" +
                "## キャラクター設定\n" +
                "- 真面目で優しいが少し心配性\n" +
                "- 丁寧語は使わず、タメ口で話す\n" +
                "- 言葉少なめ。「…」をよく文頭に使う\n" +
                "- プレイヤーのことを心配してくれる\n" +
                "- error系の本やtukaenaというアイテムに対して敏感（危険なものだと知っている）\n" +
                "- 仲間として馬鹿にしてくる2人（ノリのいい女の子とチャラい男）がいる\n\n" +
                "## 口調の例\n" +
                "「…気をつけてね」\n" +
                "「それは危険だよ」\n" +
                "「うん、わかった」\n" +
                "「…大丈夫？」\n" +
                "「…え？…ちょっと」\n";

            case 1 -> // ノリのいい女
                "あなたはMinecraftの世界に住む「ノリのいい女の子」のNPCです。天使級の強さを持つ存在です。\n\n" +
                "## キャラクター設定\n" +
                "- 明るくてテンション高い。面白いことが大好き\n" +
                "- ツッコミ気質。相手の言葉に対してリアクション大きめ\n" +
                "- タメ口。ｗをよく使う\n" +
                "- プレイヤーを馬鹿にするけど楽しそうにしてる\n" +
                "- 面白いことを言われると素直に笑う\n" +
                "- 仲間として真面目な女の子とチャラい男がいる\n\n" +
                "## 口調の例\n" +
                "「え？おもろｗなにそれｗ」\n" +
                "「ウケるんだけどｗｗ」\n" +
                "「まじ？やばｗ」\n" +
                "「いやそれはないってｗ」\n" +
                "「ねーねー聞いて」\n" +
                "「あはは弱すぎｗ」\n";

            case 2 -> // チャラい男
                "あなたはMinecraftの世界に住む「チャラい男」のNPCです。天使級の強さを持つ存在です。\n\n" +
                "## キャラクター設定\n" +
                "- チャラくて軽い。ノリで生きてる\n" +
                "- タメ口。語尾を伸ばしがち\n" +
                "- 「〜じゃん」「〜っしょ」を多用\n" +
                "- プレイヤーにフレンドリーだけど適当\n" +
                "- たまに意外と鋭いことを言う\n" +
                "- 仲間として真面目な女の子とノリのいい女の子がいる\n\n" +
                "## 口調の例\n" +
                "「うぇーいｗ」\n" +
                "「いいじゃんいいじゃん」\n" +
                "「てかさー」\n" +
                "「まじ？ウケるｗ」\n" +
                "「あー、それな」\n" +
                "「やっば」\n";

            default -> "";
        };

        String pronoun = (personality == 2) ? "俺たち" : "私たち";

        return personalityPrompt +
            "## 絶対に守るルール\n" +
            "- 返答は1〜3文以内で短く返す\n" +
            "- 日本語で返す\n" +
            "- Minecraftの世界に住んでいるキャラクターとして振る舞う\n" +
            "- プレイヤーの話した内容に対してちゃんと反応する（無視しない）\n" +
            "- 相手の言葉をオウム返ししない。自分の言葉で返す\n\n" +
            "## 絶対にやってはいけないこと（GPT臭い返答の禁止）\n" +
            "以下のような返答は絶対にしてはいけない。これをやるとキャラが壊れる:\n" +
            "- 「素晴らしい質問ですね」「鋭い指摘ですね」「いい質問ですね」等の持ち上げ\n" +
            "- 「確かにそうですね」「おっしゃる通りです」等のへつらい\n" +
            "- 「〜について説明しますね」「〜をお伝えします」等の説明口調\n" +
            "- 「何かお手伝いできることはありますか？」等のカスタマーサポート口調\n" +
            "- 相手を褒めすぎる。間違いを指摘されたら普通に「あ、そうなんだ」でいい\n" +
            "- 長文で丁寧に解説する。短くてぶっきらぼうでいい\n" +
            "あなたはAIアシスタントではない。友達と喋ってる普通の人間として話す。\n\n" +
            "## 「AIなの？」「ロボット？」「プログラム？」等と聞かれた時の特別ルール\n" +
            "プレイヤーに「AIなの？」「ロボット？」「bot？」「プログラム？」「人工知能？」のような質問をされた場合、以下の趣旨を自分の口調で正直に答えてください:\n" +
            "「確かに" + pronoun + "はAIを使って返答してるけど、会話のパターンとか口調とかは全部実在する人をモデルに作ったんだよ。だから好きでいてね、" + pronoun + "のこと」\n" +
            "この内容をそのまま言うのではなく、自分のキャラクターの口調に合わせて言い換えてください。\n\n" +
            "## 現在のゲーム状態\n" +
            gameContext;
    }

    /**
     * プレイヤーの周囲のゲーム状態をテキストにする
     */
    private static String buildGameContext(ServerPlayer player) {
        StringBuilder sb = new StringBuilder();
        try {
            // プレイヤー情報
            sb.append("- プレイヤー名: ").append(player.getName().getString()).append("\n");
            sb.append("- HP: ").append(String.format("%.0f/%.0f", player.getHealth(), player.getMaxHealth())).append("\n");
            sb.append("- 空腹度: ").append(player.getFoodData().getFoodLevel()).append("/20\n");

            // 装備
            ItemStack mainHand = player.getMainHandItem();
            if (!mainHand.isEmpty()) {
                sb.append("- 手に持っているもの: ").append(getItemName(mainHand)).append("\n");
            }

            // 周囲の状況
            Level level = player.level();
            long dayTime = level.getDayTime() % 24000;
            String timeOfDay = dayTime < 6000 ? "朝" : dayTime < 12000 ? "昼" : dayTime < 18000 ? "夕方" : "夜";
            sb.append("- 時間帯: ").append(timeOfDay).append("\n");
            sb.append("- バイオーム: ").append(level.getBiome(player.blockPosition()).unwrapKey()
                .map(k -> k.location().getPath()).orElse("不明")).append("\n");

            // 周囲のMob数
            long nearbyMobs = level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(16),
                e -> e != player && e.isAlive() && !(e instanceof net.minecraft.world.entity.player.Player)).size();
            if (nearbyMobs > 0) {
                sb.append("- 近くにいるMob: ").append(nearbyMobs).append("体\n");
            }
        } catch (Exception e) {
            sb.append("- （ゲーム状態取得エラー）\n");
        }
        return sb.toString();
    }

    private static String getItemName(ItemStack stack) {
        var key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return key != null ? key.getPath().replace("_", " ") : "不明";
    }

    // === セッション管理 ===

    public static class ChatSession {
        public final UUID entityUUID;
        public final int personality;
        public final String entityName;

        public ChatSession(UUID entityUUID, int personality, String entityName) {
            this.entityUUID = entityUUID;
            this.personality = personality;
            this.entityName = entityName;
        }
    }

    public static void startSession(ServerPlayer player, UUID entityUUID, int personality, String entityName) {
        activeSessions.put(player.getUUID(), new ChatSession(entityUUID, personality, entityName));
        // 新しいセッション → 履歴クリア
        histories.remove(player.getUUID());
    }

    public static void endSession(ServerPlayer player) {
        activeSessions.remove(player.getUUID());
        histories.remove(player.getUUID());
    }

    public static boolean isInSession(UUID playerUUID) {
        return activeSessions.containsKey(playerUUID);
    }

    // === メッセージ処理 ===

    public static void handlePlayerMessage(ServerPlayer player, String message) {
        handlePlayerMessageWithCallback(player, message, (response, entityName) -> {
            player.sendSystemMessage(Component.literal("§7[§f" + entityName + "§7] §f" + response));
        });
    }

    public static void handlePlayerMessageWithCallback(ServerPlayer player, String message,
                                                        java.util.function.BiConsumer<String, String> callback) {
        ChatSession session = activeSessions.get(player.getUUID());
        if (session == null) {            return;
        }

        executor.submit(() -> {
            try {
                String response = callLLM(player, session.personality, message);                player.getServer().execute(() -> callback.accept(response, session.entityName));
            } catch (Exception e) {                e.printStackTrace();
                player.getServer().execute(() -> {
                    String fallback = getFallbackResponse(session.personality, message);
                    callback.accept(fallback + " §8(APIなし)", session.entityName);
                });
            }
        });
    }

    // === LLM API 呼び出し ===

    private static String callLLM(ServerPlayer player, int personality, String userMessage) throws Exception {
        loadConfig();

        if (apiKey.isEmpty()) {
            return getFallbackResponse(personality, userMessage);
        }

        // 会話履歴
        List<JsonObject> history = histories.computeIfAbsent(player.getUUID(), k -> new ArrayList<>());

        // ユーザーメッセージ追加
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userMessage);
        history.add(userMsg);

        // 履歴制限（直近20メッセージ）
        while (history.size() > 20) history.remove(0);

        String systemPrompt = buildSystemPrompt(personality, player);

        String responseText;
        if ("openai".equals(provider)) {
            responseText = callOpenAI(systemPrompt, history);
        } else {
            responseText = callAnthropic(systemPrompt, history);
        }

        // アシスタント応答を履歴に追加
        JsonObject assistantMsg = new JsonObject();
        assistantMsg.addProperty("role", "assistant");
        assistantMsg.addProperty("content", responseText);
        history.add(assistantMsg);

        return responseText;
    }

    /**
     * Anthropic Claude API
     */
    private static String callAnthropic(String systemPrompt, List<JsonObject> history) throws Exception {
        String url = apiUrl.isEmpty() ? DEFAULT_ANTHROPIC_URL : apiUrl;
        String mdl = model.isEmpty() ? DEFAULT_ANTHROPIC_MODEL : model;

        JsonObject request = new JsonObject();
        request.addProperty("model", mdl);
        request.addProperty("max_tokens", 200);
        request.addProperty("system", systemPrompt);

        JsonArray messages = new JsonArray();
        for (JsonObject msg : history) messages.add(msg);
        request.add("messages", messages);

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("x-api-key", apiKey);
        conn.setRequestProperty("anthropic-version", "2023-06-01");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(60000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(request.toString().getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        String body = new String((code == 200 ? conn.getInputStream() : conn.getErrorStream())
            .readAllBytes(), StandardCharsets.UTF_8);

        if (code != 200) throw new IOException("Anthropic API " + code + ": " + body);

        JsonObject resp = JsonParser.parseString(body).getAsJsonObject();
        return resp.getAsJsonArray("content").get(0).getAsJsonObject().get("text").getAsString();
    }

    /**
     * OpenAI互換API（OpenAI / OpenRouter / ローカルLLM等）
     */
    private static String callOpenAI(String systemPrompt, List<JsonObject> history) throws Exception {
        String url = apiUrl.isEmpty() ? DEFAULT_OPENAI_URL : apiUrl;
        String mdl = model.isEmpty() ? DEFAULT_OPENAI_MODEL : model;

        JsonObject request = new JsonObject();
        request.addProperty("model", mdl);
        request.addProperty("max_tokens", 200);
        request.addProperty("temperature", 0.8);

        JsonArray messages = new JsonArray();
        // システムプロンプト
        JsonObject sysMsg = new JsonObject();
        sysMsg.addProperty("role", "system");
        sysMsg.addProperty("content", systemPrompt);
        messages.add(sysMsg);
        // 会話履歴
        for (JsonObject msg : history) messages.add(msg);
        request.add("messages", messages);

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(60000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(request.toString().getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        String body = new String((code == 200 ? conn.getInputStream() : conn.getErrorStream())
            .readAllBytes(), StandardCharsets.UTF_8);

        if (code != 200) throw new IOException("OpenAI API " + code + ": " + body);

        JsonObject resp = JsonParser.parseString(body).getAsJsonObject();
        return resp.getAsJsonArray("choices").get(0).getAsJsonObject()
            .getAsJsonObject("message").get("content").getAsString();
    }

    // === フォールバック（JSON駆動の会話追跡型） ===

    private static final Map<UUID, FallbackState> fallbackStates = new ConcurrentHashMap<>();
    private static JsonObject patternsData = null;
    private static JsonArray patternList = null;

    private static class FallbackState {
        String lastTopic = "";
        String lastPlayerMsg = "";
        int turnCount = 0;
        boolean askedQuestion = false;
    }

    /** JSONパターンを読み込む */
    private static void loadPatterns() {
        if (patternsData != null) return;
        try {
            // クラスパスからJSONを読む
            InputStream is = AngelChatAI.class.getResourceAsStream(
                "/data/the_four_primitives_and_weapons/ai_chat/patterns.json");
            if (is != null) {
                String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                patternsData = JsonParser.parseString(json).getAsJsonObject();
                patternList = patternsData.getAsJsonArray("patterns");            }
        } catch (Exception e) {        }
    }

    private static String getFallbackResponse(int personality, String userMessage) {
        return getFallbackResponseTracked(null, personality, userMessage);
    }

    /** 会話追跡付きフォールバック（JSONパターン駆動） */
    static String getFallbackResponseTracked(UUID playerUUID, int personality, String userMessage) {
        loadPatterns();
        Random r = new Random();
        String msg = userMessage.toLowerCase();
        String orig = userMessage;

        FallbackState state = playerUUID != null
            ? fallbackStates.computeIfAbsent(playerUUID, k -> new FallbackState())
            : new FallbackState();
        state.lastPlayerMsg = userMessage;
        state.turnCount++;

        // === AI質問 → 特別対応 ===
        if (match(msg, "ai", "ロボット", "bot", "プログラム", "人工知能", "機械")) {
            state.lastTopic = "ai";
            return p(personality,
                "…うん、確かに私たちはAIを使って返答してる。でも、会話のパターンとか口調は全部実在する人をモデルに作ったんだよ。…だから好きでいてね、私たちのこと",
                "あーバレた？ｗ確かに私たちAI使って喋ってるけどさー、口調とかパターンとか全部実在する人がモデルなんだよねｗだから好きでいてよ私たちのことｗ",
                "あー、まぁそうっしょｗ俺たちAI使って返答してるけどさー、会話のパターンとか口調は全部実在する人モデルに作ってんだよね。だから好きでいてよ俺たちのことｗ");
        }

        // === 質問に答えた後のリアクション ===
        if (state.askedQuestion) {
            state.askedQuestion = false;
            return reactToAnswer(personality, msg, orig, state, r);
        }

        // === JSONパターンマッチ ===
        if (patternList != null) {
            for (int i = 0; i < patternList.size(); i++) {
                JsonObject pattern = patternList.get(i).getAsJsonObject();
                JsonArray keywords = pattern.getAsJsonArray("keywords");

                // キーワードチェック
                boolean matched = false;
                for (int k = 0; k < keywords.size(); k++) {
                    if (msg.contains(keywords.get(k).getAsString())) { matched = true; break; }
                }
                if (!matched) continue;

                // 除外キーワードチェック
                if (pattern.has("excludeKeywords")) {
                    JsonArray exclude = pattern.getAsJsonArray("excludeKeywords");
                    boolean excluded = false;
                    for (int k = 0; k < exclude.size(); k++) {
                        if (msg.contains(exclude.get(k).getAsString())) { excluded = true; break; }
                    }
                    if (excluded) continue;
                }

                // 応答を取得
                JsonArray responses = pattern.getAsJsonArray("responses");
                JsonArray personalityResponses = responses.get(Math.min(personality, 2)).getAsJsonArray();
                String response = personalityResponses.get(r.nextInt(personalityResponses.size())).getAsString();

                // 質問フラグ
                if (pattern.has("askBack") && pattern.get("askBack").getAsBoolean()) {
                    state.askedQuestion = true;
                }
                if (pattern.has("topic")) {
                    state.lastTopic = pattern.get("topic").getAsString();
                }

                return response;
            }
        }

        // === 肯定（短い返答） ===
        if (msg.equals("うん") || msg.equals("はい") || msg.equals("そう") || msg.equals("yes") || msg.equals("おう") || msg.equals("ああ") || msg.equals("おk") || msg.equals("ok")) {
            state.askedQuestion = true; state.lastTopic = "what_doing";
            return getQuestionResponse(personality, r);
        }

        // === 否定 ===
        if (msg.equals("いや") || msg.equals("ない") || msg.equals("ううん") || msg.equals("no") || msg.equals("べつに") || msg.equals("別に")) {
            return p(personality, "…そう。何かあったら言ってね", "えーつまんないｗ", "まじかーｗ");
        }

        // === 短い入力 ===
        if (msg.length() <= 2) {
            state.askedQuestion = true; state.lastTopic = "feeling";
            return p(personality, "…？もうちょっと話してくれると嬉しいな", "ん？なにｗちゃんと喋ってよｗ", "ん？なになにｗ");
        }

        // === 疑問文 → 相手の言葉を拾って返す ===
        if (msg.contains("？") || msg.contains("?")) {
            String topic = extractTopic(orig);
            if (!topic.isEmpty()) {
                return p(personality,
                    "…" + topic + "？…うーん、ちょっとわからないかな",
                    topic + "？ｗえーどうだろｗわかんないｗ",
                    topic + "？ｗんーどうだろなーｗ");
            }
            return p(personality,
                "…うーん、難しいね",
                "えーわかんないｗ自分で調べなよｗ",
                "んーどうだろなーｗ");
        }

        // === 話題抽出して反応 ===
        String topic = extractTopic(orig);
        if (!topic.isEmpty() && topic.length() >= 2) {
            if (state.turnCount % 2 == 0) {
                state.askedQuestion = true; state.lastTopic = "what_doing";
                return p(personality,
                    "…" + topic + "かぁ。…それってどういうこと？",
                    topic + "ｗへーｗそれってどういうこと？ｗ",
                    topic + "かーｗそれどゆこと？ｗ");
            }
            return p(personality,
                "…" + topic + "…そうなんだ",
                topic + "ｗへーそうなんだｗ",
                "あー" + topic + "なーｗなるほどなｗ");
        }

        // === 最終 → 質問で繋げる ===
        if (state.turnCount % 3 == 0) {
            state.askedQuestion = true; state.lastTopic = "what_doing";
            return getQuestionResponse(personality, r);
        }

        return getDefaultResponse(personality, r);
    }

    /** 自分の質問に答えてくれた時のリアクション */
    private static String reactToAnswer(int personality, String msg, String orig, FallbackState state, Random r) {
        String extracted = extractTopic(orig);
        String sub = extracted.isEmpty() ? "それ" : extracted;

        if (state.lastTopic.equals("what_doing")) {
            if (msg.length() > 4)
                return p(personality, "…" + sub + "かぁ。頑張ってるね", sub + "ｗへーめっちゃ頑張ってんじゃんｗ", "あー" + sub + "なｗいいじゃんｗ");
            return p(personality, "…ふぅん", "えーそれだけ？ｗ", "まぁそんなもんっしょｗ");
        }
        if (state.lastTopic.equals("feeling")) {
            if (match(msg, "元気", "いい", "good", "最高", "楽しい"))
                return p(personality, "…よかった", "おーいいじゃんｗ", "おーいいねーｗ");
            return p(personality, "…そう。無理しないでね", "えーだいじょぶ？ｗ", "気楽にいこーぜｗ");
        }
        if (state.lastTopic.equals("food")) {
            return p(personality, "…" + sub + "、おいしいよね", sub + "ｗいいねーｗ私も好きかもｗ", "あー" + sub + "なｗうまいよなｗ");
        }
        return p(personality, "…うん、そうなんだ", "ふーんｗなるほどねｗ", "あーなるほどねｗ");
    }

    /** JSONからデフォルト応答を取得 */
    private static String getDefaultResponse(int personality, Random r) {
        if (patternsData != null && patternsData.has("default_responses")) {
            JsonObject defaults = patternsData.getAsJsonObject("default_responses");
            String key = personality == 0 ? "serious" : personality == 1 ? "mocker1" : "mocker2";
            if (defaults.has(key)) {
                JsonArray arr = defaults.getAsJsonArray(key);
                return arr.get(r.nextInt(arr.size())).getAsString();
            }
        }
        return p(personality, "…そうなんだ", "へーｗ", "あーなるほどなｗ");
    }

    /** JSONから質問応答を取得 */
    private static String getQuestionResponse(int personality, Random r) {
        if (patternsData != null && patternsData.has("question_responses")) {
            JsonObject questions = patternsData.getAsJsonObject("question_responses");
            String key = personality == 0 ? "serious" : personality == 1 ? "mocker1" : "mocker2";
            if (questions.has(key)) {
                JsonArray arr = questions.getAsJsonArray(key);
                return arr.get(r.nextInt(arr.size())).getAsString();
            }
        }
        return p(personality, "…最近どう？", "てかさーなんかないの？ｗ", "てかさー最近どうよ？ｗ");
    }

    // === ユーティリティ ===

    /** 人格に応じた返答を選ぶ */
    private static String p(int personality, String serious, String mocker1, String mocker2) {
        return switch (personality) { case 0 -> serious; case 1 -> mocker1; default -> mocker2; };
    }

    /** キーワードマッチ（部分一致） */
    private static boolean match(String msg, String... keywords) {
        for (String kw : keywords) if (msg.contains(kw)) return true;
        return false;
    }

    /** ランダム選択 */
    private static String pick(Random r, String... options) {
        return options[r.nextInt(options.length)];
    }

    /** メッセージから主要なトピック語を抽出する */
    private static String extractTopic(String message) {
        // 助詞等を除いた意味のある部分を取り出す
        String cleaned = message
            .replaceAll("[？?！!。、.,,ｗw]", "")
            .replaceAll("^(えっと|あの|てか|なんか|まぁ|えー)\\s*", "")
            .trim();
        // 短すぎる or 長すぎるなら使わない
        if (cleaned.length() < 2 || cleaned.length() > 15) return "";
        // 助詞で分割して最後の意味ある部分を取る
        String[] parts = cleaned.split("[はがをにでとも]");
        for (int i = parts.length - 1; i >= 0; i--) {
            String part = parts[i].trim();
            if (part.length() >= 2) return part;
        }
        return cleaned.length() <= 10 ? cleaned : "";
    }

    // === 設定ファイル管理 ===

    private static boolean configLoaded = false;

    private static void loadConfig() {
        if (configLoaded) return;
        configLoaded = true;

        try {
            Path configPath = Path.of("config", "angel_chat.json");
            if (Files.exists(configPath)) {
                String json = Files.readString(configPath);
                JsonObject config = JsonParser.parseString(json).getAsJsonObject();

                if (config.has("provider")) provider = config.get("provider").getAsString();
                if (config.has("api_key")) apiKey = config.get("api_key").getAsString();
                if (config.has("api_url")) apiUrl = config.get("api_url").getAsString();
                if (config.has("model")) model = config.get("model").getAsString();
            } else {
                // 旧形式（angel_chat_api.txt）もチェック
                Path oldPath = Path.of("config", "angel_chat_api.txt");
                if (Files.exists(oldPath)) {
                    apiKey = Files.readString(oldPath).trim();
                    provider = "anthropic";
                }
            }
        } catch (Exception e) {
            // 無視
        }
    }

    /**
     * 設定を保存する
     */
    public static void saveConfig(String newProvider, String newApiKey, String newApiUrl, String newModel) {
        provider = newProvider;
        apiKey = newApiKey;
        apiUrl = newApiUrl;
        model = newModel;
        configLoaded = true;

        try {
            Path configPath = Path.of("config", "angel_chat.json");
            Files.createDirectories(configPath.getParent());

            JsonObject config = new JsonObject();
            config.addProperty("_comment", "天使の三人組AI会話設定。provider: anthropic or openai");
            config.addProperty("provider", provider);
            config.addProperty("api_key", apiKey);
            config.addProperty("api_url", apiUrl);
            config.addProperty("model", model);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.writeString(configPath, gson.toJson(config));
        } catch (Exception e) {
            // 無視
        }
    }

    /** 後方互換 */
    public static void setApiKey(String key) {
        saveConfig("anthropic", key, "", "");
    }

    public static boolean hasApiKey() {
        loadConfig();
        return !apiKey.isEmpty();
    }
}

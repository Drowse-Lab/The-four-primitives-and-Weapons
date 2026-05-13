package the_four_primitives_and_weapons.ai.lisp;

import com.google.gson.*;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * RPG風の選択肢ベース会話ツリーマネージャ。
 *
 * 起動時に3人分のJSONをロードし、ノードIDを辿って会話を進める。
 */
public class DialogueManager {

    public static class Node {
        public final String text;
        public final List<Choice> choices;
        public final boolean isEnd;

        public Node(String text, List<Choice> choices, boolean isEnd) {
            this.text = text;
            this.choices = choices;
            this.isEnd = isEnd;
        }
    }

    public static class Choice {
        public final String text;
        public final String next;

        public Choice(String text, String next) {
            this.text = text;
            this.next = next;
        }
    }

    // personality (0/1/2) → ノードID → Node
    private static final Map<Integer, Map<String, Node>> trees = new HashMap<>();
    private static boolean loaded = false;

    public static void loadAll() {
        if (loaded) return;
        loaded = true;
        trees.put(0, loadTree("dialogue_serious.json"));
        trees.put(1, loadTree("dialogue_mocker1.json"));
        trees.put(2, loadTree("dialogue_mocker2.json"));
    }

    private static Map<String, Node> loadTree(String filename) {
        Map<String, Node> tree = new HashMap<>();
        try {
            InputStream is = DialogueManager.class.getResourceAsStream(
                "/data/the_four_primitives_and_weapons/ai_chat/" + filename);
            if (is == null) {                return tree;
            }
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                String key = entry.getKey();
                if (key.startsWith("_")) continue;

                JsonObject nodeObj = entry.getValue().getAsJsonObject();
                String text = nodeObj.has("text") ? nodeObj.get("text").getAsString() : "";
                boolean isEnd = nodeObj.has("end") && nodeObj.get("end").getAsBoolean();

                List<Choice> choices = new ArrayList<>();
                if (nodeObj.has("choices")) {
                    JsonArray choiceArr = nodeObj.getAsJsonArray("choices");
                    for (JsonElement c : choiceArr) {
                        JsonObject co = c.getAsJsonObject();
                        choices.add(new Choice(
                            co.get("text").getAsString(),
                            co.has("next") ? co.get("next").getAsString() : null
                        ));
                    }
                }

                tree.put(key, new Node(text, choices, isEnd));
            }        } catch (Exception e) {            e.printStackTrace();
        }
        return tree;
    }

    /**
     * 指定された人格の指定されたノードを取得する
     */
    public static Node getNode(int personality, String nodeId) {
        loadAll();
        Map<String, Node> tree = trees.get(personality);
        if (tree == null) return null;
        return tree.get(nodeId);
    }

    /**
     * 開始ノードを取得
     */
    public static Node getStartNode(int personality) {
        return getNode(personality, "start");
    }
}

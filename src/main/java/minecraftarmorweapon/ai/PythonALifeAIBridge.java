package minecraftarmorweapon.ai;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Python A-Life AIとJavaを連携させるブリッジクラス
 *
 * PythonのAIロジックを呼び出し、失敗時は既存のJavaロジックにフォールバック
 */
public class PythonALifeAIBridge {

    private static PythonAIProcess pythonProcess = null;
    private static boolean usePython = true; // Pythonを使用するかどうか
    private static final Map<String, PythonALifeAIBridge> activeInstances = new HashMap<>();

    private final Mob entity;
    private final int tier;
    private final String aiId;

    // フォールバック用のJava AI
    private final ALifeAIBridge fallbackAI;

    // Python連携用
    private final Gson gson = new Gson();
    private boolean pythonInitialized = false;

    private LivingEntity currentTarget = null;

    /**
     * コンストラクタ
     */
    public PythonALifeAIBridge(Mob entity, int tier) {
        this.entity = entity;
        this.tier = tier;
        this.aiId = entity.getUUID().toString();

        // フォールバック用のJava AIを作成
        this.fallbackAI = new ALifeAIBridge(entity, tier);

        // グローバルなPythonプロセスを初期化
        initializeGlobalPythonProcess();

        // このAIインスタンスをPythonに登録
        if (usePython && pythonProcess != null && pythonProcess.isRunning()) {
            pythonInitialized = initializePythonAI();
        }

        activeInstances.put(aiId, this);
    }

    /**
     * グローバルなPythonプロセスを初期化（1回だけ）
     */
    private static synchronized void initializeGlobalPythonProcess() {
        if (!usePython) {
            return;
        }

        if (pythonProcess == null) {
            pythonProcess = new PythonAIProcess();
            boolean started = pythonProcess.start();

            if (!started) {
                System.err.println("Failed to start Python AI process. Falling back to Java AI.");
                usePython = false;
                pythonProcess = null;
            } else {
                System.out.println("Python AI process started successfully");

                // シャットダウンフックを登録
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    if (pythonProcess != null) {
                        pythonProcess.shutdown();
                    }
                }));
            }
        }
    }

    /**
     * PythonにこのAIを初期化
     */
    private boolean initializePythonAI() {
        try {
            JsonObject command = new JsonObject();
            command.addProperty("command", "initialize");
            command.addProperty("ai_id", aiId);
            command.addProperty("tier", tier);

            JsonObject entityData = createEntityDataJson();
            command.add("entity_data", entityData);

            JsonObject response = pythonProcess.sendCommand(command);

            if ("success".equals(response.get("status").getAsString())) {
                System.out.println("Python AI initialized for entity: " + aiId);
                return true;
            } else {
                System.err.println("Failed to initialize Python AI: " + response.get("error_message").getAsString());
                return false;
            }

        } catch (Exception e) {
            System.err.println("Error initializing Python AI: " + e.getMessage());
            return false;
        }
    }

    /**
     * AIを更新（毎ティック呼ばれる）
     */
    public ALifeAIBridge.AIAction update() {
        // Pythonが使えない、または初期化に失敗している場合はフォールバック
        if (!usePython || !pythonInitialized || pythonProcess == null || !pythonProcess.isRunning()) {
            return fallbackAI.update();
        }

        try {
            // Pythonに更新コマンドを送信
            JsonObject command = new JsonObject();
            command.addProperty("command", "update");
            command.addProperty("ai_id", aiId);
            command.addProperty("delta_time", 0.05); // 1tick = 50ms

            JsonObject worldData = createWorldDataJson();
            command.add("world_data", worldData);

            JsonObject entityData = createEntityDataJson();
            command.add("entity_data", entityData);

            JsonObject response = pythonProcess.sendCommand(command);

            if ("success".equals(response.get("status").getAsString())) {
                // Pythonからのアクションを解析
                JsonObject actionJson = response.getAsJsonObject("action");
                return parseActionFromJson(actionJson);
            } else {
                System.err.println("Python AI update failed: " + response.get("error_message").getAsString());
                // フォールバック
                return fallbackAI.update();
            }

        } catch (Exception e) {
            System.err.println("Error updating Python AI: " + e.getMessage());
            // フォールバック
            return fallbackAI.update();
        }
    }

    /**
     * エンティティデータをJSON形式で作成
     */
    private JsonObject createEntityDataJson() {
        JsonObject data = new JsonObject();

        Vec3 pos = entity.position();
        data.addProperty("health", entity.getHealth());
        data.addProperty("max_health", entity.getMaxHealth());

        JsonObject position = new JsonObject();
        position.addProperty("x", pos.x);
        position.addProperty("y", pos.y);
        position.addProperty("z", pos.z);
        data.add("position", position);

        // 装備情報を追加
        JsonObject equipment = new JsonObject();
        equipment.addProperty("mainhand_item", entity.getMainHandItem().getItem().toString());
        equipment.addProperty("offhand_item", entity.getOffhandItem().getItem().toString());
        equipment.addProperty("helmet", entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).getItem().toString());
        equipment.addProperty("chestplate", entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).getItem().toString());
        equipment.addProperty("leggings", entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS).getItem().toString());
        equipment.addProperty("boots", entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET).getItem().toString());
        data.add("equipment", equipment);

        return data;
    }

    /**
     * ワールドデータをJSON形式で作成
     */
    private JsonObject createWorldDataJson() {
        JsonObject data = new JsonObject();

        // 現在時刻
        data.addProperty("current_time", System.currentTimeMillis() / 1000.0);

        // 最も近い敵を探す
        Optional<LivingEntity> nearestEnemy = findNearestEnemy();
        if (nearestEnemy.isPresent()) {
            LivingEntity enemy = nearestEnemy.get();
            currentTarget = enemy;

            Vec3 enemyPos = enemy.position();
            JsonObject enemyPosition = new JsonObject();
            enemyPosition.addProperty("x", enemyPos.x);
            enemyPosition.addProperty("y", enemyPos.y);
            enemyPosition.addProperty("z", enemyPos.z);
            data.add("nearest_enemy_position", enemyPosition);

            data.addProperty("nearest_enemy_distance", entity.distanceTo(enemy));
        } else {
            data.addProperty("nearest_enemy_distance", Double.MAX_VALUE);
        }

        return data;
    }

    /**
     * 最も近い敵を探す
     */
    private Optional<LivingEntity> findNearestEnemy() {
        return entity.level.getEntitiesOfClass(
            LivingEntity.class,
            entity.getBoundingBox().inflate(16.0),
            e -> e != entity &&
                 e.isAlive() &&
                 !e.isAlliedTo(entity) &&
                 (e instanceof Player || e.getTeam() != entity.getTeam())
        ).stream()
        .min((e1, e2) -> Double.compare(entity.distanceTo(e1), entity.distanceTo(e2)));
    }

    /**
     * PythonのJSONアクションをJavaのAIActionに変換
     */
    private ALifeAIBridge.AIAction parseActionFromJson(JsonObject actionJson) {
        String actionType = actionJson.get("action").getAsString();
        ALifeAIBridge.AIAction action = new ALifeAIBridge.AIAction(actionType);

        JsonObject actionData = actionJson.has("data") ? actionJson.getAsJsonObject("data") : new JsonObject();

        // 共通フィールド
        if (actionData.has("speed")) {
            action.speed = actionData.get("speed").getAsFloat();
        }

        if (actionData.has("combo")) {
            action.combo = actionData.get("combo").getAsInt();
        }

        if (actionData.has("type")) {
            action.attackType = actionData.get("type").getAsString();
        }

        // 方向ベクトル
        if (actionData.has("direction")) {
            JsonObject direction = actionData.getAsJsonObject("direction");
            if (direction.isJsonArray()) {
                // 配列形式: [x, y, z]
                double x = direction.getAsJsonArray().get(0).getAsDouble();
                double y = direction.getAsJsonArray().get(1).getAsDouble();
                double z = direction.getAsJsonArray().get(2).getAsDouble();
                action.direction = new Vec3(x, y, z);
            } else {
                // オブジェクト形式: {x: 1, y: 0, z: 0}
                double x = direction.get("x").getAsDouble();
                double y = direction.get("y").getAsDouble();
                double z = direction.get("z").getAsDouble();
                action.direction = new Vec3(x, y, z);
            }
        }

        // ターゲット位置
        if (actionData.has("look_at")) {
            JsonObject target = actionData.getAsJsonObject("look_at");
            if (target.isJsonArray()) {
                double x = target.getAsJsonArray().get(0).getAsDouble();
                double y = target.getAsJsonArray().get(1).getAsDouble();
                double z = target.getAsJsonArray().get(2).getAsDouble();
                action.target = new Vec3(x, y, z);
            } else {
                double x = target.get("x").getAsDouble();
                double y = target.get("y").getAsDouble();
                double z = target.get("z").getAsDouble();
                action.target = new Vec3(x, y, z);
            }
        }

        // プレイヤー動作用フィールド
        if (actionData.has("distance")) {
            action.distance = actionData.get("distance").getAsFloat();
        }

        if (actionData.has("damage_multiplier")) {
            action.damageMultiplier = actionData.get("damage_multiplier").getAsFloat();
        }

        if (actionData.has("charge_percent")) {
            action.chargePercent = actionData.get("charge_percent").getAsFloat();
        }

        if (actionData.has("skill_type")) {
            action.skillType = actionData.get("skill_type").getAsString();
        }

        if (actionData.has("duration")) {
            action.duration = actionData.get("duration").getAsFloat();
        }

        return action;
    }

    /**
     * このAIを破棄
     */
    public void destroy() {
        if (usePython && pythonInitialized && pythonProcess != null && pythonProcess.isRunning()) {
            try {
                JsonObject command = new JsonObject();
                command.addProperty("command", "destroy");
                command.addProperty("ai_id", aiId);

                pythonProcess.sendCommand(command);

            } catch (Exception e) {
                System.err.println("Error destroying Python AI: " + e.getMessage());
            }
        }

        activeInstances.remove(aiId);
    }

    /**
     * 現在の状態を取得
     */
    public ALifeAIBridge.AIState getCurrentState() {
        return fallbackAI.getCurrentState();
    }

    /**
     * ティアを取得
     */
    public int getTier() {
        return tier;
    }

    /**
     * Python使用を無効化（デバッグ用）
     */
    public static void disablePython() {
        usePython = false;
        if (pythonProcess != null) {
            pythonProcess.shutdown();
            pythonProcess = null;
        }
    }

    /**
     * Python使用を有効化
     */
    public static void enablePython() {
        usePython = true;
        initializeGlobalPythonProcess();
    }

    /**
     * Pythonが使用されているか確認
     */
    public static boolean isPythonEnabled() {
        return usePython && pythonProcess != null && pythonProcess.isRunning();
    }
}

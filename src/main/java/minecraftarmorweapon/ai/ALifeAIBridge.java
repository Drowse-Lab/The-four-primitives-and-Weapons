package minecraftarmorweapon.ai;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * ALife AIシステムとJavaの橋渡しクラス
 *
 * PythonのAIロジックとMinecraftのエンティティを連携させます
 * (現在はPython連携の代わりにJavaで直接実装)
 */
public class ALifeAIBridge {

    private final Mob entity;
    private final int tier;

    // AI状態
    private AIState currentState = AIState.IDLE;
    private long lastStateChange = 0;
    private long lastAttackTime = 0;
    private long lastDodgeTime = 0;

    // 戦闘データ
    private LivingEntity currentTarget = null;
    private int comboCount = 0;
    private float dodgeSuccessRate = 0.3f;

    public ALifeAIBridge(Mob entity, int tier) {
        this.entity = entity;
        this.tier = tier;

        // ティアに応じてパラメータを設定
        this.dodgeSuccessRate = getTierDodgeRate(tier);
    }

    /**
     * AIを更新（毎ティック呼ばれる）
     */
    public AIAction update() {
        // ワールドデータを収集
        WorldData worldData = collectWorldData();

        // 状態遷移を評価
        AIState newState = evaluateStateTransition(worldData);
        if (newState != currentState) {
            changeState(newState);
        }

        // 現在の状態に応じた行動を実行
        return executeCurrentState(worldData);
    }

    /**
     * ワールドデータを収集
     */
    private WorldData collectWorldData() {
        WorldData data = new WorldData();

        // 現在の位置
        data.position = entity.position();
        data.health = entity.getHealth();
        data.maxHealth = entity.getMaxHealth();

        // 最も近い敵を探す
        Optional<LivingEntity> nearestEnemy = findNearestEnemy();
        if (nearestEnemy.isPresent()) {
            LivingEntity enemy = nearestEnemy.get();
            data.nearestEnemyPosition = enemy.position();
            data.nearestEnemyDistance = entity.distanceTo(enemy);
            currentTarget = enemy;
        } else {
            data.nearestEnemyDistance = Double.MAX_VALUE;
        }

        data.currentTime = System.currentTimeMillis();

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
     * 状態遷移を評価
     */
    private AIState evaluateStateTransition(WorldData data) {
        float healthRatio = data.health / data.maxHealth;

        // HP30%以下で撤退
        if (healthRatio < 0.3f) {
            return AIState.RETREAT;
        }

        // 敵が16ブロック以内なら戦闘
        if (data.nearestEnemyDistance < 16.0) {
            return AIState.COMBAT;
        }

        // 敵を見失ったら探索
        if (currentState == AIState.COMBAT && data.nearestEnemyDistance > 16.0) {
            return AIState.SEARCH;
        }

        // デフォルトは巡回
        return AIState.PATROL;
    }

    /**
     * 状態を変更
     */
    private void changeState(AIState newState) {
        currentState = newState;
        lastStateChange = System.currentTimeMillis();
    }

    /**
     * 現在の状態に応じた行動を実行
     */
    private AIAction executeCurrentState(WorldData data) {
        switch (currentState) {
            case IDLE:
                return handleIdle(data);
            case PATROL:
                return handlePatrol(data);
            case SEARCH:
                return handleSearch(data);
            case COMBAT:
                return handleCombat(data);
            case RETREAT:
                return handleRetreat(data);
            case DODGE:
                return handleDodge(data);
            default:
                return new AIAction("idle");
        }
    }

    private AIAction handleIdle(WorldData data) {
        return new AIAction("idle");
    }

    private AIAction handlePatrol(WorldData data) {
        AIAction action = new AIAction("move");
        action.speed = 0.3f;
        return action;
    }

    private AIAction handleSearch(WorldData data) {
        AIAction action = new AIAction("search");
        action.speed = 0.4f;
        return action;
    }

    private AIAction handleCombat(WorldData data) {
        if (currentTarget == null || !currentTarget.isAlive()) {
            return new AIAction("idle");
        }

        double distance = data.nearestEnemyDistance;

        // 攻撃範囲外なら接近
        if (distance > 3.0) {
            AIAction action = new AIAction("move_to_target");
            action.target = data.nearestEnemyPosition;
            action.speed = 0.3f;
            return action;
        }

        // 攻撃範囲内なら攻撃
        if (distance <= 3.0) {
            long currentTime = data.currentTime;
            long attackCooldown = 1500; // 1.5秒（ティア1）

            if (currentTime - lastAttackTime >= attackCooldown) {
                lastAttackTime = currentTime;
                comboCount++;
                if (comboCount > 2) {
                    comboCount = 0;
                }

                AIAction action = new AIAction("attack");
                action.attackType = comboCount == 1 ? "slash" : "thrust";
                action.combo = comboCount;
                return action;
            } else {
                // 攻撃クールダウン中は周囲を移動
                AIAction action = new AIAction("strafe");
                action.target = data.nearestEnemyPosition;
                action.speed = 0.2f;
                return action;
            }
        }

        return new AIAction("idle");
    }

    private AIAction handleRetreat(WorldData data) {
        if (data.nearestEnemyPosition != null) {
            AIAction action = new AIAction("move_away");
            action.target = data.nearestEnemyPosition;
            action.speed = 0.5f;
            return action;
        }
        return new AIAction("idle");
    }

    private AIAction handleDodge(WorldData data) {
        AIAction action = new AIAction("dodge");
        action.speed = 0.6f;
        return action;
    }

    /**
     * ティアに応じた回避成功率を取得
     */
    private float getTierDodgeRate(int tier) {
        switch (tier) {
            case 1: return 0.3f;  // 一般兵: 30%
            case 2: return 0.5f;  // エリート兵: 50%
            case 3: return 0.7f;  // 特異点: 70%
            case 4: return 0.8f;  // 英雄級: 80%
            case 5: return 0.9f;  // 神話級: 90%
            case 6: return 0.95f; // 天使級: 95%
            case 7: return 0.99f; // 神聖級: 99%
            default: return 0.3f;
        }
    }

    public AIState getCurrentState() {
        return currentState;
    }

    public int getTier() {
        return tier;
    }

    /**
     * AI状態
     */
    public enum AIState {
        IDLE,
        PATROL,
        SEARCH,
        COMBAT,
        RETREAT,
        DODGE
    }

    /**
     * ワールドデータ
     */
    private static class WorldData {
        Vec3 position;
        float health;
        float maxHealth;
        Vec3 nearestEnemyPosition;
        double nearestEnemyDistance;
        long currentTime;
    }

    /**
     * AI行動
     */
    public static class AIAction {
        public String action;
        public String attackType;
        public Vec3 target;
        public float speed;
        public int combo;

        public AIAction(String action) {
            this.action = action;
        }
    }
}

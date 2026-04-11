package minecraftarmorweapon.ai.lisp;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;

import java.util.*;

/**
 * Lisp S式で定義された行動木を評価してAIActionを返すMobの「脳」。
 *
 * 毎ティック:
 *  1. センサーデータ（距離、HP、等）をLisp変数にバインド
 *  2. S式の行動木を評価
 *  3. 評価結果をAIActionに変換して返す
 *
 * 個体レベル（ai-level）が高いほど:
 *  - 使える行動プリミティブが増える
 *  - 反応速度（クールダウン）が短くなる
 *  - 判断の精度が上がる（ノイズが減る）
 */
public class MobAIBrain {

    private final Mob entity;
    private final LispInterpreter lisp;
    private Object genome; // パース済みのS式AST
    private int aiLevel;   // 個体AIレベル（1〜10）

    // 戦闘スコア（進化用）
    private float fitnessScore = 0;
    private int damageDealt = 0;
    private int damageTaken = 0;
    private int killCount = 0;
    private long aliveTime = 0;
    private long spawnTime;

    // クールダウン管理
    private int attackCooldown = 0;
    private int dodgeCooldown = 0;
    private int skillCooldown = 0;
    private int blockCooldown = 0;

    public MobAIBrain(Mob entity, int aiLevel) {
        this.entity = entity;
        this.aiLevel = aiLevel;
        this.lisp = new LispInterpreter();
        this.spawnTime = System.currentTimeMillis();
        registerPrimitives();
    }

    /**
     * S式文字列からゲノムを設定する。
     */
    public void setGenome(String sExpression) {
        this.genome = lisp.parse(sExpression);
    }

    /**
     * パース済みASTからゲノムを設定する。
     */
    public void setGenomeAST(Object ast) {
        this.genome = ast;
    }

    /**
     * 毎ティック呼ばれる。センサーを更新し、S式を評価してアクション文字列を返す。
     */
    public String tick() {
        if (genome == null || !entity.isAlive()) return "idle";

        // クールダウン減算
        if (attackCooldown > 0) attackCooldown--;
        if (dodgeCooldown > 0) dodgeCooldown--;
        if (skillCooldown > 0) skillCooldown--;
        if (blockCooldown > 0) blockCooldown--;

        // センサーデータを注入
        updateSensors();

        // S式を評価
        try {
            Object result = lisp.eval(genome);
            return extractAction(result);
        } catch (Exception e) {
            return "idle";
        }
    }

    // === センサー ===

    private void updateSensors() {
        lisp.setVariable("ai-level", aiLevel);
        lisp.setVariable("health", (double) entity.getHealth());
        lisp.setVariable("max-health", (double) entity.getMaxHealth());
        lisp.setVariable("health-ratio", entity.getHealth() / Math.max(1, entity.getMaxHealth()));
        lisp.setVariable("on-ground", entity.onGround());

        // ターゲット情報
        LivingEntity target = entity.getTarget();
        if (target != null && target.isAlive()) {
            double dist = entity.distanceTo(target);
            lisp.setVariable("has-target", true);
            lisp.setVariable("distance", dist);
            lisp.setVariable("target-health", (double) target.getHealth());
            lisp.setVariable("target-health-ratio", target.getHealth() / Math.max(1, target.getMaxHealth()));
            lisp.setVariable("target-is-player", target instanceof Player);
            lisp.setVariable("target-is-blocking",
                target.isBlocking());
            lisp.setVariable("target-has-shield",
                !target.getItemBySlot(EquipmentSlot.OFFHAND).isEmpty()
                && target.getItemBySlot(EquipmentSlot.OFFHAND).getItem() instanceof ShieldItem);
            lisp.setVariable("target-has-bow",
                target.getMainHandItem().getItem() instanceof BowItem);
            lisp.setVariable("target-has-sword",
                target.getMainHandItem().getItem() instanceof SwordItem);
            lisp.setVariable("can-see-target",
                entity.getSensing().hasLineOfSight(target));

            // 高度差
            lisp.setVariable("height-diff", target.getY() - entity.getY());

            // プレイヤーの行動パターンを注入（学習データ）
            if (target instanceof Player player) {
                PlayerBehaviorTracker.injectIntoLisp(lisp, player);
            }
        } else {
            lisp.setVariable("has-target", false);
            lisp.setVariable("distance", 999.0);
            lisp.setVariable("target-health", 0.0);
            lisp.setVariable("target-health-ratio", 0.0);
            lisp.setVariable("target-is-player", false);
            lisp.setVariable("target-is-blocking", false);
            lisp.setVariable("target-has-shield", false);
            lisp.setVariable("target-has-bow", false);
            lisp.setVariable("target-has-sword", false);
            lisp.setVariable("can-see-target", false);
            lisp.setVariable("height-diff", 0.0);
        }

        // 周囲の敵数
        long nearbyCount = entity.level().getEntitiesOfClass(
            LivingEntity.class,
            entity.getBoundingBox().inflate(8.0),
            e -> e != entity && e.isAlive() && !e.isAlliedTo(entity) && entity.canAttack(e)
        ).size();
        lisp.setVariable("nearby-enemy-count", (double) nearbyCount);

        // クールダウン状態
        lisp.setVariable("can-attack", attackCooldown <= 0);
        lisp.setVariable("can-dodge", dodgeCooldown <= 0);
        lisp.setVariable("can-skill", skillCooldown <= 0);
        lisp.setVariable("can-block", blockCooldown <= 0);
    }

    // === 行動プリミティブ ===

    private void registerPrimitives() {
        // 攻撃系 — aiLevelに応じてクールダウンが変わる
        lisp.registerFunction("attack", args -> {
            if (attackCooldown > 0) return "idle";
            attackCooldown = Math.max(5, 20 - aiLevel * 2); // Lv1: 18tick, Lv10: 0tick
            return "attack";
        });

        lisp.registerFunction("charge-attack", args -> {
            if (attackCooldown > 0 || aiLevel < 2) return "idle";
            attackCooldown = Math.max(10, 40 - aiLevel * 3);
            return "charge_attack";
        });

        lisp.registerFunction("combo-attack", args -> {
            if (attackCooldown > 0 || aiLevel < 4) return "idle";
            int comboLen = Math.min(aiLevel, args.isEmpty() ? 3 : LispInterpreter.toInt(args.get(0)));
            attackCooldown = Math.max(5, 30 - aiLevel * 2);
            return "combo_" + comboLen;
        });

        lisp.registerFunction("ranged-attack", args -> {
            if (attackCooldown > 0) return "idle";
            attackCooldown = Math.max(10, 30 - aiLevel * 2);
            return "ranged_attack";
        });

        // 移動系
        lisp.registerFunction("approach", args -> "approach");

        lisp.registerFunction("retreat", args -> "retreat");

        lisp.registerFunction("circle-strafe", args -> "circle_strafe");

        lisp.registerFunction("leap", args -> {
            if (aiLevel < 3) return "idle";
            return "leap";
        });

        // 防御系
        lisp.registerFunction("dodge", args -> {
            if (dodgeCooldown > 0 || aiLevel < 2) return "idle";
            // aiLevelが高いほど回避成功率が上がる
            float successRate = 0.1f + aiLevel * 0.08f; // Lv1: 18%, Lv10: 90%
            if (entity.getRandom().nextFloat() < successRate) {
                dodgeCooldown = Math.max(5, 20 - aiLevel);
                return "dodge";
            }
            dodgeCooldown = 10;
            return "idle"; // 回避失敗
        });

        lisp.registerFunction("block-shield", args -> {
            if (blockCooldown > 0 || aiLevel < 3) return "idle";
            blockCooldown = Math.max(5, 30 - aiLevel * 2);
            return "block_shield";
        });

        lisp.registerFunction("parry", args -> {
            if (aiLevel < 5) return "idle";
            float parryRate = 0.1f + (aiLevel - 5) * 0.1f; // Lv5: 10%, Lv10: 60%
            return entity.getRandom().nextFloat() < parryRate ? "parry" : "idle";
        });

        // 特殊
        lisp.registerFunction("skill", args -> {
            if (skillCooldown > 0 || aiLevel < 4) return "idle";
            String skillType = args.isEmpty() ? "default" : args.get(0).toString();
            skillCooldown = Math.max(20, 60 - aiLevel * 4);
            return "skill_" + skillType;
        });

        lisp.registerFunction("wait", args -> {
            int ticks = args.isEmpty() ? 10 : LispInterpreter.toInt(args.get(0));
            return "wait_" + ticks;
        });

        // ユーティリティ関数（判定のみ）
        lisp.registerFunction("chance", args -> {
            double prob = args.isEmpty() ? 0.5 : LispInterpreter.toDouble(args.get(0));
            return entity.getRandom().nextDouble() < prob;
        });
    }

    // === 結果変換 ===

    private String extractAction(Object result) {
        if (result == null) return "idle";
        if (result instanceof String s) return s;
        if (result instanceof LispInterpreter.Symbol s) return s.name;
        if (result instanceof List<?> list && !list.isEmpty()) {
            return list.get(0).toString();
        }
        if (result instanceof Boolean b) return b ? "approach" : "idle";
        return "idle";
    }

    // === スコアリング（進化用） ===

    public void recordDamageDealt(float amount) {
        damageDealt += (int) amount;
    }

    public void recordDamageTaken(float amount) {
        damageTaken += (int) amount;
    }

    public void recordKill() {
        killCount++;
    }

    /**
     * フィットネス計算。高いほど優秀なAI。
     */
    public float calculateFitness() {
        aliveTime = System.currentTimeMillis() - spawnTime;
        float timeBonus = Math.min(aliveTime / 1000f, 300f); // 最大5分分のボーナス
        fitnessScore = (damageDealt * 2.0f) - (damageTaken * 0.5f) + (killCount * 50.0f) + timeBonus;
        return fitnessScore;
    }

    // === ゲッター ===

    public int getAiLevel() { return aiLevel; }
    public void setAiLevel(int level) { this.aiLevel = Math.max(1, Math.min(10, level)); }
    public float getFitnessScore() { return fitnessScore; }
    public Object getGenomeAST() { return genome; }
    public LispInterpreter getLisp() { return lisp; }

    public String getGenomeString() {
        return genome != null ? LispInterpreter.toSExpression(genome) : "";
    }
}

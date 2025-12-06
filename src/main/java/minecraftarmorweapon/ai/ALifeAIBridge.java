package minecraftarmorweapon.ai;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;

import minecraftarmorweapon.init.MinecraftArmorWeaponModItems;

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

    // プレイヤー動作用のタイミング管理
    private long lastChargeTime = 0;
    private long lastSkillTime = 0;
    private boolean hasChargeTime = false;
    private boolean hasSkillTime = false;

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
            System.out.println("[ALifeAI] " + entity.getName().getString() + " 状態変更: " + currentState + " (距離: " + String.format("%.1f", worldData.nearestEnemyDistance) + ")");
        }

        // 現在の状態に応じた行動を実行
        AIAction action = executeCurrentState(worldData);

        // デバッグログ（アクションが空でない場合のみ）
        if (action != null && !"idle".equals(action.action)) {
            System.out.println("[ALifeAI] " + entity.getName().getString() + " アクション: " + action.action +
                             " (速度: " + action.speed + ", スプリント: " + action.isSprinting + ")");
        }

        return action;
    }

    /**
     * ワールドデータを収集
     */
    private WorldData collectWorldData() {
        WorldData data = new WorldData();

        try {
            // エンティティの生存確認
            if (!entity.isAlive() || entity.level == null) {
                data.nearbyEnemies = new java.util.ArrayList<>();
                data.nearbyEnemyCount = 0;
                data.nearestEnemyDistance = Double.MAX_VALUE;
                data.currentTime = System.currentTimeMillis();
                return data;
            }

            // 現在の位置
            data.position = entity.position();
            data.health = entity.getHealth();
            data.maxHealth = entity.getMaxHealth();

            // 周囲の敵をすべて取得（クリエイティブ/スペクテーターを除外）
            // FOLLOW_RANGE分の範囲で敵を探す（64ブロック）
            data.nearbyEnemies = entity.level.getEntitiesOfClass(
                LivingEntity.class,
                entity.getBoundingBox().inflate(64.0),
                e -> e != null &&
                     e != entity &&
                     e.isAlive() &&
                     !e.isAlliedTo(entity) &&
                     entity.canAttack(e) &&
                     (!(e instanceof Player player) || (!player.isCreative() && !player.isSpectator()))
            );

            // 近くの敵の数をカウント（5ブロック以内）
            data.nearbyEnemyCount = (int) data.nearbyEnemies.stream()
                .filter(e -> e != null && entity.distanceTo(e) <= 5.0)
                .count();

            // 最も近い敵を探す
            Optional<LivingEntity> nearestEnemy = findNearestEnemy();
            if (nearestEnemy.isPresent()) {
                LivingEntity enemy = nearestEnemy.get();
                if (enemy != null && enemy.isAlive()) {
                    data.nearestEnemyPosition = enemy.position();
                    data.nearestEnemyDistance = entity.distanceTo(enemy);
                    currentTarget = enemy;
                } else {
                    data.nearestEnemyDistance = Double.MAX_VALUE;
                    currentTarget = null;
                }
            } else {
                data.nearestEnemyDistance = Double.MAX_VALUE;
                currentTarget = null;
            }

            data.currentTime = System.currentTimeMillis();

        } catch (Exception e) {
            System.err.println("[ALifeAI] Error collecting world data for " + entity.getName().getString() + ": " + e.getMessage());
            e.printStackTrace();
            // フォールバック
            data.nearbyEnemies = new java.util.ArrayList<>();
            data.nearbyEnemyCount = 0;
            data.nearestEnemyDistance = Double.MAX_VALUE;
            data.currentTime = System.currentTimeMillis();
        }

        return data;
    }

    /**
     * 最も近い敵を探す（優先度: entity.getTarget() → 最も近い敵）
     * クリエイティブ/スペクテーターモードのプレイヤーは除外
     */
    private Optional<LivingEntity> findNearestEnemy() {
        // 優先度1: entity.getTarget()（攻撃されたMobを追いかける）
        LivingEntity target = entity.getTarget();
        if (target != null && target.isAlive() && entity.distanceTo(target) < 64.0) {
            // クリエイティブ/スペクテーターのプレイヤーの場合はターゲット解除
            if (target instanceof Player player) {
                if (player.isCreative() || player.isSpectator()) {
                    entity.setTarget(null);
                    return Optional.empty();
                }
            }
            return Optional.of(target);
        }

        // 優先度2: 最も近い有効な敵（クリエイティブ/スペクテーターを除外）
        // FOLLOW_RANGE分の範囲で敵を探す（64ブロック）
        return entity.level.getEntitiesOfClass(
            LivingEntity.class,
            entity.getBoundingBox().inflate(64.0),
            e -> e != entity &&
                 e.isAlive() &&
                 !e.isAlliedTo(entity) &&
                 entity.canAttack(e) &&
                 (!(e instanceof Player player) || (!player.isCreative() && !player.isSpectator()))
        ).stream()
        .min((e1, e2) -> Double.compare(entity.distanceTo(e1), entity.distanceTo(e2)));
    }

    /**
     * 最も危険な敵を見つける（脅威度評価）
     * 最寄りの敵を優先的に倒すように変更
     */
    private LivingEntity findMostDangerousEnemy(WorldData data) {
        LivingEntity nearestEnemy = null;
        double nearestDistance = Double.MAX_VALUE;

        for (LivingEntity enemy : data.nearbyEnemies) {
            if (enemy == null || !enemy.isAlive()) {
                continue;
            }

            double dist = entity.distanceTo(enemy);

            // プレイヤーは常に最優先
            if (enemy instanceof Player) {
                Player player = (Player) enemy;
                // クリエイティブ/スペクテーターは除外
                if (player.isCreative() || player.isSpectator()) {
                    continue;
                }
                return enemy;
            }

            // 最も近い敵を選択
            if (dist < nearestDistance) {
                nearestDistance = dist;
                nearestEnemy = enemy;
            }
        }

        return nearestEnemy;
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

        // 敵が16ブロック以内なら戦闘（敵対範囲）
        if (data.nearestEnemyDistance < 16.0) {
            return AIState.COMBAT;
        }

        // 敵を見失ったら探索（16ブロック以上離れたら）
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
     * 現在の状態に応じた行動を実行（プレイヤーと同じ動作を含む）
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
                return handleCombatLikePlayer(data);  // プレイヤーのような戦闘
            case RETREAT:
                return handleRetreat(data);
            case DODGE:
                return handleDodge(data);
            default:
                return new AIAction("idle");
        }
    }

    /**
     * プレイヤーのような戦闘行動
     */
    private AIAction handleCombatLikePlayer(WorldData data) {
        if (currentTarget == null || !currentTarget.isAlive()) {
            return new AIAction("idle");
        }

        double distance = data.nearestEnemyDistance;
        long currentTime = data.currentTime;
        int nearbyEnemyCount = data.nearbyEnemyCount;
        boolean isPlayerTarget = currentTarget instanceof Player;

        // 複数の敵に囲まれている場合の判定（3体以上が5ブロック以内）
        if (nearbyEnemyCount >= 3) {
            System.out.println("[ALifeAI] " + entity.getName().getString() + " は " + nearbyEnemyCount + "体の敵に囲まれている！");

            // HPが50%以下なら後退優先
            float hpPercent = data.health / data.maxHealth;
            if (hpPercent < 0.5f) {
                return handleRetreat(data);
            }

            // チャージ攻撃で複数を巻き込む（クールダウン2秒に短縮）
            if (currentTime - lastChargeTime >= 2000) {
                return executeChargeAttack(data);
            }
        }

        // 複数の敵がいる場合、ターゲット優先度を評価（プレイヤー以外の場合）
        if (nearbyEnemyCount >= 2 && !isPlayerTarget) {
            LivingEntity mostDangerous = findMostDangerousEnemy(data);
            if (mostDangerous != null && mostDangerous != currentTarget) {
                System.out.println("[ALifeAI] ターゲット変更: " + currentTarget.getName().getString() + " → " + mostDangerous.getName().getString());
                currentTarget = mostDangerous;
                entity.setTarget(mostDangerous);

                AIAction action = new AIAction("change_target");
                action.target = mostDangerous.position();
                return action;
            }
        }

        // プレイヤー相手の場合：間合い調整・回避・スキルを使用
        if (isPlayerTarget) {
            // 回避判定（プレイヤーの右クリック相当）
            if (shouldDodge(data, currentTime)) {
                return executeDodge(data);
            }

            // 武器スキル使用判定（プレイヤーの右クリック相当）
            if (shouldUseWeaponSkill(data, currentTime, distance)) {
                return executeWeaponSkill(data);
            }

            // チャージ攻撃判定（プレイヤー相手：高確率80%）
            if (shouldChargeAttack(data, currentTime, distance, true)) {
                return executeChargeAttack(data);
            }

            // 攻撃範囲外なら接近（間合い調整あり）
            if (distance > 3.0) {
                return handleCombat(data);
            }

            // 通常攻撃
            return handleCombat(data);
        }

        // Mob相手の場合：シンプルに追いかけて攻撃
        else {
            // チャージ攻撃判定（Mob相手：低確率40%）
            if (shouldChargeAttack(data, currentTime, distance, false)) {
                return executeChargeAttack(data);
            }

            // 攻撃範囲外なら速く接近（速度0.4、スプリント有効）
            if (distance > 3.0) {
                AIAction action = new AIAction("move_to_target");
                action.target = data.nearestEnemyPosition;
                action.speed = 0.1f;  // 基本速度（プレイヤーと同じ）
                // 5～15ブロック離れている場合はスプリント
                action.isSprinting = (distance > 5.0 && distance <= 15.0);
                return action;
            }

            // 通常攻撃
            return handleCombat(data);
        }
    }

    /**
     * 回避すべきかを判定
     */
    private boolean shouldDodge(WorldData data, long currentTime) {
        // クールダウン中は回避できない（2秒）
        if (currentTime - lastDodgeTime < 2000) {
            return false;
        }

        // ティアに応じた回避確率
        float dodgeChance = getTierDodgeRate(tier);

        // 敵が近すぎる場合は回避を検討
        if (data.nearestEnemyDistance < 2.0) {
            return Math.random() < dodgeChance * 0.3;
        }

        return false;
    }

    /**
     * チャージ攻撃すべきかを判定（プレイヤー vs Mob で確率が異なる）
     */
    private boolean shouldChargeAttack(WorldData data, long currentTime, double distance, boolean isPlayerTarget) {
        // katanaを持っている場合はチャージ攻撃（突き系）を使わない
        String weaponType = detectWeaponType();
        if ("katana".equals(weaponType)) {
            return false;
        }

        // チャージ攻撃のクールダウン（3秒）
        if (!hasChargeTime) {
            lastChargeTime = 0;
            hasChargeTime = true;
        }

        if (currentTime - lastChargeTime < 3000) {
            return false;
        }

        // 適切な距離（3～10ブロック）でチャージ攻撃を検討
        if (3.0 <= distance && distance <= 10.0) {
            float tacticalAwareness = getTierTacticalAwareness(tier);

            // プレイヤー相手：高確率（80%）
            if (isPlayerTarget) {
                return Math.random() < tacticalAwareness * 0.8;
            }
            // Mob相手：低確率（40%）
            else {
                return Math.random() < tacticalAwareness * 0.4;
            }
        }

        return false;
    }

    /**
     * 武器スキルを使用すべきかを判定
     */
    private boolean shouldUseWeaponSkill(WorldData data, long currentTime, double distance) {
        // スキルのクールダウン（3秒）
        if (!hasSkillTime) {
            lastSkillTime = 0;
            hasSkillTime = true;
        }

        if (currentTime - lastSkillTime < 3000) {
            return false;
        }

        // 敵が近い場合に防御スキルを使用
        if (distance < 5.0) {
            float tacticalAwareness = getTierTacticalAwareness(tier);
            return Math.random() < tacticalAwareness * 0.4;
        }

        return false;
    }

    /**
     * 回避を実行
     */
    private AIAction executeDodge(WorldData data) {
        lastDodgeTime = data.currentTime;

        AIAction action = new AIAction("dodge");

        // 敵の位置に基づいて、前方・後方・左・右のいずれかにランダムに回避
        if (data.nearestEnemyPosition != null) {
            Vec3 entityPos = entity.position();
            Vec3 enemyPos = data.nearestEnemyPosition;
            Vec3 toEnemy = enemyPos.subtract(entityPos).normalize();

            // 4方向からランダムに選択
            int direction = (int)(Math.random() * 4);
            Vec3 dodgeDirection;

            switch (direction) {
                case 0: // 後方（敵から離れる）
                    dodgeDirection = toEnemy.scale(-1.0);
                    break;
                case 1: // 前方（敵に向かう）
                    dodgeDirection = toEnemy;
                    break;
                case 2: // 左方向（敵に対して左）
                    dodgeDirection = new Vec3(-toEnemy.z, 0, toEnemy.x).normalize();
                    break;
                case 3: // 右方向（敵に対して右）
                    dodgeDirection = new Vec3(toEnemy.z, 0, -toEnemy.x).normalize();
                    break;
                default:
                    dodgeDirection = toEnemy.scale(-1.0);
                    break;
            }

            action.direction = dodgeDirection;
        } else {
            // 敵がいない場合はランダムな方向に回避
            double angle = Math.random() * Math.PI * 2;
            action.direction = new Vec3(Math.cos(angle), 0, Math.sin(angle));
        }

        action.speed = 0.8f;
        action.verticalBoost = 0.15f;
        action.distance = 0.8f;

        return action;
    }

    /**
     * チャージ攻撃を実行
     */
    private AIAction executeChargeAttack(WorldData data) {
        lastChargeTime = data.currentTime;

        AIAction action = new AIAction("charge_attack");

        // チャージ率を決定（ティアが高いほど長くチャージ）
        float tacticalAwareness = getTierTacticalAwareness(tier);
        float chargePercent = 0.5f + (tacticalAwareness * 0.5f);
        chargePercent = Math.min(chargePercent, 1.0f);

        action.chargePercent = chargePercent;
        action.damageMultiplier = 1.0f + chargePercent * 2.0f;
        action.target = data.nearestEnemyPosition;

        return action;
    }

    /**
     * 武器スキルを実行
     */
    private AIAction executeWeaponSkill(WorldData data) {
        lastSkillTime = data.currentTime;

        AIAction action = new AIAction("use_weapon_skill");
        action.skillType = "guard";  // ガードスキル
        action.duration = 5.0f;      // 5秒間

        return action;
    }

    /**
     * ティアに応じた戦術認識度を取得
     */
    private float getTierTacticalAwareness(int tier) {
        switch (tier) {
            case 1: return 0.3f;   // 一般兵
            case 2: return 0.5f;   // エリート兵
            case 3: return 0.7f;   // 特異点
            case 4: return 0.8f;   // 英雄級
            case 5: return 0.9f;   // 神話級
            case 6: return 0.95f;  // 天使級
            case 7: return 1.0f;   // 神聖級
            default: return 0.3f;
        }
    }

    private AIAction handleIdle(WorldData data) {
        return new AIAction("idle");
    }

    private AIAction handlePatrol(WorldData data) {
        // パトロール中は待機（敵が来るまで）
        return new AIAction("idle");
    }

    private AIAction handleSearch(WorldData data) {
        // 探索中も待機（敵が来るまで）
        return new AIAction("idle");
    }

    /**
     * 武器タイプを検出
     */
    private String detectWeaponType() {
        ItemStack mainHand = entity.getMainHandItem();
        if (mainHand.isEmpty()) {
            System.out.println("[ALifeAI] " + entity.getName().getString() + " の手持ちアイテムが空です");
            return "sword";
        }

        // アイテムの完全な名前を取得
        String itemName = mainHand.getItem().toString().toLowerCase();
        String displayName = mainHand.getHoverName().getString().toLowerCase();

        System.out.println("[ALifeAI] " + entity.getName().getString() + " の武器検出:");
        System.out.println("  - Item名: " + itemName);
        System.out.println("  - 表示名: " + displayName);

        // 直接的なアイテム比較を最優先（IRON_KATANAなどの特定アイテム）
        if (mainHand.getItem() == MinecraftArmorWeaponModItems.IRON_KATANA.get()) {
            System.out.println("  → 武器タイプ: katana (直接比較: IRON_KATANA)");
            return "katana";
        }

        // katanaを最優先でチェック（swordより先に）
        if (itemName.contains("katana") || displayName.contains("katana") ||
            displayName.contains("刀") || displayName.contains("カタナ")) {
            System.out.println("  → 武器タイプ: katana");
            return "katana";
        } else if (itemName.contains("spear") || displayName.contains("spear") ||
                   displayName.contains("槍") || displayName.contains("ヤリ")) {
            System.out.println("  → 武器タイプ: spear");
            return "spear";
        } else if (itemName.contains("lance") || displayName.contains("lance") ||
                   displayName.contains("ランス")) {
            System.out.println("  → 武器タイプ: lance");
            return "lance";
        } else if (itemName.contains("axe") || displayName.contains("axe") ||
                   displayName.contains("斧") || displayName.contains("アックス")) {
            System.out.println("  → 武器タイプ: axe");
            return "axe";
        } else if (itemName.contains("sword") || displayName.contains("sword") ||
                   displayName.contains("剣") || displayName.contains("ソード")) {
            System.out.println("  → 武器タイプ: sword");
            return "sword";
        }

        System.out.println("  → 武器タイプ: sword (デフォルト)");
        return "sword";
    }

    /**
     * 武器タイプに応じた攻撃パターンを取得
     */
    private String getWeaponAttackPattern(String weaponType, int combo) {
        switch (weaponType) {
            case "katana":
                switch (combo % 3) {
                    case 0: return "iai_slash";       // 居合斬り
                    case 1: return "downward_cut";    // 下段斬り
                    case 2: return "quick_slash";     // 素早い斬撃
                }
                break;
            case "sword":
                switch (combo % 3) {
                    case 0: return "slash";           // 斬撃
                    case 1: return "thrust";          // 突き
                    case 2: return "spin_slash";      // 回転斬り
                }
                break;
            case "spear":
                switch (combo % 3) {
                    case 0: return "thrust";          // 突き
                    case 1: return "sweep";           // 薙ぎ払い
                    case 2: return "pierce";          // 貫通
                }
                break;
            case "axe":
                switch (combo % 3) {
                    case 0: return "overhead_smash";  // 振り下ろし
                    case 1: return "horizontal_swing";// 横薙ぎ
                    case 2: return "cleave";          // 裂斬
                }
                break;
            case "lance":
                switch (combo % 3) {
                    case 0: return "charge_thrust";   // チャージ突き
                    case 1: return "guard_counter";   // ガードカウンター
                    case 2: return "pierce";          // 貫通
                }
                break;
        }
        return "slash";
    }

    private AIAction handleCombat(WorldData data) {
        if (currentTarget == null || !currentTarget.isAlive()) {
            return new AIAction("idle");
        }

        double distance = data.nearestEnemyDistance;

        // 攻撃範囲外なら接近（スプリント対応）
        if (distance > 3.0) {
            AIAction action = new AIAction("move_to_target");
            action.target = data.nearestEnemyPosition;
            action.speed = 0.1f;  // プレイヤーと同じ基本速度
            // 5～15ブロック離れている場合はスプリント
            action.isSprinting = (distance > 5.0 && distance <= 15.0);
            return action;
        }

        // 攻撃範囲内なら攻撃
        if (distance <= 3.0) {
            long currentTime = data.currentTime;
            // ティアに応じた攻撃速度（高ティアほど速い）
            long attackCooldown;
            if (tier == 1) {
                attackCooldown = 800;  // 0.8秒
            } else if (tier == 2) {
                attackCooldown = 700;  // 0.7秒
            } else if (tier == 3) {
                attackCooldown = 600;  // 0.6秒
            } else if (tier == 4) {
                attackCooldown = 500;  // 0.5秒
            } else {
                attackCooldown = 800;  // デフォルト
            }

            if (currentTime - lastAttackTime >= attackCooldown) {
                lastAttackTime = currentTime;
                comboCount++;
                if (comboCount > 2) {
                    comboCount = 0;
                }

                // 武器タイプを検出して適切な攻撃パターンを選択
                String weaponType = detectWeaponType();
                String attackPattern = getWeaponAttackPattern(weaponType, comboCount);

                System.out.println("[ALifeAI] " + entity.getName().getString() + " の攻撃:");
                System.out.println("  - 武器タイプ: " + weaponType);
                System.out.println("  - コンボ: " + comboCount);
                System.out.println("  - 攻撃パターン: " + attackPattern);

                AIAction action = new AIAction("attack");
                action.attackType = attackPattern;
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
            AIAction action = new AIAction("retreat");

            // 敵から遠ざかる方向を計算
            Vec3 entityPos = entity.position();
            Vec3 enemyPos = data.nearestEnemyPosition;
            Vec3 awayDirection = entityPos.subtract(enemyPos).normalize();

            action.direction = awayDirection;
            action.speed = 0.1f;  // 基本速度
            action.isSprinting = true;  // 撤退時は常にスプリント
            return action;
        }
        return new AIAction("idle");
    }

    private AIAction handleDodge(WorldData data) {
        AIAction action = new AIAction("dodge");
        action.speed = 0.6f;
        action.isSprinting = false;  // 回避中はスプリントしない
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
        List<LivingEntity> nearbyEnemies = new ArrayList<>();
        int nearbyEnemyCount = 0;
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

        // プレイヤー動作用の追加フィールド
        public Vec3 direction;          // 回避方向
        public float verticalBoost;     // 上方向の加速
        public float distance;          // 回避距離
        public float chargePercent;     // チャージ率
        public float damageMultiplier;  // ダメージ倍率
        public String skillType;        // スキルタイプ
        public float duration;          // 持続時間
        public boolean isSprinting;     // スプリント状態

        public AIAction(String action) {
            this.action = action;
        }
    }
}

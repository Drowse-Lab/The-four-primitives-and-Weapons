"""
ALife-based AI System for Player-like Mob Behavior
プレイヤーっぽい動きをするMob用のALife（人工生命）ベースAIシステム

このシステムは以下の要素を含みます：
- 状態機械（State Machine）
- 意思決定システム（Decision Making）
- 行動選択（Behavior Selection）
- 学習と適応（Learning and Adaptation）
"""

import random
import math
from enum import Enum
from typing import Dict, List, Tuple, Optional


class AIState(Enum):
    """AI状態"""
    IDLE = "idle"              # 待機
    PATROL = "patrol"          # 巡回
    SEARCH = "search"          # 探索
    COMBAT = "combat"          # 戦闘
    RETREAT = "retreat"        # 撤退
    DODGE = "dodge"            # 回避
    HEAL = "heal"              # 回復
    REPOSITION = "reposition"  # 位置調整


class WeaponType(Enum):
    """武器タイプ"""
    MELEE = "melee"      # 近接武器
    RANGED = "ranged"    # 遠距離武器
    MAGIC = "magic"      # 魔法武器
    SHIELD = "shield"    # 盾


class TacticalDecision:
    """戦術的決定"""
    def __init__(self, action: str, priority: float, reason: str):
        self.action = action
        self.priority = priority
        self.reason = reason


class BaseALifeAI:
    """
    ALifeベースAIの基本クラス

    各ティアのAIはこのクラスを継承して実装します
    """

    def __init__(self, tier: int, entity_data: Dict):
        self.tier = tier
        self.entity_data = entity_data

        # 状態管理
        self.current_state = AIState.IDLE
        self.previous_state = AIState.IDLE
        self.state_duration = 0

        # 戦闘データ
        self.target = None
        self.last_attack_time = 0
        self.last_dodge_time = 0
        self.health_threshold = 0.3  # HP30%以下で撤退を考慮

        # 武器管理
        self.current_weapon = WeaponType.MELEE
        self.weapon_switch_cooldown = 0

        # 記憶と学習
        self.enemy_patterns = {}  # 敵の行動パターンを記憶
        self.damage_received = []  # 受けたダメージの履歴
        self.successful_dodges = 0
        self.failed_dodges = 0

        # ティア依存パラメータ
        self.reaction_speed = self._get_reaction_speed()
        self.tactical_awareness = self._get_tactical_awareness()
        self.learning_rate = self._get_learning_rate()

    def _get_reaction_speed(self) -> float:
        """ティアに応じた反応速度を取得"""
        speeds = {
            343014: 1.0,   # tester0: 普通
            1: 1.0,   # 一般兵: 普通
            2: 0.8,   # エリート兵: 少し速い
            3: 0.6,   # 特異点: かなり速い
            4: 0.5,   # 英雄級: 非常に速い
            5: 0.3,   # 神話級: 超速
            6: 0.2,   # 天使級: 極速
            7: 0.1    # 神級: 光速
        }
        return speeds.get(self.tier, 1.0)

    def _get_tactical_awareness(self) -> float:
        """ティアに応じた戦術認識度を取得 (0.0-1.0)"""
        awareness = {
            343014: 0.3,   # 一般兵: 基本的な戦術のみ
            1: 0.3,   # 一般兵: 基本的な戦術のみ
            2: 0.5,   # エリート兵: 中級戦術
            3: 0.7,   # 特異点: 高度な戦術
            4: 0.8,   # 英雄級: 非常に高度
            5: 0.9,   # 神話級: プロレベル
            6: 0.95,  # 天使級: 超人レベル
            7: 1.0    # 神聖級: 完璧
        }
        return awareness.get(self.tier, 0.3)

    def _get_learning_rate(self) -> float:
        """ティアに応じた学習速度を取得"""
        rates = {
            343014: 1.0    # 神聖級: 即座に学習
            1: 0.1,   # 一般兵: 遅い学習
            2: 0.2,   # エリート兵: 少し速い
            3: 0.4,   # 特異点: 速い
            4: 0.6,   # 英雄級: かなり速い
            5: 0.8,   # 神話級: 非常に速い
            6: 0.9,   # 天使級: 超速
            7: 1.0    # 神聖級: 即座に学習
        }
        return rates.get(self.tier, 0.1)

    def update(self, delta_time: float, world_data: Dict) -> Dict:
        """
        AIを更新（毎ティック呼ばれる）

        Args:
            delta_time: 前回の更新からの経過時間
            world_data: ワールド情報（近くの敵、地形など）

        Returns:
            実行する行動のデータ
        """
        self.state_duration += delta_time

        # 状態遷移の判定
        new_state = self._evaluate_state_transition(world_data)
        if new_state != self.current_state:
            self._change_state(new_state)

        # 現在の状態に応じた行動を選択
        action = self._execute_current_state(world_data)

        # 学習と適応
        self._learn_from_experience(world_data)

        return action

    def _evaluate_state_transition(self, world_data: Dict) -> AIState:
        """状態遷移を評価"""
        # 基本実装（サブクラスでオーバーライド可能）
        current_health_ratio = self.entity_data.get("health", 1.0) / self.entity_data.get("max_health", 1.0)

        # 瀕死なら撤退
        if current_health_ratio < self.health_threshold:
            return AIState.RETREAT

        # 敵が近くにいれば戦闘
        if world_data.get("nearest_enemy_distance", float('inf')) < 16.0:
            return AIState.COMBAT

        # デフォルトは巡回
        return AIState.PATROL

    def _change_state(self, new_state: AIState):
        """状態を変更"""
        self.previous_state = self.current_state
        self.current_state = new_state
        self.state_duration = 0
        print(f"[AI Tier {self.tier}] State changed: {self.previous_state.value} -> {new_state.value}")

    def _execute_current_state(self, world_data: Dict) -> Dict:
        """現在の状態に応じた行動を実行"""
        state_handlers = {
            AIState.IDLE: self._handle_idle,
            AIState.PATROL: self._handle_patrol,
            AIState.SEARCH: self._handle_search,
            AIState.COMBAT: self._handle_combat,
            AIState.RETREAT: self._handle_retreat,
            AIState.DODGE: self._handle_dodge,
            AIState.HEAL: self._handle_heal,
            AIState.REPOSITION: self._handle_reposition,
        }

        handler = state_handlers.get(self.current_state, self._handle_idle)
        return handler(world_data)

    def _handle_idle(self, world_data: Dict) -> Dict:
        """待機状態の処理"""
        return {"action": "idle", "data": {}}

    def _handle_patrol(self, world_data: Dict) -> Dict:
        """巡回状態の処理"""
        return {"action": "patrol", "data": {"speed": 0.3}}

    def _handle_search(self, world_data: Dict) -> Dict:
        """探索状態の処理"""
        return {"action": "search", "data": {}}

    def _handle_combat(self, world_data: Dict) -> Dict:
        """
        戦闘状態の処理

        プレイヤーのような戦闘行動を実行:
        - 回避判定（右クリック相当）
        - チャージ攻撃判定（左クリック長押し相当）
        - 通常攻撃
        """
        enemy_distance = world_data.get("nearest_enemy_distance", float('inf'))
        current_time = world_data.get("current_time", 0)

        # 回避判定（危険な攻撃が来ている場合）
        if self.should_dodge(world_data):
            return self._execute_dodge(world_data)

        # チャージ攻撃判定（適切な距離とタイミングの場合）
        if self.should_charge_attack(world_data):
            return self._execute_charge_attack(world_data)

        # 武器スキル使用判定（右クリック相当）
        if self.should_use_weapon_skill(world_data):
            return self._execute_weapon_skill(world_data)

        # 攻撃範囲外なら接近
        if enemy_distance > 3.0:
            return self._move_towards_target(world_data)

        # 攻撃範囲内なら通常攻撃
        if enemy_distance <= 3.0 and current_time - self.last_attack_time >= 1.5:
            self.last_attack_time = current_time
            return self._execute_normal_attack(world_data)

        return {"action": "idle", "data": {}}

    def _handle_retreat(self, world_data: Dict) -> Dict:
        """撤退状態の処理"""
        enemy_pos = world_data.get("nearest_enemy_position", (0, 0, 0))
        return {
            "action": "move_away",
            "data": {
                "target": enemy_pos,
                "speed": 0.5
            }
        }

    def _handle_dodge(self, world_data: Dict) -> Dict:
        """回避状態の処理"""
        dodge_direction = self._calculate_dodge_direction(world_data)
        return {
            "action": "dodge",
            "data": {
                "direction": dodge_direction,
                "speed": 0.8
            }
        }

    def _execute_dodge(self, world_data: Dict) -> Dict:
        """
        回避を実行（プレイヤーの右クリック動作を模倣）

        プレイヤーの回避と同じ:
        - 移動方向に基づいて回避
        - 上方向に少し浮く
        - クールダウンを設定
        """
        current_time = world_data.get("current_time", 0)
        self.last_dodge_time = current_time
        self.successful_dodges += 1

        # 回避方向を計算
        dodge_direction = self._calculate_dodge_direction(world_data)

        return {
            "action": "dodge",
            "data": {
                "direction": dodge_direction,
                "speed": 0.8,  # プレイヤーと同じ速度
                "vertical_boost": 0.15,  # 上方向の加速（プレイヤーと同じ）
                "distance": 0.8,  # 回避距離
                "fall_damage_immunity": 1.5  # 1.5秒間落下ダメージ無効
            }
        }

    def _execute_charge_attack(self, world_data: Dict) -> Dict:
        """
        チャージ攻撃を実行（プレイヤーの左クリック長押しを模倣）

        チャージ時間は戦術認識度に応じて決定:
        - 低ティア: 短いチャージ（50%）
        - 高ティア: 最大チャージ（100%）
        """
        current_time = world_data.get("current_time", 0)
        if not hasattr(self, 'last_charge_time'):
            self.last_charge_time = 0
        self.last_charge_time = current_time

        # チャージ率を決定（ティアが高いほど長くチャージ）
        charge_percent = 0.5 + (self.tactical_awareness * 0.5)
        charge_percent = min(charge_percent, 1.0)

        return {
            "action": "charge_attack",
            "data": {
                "charge_percent": charge_percent,
                "damage_multiplier": 1.0 + charge_percent * 2.0,  # チャージ率に応じた倍率
                "cooldown": 1.0 + charge_percent  # チャージ率に応じたクールダウン
            }
        }

    def _execute_weapon_skill(self, world_data: Dict) -> Dict:
        """
        武器スキルを使用（プレイヤーの右クリックを模倣）

        ReplicaSwordOfLightなどの固有スキル
        """
        current_time = world_data.get("current_time", 0)
        if not hasattr(self, 'last_skill_time'):
            self.last_skill_time = 0
        self.last_skill_time = current_time

        return {
            "action": "use_weapon_skill",
            "data": {
                "skill_type": "guard",  # ガードスキル（ReplicaSwordOfLight）
                "duration": 5.0  # 5秒間
            }
        }

    def _execute_normal_attack(self, world_data: Dict) -> Dict:
        """
        通常攻撃を実行（プレイヤーの左クリックを模倣）
        """
        return {
            "action": "attack",
            "data": {
                "type": "normal",
                "weapon": self.current_weapon.value,
                "damage_multiplier": 1.0
            }
        }

    def _move_towards_target(self, world_data: Dict) -> Dict:
        """ターゲットに向かって移動"""
        enemy_pos = world_data.get("nearest_enemy_position", None)
        if enemy_pos is None:
            return {"action": "idle", "data": {}}

        return {
            "action": "move_to_target",
            "data": {
                "target": enemy_pos,
                "speed": 0.3
            }
        }

    def _handle_heal(self, world_data: Dict) -> Dict:
        """回復状態の処理"""
        return {"action": "use_healing_item", "data": {}}

    def _handle_reposition(self, world_data: Dict) -> Dict:
        """位置調整状態の処理"""
        optimal_pos = self._calculate_optimal_position(world_data)
        return {
            "action": "move_to",
            "data": {
                "target": optimal_pos,
                "speed": 0.4
            }
        }

    def _calculate_dodge_direction(self, world_data: Dict) -> Tuple[float, float, float]:
        """回避方向を計算"""
        # ランダムな横方向への回避
        angle = random.uniform(0, 2 * math.pi)
        return (math.cos(angle), 0, math.sin(angle))

    def _calculate_optimal_position(self, world_data: Dict) -> Tuple[float, float, float]:
        """最適な位置を計算"""
        # 基本実装: 敵から適切な距離を保つ
        enemy_pos = world_data.get("nearest_enemy_position", (0, 0, 0))
        current_pos = self.entity_data.get("position", (0, 0, 0))

        # ベクトル計算
        dx = current_pos[0] - enemy_pos[0]
        dz = current_pos[2] - enemy_pos[2]
        distance = math.sqrt(dx*dx + dz*dz)

        if distance > 0:
            # 適切な距離（5ブロック）を保つ
            optimal_distance = 5.0
            ratio = optimal_distance / distance
            return (
                enemy_pos[0] + dx * ratio,
                current_pos[1],
                enemy_pos[2] + dz * ratio
            )

        return current_pos

    def _learn_from_experience(self, world_data: Dict):
        """経験から学習"""
        # ダメージを受けた場合の学習
        if "damage_received" in world_data:
            self.damage_received.append({
                "amount": world_data["damage_received"],
                "time": self.state_duration,
                "state": self.current_state
            })

            # 回避率の調整
            if self.current_state == AIState.DODGE:
                self.failed_dodges += 1
            else:
                # 次回は回避を試みる確率を上げる
                self.health_threshold += 0.05 * self.learning_rate

    def should_dodge(self, world_data: Dict) -> bool:
        """
        回避すべきかを判定（プレイヤーの右クリック相当）

        判定基準:
        - 敵の攻撃が来ている
        - 回避のクールダウンが終わっている
        - ティアに応じた成功率
        """
        current_time = world_data.get("current_time", 0)

        # クールダウン中は回避できない（プレイヤーと同じく2秒）
        dodge_cooldown = 2.0
        if current_time - self.last_dodge_time < dodge_cooldown:
            return False

        # ティアが高いほど、より確実に回避する
        dodge_chance = self.tactical_awareness

        # 敵の攻撃を検知
        if world_data.get("incoming_attack", False):
            return random.random() < dodge_chance

        # 敵が近すぎる場合も回避を検討（予防的回避）
        enemy_distance = world_data.get("nearest_enemy_distance", float('inf'))
        if enemy_distance < 2.0 and random.random() < dodge_chance * 0.3:
            return True

        return False

    def should_charge_attack(self, world_data: Dict) -> bool:
        """
        チャージ攻撃すべきかを判定（プレイヤーの左クリック長押し相当）

        判定基準:
        - 敵が適切な距離にいる
        - チャージ攻撃のクールダウンが終わっている
        - 戦術認識度が高い
        """
        current_time = world_data.get("current_time", 0)
        enemy_distance = world_data.get("nearest_enemy_distance", float('inf'))

        # チャージ攻撃のクールダウン（プレイヤーと同じく1～2秒）
        charge_cooldown = 2.0
        if not hasattr(self, 'last_charge_time'):
            self.last_charge_time = 0

        if current_time - self.last_charge_time < charge_cooldown:
            return False

        # 適切な距離（3～7ブロック）で高確率でチャージ
        if 3.0 <= enemy_distance <= 7.0:
            charge_chance = self.tactical_awareness * 0.6
            return random.random() < charge_chance

        # 近すぎる場合は低確率
        if enemy_distance < 3.0 and random.random() < self.tactical_awareness * 0.2:
            return True

        return False

    def should_use_weapon_skill(self, world_data: Dict) -> bool:
        """
        武器スキルを使用すべきかを判定（プレイヤーの右クリック相当）

        武器固有のスキル（ReplicaSwordOfLightのガードなど）
        """
        current_time = world_data.get("current_time", 0)

        # スキルのクールダウン
        skill_cooldown = 3.0
        if not hasattr(self, 'last_skill_time'):
            self.last_skill_time = 0

        if current_time - self.last_skill_time < skill_cooldown:
            return False

        # 敵が近い場合に防御スキルを使用
        enemy_distance = world_data.get("nearest_enemy_distance", float('inf'))
        if enemy_distance < 5.0 and random.random() < self.tactical_awareness * 0.4:
            return True

        return False

    def select_weapon(self, world_data: Dict) -> WeaponType:
        """状況に応じて武器を選択"""
        enemy_distance = world_data.get("nearest_enemy_distance", float('inf'))

        # 距離に応じて武器を切り替え
        if enemy_distance < 3.0:
            return WeaponType.MELEE
        elif enemy_distance < 10.0:
            return WeaponType.RANGED
        else:
            return WeaponType.MAGIC

    def get_tactical_decisions(self, world_data: Dict) -> List[TacticalDecision]:
        """戦術的決定のリストを取得"""
        decisions = []

        # 各種行動の優先度を計算
        enemy_distance = world_data.get("nearest_enemy_distance", float('inf'))
        health_ratio = self.entity_data.get("health", 1.0) / self.entity_data.get("max_health", 1.0)

        # 攻撃
        if enemy_distance < 16.0:
            priority = (1.0 - enemy_distance / 16.0) * self.tactical_awareness
            decisions.append(TacticalDecision("attack", priority, "Enemy in range"))

        # 回避
        if world_data.get("incoming_attack", False):
            priority = 0.9 * self.tactical_awareness
            decisions.append(TacticalDecision("dodge", priority, "Incoming attack detected"))

        # 撤退
        if health_ratio < self.health_threshold:
            priority = (1.0 - health_ratio) * 0.8
            decisions.append(TacticalDecision("retreat", priority, "Low health"))

        # 位置調整
        if 3.0 < enemy_distance < 5.0:
            priority = 0.4 * self.tactical_awareness
            decisions.append(TacticalDecision("reposition", priority, "Maintain optimal distance"))

        # 優先度順にソート
        decisions.sort(key=lambda d: d.priority, reverse=True)

        return decisions

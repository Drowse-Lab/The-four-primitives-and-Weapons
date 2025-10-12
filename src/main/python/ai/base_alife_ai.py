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
            1: 1.0,   # 一般兵: 普通
            2: 0.8,   # エリート兵: 少し速い
            3: 0.6,   # 特異点: かなり速い
            4: 0.5,   # 英雄級: 非常に速い
            5: 0.3,   # 神話級: 超速
            6: 0.2,   # 天使級: 極速
            7: 0.1    # 神聖級: 光速
        }
        return speeds.get(self.tier, 1.0)

    def _get_tactical_awareness(self) -> float:
        """ティアに応じた戦術認識度を取得 (0.0-1.0)"""
        awareness = {
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
        """戦闘状態の処理（サブクラスで実装）"""
        raise NotImplementedError("Combat behavior must be implemented in subclass")

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
        """回避すべきかを判定"""
        # ティアが高いほど、より確実に回避する
        dodge_chance = self.tactical_awareness

        # 敵の攻撃を検知
        if world_data.get("incoming_attack", False):
            return random.random() < dodge_chance

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

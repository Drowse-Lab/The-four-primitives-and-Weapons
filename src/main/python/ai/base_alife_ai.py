"""
Base A-Life AI - すべてのAIの基底クラス

A-Life (Artificial Life) システムの基本機能を提供します。
"""

from enum import Enum
from typing import Dict, Tuple, Optional
import random


class AIState(Enum):
    """AI状態"""
    IDLE = "idle"
    PATROL = "patrol"
    SEARCH = "search"
    COMBAT = "combat"
    RETREAT = "retreat"
    DODGE = "dodge"
    CHARGING = "charging"
    SKILL_USE = "skill_use"


class WeaponType(Enum):
    """武器タイプ"""
    MELEE = "melee"
    RANGED = "ranged"
    MAGIC = "magic"
    HYBRID = "hybrid"


class TacticalDecision(Enum):
    """戦術判断"""
    AGGRESSIVE = "aggressive"
    DEFENSIVE = "defensive"
    BALANCED = "balanced"
    RETREAT = "retreat"


class BaseALifeAI:
    """
    A-Life AIの基底クラス

    すべてのティアのAIはこのクラスを継承します。
    """

    def __init__(self, tier: int, entity_data: Dict):
        self.tier = tier
        self.entity_data = entity_data

        # 基本パラメータ
        self.current_state = AIState.IDLE
        self.previous_state = AIState.IDLE

        # 戦闘データ
        self.last_attack_time = 0.0
        self.last_dodge_time = 0.0
        self.last_skill_time = 0.0

        # 統計データ
        self.successful_dodges = 0
        self.failed_dodges = 0
        self.total_damage_dealt = 0.0
        self.total_damage_taken = 0.0

        # 学習データ
        self.enemy_patterns = {}
        self.combat_experience = 0

        # 共通設定
        self.health_threshold = 0.3  # HP30%以下で撤退
        self.vision_range = 16.0     # 視界範囲

    def update(self, delta_time: float, world_data: Dict) -> Dict:
        """
        AIを更新

        Args:
            delta_time: 前回の更新からの経過時間（秒）
            world_data: ワールド情報

        Returns:
            実行するアクションの辞書
        """
        # 状態遷移を評価
        new_state = self._evaluate_state_transition(world_data)
        if new_state != self.current_state:
            self._change_state(new_state)

        # 現在の状態に応じた行動を実行
        action = self._execute_current_state(world_data)

        return action

    def _evaluate_state_transition(self, world_data: Dict) -> AIState:
        """
        状態遷移を評価

        サブクラスでオーバーライドしてカスタマイズします。
        """
        current_health_ratio = self.entity_data.get("health", 1.0) / self.entity_data.get("max_health", 1.0)
        enemy_distance = world_data.get("nearest_enemy_distance", float('inf'))

        # 瀕死なら撤退
        if current_health_ratio < self.health_threshold:
            return AIState.RETREAT

        # 敵が視界内なら戦闘
        if enemy_distance < self.vision_range:
            return AIState.COMBAT

        # 敵を見失ったら探索
        if self.previous_state == AIState.COMBAT and enemy_distance > self.vision_range:
            return AIState.SEARCH

        # デフォルトは巡回
        return AIState.PATROL

    def _change_state(self, new_state: AIState):
        """状態を変更"""
        self.previous_state = self.current_state
        self.current_state = new_state

    def _execute_current_state(self, world_data: Dict) -> Dict:
        """現在の状態に応じた行動を実行"""
        if self.current_state == AIState.IDLE:
            return self._handle_idle(world_data)
        elif self.current_state == AIState.PATROL:
            return self._handle_patrol(world_data)
        elif self.current_state == AIState.SEARCH:
            return self._handle_search(world_data)
        elif self.current_state == AIState.COMBAT:
            return self._handle_combat(world_data)
        elif self.current_state == AIState.RETREAT:
            return self._handle_retreat(world_data)
        elif self.current_state == AIState.DODGE:
            return self._handle_dodge(world_data)
        else:
            return {"action": "idle", "data": {}}

    # ========================================
    # 状態ハンドラー（サブクラスでオーバーライド）
    # ========================================

    def _handle_idle(self, world_data: Dict) -> Dict:
        """待機状態"""
        return {"action": "idle", "data": {}}

    def _handle_patrol(self, world_data: Dict) -> Dict:
        """巡回状態"""
        return {
            "action": "move",
            "data": {
                "speed": 0.3,
                "direction": (random.random() * 2 - 1, 0, random.random() * 2 - 1)
            }
        }

    def _handle_search(self, world_data: Dict) -> Dict:
        """探索状態"""
        return {
            "action": "search",
            "data": {
                "speed": 0.4
            }
        }

    def _handle_combat(self, world_data: Dict) -> Dict:
        """戦闘状態（サブクラスで必ずオーバーライド）"""
        raise NotImplementedError("Subclass must implement _handle_combat")

    def _handle_retreat(self, world_data: Dict) -> Dict:
        """撤退状態"""
        enemy_pos = world_data.get("nearest_enemy_position", None)
        if enemy_pos is not None:
            current_pos = self.entity_data.get("position", (0, 0, 0))

            # 敵から離れる方向を計算
            dx = current_pos[0] - enemy_pos[0]
            dz = current_pos[2] - enemy_pos[2]

            # 正規化
            import math
            distance = math.sqrt(dx*dx + dz*dz)
            if distance > 0:
                dx /= distance
                dz /= distance

            return {
                "action": "move",
                "data": {
                    "direction": (dx, 0, dz),
                    "speed": 0.5
                }
            }

        return {"action": "idle", "data": {}}

    def _handle_dodge(self, world_data: Dict) -> Dict:
        """回避状態"""
        return {
            "action": "dodge",
            "data": {
                "speed": 0.6
            }
        }

    # ========================================
    # ユーティリティメソッド
    # ========================================

    def should_dodge(self, world_data: Dict) -> bool:
        """
        回避すべきかを判定

        サブクラスでオーバーライドしてカスタマイズします。
        """
        incoming_attack = world_data.get("incoming_attack", False)
        return incoming_attack

    def calculate_tactical_decision(self, world_data: Dict) -> TacticalDecision:
        """
        戦術判断を計算

        HP、敵の強さ、周囲の状況から最適な戦術を決定
        """
        current_health_ratio = self.entity_data.get("health", 1.0) / self.entity_data.get("max_health", 1.0)

        if current_health_ratio < 0.3:
            return TacticalDecision.RETREAT
        elif current_health_ratio < 0.5:
            return TacticalDecision.DEFENSIVE
        elif current_health_ratio > 0.8:
            return TacticalDecision.AGGRESSIVE
        else:
            return TacticalDecision.BALANCED

    def get_combat_style(self) -> str:
        """戦闘スタイルを取得（サブクラスで実装）"""
        return "balanced"

    def get_skill_level(self) -> Dict:
        """
        スキルレベルを取得（サブクラスで実装）

        Returns:
            スキルレベルの辞書 (0.0 ~ 1.0)
        """
        return {
            "melee": 0.5,
            "ranged": 0.5,
            "magic": 0.5,
            "defense": 0.5,
            "dodge": 0.5,
            "tactics": 0.5,
            "learning": 0.5
        }

    # ========================================
    # 統計・学習
    # ========================================

    def record_damage_dealt(self, damage: float):
        """与えたダメージを記録"""
        self.total_damage_dealt += damage

    def record_damage_taken(self, damage: float):
        """受けたダメージを記録"""
        self.total_damage_taken += damage

    def record_dodge_success(self):
        """回避成功を記録"""
        self.successful_dodges += 1

    def record_dodge_failure(self):
        """回避失敗を記録"""
        self.failed_dodges += 1

    def get_statistics(self) -> Dict:
        """統計データを取得"""
        total_dodges = self.successful_dodges + self.failed_dodges
        dodge_rate = self.successful_dodges / total_dodges if total_dodges > 0 else 0.0

        return {
            "tier": self.tier,
            "state": self.current_state.value,
            "damage_dealt": self.total_damage_dealt,
            "damage_taken": self.total_damage_taken,
            "dodge_success": self.successful_dodges,
            "dodge_failed": self.failed_dodges,
            "dodge_rate": dodge_rate,
            "combat_experience": self.combat_experience
        }

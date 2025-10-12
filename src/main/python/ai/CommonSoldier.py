"""
CommonSoldier AI - 一般兵（ティア1）

基本的な戦闘能力を持つAI
- 基本的な近接戦闘
- 簡単な回避行動
- 単純な戦術判断
"""

# 1

import random
import math
from typing import Dict, Tuple
from base_alife_ai import BaseALifeAI, AIState, WeaponType, TacticalDecision


class CommonSoldierAI(BaseALifeAI):
    """
    一般兵のAI（ティア1）

    特徴:
    - 基本的な近接戦闘が中心
    - 回避は遅く、時々失敗する
    - 単純な攻撃パターン
    - 学習速度は遅い
    """

    def __init__(self, entity_data: Dict):
        super().__init__(tier=1, entity_data=entity_data)

        # 一般兵固有のパラメータ
        self.attack_range = 3.0        # 攻撃範囲
        self.attack_cooldown = 1.5     # 攻撃クールダウン（秒）
        self.dodge_success_rate = 0.3  # 回避成功率30%
        self.combo_max = 2             # 最大コンボ数
        self.current_combo = 0

        # 行動パターン
        self.preferred_weapon = WeaponType.MELEE
        self.can_use_ranged = False    # 遠距離攻撃は使えない

    def _handle_combat(self, world_data: Dict) -> Dict:
        """
        戦闘状態の処理

        一般兵の戦闘パターン:
        1. 敵に近づく
        2. 攻撃範囲に入ったら攻撃
        3. ダメージを受けたら低確率で回避
        4. HPが低くなったら撤退
        """
        enemy_pos = world_data.get("nearest_enemy_position", None)
        enemy_distance = world_data.get("nearest_enemy_distance", float('inf'))
        current_pos = self.entity_data.get("position", (0, 0, 0))

        # 回避判定（敵の攻撃が来たら）
        if self.should_dodge(world_data):
            return self._execute_dodge(world_data)

        # 攻撃範囲外なら接近
        if enemy_distance > self.attack_range:
            return self._move_towards_enemy(enemy_pos, current_pos)

        # 攻撃範囲内なら攻撃
        if enemy_distance <= self.attack_range:
            return self._execute_attack(world_data)

        # デフォルト: 待機
        return {"action": "idle", "data": {}}

    def _move_towards_enemy(self, enemy_pos: Tuple[float, float, float],
                           current_pos: Tuple[float, float, float]) -> Dict:
        """敵に向かって移動"""
        # 方向を計算
        dx = enemy_pos[0] - current_pos[0]
        dz = enemy_pos[2] - current_pos[2]
        distance = math.sqrt(dx*dx + dz*dz)

        if distance > 0:
            # 正規化して移動ベクトルを作成
            move_x = dx / distance
            move_z = dz / distance

            return {
                "action": "move",
                "data": {
                    "direction": (move_x, 0, move_z),
                    "speed": 0.3,  # 一般兵の移動速度は遅め
                    "look_at": enemy_pos
                }
            }

        return {"action": "idle", "data": {}}

    def _execute_attack(self, world_data: Dict) -> Dict:
        """攻撃を実行"""
        current_time = world_data.get("current_time", 0)

        # クールダウン中かチェック
        if current_time - self.last_attack_time < self.attack_cooldown:
            # クールダウン中は軽く移動
            return self._strafe_around_enemy(world_data)

        # 攻撃実行
        self.last_attack_time = current_time
        self.current_combo += 1

        # コンボリセット判定
        if self.current_combo > self.combo_max:
            self.current_combo = 0

        # 基本的な近接攻撃
        attack_type = "slash" if self.current_combo == 1 else "thrust"

        return {
            "action": "attack",
            "data": {
                "type": attack_type,
                "weapon": self.preferred_weapon.value,
                "combo": self.current_combo,
                "damage_multiplier": 1.0  # 一般兵は等倍ダメージ
            }
        }

    def _execute_dodge(self, world_data: Dict) -> Dict:
        """回避を実行"""
        current_time = world_data.get("current_time", 0)

        # 回避のクールダウンチェック
        dodge_cooldown = 2.0  # 2秒
        if current_time - self.last_dodge_time < dodge_cooldown:
            return {"action": "idle", "data": {}}

        # 回避成功判定
        if random.random() > self.dodge_success_rate:
            # 失敗: そのまま攻撃を受ける
            self.failed_dodges += 1
            return {"action": "idle", "data": {}}

        # 成功: 回避実行
        self.last_dodge_time = current_time
        self.successful_dodges += 1

        # ランダムな横方向に回避
        dodge_angle = random.choice([-90, 90])  # 左右どちらかに90度
        enemy_pos = world_data.get("nearest_enemy_position", (0, 0, 0))
        current_pos = self.entity_data.get("position", (0, 0, 0))

        # 敵への方向ベクトル
        dx = enemy_pos[0] - current_pos[0]
        dz = enemy_pos[2] - current_pos[2]

        # 回避方向を計算（敵への方向から90度回転）
        angle_rad = math.radians(dodge_angle)
        dodge_x = dx * math.cos(angle_rad) - dz * math.sin(angle_rad)
        dodge_z = dx * math.sin(angle_rad) + dz * math.cos(angle_rad)

        # 正規化
        distance = math.sqrt(dodge_x*dodge_x + dodge_z*dodge_z)
        if distance > 0:
            dodge_x /= distance
            dodge_z /= distance

        return {
            "action": "dodge",
            "data": {
                "direction": (dodge_x, 0, dodge_z),
                "distance": 2.0,  # 2ブロック回避
                "speed": 0.6
            }
        }

    def _strafe_around_enemy(self, world_data: Dict) -> Dict:
        """敵の周囲を移動（攻撃待機中）"""
        enemy_pos = world_data.get("nearest_enemy_position", (0, 0, 0))
        current_pos = self.entity_data.get("position", (0, 0, 0))

        # 円周上を移動
        dx = current_pos[0] - enemy_pos[0]
        dz = current_pos[2] - enemy_pos[2]

        # 接線方向に移動（反時計回り）
        strafe_x = -dz
        strafe_z = dx

        # 正規化
        distance = math.sqrt(strafe_x*strafe_x + strafe_z*strafe_z)
        if distance > 0:
            strafe_x /= distance
            strafe_z /= distance

        return {
            "action": "move",
            "data": {
                "direction": (strafe_x, 0, strafe_z),
                "speed": 0.2,  # ゆっくり移動
                "look_at": enemy_pos
            }
        }

    def should_dodge(self, world_data: Dict) -> bool:
        """
        回避すべきかを判定

        一般兵は:
        - 反応が遅い
        - 回避成功率が低い
        - 明らかな攻撃予兆のみ反応
        """
        # 基本判定
        if not super().should_dodge(world_data):
            return False

        # 敵の攻撃の種類によって判定
        attack_type = world_data.get("incoming_attack_type", None)

        # 一般兵は遅い攻撃にしか反応できない
        if attack_type in ["charged_attack", "heavy_attack"]:
            # 遅い攻撃には反応できる
            return random.random() < self.dodge_success_rate

        # 速い攻撃には反応できない
        return False

    def _evaluate_state_transition(self, world_data: Dict) -> AIState:
        """
        状態遷移を評価

        一般兵の状態遷移ロジック:
        - シンプルで予測可能
        - HP閾値で撤退
        """
        current_health_ratio = self.entity_data.get("health", 1.0) / self.entity_data.get("max_health", 1.0)
        enemy_distance = world_data.get("nearest_enemy_distance", float('inf'))

        # 瀕死なら撤退（HP30%以下）
        if current_health_ratio < self.health_threshold:
            return AIState.RETREAT

        # 敵が視界内（16ブロック以内）なら戦闘
        if enemy_distance < 16.0:
            return AIState.COMBAT

        # 敵を見失ったら探索
        if self.previous_state == AIState.COMBAT and enemy_distance > 16.0:
            return AIState.SEARCH

        # 何もなければ巡回
        return AIState.PATROL

    def get_combat_style(self) -> str:
        """戦闘スタイルを取得"""
        return "aggressive_melee"  # 一般兵は積極的な近接戦闘

    def get_skill_level(self) -> Dict:
        """スキルレベルを取得"""
        return {
            "melee": 0.3,      # 近接戦闘: 30%
            "ranged": 0.0,     # 遠距離戦闘: 不可
            "magic": 0.0,      # 魔法: 不可
            "defense": 0.2,    # 防御: 20%
            "dodge": 0.3,      # 回避: 30%
            "tactics": 0.2,    # 戦術: 20%
            "learning": 0.1    # 学習: 10%
        }


# テスト用のエントリーポイント
def create_ai(entity_data: Dict) -> CommonSoldierAI:
    """一般兵のAIインスタンスを作成"""
    return CommonSoldierAI(entity_data)


# デバッグ用
if __name__ == "__main__":
    # テストデータ
    test_entity = {
        "health": 20.0,
        "max_health": 20.0,
        "position": (0, 0, 0)
    }

    test_world = {
        "nearest_enemy_position": (5, 0, 0),
        "nearest_enemy_distance": 5.0,
        "current_time": 0,
        "incoming_attack": False
    }

    # AIインスタンス作成
    ai = create_ai(test_entity)

    # 更新テスト
    for i in range(10):
        action = ai.update(0.05, test_world)  # 50ms毎に更新
        print(f"Tick {i}: State={ai.current_state.value}, Action={action['action']}")

        # 時間を進める
        test_world["current_time"] += 0.05

"""
PlayerStyleBase - プレイヤースタイル戦闘の共通ベースクラス

すべてのティアで共通の、プレイヤーと同じ戦闘スタイルを提供
"""

import random
import math
from typing import Dict, Tuple, Optional
from base_alife_ai import BaseALifeAI, AIState


class PlayerStyleBaseAI(BaseALifeAI):
    """
    プレイヤースタイル戦闘の基底クラス

    共通機能:
    - 武器と鞘の検出
    - ダッシュ攻撃、チャージ攻撃、通常攻撃
    - 武器タイプ別の攻撃パターン
    - コンボシステム
    """

    def __init__(self, tier: int, entity_data: Dict):
        super().__init__(tier, entity_data)

        # 装備情報
        self.current_weapon = None
        self.has_sheath = False
        self.weapon_type = "sword"

        # 装備データ（防具+エンチャント）
        self.equipment_data = self._initialize_equipment()

        # スプリント（ダッシュ移動）
        self.is_sprinting = False
        self.sprint_duration = 0.0
        self.max_sprint_duration = 5.0  # 最大5秒間スプリント可能
        self.sprint_cooldown = 3.0
        self.last_sprint_time = 0.0
        self.sprint_speed_multiplier = 1.3  # プレイヤーのスプリントと同じ速度倍率

        # 属性攻撃（ティア3以上）
        self.elemental_types = []  # ["fire", "ice", "lightning", "holy", "dark"]
        self.elemental_attack_chance = 0.0
        self.last_elemental_time = 0.0
        self.elemental_cooldown = 5.0

        # 攻撃範囲（サブクラスでオーバーライド可能）
        self.attack_range = 3.0
        self.dash_attack_range = 7.0
        self.charge_attack_range = 10.0  # チャージ攻撃範囲を拡大（3～10ブロック）

        # クールダウン（サブクラスでオーバーライド可能）
        self.attack_cooldown = 1.5
        self.dash_cooldown = 3.0
        self.charge_cooldown = 3.0  # チャージ攻撃のクールダウン

        # 攻撃タイミング
        self.last_dash_time = 0.0
        self.last_charge_time = 0.0
        self.charge_start_time = 0.0
        self.is_charging = False
        self.charge_duration = 0.0

        # コンボ
        self.combo_max = 3
        self.current_combo = 0
        self.combo_reset_time = 2.0
        self.last_combo_time = 0.0

        # 攻撃確率（サブクラスでオーバーライド可能）
        self.dash_attack_chance = 0.4
        self.charge_attack_chance = 0.3  # 基本チャージ攻撃確率
        self.charge_attack_chance_vs_player = 0.8  # プレイヤー相手の確率
        self.charge_attack_chance_vs_mob = 0.4  # Mob相手の確率

        # スキル使用
        self.last_skill_time = 0.0
        self.skill_cooldown = 3.0
        self.skill_use_chance = 0.4

        # ドロップテーブル
        self.drop_table = self._initialize_drop_table()

        # 撤退・回復システム
        self.retreat_hp_threshold = 0.3  # HP30%以下で撤退判断（ティアで変動）
        self.is_retreating = False
        self.retreat_distance = 20.0
        self.last_heal_time = 0.0
        self.heal_cooldown = 10.0  # 10秒ごとに回復可能
        self.heal_amount_per_tick = 0.5  # 1tickあたりの回復量

        # 複数敵対応
        self.surrounded_threshold = 3  # 3体以上で囲まれていると判断
        self.surrounded_distance = 5.0  # 5ブロック以内の敵をカウント

    def update(self, delta_time: float, world_data: Dict) -> Dict:
        """装備情報を更新してから基底クラスの更新"""
        self._update_equipment_info(world_data)
        return super().update(delta_time, world_data)

    def _update_equipment_info(self, world_data: Dict):
        """装備情報を更新"""
        equipment = world_data.get("equipment", {})
        self.current_weapon = equipment.get("mainhand_item", None)
        if self.current_weapon:
            self.weapon_type = self._detect_weapon_type(self.current_weapon)

        offhand = equipment.get("offhand_item", None)
        self.has_sheath = offhand is not None and "saya" in str(offhand).lower()

    def _detect_weapon_type(self, weapon_name: str) -> str:
        """武器タイプを判定"""
        weapon_lower = str(weapon_name).lower()
        if "katana" in weapon_lower: return "katana"
        elif "sword" in weapon_lower: return "sword"
        elif "spear" in weapon_lower: return "spear"
        elif "axe" in weapon_lower: return "axe"
        elif "lance" in weapon_lower: return "lance"
        return "sword"

    def _handle_combat(self, world_data: Dict) -> Dict:
        """プレイヤースタイル戦闘処理（撤退・回復・複数敵対応含む）"""
        enemy_distance = world_data.get("nearest_enemy_distance", float('inf'))
        current_time = world_data.get("current_time", 0)
        current_target = world_data.get("current_target", None)
        nearby_enemies = world_data.get("nearby_enemies", [])

        # HP状態を確認
        current_hp = self.entity_data.get("health", 20.0)
        max_hp = self.entity_data.get("max_health", 20.0)
        hp_percent = current_hp / max_hp if max_hp > 0 else 0

        # 撤退判断（HP低下時）
        if hp_percent <= self.retreat_hp_threshold:
            return self._handle_retreat_and_heal(world_data, enemy_distance, current_time)

        # 撤退モードから復帰（HP回復した場合）
        if self.is_retreating and hp_percent > self.retreat_hp_threshold + 0.2:
            self.is_retreating = False

        # 周囲の敵の数をカウント（5ブロック以内）
        nearby_enemy_count = self._count_nearby_enemies(nearby_enemies, self.surrounded_distance)
        is_player_target = self._is_player_target(current_target)

        # 複数の敵に囲まれている場合の判定（3体以上が5ブロック以内）
        if nearby_enemy_count >= self.surrounded_threshold:
            print(f"[PlayerStyleAI] 囲まれている！ 敵の数: {nearby_enemy_count}")

            # HPが50%以下なら後退優先
            if hp_percent < 0.5:
                return self._execute_retreat(world_data)

            # チャージ攻撃で複数を巻き込む（クールダウン2秒に短縮）
            if current_time - self.last_charge_time >= 2.0:
                return self._execute_charge_attack(world_data)

        # 複数の敵がいる場合、最も危険な敵を優先（Mob相手のみ）
        if nearby_enemy_count >= 2 and not is_player_target:
            most_dangerous = self._find_most_dangerous_enemy(nearby_enemies)
            if most_dangerous and most_dangerous != current_target:
                print(f"[PlayerStyleAI] より危険な敵にターゲット変更")
                # ターゲット変更をJava側に通知
                return {
                    "action": "change_target",
                    "data": {
                        "new_target": most_dangerous,
                        "reason": "threat_assessment"
                    }
                }

        # 回避判定（プレイヤー相手のときのみ）
        if is_player_target and self.should_dodge(world_data):
            return self._execute_dodge(world_data)

        if current_time - self.last_combo_time > self.combo_reset_time:
            self.current_combo = 0

        # チャージ攻撃判定（3～10ブロック）
        if 3.0 <= enemy_distance <= self.charge_attack_range:
            if self._can_use_charge_attack(current_time):
                # プレイヤー相手なら高確率、Mob相手なら低確率
                chance = self.charge_attack_chance_vs_player if is_player_target else self.charge_attack_chance_vs_mob
                if random.random() < chance * self._get_tactical_awareness():
                    return self._execute_charge_attack(world_data)

        # 武器スキル使用判定（プレイヤー相手のときのみ）
        if is_player_target and enemy_distance < 5.0:
            if self._can_use_skill(current_time):
                if random.random() < self.skill_use_chance * self._get_tactical_awareness():
                    return self._execute_weapon_skill(world_data)

        # 鞘装備時のダッシュ攻撃
        if self.has_sheath:
            if 4.0 < enemy_distance <= self.dash_attack_range:
                if self._can_use_dash_attack(current_time):
                    if random.random() < self.dash_attack_chance:
                        return self._execute_dash_attack(world_data)

        # 間合い調整はプレイヤー相手のときのみ
        if is_player_target:
            # プレイヤー相手：間合い調整あり
            if enemy_distance > self.attack_range:
                return self._move_towards_enemy(world_data.get("nearest_enemy_position"),
                                               self.entity_data.get("position"))

            if enemy_distance <= self.attack_range:
                return self._execute_normal_attack(world_data)
        else:
            # Mob相手：シンプルに追いかけて攻撃
            if enemy_distance > self.attack_range:
                # 直接追いかける（速度アップ）
                return self._move_towards_enemy_fast(world_data.get("nearest_enemy_position"),
                                                     self.entity_data.get("position"))
            else:
                return self._execute_normal_attack(world_data)

        return {"action": "idle", "data": {}}

    def _handle_retreat_and_heal(self, world_data: Dict, enemy_distance: float, current_time: float) -> Dict:
        """撤退・回復処理"""
        self.is_retreating = True

        # 敵から十分離れている場合は回復
        if enemy_distance > 15.0:
            if self._can_heal(current_time):
                return self._execute_heal(world_data, current_time)
            # 回復中は待機
            return {"action": "guard", "data": {"type": "defensive_stance"}}

        # 敵が近い場合は撤退
        return self._execute_retreat(world_data)

    def _can_heal(self, current_time: float) -> bool:
        """回復可能かチェック"""
        return current_time - self.last_heal_time >= self.heal_cooldown

    def _execute_heal(self, world_data: Dict, current_time: float) -> Dict:
        """回復実行"""
        self.last_heal_time = current_time

        return {
            "action": "heal",
            "data": {
                "heal_amount": self.heal_amount_per_tick * (1.0 + self.tier * 0.2),
                "duration": 3.0,  # 3秒間回復
                "animation": "healing_pose"
            }
        }

    def _execute_retreat(self, world_data: Dict) -> Dict:
        """撤退実行（スプリント対応）"""
        enemy_pos = world_data.get("nearest_enemy_position")
        current_pos = self.entity_data.get("position", (0, 0, 0))

        if not enemy_pos:
            return {"action": "idle", "data": {}}

        # 敵から遠ざかる方向に移動
        dx = current_pos[0] - enemy_pos[0]
        dz = current_pos[2] - enemy_pos[2]
        distance = math.sqrt(dx**2 + dz**2)

        if distance > 0:
            # 撤退時は常にスプリント
            base_speed = self._get_move_speed()
            sprint_speed = base_speed * self.sprint_speed_multiplier * 1.2  # さらに速く

            return {
                "action": "retreat",
                "data": {
                    "direction": (dx/distance, 0, dz/distance),
                    "speed": sprint_speed,
                    "is_fleeing": True,
                    "is_sprinting": True
                }
            }

        return {"action": "idle", "data": {}}

    def _can_use_dash_attack(self, current_time: float) -> bool:
        return current_time - self.last_dash_time >= self.dash_cooldown

    def _can_use_charge_attack(self, current_time: float) -> bool:
        return current_time - self.last_charge_time >= self.charge_cooldown

    def _can_use_skill(self, current_time: float) -> bool:
        """スキルが使用可能か"""
        return current_time - self.last_skill_time >= self.skill_cooldown

    def _can_use_elemental_attack(self, current_time: float) -> bool:
        """属性攻撃が使用可能か"""
        return current_time - self.last_elemental_time >= self.elemental_cooldown

    def _count_nearby_enemies(self, nearby_enemies: list, distance: float) -> int:
        """指定距離内の敵の数をカウント"""
        if not nearby_enemies:
            return 0
        current_pos = self.entity_data.get("position", (0, 0, 0))
        count = 0
        for enemy in nearby_enemies:
            enemy_pos = enemy.get("position", (0, 0, 0))
            dx = enemy_pos[0] - current_pos[0]
            dz = enemy_pos[2] - current_pos[2]
            dist = math.sqrt(dx**2 + dz**2)
            if dist <= distance:
                count += 1
        return count

    def _is_player_target(self, target) -> bool:
        """ターゲットがプレイヤーかどうか"""
        if not target:
            return False
        target_type = target.get("type", "")
        return target_type == "player"

    def _find_most_dangerous_enemy(self, nearby_enemies: list):
        """最も危険な敵を見つける"""
        if not nearby_enemies:
            return None

        current_pos = self.entity_data.get("position", (0, 0, 0))
        most_dangerous = None
        highest_threat = 0.0

        for enemy in nearby_enemies:
            threat = 0.0
            enemy_pos = enemy.get("position", (0, 0, 0))

            # 距離計算
            dx = enemy_pos[0] - current_pos[0]
            dz = enemy_pos[2] - current_pos[2]
            dist = math.sqrt(dx**2 + dz**2)

            # 距離が近いほど脅威度が高い
            if dist <= 2.0:
                threat += 10.0
            elif dist <= 5.0:
                threat += 5.0 / dist

            # 体力が低い敵は優先的に倒す
            enemy_hp = enemy.get("health", 20.0)
            enemy_max_hp = enemy.get("max_health", 20.0)
            health_ratio = enemy_hp / enemy_max_hp if enemy_max_hp > 0 else 1.0
            if health_ratio < 0.5:
                threat += 3.0

            # プレイヤーは最優先
            if enemy.get("type") == "player":
                threat += 15.0

            if threat > highest_threat:
                highest_threat = threat
                most_dangerous = enemy

        return most_dangerous

    def _get_tactical_awareness(self) -> float:
        """ティアに応じた戦術認識度を取得"""
        tier_awareness = {
            1: 0.3,   # 一般兵
            2: 0.5,   # エリート兵
            3: 0.7,   # 特異点
            4: 0.8,   # 英雄級
            5: 0.9,   # 神話級
            6: 0.95,  # 天使級
            7: 1.0    # 神聖級
        }
        return tier_awareness.get(self.tier, 0.3)

    def _move_towards_enemy_fast(self, enemy_pos: Optional[Tuple], current_pos: Tuple) -> Dict:
        """敵に素早く接近（Mob相手用、スプリント対応）"""
        if not enemy_pos:
            return {"action": "idle", "data": {}}
        dx, dz = enemy_pos[0] - current_pos[0], enemy_pos[2] - current_pos[2]
        distance = math.sqrt(dx**2 + dz**2)

        if distance > 0:
            # スプリント判定
            base_speed = self._get_move_speed() * 1.3  # Mob相手は基本速度を上げる
            speed = base_speed
            is_sprinting = False

            # 距離が離れている場合はさらにスプリント
            if self._should_sprint(distance):
                speed = base_speed * self.sprint_speed_multiplier
                is_sprinting = True

            return {
                "action": "move_to_target",
                "data": {
                    "direction": (dx/distance, 0, dz/distance),
                    "speed": speed,
                    "look_at": enemy_pos,
                    "is_sprinting": is_sprinting
                }
            }
        return {"action": "idle", "data": {}}

    def _execute_elemental_attack(self, world_data: Dict) -> Dict:
        """属性攻撃を実行"""
        current_time = world_data.get("current_time", 0)
        self.last_elemental_time = current_time

        # ランダムに属性を選択
        element = random.choice(self.elemental_types)

        return {
            "action": "elemental_attack",
            "data": {
                "type": f"{element}_attack",
                "element": element,
                "weapon": self.weapon_type,
                "damage_multiplier": self._get_elemental_damage_multiplier(element),
                "aoe_range": self._get_elemental_aoe_range(element),
                "status_effect": self._get_elemental_status_effect(element)
            }
        }

    def _get_elemental_damage_multiplier(self, element: str) -> float:
        """属性攻撃のダメージ倍率"""
        base_multiplier = 2.0
        return base_multiplier * (1.0 + self.tier * 0.3)

    def _get_elemental_aoe_range(self, element: str) -> float:
        """属性攻撃のAoE範囲"""
        ranges = {
            "fire": 4.0,       # 炎: 広範囲
            "ice": 3.5,        # 氷: 中範囲
            "lightning": 5.0,  # 雷: 最広範囲
            "holy": 6.0,       # 聖: 超広範囲
            "dark": 3.0        # 闇: 狭範囲だが強力
        }
        return ranges.get(element, 3.0)

    def _get_elemental_status_effect(self, element: str) -> Dict:
        """属性攻撃の状態異常効果"""
        effects = {
            "fire": {"type": "burning", "duration": 5.0, "damage_per_tick": 1.0},
            "ice": {"type": "slowness", "duration": 3.0, "amplifier": 2},
            "lightning": {"type": "paralysis", "duration": 2.0, "amplifier": 1},
            "holy": {"type": "weakness", "duration": 4.0, "amplifier": 2},
            "dark": {"type": "wither", "duration": 5.0, "amplifier": 1}
        }
        return effects.get(element, {})

    def _execute_dash_attack(self, world_data: Dict) -> Dict:
        """ダッシュ攻撃"""
        current_time = world_data.get("current_time", 0)
        self.last_dash_time = current_time
        self.current_combo = 0

        return {
            "action": "dash_attack",
            "data": {
                "type": f"{self.weapon_type}_dash",
                "weapon": self.weapon_type,
                "target": world_data.get("nearest_enemy_position"),
                "damage_multiplier": self._get_dash_damage_multiplier(),
                "dash_speed": 0.8,
                "dash_distance": 5.0,
                "has_sheath": True
            }
        }

    def _execute_charge_attack(self, world_data: Dict) -> Dict:
        """チャージ攻撃"""
        current_time = world_data.get("current_time", 0)
        self.last_charge_time = current_time
        self.current_combo = 0

        # ティアに応じたチャージ率を決定
        tactical_awareness = self._get_tactical_awareness()
        charge_percent = 0.5 + (tactical_awareness * 0.5)
        charge_percent = min(charge_percent, 1.0)

        return {
            "action": "charge_attack",
            "data": {
                "type": f"{self.weapon_type}_charge",
                "weapon": self.weapon_type,
                "charge_percent": charge_percent,
                "damage_multiplier": 1.0 + charge_percent * 2.0,
                "target": world_data.get("nearest_enemy_position")
            }
        }

    def _execute_weapon_skill(self, world_data: Dict) -> Dict:
        """武器スキルを使用"""
        current_time = world_data.get("current_time", 0)
        self.last_skill_time = current_time

        return {
            "action": "use_weapon_skill",
            "data": {
                "skill_type": "guard",
                "duration": 5.0
            }
        }

    def _execute_normal_attack(self, world_data: Dict) -> Dict:
        """通常攻撃"""
        current_time = world_data.get("current_time", 0)

        if current_time - self.last_attack_time < self.attack_cooldown:
            return self._strafe_around_enemy(world_data)

        self.last_attack_time = current_time
        self.last_combo_time = current_time
        self.current_combo = (self.current_combo + 1) % (self.combo_max + 1)

        attack_pattern = self._get_weapon_attack_pattern(self.weapon_type, self.current_combo)

        return {
            "action": "attack",
            "data": {
                "type": attack_pattern["type"],
                "weapon": self.weapon_type,
                "combo": self.current_combo,
                "damage_multiplier": attack_pattern["damage_multiplier"] * self._get_base_damage_multiplier(),
                "attack_speed": attack_pattern["speed"],
                "reach": attack_pattern["reach"]
            }
        }

    def _get_weapon_attack_pattern(self, weapon_type: str, combo: int) -> Dict:
        """武器タイプごとの攻撃パターン"""
        patterns = {
            "sword": [
                {"type": "slash", "damage_multiplier": 1.0, "speed": 1.0, "reach": 3.0},
                {"type": "thrust", "damage_multiplier": 1.1, "speed": 0.9, "reach": 3.5},
                {"type": "spin_slash", "damage_multiplier": 1.3, "speed": 1.2, "reach": 3.0},
            ],
            "katana": [
                {"type": "iai_slash", "damage_multiplier": 1.2, "speed": 1.1, "reach": 3.5},
                {"type": "downward_cut", "damage_multiplier": 1.1, "speed": 0.95, "reach": 3.0},
                {"type": "quick_slash", "damage_multiplier": 1.0, "speed": 1.3, "reach": 3.2},
            ],
            "spear": [
                {"type": "thrust", "damage_multiplier": 1.1, "speed": 0.9, "reach": 5.0},
                {"type": "sweep", "damage_multiplier": 0.9, "speed": 1.0, "reach": 4.0},
                {"type": "pierce", "damage_multiplier": 1.4, "speed": 0.8, "reach": 5.5},
            ],
            "axe": [
                {"type": "overhead_smash", "damage_multiplier": 1.5, "speed": 0.7, "reach": 2.5},
                {"type": "horizontal_swing", "damage_multiplier": 1.2, "speed": 0.8, "reach": 3.0},
                {"type": "cleave", "damage_multiplier": 1.3, "speed": 0.75, "reach": 2.8},
            ],
            "lance": [
                {"type": "charge_thrust", "damage_multiplier": 1.3, "speed": 0.85, "reach": 6.0},
                {"type": "guard_counter", "damage_multiplier": 1.1, "speed": 0.9, "reach": 5.0},
                {"type": "pierce", "damage_multiplier": 1.4, "speed": 0.8, "reach": 6.5},
            ]
        }
        weapon_patterns = patterns.get(weapon_type, patterns["sword"])
        return weapon_patterns[combo % len(weapon_patterns)]

    def _can_sprint(self, current_time: float) -> bool:
        """スプリント可能かチェック"""
        return current_time - self.last_sprint_time >= self.sprint_cooldown

    def _should_sprint(self, enemy_distance: float) -> bool:
        """スプリントすべきかどうか判定"""
        # 敵が5～15ブロック離れている場合にスプリント
        return 5.0 < enemy_distance <= 15.0

    def _move_towards_enemy(self, enemy_pos: Optional[Tuple], current_pos: Tuple) -> Dict:
        """敵に接近（スプリント対応）"""
        if not enemy_pos: return {"action": "idle", "data": {}}
        dx, dz = enemy_pos[0] - current_pos[0], enemy_pos[2] - current_pos[2]
        distance = math.sqrt(dx**2 + dz**2)

        if distance > 0:
            # スプリント判定
            base_speed = self._get_move_speed()
            speed = base_speed
            is_sprinting = False

            # 距離が離れている場合はスプリント
            if self._should_sprint(distance):
                if self._can_sprint(0):  # current_timeは外から取得すべきだが、簡略化
                    speed = base_speed * self.sprint_speed_multiplier
                    is_sprinting = True

            return {
                "action": "move_to_target",
                "data": {
                    "direction": (dx/distance, 0, dz/distance),
                    "speed": speed,
                    "look_at": enemy_pos,
                    "is_sprinting": is_sprinting
                }
            }
        return {"action": "idle", "data": {}}

    def _execute_dodge(self, world_data: Dict) -> Dict:
        """回避"""
        current_time = world_data.get("current_time", 0)
        if current_time - self.last_dodge_time < 2.0: return {"action": "idle", "data": {}}
        if random.random() > self.dodge_success_rate:
            self.failed_dodges += 1
            return {"action": "idle", "data": {}}

        self.last_dodge_time = current_time
        self.successful_dodges += 1

        enemy_pos = world_data.get("nearest_enemy_position", (0, 0, 0))
        current_pos = self.entity_data.get("position", (0, 0, 0))
        dx, dz = enemy_pos[0] - current_pos[0], enemy_pos[2] - current_pos[2]

        dodge_angle = random.choice([-90, 90])
        angle_rad = math.radians(dodge_angle)
        dodge_x = dx * math.cos(angle_rad) - dz * math.sin(angle_rad)
        dodge_z = dx * math.sin(angle_rad) + dz * math.cos(angle_rad)

        distance = math.sqrt(dodge_x**2 + dodge_z**2)
        if distance > 0:
            dodge_x, dodge_z = dodge_x/distance, dodge_z/distance

        return {"action": "dodge", "data": {"direction": (dodge_x, 0, dodge_z), "distance": 2.0, "speed": self._get_dodge_speed()}}

    def _strafe_around_enemy(self, world_data: Dict) -> Dict:
        """敵の周囲を移動"""
        enemy_pos = world_data.get("nearest_enemy_position", (0, 0, 0))
        current_pos = self.entity_data.get("position", (0, 0, 0))
        dx, dz = current_pos[0] - enemy_pos[0], current_pos[2] - enemy_pos[2]
        strafe_x, strafe_z = -dz, dx
        distance = math.sqrt(strafe_x**2 + strafe_z**2)
        if distance > 0:
            return {"action": "strafe", "data": {"direction": (strafe_x/distance, 0, strafe_z/distance), "speed": 0.2, "look_at": enemy_pos}}
        return {"action": "idle", "data": {}}

    # サブクラスでオーバーライド可能なメソッド
    def _get_base_damage_multiplier(self) -> float:
        """基本ダメージ倍率（ティアごとに異なる）"""
        return 1.0

    def _get_dash_damage_multiplier(self) -> float:
        """ダッシュ攻撃のダメージ倍率"""
        return 1.5

    def _get_charge_damage_multiplier(self, charge_level: float) -> float:
        """チャージ攻撃のダメージ倍率"""
        return 2.0 + charge_level

    def _get_move_speed(self) -> float:
        """移動速度"""
        return 0.3

    def _get_dodge_speed(self) -> float:
        """回避速度"""
        return 0.6

    def _initialize_equipment(self) -> Dict:
        """装備データを初期化（サブクラスでオーバーライド）"""
        return {
            "helmet": "iron_helmet",
            "chestplate": "iron_chestplate",
            "leggings": "iron_leggings",
            "boots": "iron_boots",
            "enchantments": {}
        }

    def _initialize_drop_table(self) -> Dict:
        """ドロップテーブルを初期化（サブクラスでオーバーライド）"""
        return {
            "common": [],      # 高確率（50%）
            "uncommon": [],    # 中確率（30%）
            "rare": [],        # 低確率（15%）
            "epic": [],        # 超低確率（5%）
            "legendary": []    # 極低確率（1%）
        }

    def get_equipment_data(self) -> Dict:
        """装備データを取得"""
        return self.equipment_data

    def calculate_drops(self) -> list:
        """死亡時のドロップアイテムを計算"""
        drops = []
        drop_chances = {
            "common": 0.5,
            "uncommon": 0.3,
            "rare": 0.15,
            "epic": 0.05,
            "legendary": 0.01
        }
        for rarity, chance in drop_chances.items():
            if random.random() < chance:
                items = self.drop_table.get(rarity, [])
                if items:
                    drops.append(random.choice(items))
        return drops

    def get_drop_table(self) -> Dict:
        """ドロップテーブルを取得"""
        return self.drop_table

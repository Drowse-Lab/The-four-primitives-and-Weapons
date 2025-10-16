"""CommonSoldier AI - 一般兵（ティア1）- プレイヤースタイル"""
from typing import Dict
from player_style_base import PlayerStyleBaseAI

class CommonSoldierAI(PlayerStyleBaseAI):
    """一般兵（ティア1）- 基本的な戦闘能力"""

    def __init__(self, entity_data: Dict):
        super().__init__(tier=1, entity_data=entity_data)

        # 一般兵の基本パラメータ
        self.attack_range = 3.0
        self.dash_attack_range = 7.0
        self.charge_attack_range = 5.0

        self.attack_cooldown = 1.5
        self.dash_cooldown = 3.0
        self.charge_cooldown = 4.0

        self.combo_max = 3
        self.dodge_success_rate = 0.3

        self.dash_attack_chance = 0.4
        self.charge_attack_chance = 0.3

        # 属性攻撃なし（ティア1）
        self.elemental_types = []
        self.elemental_attack_chance = 0.0

    def _get_base_damage_multiplier(self) -> float:
        return 1.0

    def _get_dash_damage_multiplier(self) -> float:
        return 1.5

    def _get_charge_damage_multiplier(self, charge_level: float) -> float:
        return 2.0 + charge_level

    def _get_move_speed(self) -> float:
        return 0.3

    def _get_dodge_speed(self) -> float:
        return 0.6

    def _initialize_equipment(self) -> Dict:
        """鉄フル装備（エンチャントなし）"""
        return {
            "helmet": "iron_helmet",
            "chestplate": "iron_chestplate",
            "leggings": "iron_leggings",
            "boots": "iron_boots",
            "enchantments": {}
        }

    def _initialize_drop_table(self) -> Dict:
        """ティア1のドロップテーブル"""
        return {
            "common": ["iron_sword", "iron_ingot", "bread"],
            "uncommon": ["iron_chestplate", "iron_helmet"],
            "rare": ["enchanted_book"],
            "epic": [],
            "legendary": []
        }

    def get_skill_level(self) -> Dict:
        return {
            "melee": 0.3,
            "ranged": 0.0,
            "magic": 0.0,
            "defense": 0.2,
            "dodge": 0.3,
            "tactics": 0.2,
            "learning": 0.1
        }

def create_ai(entity_data: Dict) -> CommonSoldierAI:
    return CommonSoldierAI(entity_data)

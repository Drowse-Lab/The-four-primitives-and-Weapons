"""EliteSoldier AI - エリート兵（ティア2）- プレイヤースタイル"""
from typing import Dict
from player_style_base import PlayerStyleBaseAI

class EliteSoldierAI(PlayerStyleBaseAI):
    """エリート兵（ティア2）- より高速で正確な戦闘"""

    def __init__(self, entity_data: Dict):
        super().__init__(tier=2, entity_data=entity_data)

        # エリート兵の強化パラメータ
        self.attack_range = 4.0
        self.dash_attack_range = 10.0
        self.charge_attack_range = 6.0

        self.attack_cooldown = 1.2
        self.dash_cooldown = 2.5
        self.charge_cooldown = 3.5

        self.combo_max = 4
        self.dodge_success_rate = 0.5

        self.dash_attack_chance = 0.5
        self.charge_attack_chance = 0.4

        # 属性攻撃なし（ティア2）
        self.elemental_types = []
        self.elemental_attack_chance = 0.0

    def _get_base_damage_multiplier(self) -> float:
        return 1.2

    def _get_dash_damage_multiplier(self) -> float:
        return 1.8

    def _get_charge_damage_multiplier(self, charge_level: float) -> float:
        return 2.5 + charge_level * 1.2

    def _get_move_speed(self) -> float:
        return 0.4

    def _get_dodge_speed(self) -> float:
        return 0.8

    def _initialize_equipment(self) -> Dict:
        """鉄装備+エンチャント（保護I）"""
        return {
            "helmet": "iron_helmet",
            "chestplate": "iron_chestplate",
            "leggings": "iron_leggings",
            "boots": "iron_boots",
            "enchantments": {
                "protection": 1
            }
        }

    def _initialize_drop_table(self) -> Dict:
        """ティア2のドロップテーブル"""
        return {
            "common": ["iron_sword", "diamond", "golden_apple"],
            "uncommon": ["iron_chestplate", "enchanted_book"],
            "rare": ["diamond_sword", "iron_helmet"],
            "epic": ["enchanted_golden_apple"],
            "legendary": []
        }

    def get_skill_level(self) -> Dict:
        return {"melee": 0.5, "ranged": 0.4, "magic": 0.0, "defense": 0.4, "dodge": 0.5, "tactics": 0.5, "learning": 0.3}

def create_ai(entity_data: Dict) -> EliteSoldierAI:
    return EliteSoldierAI(entity_data)

"""Singularity AI - 特異点（ティア3）- プレイヤースタイル"""
from typing import Dict
from player_style_base import PlayerStyleBaseAI

class SingularityAI(PlayerStyleBaseAI):
    """特異点（ティア3）- 完璧な戦術判断"""

    def __init__(self, entity_data: Dict):
        super().__init__(tier=3, entity_data=entity_data)

        self.attack_range = 5.0
        self.dash_attack_range = 12.0
        self.charge_attack_range = 7.0

        self.attack_cooldown = 1.0
        self.dash_cooldown = 2.0
        self.charge_cooldown = 3.0

        self.combo_max = 6
        self.dodge_success_rate = 0.7

        self.dash_attack_chance = 0.6
        self.charge_attack_chance = 0.5

        # 属性攻撃開始（ティア3）- 火と氷
        self.elemental_types = ["fire", "ice"]
        self.elemental_attack_chance = 0.2
        self.elemental_cooldown = 8.0

    def _get_base_damage_multiplier(self) -> float:
        return 1.5

    def _get_dash_damage_multiplier(self) -> float:
        return 2.2

    def _get_charge_damage_multiplier(self, charge_level: float) -> float:
        return 3.0 + charge_level * 1.5

    def _get_move_speed(self) -> float:
        return 0.5

    def _get_dodge_speed(self) -> float:
        return 1.0

    def _initialize_equipment(self) -> Dict:
        """ダイヤ装備+エンチャント（保護II、棘の鎧I）"""
        return {
            "helmet": "diamond_helmet",
            "chestplate": "diamond_chestplate",
            "leggings": "diamond_leggings",
            "boots": "diamond_boots",
            "enchantments": {
                "protection": 2,
                "thorns": 1
            }
        }

    def _initialize_drop_table(self) -> Dict:
        """ティア3のドロップテーブル"""
        return {
            "common": ["diamond_sword", "diamond", "enchanted_book"],
            "uncommon": ["diamond_chestplate", "fire_charge"],
            "rare": ["netherite_scrap", "totem_of_undying"],
            "epic": ["netherite_sword", "enchanted_golden_apple"],
            "legendary": ["nether_star"]
        }

    def get_skill_level(self) -> Dict:
        return {"melee": 0.7, "ranged": 0.6, "magic": 0.3, "defense": 0.6, "dodge": 0.7, "tactics": 0.8, "learning": 0.7}

def create_ai(entity_data: Dict) -> SingularityAI:
    return SingularityAI(entity_data)

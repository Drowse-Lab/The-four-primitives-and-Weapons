"""MythicalTier AI - 神話級（ティア5）- プレイヤースタイル"""
from typing import Dict
from player_style_base import PlayerStyleBaseAI

class MythicalTierAI(PlayerStyleBaseAI):
    """神話級（ティア5）- 神話級の戦闘技術"""

    def __init__(self, entity_data: Dict):
        super().__init__(tier=5, entity_data=entity_data)

        self.attack_range = 8.0
        self.dash_attack_range = 16.0
        self.charge_attack_range = 10.0

        self.attack_cooldown = 0.6
        self.dash_cooldown = 1.5
        self.charge_cooldown = 2.0

        self.combo_max = 12
        self.dodge_success_rate = 0.9

        self.dash_attack_chance = 0.8
        self.charge_attack_chance = 0.7

        # 属性攻撃（ティア5）- 火、氷、雷、聖
        self.elemental_types = ["fire", "ice", "lightning", "holy"]
        self.elemental_attack_chance = 0.4
        self.elemental_cooldown = 5.0

    def _get_base_damage_multiplier(self) -> float:
        return 2.5

    def _get_dash_damage_multiplier(self) -> float:
        return 3.5

    def _get_charge_damage_multiplier(self, charge_level: float) -> float:
        return 5.0 + charge_level * 2.5

    def _get_move_speed(self) -> float:
        return 0.7

    def _get_dodge_speed(self) -> float:
        return 1.5

    def _initialize_equipment(self) -> Dict:
        """ネザライト装備+強力エンチャント（保護IV、棘の鎧III、火炎耐性II）"""
        return {
            "helmet": "netherite_helmet",
            "chestplate": "netherite_chestplate",
            "leggings": "netherite_leggings",
            "boots": "netherite_boots",
            "enchantments": {
                "protection": 4,
                "thorns": 3,
                "fire_protection": 2,
                "blast_protection": 1
            }
        }

    def _initialize_drop_table(self) -> Dict:
        """ティア5のドロップテーブル"""
        return {
            "common": ["netherite_sword", "diamond", "emerald"],
            "uncommon": ["netherite_ingot", "enchanted_book"],
            "rare": ["netherite_chestplate", "totem_of_undying"],
            "epic": ["elytra", "enchanted_golden_apple"],
            "legendary": ["nether_star", "dragon_egg", "beacon"]
        }

    def get_skill_level(self) -> Dict:
        return {"melee": 0.9, "ranged": 0.8, "magic": 0.7, "defense": 0.8, "dodge": 0.9, "tactics": 0.95, "learning": 0.9}

def create_ai(entity_data: Dict) -> MythicalTierAI:
    return MythicalTierAI(entity_data)

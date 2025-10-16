"""HeroicTier AI - 英雄級（ティア4）- プレイヤースタイル"""
from typing import Dict
from player_style_base import PlayerStyleBaseAI

class HeroicTierAI(PlayerStyleBaseAI):
    """英雄級（ティア4）- 圧倒的な戦闘力"""

    def __init__(self, entity_data: Dict):
        super().__init__(tier=4, entity_data=entity_data)

        self.attack_range = 6.0
        self.dash_attack_range = 14.0
        self.charge_attack_range = 8.0

        self.attack_cooldown = 0.8
        self.dash_cooldown = 1.8
        self.charge_cooldown = 2.5

        self.combo_max = 8
        self.dodge_success_rate = 0.8

        self.dash_attack_chance = 0.7
        self.charge_attack_chance = 0.6

        # 属性攻撃（ティア4）- 火、氷、雷
        self.elemental_types = ["fire", "ice", "lightning"]
        self.elemental_attack_chance = 0.3
        self.elemental_cooldown = 6.0

    def _get_base_damage_multiplier(self) -> float:
        return 2.0

    def _get_dash_damage_multiplier(self) -> float:
        return 2.8

    def _get_charge_damage_multiplier(self, charge_level: float) -> float:
        return 4.0 + charge_level * 2.0

    def _get_move_speed(self) -> float:
        return 0.6

    def _get_dodge_speed(self) -> float:
        return 1.2

    def _initialize_equipment(self) -> Dict:
        """ダイヤ装備+強化エンチャント（保護III、棘の鎧II、火炎耐性I）"""
        return {
            "helmet": "diamond_helmet",
            "chestplate": "diamond_chestplate",
            "leggings": "diamond_leggings",
            "boots": "diamond_boots",
            "enchantments": {
                "protection": 3,
                "thorns": 2,
                "fire_protection": 1
            }
        }

    def _initialize_drop_table(self) -> Dict:
        """ティア4のドロップテーブル"""
        return {
            "common": ["diamond_sword", "diamond", "emerald"],
            "uncommon": ["netherite_scrap", "enchanted_book"],
            "rare": ["netherite_sword", "totem_of_undying"],
            "epic": ["netherite_chestplate", "enchanted_golden_apple"],
            "legendary": ["nether_star", "dragon_breath"]
        }

    def get_skill_level(self) -> Dict:
        return {"melee": 0.8, "ranged": 0.7, "magic": 0.5, "defense": 0.7, "dodge": 0.8, "tactics": 0.9, "learning": 0.8}

def create_ai(entity_data: Dict) -> HeroicTierAI:
    return HeroicTierAI(entity_data)

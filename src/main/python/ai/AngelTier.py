"""AngelTier AI - 天使級（ティア6）- プレイヤースタイル"""
from typing import Dict
from player_style_base import PlayerStyleBaseAI

class AngelTierAI(PlayerStyleBaseAI):
    """天使級（ティア6）- 天使の如き完璧な戦闘"""

    def __init__(self, entity_data: Dict):
        super().__init__(tier=6, entity_data=entity_data)

        self.attack_range = 10.0
        self.dash_attack_range = 20.0
        self.charge_attack_range = 12.0

        self.attack_cooldown = 0.4
        self.dash_cooldown = 1.2
        self.charge_cooldown = 1.5

        self.combo_max = 16
        self.dodge_success_rate = 0.95

        self.dash_attack_chance = 0.85
        self.charge_attack_chance = 0.8

        # 属性攻撃（ティア6）- 火、氷、雷、聖、闇
        self.elemental_types = ["fire", "ice", "lightning", "holy", "dark"]
        self.elemental_attack_chance = 0.5
        self.elemental_cooldown = 4.0

    def _get_base_damage_multiplier(self) -> float:
        return 3.0

    def _get_dash_damage_multiplier(self) -> float:
        return 4.5

    def _get_charge_damage_multiplier(self, charge_level: float) -> float:
        return 6.0 + charge_level * 3.0

    def _get_move_speed(self) -> float:
        return 0.8

    def _get_dodge_speed(self) -> float:
        return 2.0

    def _initialize_equipment(self) -> Dict:
        """ネザライト装備+最強エンチャント（保護IV、棘の鎧III、全耐性III）"""
        return {
            "helmet": "netherite_helmet",
            "chestplate": "netherite_chestplate",
            "leggings": "netherite_leggings",
            "boots": "netherite_boots",
            "enchantments": {
                "protection": 4,
                "thorns": 3,
                "fire_protection": 3,
                "blast_protection": 3,
                "projectile_protection": 3,
                "feather_falling": 4
            }
        }

    def _initialize_drop_table(self) -> Dict:
        """ティア6のドロップテーブル"""
        return {
            "common": ["netherite_sword", "netherite_ingot", "emerald_block"],
            "uncommon": ["enchanted_book", "totem_of_undying"],
            "rare": ["netherite_chestplate", "elytra"],
            "epic": ["nether_star", "dragon_egg", "enchanted_golden_apple"],
            "legendary": ["beacon", "end_crystal", "shulker_box"]
        }

    def get_skill_level(self) -> Dict:
        return {"melee": 0.95, "ranged": 0.9, "magic": 0.9, "defense": 0.9, "dodge": 0.95, "tactics": 0.98, "learning": 0.95}

def create_ai(entity_data: Dict) -> AngelTierAI:
    return AngelTierAI(entity_data)

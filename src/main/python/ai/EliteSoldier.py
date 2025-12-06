"""EliteSoldier AI - 精鋭兵（ティア2）- プレイヤースタイル"""
from typing import Dict
from player_style_base import PlayerStyleBaseAI

class EliteSoldierAI(PlayerStyleBaseAI):
    """
    精鋭兵（ティア2）- 強化された戦闘能力

    特徴:
    - より高速で正確な攻撃
    - 4段コンボ
    - 50%の回避率
    - 基本的な属性攻撃（火、氷）
    - ダイヤモンド装備
    - 改善された戦術判断
    """

    def __init__(self, entity_data: Dict):
        super().__init__(tier=2, entity_data=entity_data)

        # 精鋭兵の強化パラメータ
        self.attack_range = 3.5
        self.dash_attack_range = 9.0
        self.charge_attack_range = 6.0

        # クールダウン（より速い攻撃）
        self.attack_cooldown = 1.2
        self.dash_cooldown = 2.5
        self.charge_cooldown = 3.5

        # コンボと回避
        self.combo_max = 4
        self.dodge_success_rate = 0.5  # 50%の回避率

        # 攻撃確率
        self.dash_attack_chance = 0.5
        self.charge_attack_chance = 0.4
        self.charge_attack_chance_vs_player = 0.65  # プレイヤー相手
        self.charge_attack_chance_vs_mob = 0.45     # Mob相手

        # 属性攻撃（火と氷）
        self.elemental_types = ["fire", "ice"]
        self.elemental_attack_chance = 0.2  # 20%の確率
        self.elemental_cooldown = 4.0

        # 撤退・回復システム
        self.retreat_hp_threshold = 0.25  # HP25%以下で撤退判断
        self.heal_amount_per_tick = 0.8   # より速い回復
        self.heal_cooldown = 8.0

        # 複数敵対応
        self.surrounded_threshold = 3
        self.surrounded_distance = 5.0

    def _get_base_damage_multiplier(self) -> float:
        """基本ダメージ倍率: 1.3倍"""
        return 1.3

    def _get_dash_damage_multiplier(self) -> float:
        """ダッシュ攻撃倍率: 1.8倍"""
        return 1.8

    def _get_charge_damage_multiplier(self, charge_level: float) -> float:
        """チャージ攻撃倍率: 2.5倍 + チャージレベル × 1.2"""
        return 2.5 + charge_level * 1.2

    def _get_move_speed(self) -> float:
        """移動速度: 2.3（一般兵より速い）"""
        return 2.3

    def _get_dodge_speed(self) -> float:
        """回避速度: 0.75（一般兵より速い）"""
        return 0.75

    def _initialize_equipment(self) -> Dict:
        """ダイヤモンド装備（基本エンチャント付き）"""
        return {
            "helmet": "diamond_helmet",
            "chestplate": "diamond_chestplate",
            "leggings": "diamond_leggings",
            "boots": "diamond_boots",
            "enchantments": {
                "protection": 2,      # 保護II
                "sharpness": 2,       # 鋭さII
                "unbreaking": 2       # 耐久力II
            }
        }

    def _initialize_drop_table(self) -> Dict:
        """ティア2のドロップテーブル"""
        return {
            "common": ["diamond_sword", "diamond", "golden_apple"],
            "uncommon": ["diamond_chestplate", "diamond_helmet", "enchanted_book"],
            "rare": ["netherite_scrap", "enchanted_golden_apple"],
            "epic": ["netherite_sword"],
            "legendary": []
        }

    def get_skill_level(self) -> Dict:
        """
        精鋭兵のスキルレベル

        ティア1（一般兵）より全体的に向上
        """
        return {
            "melee": 0.5,      # 近接戦闘（0.3→0.5）
            "ranged": 0.1,     # 遠距離（まだ弱い）
            "magic": 0.2,      # 魔法（属性攻撃開始）
            "defense": 0.4,    # 防御（0.2→0.4）
            "dodge": 0.5,      # 回避（0.3→0.5）
            "tactics": 0.4,    # 戦術（0.2→0.4）
            "learning": 0.2    # 学習（0.1→0.2）
        }

    def _should_use_charge_attack(self, target_data: Dict) -> bool:
        """
        チャージ攻撃の使用判断

        精鋭兵はプレイヤー相手により積極的にチャージ攻撃を使用
        """
        is_player = target_data.get("is_player", False)

        # プレイヤー相手には積極的（65%）
        if is_player:
            return self._random.random() < self.charge_attack_chance_vs_player

        # Mob相手は通常確率（45%）
        return self._random.random() < self.charge_attack_chance_vs_mob

    def _should_retreat(self, world_data: Dict) -> bool:
        """
        撤退判断

        精鋭兵はより賢く撤退する
        - HP25%以下
        - 複数の敵に囲まれている
        - 強力な敵と対峙している
        """
        # 基本的なHP判断
        hp_ratio = self.entity_data.get("health", 1.0) / self.entity_data.get("max_health", 1.0)
        if hp_ratio <= self.retreat_hp_threshold:
            return True

        # 複数の敵に囲まれている
        nearby_enemies = world_data.get("nearby_enemies", [])
        if len(nearby_enemies) >= self.surrounded_threshold:
            return True

        return False

def create_ai(entity_data: Dict) -> EliteSoldierAI:
    """AIインスタンスを作成"""
    return EliteSoldierAI(entity_data)

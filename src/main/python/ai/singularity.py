"""Singularity AI - 特異点（ティア3）- プレイヤースタイル"""
from typing import Dict
from player_style_base import PlayerStyleBaseAI

class SingularityAI(PlayerStyleBaseAI):
    """
    特異点（ティア3）- 完璧に近い戦術判断

    特徴:
    - 非常に高速で精密な攻撃
    - 6段コンボ
    - 70%の回避率
    - 属性攻撃（火、氷）を積極的に使用
    - ネザライト級装備
    - 高度な戦術判断
    - 学習能力が高い
    """

    def __init__(self, entity_data: Dict):
        super().__init__(tier=3, entity_data=entity_data)

        # 特異点の高性能パラメータ
        self.attack_range = 4.5
        self.dash_attack_range = 11.0
        self.charge_attack_range = 7.0

        # クールダウン（さらに速い）
        self.attack_cooldown = 1.0
        self.dash_cooldown = 2.0
        self.charge_cooldown = 3.0

        # コンボと回避
        self.combo_max = 6
        self.dodge_success_rate = 0.7  # 70%の回避率

        # 攻撃確率
        self.dash_attack_chance = 0.6
        self.charge_attack_chance = 0.5
        self.charge_attack_chance_vs_player = 0.75  # プレイヤー相手
        self.charge_attack_chance_vs_mob = 0.55     # Mob相手

        # 属性攻撃（火と氷）- より積極的
        self.elemental_types = ["fire", "ice"]
        self.elemental_attack_chance = 0.3  # 30%の確率
        self.elemental_cooldown = 3.5

        # 撤退・回復システム
        self.retreat_hp_threshold = 0.20  # HP20%以下で撤退判断
        self.heal_amount_per_tick = 1.0   # より速い回復
        self.heal_cooldown = 6.0

        # 複数敵対応
        self.surrounded_threshold = 4  # より多くの敵に対応
        self.surrounded_distance = 6.0

        # 戦術的判断の強化
        self.tactical_retreat_enabled = True
        self.counter_attack_chance = 0.6  # 60%でカウンター攻撃

    def _get_base_damage_multiplier(self) -> float:
        """基本ダメージ倍率: 1.6倍"""
        return 1.6

    def _get_dash_damage_multiplier(self) -> float:
        """ダッシュ攻撃倍率: 2.2倍"""
        return 2.2

    def _get_charge_damage_multiplier(self, charge_level: float) -> float:
        """チャージ攻撃倍率: 3.0倍 + チャージレベル × 1.5"""
        return 3.0 + charge_level * 1.5

    def _get_move_speed(self) -> float:
        """移動速度: 2.6（精鋭兵より速い）"""
        return 2.6

    def _get_dodge_speed(self) -> float:
        """回避速度: 1.0（精鋭兵より速い）"""
        return 1.0

    def _initialize_equipment(self) -> Dict:
        """ダイヤモンド装備（強化エンチャント付き）"""
        return {
            "helmet": "diamond_helmet",
            "chestplate": "diamond_chestplate",
            "leggings": "diamond_leggings",
            "boots": "diamond_boots",
            "enchantments": {
                "protection": 3,      # 保護III
                "sharpness": 3,       # 鋭さIII
                "unbreaking": 3,      # 耐久力III
                "thorns": 1,          # 棘の鎧I
                "fire_protection": 1  # 火炎耐性I
            }
        }

    def _initialize_drop_table(self) -> Dict:
        """ティア3のドロップテーブル"""
        return {
            "common": ["diamond_sword", "diamond", "enchanted_book"],
            "uncommon": ["diamond_chestplate", "netherite_scrap", "fire_charge"],
            "rare": ["netherite_sword", "totem_of_undying", "enchanted_golden_apple"],
            "epic": ["netherite_chestplate", "nether_star"],
            "legendary": ["elytra"]
        }

    def get_skill_level(self) -> Dict:
        """
        特異点のスキルレベル

        ティア2（精鋭兵）より大幅に向上
        """
        return {
            "melee": 0.7,      # 近接戦闘（0.5→0.7）
            "ranged": 0.3,     # 遠距離（0.1→0.3）
            "magic": 0.4,      # 魔法（0.2→0.4、属性攻撃強化）
            "defense": 0.6,    # 防御（0.4→0.6）
            "dodge": 0.7,      # 回避（0.5→0.7）
            "tactics": 0.8,    # 戦術（0.4→0.8）
            "learning": 0.7    # 学習（0.2→0.7、大幅向上）
        }

    def _should_use_charge_attack(self, target_data: Dict) -> bool:
        """
        チャージ攻撃の使用判断

        特異点は状況に応じて最適な判断を行う
        """
        is_player = target_data.get("is_player", False)
        target_hp_ratio = target_data.get("hp_ratio", 1.0)

        # プレイヤー相手には非常に積極的（75%）
        if is_player:
            # ターゲットのHPが高い場合はさらに積極的
            if target_hp_ratio > 0.7:
                return self._random.random() < 0.85
            return self._random.random() < self.charge_attack_chance_vs_player

        # Mob相手も比較的積極的（55%）
        return self._random.random() < self.charge_attack_chance_vs_mob

    def _should_retreat(self, world_data: Dict) -> bool:
        """
        撤退判断

        特異点は非常に賢く、戦術的に撤退する
        - HP20%以下
        - 複数の敵に囲まれている（4体以上）
        - 強力な敵と不利な状況で対峙
        """
        # 基本的なHP判断
        hp_ratio = self.entity_data.get("health", 1.0) / self.entity_data.get("max_health", 1.0)
        if hp_ratio <= self.retreat_hp_threshold:
            return True

        # 複数の敵に囲まれている
        nearby_enemies = world_data.get("nearby_enemies", [])
        if len(nearby_enemies) >= self.surrounded_threshold:
            # HPが50%以下の場合は撤退
            if hp_ratio < 0.5:
                return True

        # 強力な敵（プレイヤーまたはボス）が近くにいて、HPが低い
        has_strong_enemy = any(
            enemy.get("is_player", False) or enemy.get("is_boss", False)
            for enemy in nearby_enemies
        )
        if has_strong_enemy and hp_ratio < 0.4:
            return True

        return False

    def _should_use_elemental_attack(self, world_data: Dict) -> bool:
        """
        属性攻撃の使用判断

        特異点は状況に応じて属性攻撃を使い分ける
        """
        current_time = world_data.get("game_time", 0.0)

        # クールダウン中はfalse
        if current_time - self.last_elemental_time < self.elemental_cooldown:
            return False

        # 通常の確率判定
        if self._random.random() < self.elemental_attack_chance:
            return True

        # 複数の敵がいる場合は積極的に使用
        nearby_enemies = world_data.get("nearby_enemies", [])
        if len(nearby_enemies) >= 2:
            return self._random.random() < 0.5

        return False

def create_ai(entity_data: Dict) -> SingularityAI:
    """AIインスタンスを作成"""
    return SingularityAI(entity_data)

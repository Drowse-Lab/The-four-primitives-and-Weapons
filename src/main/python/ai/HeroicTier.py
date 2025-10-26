"""HeroicTier AI - 英雄級（ティア4）- プレイヤースタイル"""
from typing import Dict
from player_style_base import PlayerStyleBaseAI

class HeroicTierAI(PlayerStyleBaseAI):
    """
    英雄級（ティア4）- 圧倒的な戦闘力

    特徴:
    - 極めて高速で完璧な攻撃
    - 8段コンボ
    - 80%の回避率
    - 全属性攻撃（火、氷、雷）
    - 最強装備（ネザライト）
    - 完璧な戦術判断
    - 高い学習能力と適応力
    - カウンター攻撃とパリィ
    """

    def __init__(self, entity_data: Dict):
        super().__init__(tier=4, entity_data=entity_data)

        # 英雄級の最強パラメータ
        self.attack_range = 5.0
        self.dash_attack_range = 13.0
        self.charge_attack_range = 8.0

        # クールダウン（最速）
        self.attack_cooldown = 0.8
        self.dash_cooldown = 1.8
        self.charge_cooldown = 2.5

        # コンボと回避
        self.combo_max = 8
        self.dodge_success_rate = 0.8  # 80%の回避率

        # 攻撃確率
        self.dash_attack_chance = 0.7
        self.charge_attack_chance = 0.6
        self.charge_attack_chance_vs_player = 0.85  # プレイヤー相手
        self.charge_attack_chance_vs_mob = 0.65     # Mob相手

        # 属性攻撃（火、氷、雷）- 非常に積極的
        self.elemental_types = ["fire", "ice", "lightning"]
        self.elemental_attack_chance = 0.4  # 40%の確率
        self.elemental_cooldown = 3.0

        # 撤退・回復システム
        self.retreat_hp_threshold = 0.15  # HP15%以下で撤退判断
        self.heal_amount_per_tick = 1.2   # 最速回復
        self.heal_cooldown = 5.0

        # 複数敵対応
        self.surrounded_threshold = 5  # 5体以上の敵にも対応
        self.surrounded_distance = 7.0

        # 戦術的判断の最大化
        self.tactical_retreat_enabled = True
        self.counter_attack_chance = 0.75  # 75%でカウンター攻撃
        self.parry_chance = 0.4  # 40%でパリィ成功

        # 適応学習
        self.adaptive_learning_enabled = True
        self.pattern_recognition_level = 0.8

    def _get_base_damage_multiplier(self) -> float:
        """基本ダメージ倍率: 2.0倍"""
        return 2.0

    def _get_dash_damage_multiplier(self) -> float:
        """ダッシュ攻撃倍率: 2.8倍"""
        return 2.8

    def _get_charge_damage_multiplier(self, charge_level: float) -> float:
        """チャージ攻撃倍率: 4.0倍 + チャージレベル × 2.0"""
        return 4.0 + charge_level * 2.0

    def _get_move_speed(self) -> float:
        """移動速度: 3.0（最速）"""
        return 3.0

    def _get_dodge_speed(self) -> float:
        """回避速度: 1.2（最速）"""
        return 1.2

    def _initialize_equipment(self) -> Dict:
        """ネザライト装備（最強エンチャント付き）"""
        return {
            "helmet": "netherite_helmet",
            "chestplate": "netherite_chestplate",
            "leggings": "netherite_leggings",
            "boots": "netherite_boots",
            "enchantments": {
                "protection": 4,         # 保護IV
                "sharpness": 5,          # 鋭さV
                "unbreaking": 3,         # 耐久力III
                "thorns": 2,             # 棘の鎧II
                "fire_protection": 2,    # 火炎耐性II
                "blast_protection": 1,   # 爆発耐性I
                "feather_falling": 4     # 落下軽減IV
            }
        }

    def _initialize_drop_table(self) -> Dict:
        """ティア4のドロップテーブル"""
        return {
            "common": ["netherite_ingot", "diamond", "emerald"],
            "uncommon": ["netherite_scrap", "enchanted_book", "totem_of_undying"],
            "rare": ["netherite_sword", "netherite_chestplate", "enchanted_golden_apple"],
            "epic": ["nether_star", "dragon_breath", "elytra"],
            "legendary": ["beacon", "dragon_egg"]
        }

    def get_skill_level(self) -> Dict:
        """
        英雄級のスキルレベル

        全てのスキルが最高レベル
        """
        return {
            "melee": 0.9,      # 近接戦闘（0.7→0.9、ほぼ完璧）
            "ranged": 0.6,     # 遠距離（0.3→0.6）
            "magic": 0.7,      # 魔法（0.4→0.7、全属性使用）
            "defense": 0.8,    # 防御（0.6→0.8）
            "dodge": 0.9,      # 回避（0.7→0.9、ほぼ完璧）
            "tactics": 0.95,   # 戦術（0.8→0.95、完璧に近い）
            "learning": 0.9    # 学習（0.7→0.9、高度な適応）
        }

    def _should_use_charge_attack(self, target_data: Dict) -> bool:
        """
        チャージ攻撃の使用判断

        英雄級は状況を完璧に判断し、最適なタイミングで使用
        """
        is_player = target_data.get("is_player", False)
        target_hp_ratio = target_data.get("hp_ratio", 1.0)
        target_distance = target_data.get("distance", 10.0)

        # プレイヤー相手には極めて積極的（85%）
        if is_player:
            # ターゲットのHPが高い場合はほぼ確実
            if target_hp_ratio > 0.7:
                return self._random.random() < 0.9
            # 距離が遠い場合も積極的
            if target_distance > 5.0:
                return self._random.random() < 0.85
            return self._random.random() < self.charge_attack_chance_vs_player

        # Mob相手も積極的（65%）
        if target_hp_ratio > 0.8:
            return self._random.random() < 0.75
        return self._random.random() < self.charge_attack_chance_vs_mob

    def _should_retreat(self, world_data: Dict) -> bool:
        """
        撤退判断

        英雄級は極めて賢く、戦術的撤退を完璧に実行
        - HP15%以下
        - 複数の敵に囲まれている（5体以上）
        - 圧倒的不利な状況
        """
        # 基本的なHP判断
        hp_ratio = self.entity_data.get("health", 1.0) / self.entity_data.get("max_health", 1.0)
        if hp_ratio <= self.retreat_hp_threshold:
            return True

        # 複数の敵に囲まれている
        nearby_enemies = world_data.get("nearby_enemies", [])
        enemy_count = len(nearby_enemies)

        # 5体以上の敵に囲まれている場合
        if enemy_count >= self.surrounded_threshold:
            # HPが40%以下の場合は撤退
            if hp_ratio < 0.4:
                return True
            # HPが60%以下で7体以上なら撤退
            if hp_ratio < 0.6 and enemy_count >= 7:
                return True

        # 強力な敵（プレイヤーまたはボス）が近くにいて、HPが低い
        strong_enemies = [
            enemy for enemy in nearby_enemies
            if enemy.get("is_player", False) or enemy.get("is_boss", False)
        ]
        if len(strong_enemies) >= 2 and hp_ratio < 0.3:
            return True

        return False

    def _should_use_elemental_attack(self, world_data: Dict) -> bool:
        """
        属性攻撃の使用判断

        英雄級は状況に応じて最適な属性攻撃を選択
        """
        current_time = world_data.get("game_time", 0.0)

        # クールダウン中はfalse
        if current_time - self.last_elemental_time < self.elemental_cooldown:
            return False

        # 通常の確率判定（40%）
        if self._random.random() < self.elemental_attack_chance:
            return True

        # 複数の敵がいる場合は非常に積極的
        nearby_enemies = world_data.get("nearby_enemies", [])
        if len(nearby_enemies) >= 2:
            return self._random.random() < 0.6

        # 強力な敵相手にも積極的
        has_strong_enemy = any(
            enemy.get("is_player", False) or enemy.get("is_boss", False)
            for enemy in nearby_enemies
        )
        if has_strong_enemy:
            return self._random.random() < 0.5

        return False

    def _should_use_counter_attack(self, world_data: Dict) -> bool:
        """
        カウンター攻撃の判断

        英雄級は高確率でカウンター攻撃を成功させる
        """
        # 被ダメージタイミングかチェック
        is_taking_damage = world_data.get("is_taking_damage", False)
        if not is_taking_damage:
            return False

        # 75%の確率でカウンター
        return self._random.random() < self.counter_attack_chance

    def _should_parry(self, world_data: Dict) -> bool:
        """
        パリィの判断

        英雄級は攻撃を読んでパリィを試みる
        """
        # 攻撃を受けそうなタイミングかチェック
        incoming_attack = world_data.get("incoming_attack", False)
        if not incoming_attack:
            return False

        # 40%の確率でパリィ成功
        return self._random.random() < self.parry_chance

def create_ai(entity_data: Dict) -> HeroicTierAI:
    """AIインスタンスを作成"""
    return HeroicTierAI(entity_data)

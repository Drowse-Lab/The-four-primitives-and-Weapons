# ALife AI System - プレイヤーっぽい動きをするMobシステム

## 概要

このシステムは、ALife（人工生命）の概念を使用して、プレイヤーのような知的な動きをするMobを実現します。

## ティア構成

各ティアは異なる能力レベルを持ちます（数字が小さいほど弱い）：

### ティア1: CommonSoldier（一般兵） ✅ **実装済み**
- **戦闘スタイル**: 基本的な近接戦闘
- **回避成功率**: 30%
- **攻撃パターン**: 単純（スラッシュ→突き、2コンボ）
- **反応速度**: 遅い（1.0倍）
- **戦術レベル**: 低い（30%）
- **学習速度**: 遅い（10%）
- **使用可能武器**: 近接武器のみ

### ティア2: EliteSoldier（エリート兵） 🚧 **未実装**
- **戦闘スタイル**: 中級近接戦闘＋弓
- **回避成功率**: 50%
- **攻撃パターン**: 中級（3-4コンボ、フェイント）
- **反応速度**: 速い（0.8倍）
- **戦術レベル**: 中級（50%）
- **学習速度**: 普通（20%）

### ティア3: Singularity（特異点） 🚧 **未実装**
- **戦闘スタイル**: 高度な近接＋遠距離＋魔法
- **回避成功率**: 70%
- **攻撃パターン**: 高度（5-6コンボ、カウンター）
- **反応速度**: かなり速い（0.6倍）
- **戦術レベル**: 高い（70%）
- **学習速度**: 速い（40%）

### ティア4: HeroicTier（英雄級） 🚧 **未実装**
- **戦闘スタイル**: 非常に高度、全武器マスター
- **回避成功率**: 80%
- **攻撃パターン**: 非常に高度（無限コンボ、完璧なタイミング）
- **反応速度**: 非常に速い（0.5倍）
- **戦術レベル**: 非常に高い（80%）
- **学習速度**: 非常に速い（60%）

### ティア5: MythicalTier（神話級） 🚧 **未実装**
- **戦闘スタイル**: プロレベル、予測攻撃
- **回避成功率**: 90%
- **攻撃パターン**: プロレベル（状況対応型）
- **反応速度**: 超速（0.3倍）
- **戦術レベル**: プロレベル（90%）
- **学習速度**: 非常に速い（80%）

### ティア6: AngelTier（天使級） 🚧 **未実装**
- **戦闘スタイル**: 超人レベル、完璧な予測
- **回避成功率**: 95%
- **攻撃パターン**: 超人レベル
- **反応速度**: 極速（0.2倍）
- **戦術レベル**: 超人レベル（95%）
- **学習速度**: 超速（90%）

### ティア7: DivineTier（神聖級） 🚧 **未実装**
- **戦闘スタイル**: 神レベル、完全に未来を予測
- **回避成功率**: 99%
- **攻撃パターン**: 神レベル
- **反応速度**: 光速（0.1倍）
- **戦術レベル**: 完璧（100%）
- **学習速度**: 即座（100%）

## ファイル構成

```
src/main/python/ai/
├── README.md                   # このファイル
├── base_alife_ai.py           # 基本AIシステム
├── CommonSoldier.py           # ティア1: 一般兵（実装済み） ✅
├── EliteSoldier.py            # ティア2: エリート兵（準備中）
├── singularity.py             # ティア3: 特異点（準備中）
├── HeroicTier.py              # ティア4: 英雄級（準備中）
├── MythicalTier.py            # ティア5: 神話級（準備中）
├── AngelTier.py               # ティア6: 天使級（準備中）
└── DivineTier.py              # ティア7: 神聖級（準備中）

src/main/java/minecraftarmorweapon/ai/
└── ALifeAIBridge.java         # Java連携ブリッジ ✅
```

## 使い方

### 1. PythonでAIをテストする

```bash
cd src/main/python/ai
python CommonSoldier.py
```

### 2. Javaから使用する

```java
// MobエンティティにAIを追加
Mob mob = ...;
ALifeAIBridge ai = new ALifeAIBridge(mob, 1); // ティア1

// 毎ティック更新
AIAction action = ai.update();

// 行動を実行
switch (action.action) {
    case "move":
        // 移動処理
        break;
    case "attack":
        // 攻撃処理
        break;
    case "dodge":
        // 回避処理
        break;
}
```

## 主要機能

### 状態機械（State Machine）
- **IDLE**: 待機
- **PATROL**: 巡回
- **SEARCH**: 探索
- **COMBAT**: 戦闘
- **RETREAT**: 撤退
- **DODGE**: 回避
- **HEAL**: 回復
- **REPOSITION**: 位置調整

### 行動パターン
1. **基本移動**: 敵に接近、距離を保つ
2. **攻撃**: コンボ攻撃、フェイント
3. **回避**: 横回避、バックステップ
4. **戦術**: 最適な位置取り、武器切り替え

### 学習と適応
- ダメージを受けた状況を記憶
- 回避成功/失敗から学習
- 敵の行動パターンを分析
- ティアに応じた学習速度

## 開発予定

### Phase 1: 基礎システム（完了） ✅
- [x] BaseALifeAI実装
- [x] CommonSoldierAI実装
- [x] Java連携ブリッジ実装
- [x] 基本的な状態機械
- [x] 戦闘システム

### Phase 2: エリート兵実装（次回）
- [ ] EliteSoldierAI実装
- [ ] 弓攻撃システム
- [ ] フェイント動作
- [ ] 中級戦術

### Phase 3: 特異点実装
- [ ] SingularityAI実装
- [ ] 魔法攻撃システム
- [ ] カウンターシステム
- [ ] 高度な学習機能

### Phase 4: 高ティア実装
- [ ] HeroicTier, MythicalTier実装
- [ ] 予測攻撃システム
- [ ] 完璧なタイミング
- [ ] プロレベルの戦術

### Phase 5: 最高ティア実装
- [ ] AngelTier, DivineTier実装
- [ ] 完全な未来予測
- [ ] 神レベルの戦闘
- [ ] 究極の学習能力

## テスト方法

### CommonSoldier（ティア1）のテスト

```python
from CommonSoldier import create_ai

# テストデータ
entity_data = {
    "health": 20.0,
    "max_health": 20.0,
    "position": (0, 0, 0)
}

world_data = {
    "nearest_enemy_position": (5, 0, 0),
    "nearest_enemy_distance": 5.0,
    "current_time": 0,
    "incoming_attack": False
}

# AI作成
ai = create_ai(entity_data)

# 更新
for i in range(100):
    action = ai.update(0.05, world_data)  # 50ms毎
    print(f"State: {ai.current_state.value}, Action: {action['action']}")
```

## 技術詳細

### AIアーキテクチャ

```
BaseALifeAI (基本クラス)
    ├── 状態機械
    ├── 意思決定システム
    ├── 学習システム
    └── 行動選択
         │
         └── CommonSoldierAI (ティア1)
             ├── 近接戦闘特化
             ├── 基本的な回避
             └── 単純な戦術
```

### パラメータ調整

各ティアのパラメータは`base_alife_ai.py`の以下のメソッドで調整できます：
- `_get_reaction_speed()`: 反応速度
- `_get_tactical_awareness()`: 戦術認識度
- `_get_learning_rate()`: 学習速度

## 注意事項

1. **パフォーマンス**: 高ティアのAIは計算量が多いため、同時に多数スポーンすると重くなる可能性があります
2. **バランス調整**: 各ティアの強さは継続的に調整が必要です
3. **Python実行環境**: 現在はJavaで直接実装していますが、将来的にはJythonやGraalVMを使用してPythonコードを実行予定です

## ライセンス

このプロジェクトのライセンスに従います。

## 作成者

minecraft_armor_weapon mod team

## 更新履歴

- 2025-10-12: 初版作成、CommonSoldier実装完了

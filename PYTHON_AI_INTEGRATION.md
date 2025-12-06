# Python A-Life AI 連携システム

JavaとPythonを連携させて、柔軟で拡張可能なA-Life AIシステムを実現します。

## 概要

このシステムでは、**Pythonを変更するだけでAIの動作を変更できます**。Javaのコードを触る必要はありません。

### システム構成

```
┌──────────────┐      JSON      ┌─────────────────┐
│              │  ◄──────────►  │                 │
│  Java (MC)   │                │  Python AI      │
│  PythonALife │                │  (CommonSoldier)│
│  AIBridge    │                │                 │
└──────────────┘                └─────────────────┘
      ▲
      │ フォールバック
      ▼
┌──────────────┐
│  Java AI     │
│  (既存)      │
└──────────────┘
```

## 主要ファイル

### Java側

1. **PythonAIProcess.java** - Pythonプロセス管理
   - Pythonスクリプトを起動
   - JSON通信を管理
   - エラーハンドリング

2. **PythonALifeAIBridge.java** - Python連携ブリッジ
   - MobからPythonへのデータ変換
   - Pythonからのアクションを実行
   - フォールバック機能（Python失敗時は既存のJava AIを使用）

3. **ALifeAIBridge.java** - 既存のJava AI（フォールバック用）

### Python側

1. **ai_bridge_wrapper.py** - JSON通信ラッパー
   - 標準入出力でJSON通信
   - AIインスタンスの管理
   - エラーハンドリング

2. **CommonSoldier.py** - 一般兵AI（ティア1）
   - 戦闘ロジック
   - 回避判定
   - 状態遷移

3. **base_alife_ai.py** - AIの基底クラス

## 使い方

### 1. 基本的な使い方

**Javaから使用:**

```java
// PythonALifeAIBridgeを作成
PythonALifeAIBridge ai = new PythonALifeAIBridge(mob, tier);

// 毎ティック更新
ALifeAIBridge.AIAction action = ai.update();

// アクションを実行
switch (action.action) {
    case "attack":
        // 攻撃処理
        break;
    case "move":
        // 移動処理
        break;
    // ...
}

// エンティティ削除時
ai.destroy();
```

### 2. PythonのAIを変更する

**新しいAIを追加する場合:**

1. `src/main/python/ai/` に新しいAIファイルを作成（例: `EliteSoldier.py`）

```python
from base_alife_ai import BaseALifeAI

class EliteSoldierAI(BaseALifeAI):
    def __init__(self, entity_data):
        super().__init__(tier=2, entity_data=entity_data)

        # エリート兵のパラメータ
        self.attack_range = 5.0
        self.dodge_success_rate = 0.5

    def _handle_combat(self, world_data):
        # エリート兵の戦闘ロジック
        # ...

def create_ai(entity_data):
    return EliteSoldierAI(entity_data)
```

2. `ai_bridge_wrapper.py` のマッピングに追加

```python
from EliteSoldier import create_ai as create_elite_soldier_ai

AI_FACTORY_MAP = {
    1: create_common_soldier_ai,  # 一般兵
    2: create_elite_soldier_ai,   # エリート兵（追加）
    # ...
}
```

**これだけです！Javaのコードは一切変更不要です。**

### 3. 既存のAIを変更する

`CommonSoldier.py` を直接編集してください。例えば:

```python
# 回避成功率を変更
self.dodge_success_rate = 0.5  # 30% → 50%

# 攻撃範囲を変更
self.attack_range = 5.0  # 3.0 → 5.0

# 戦闘ロジックを変更
def _execute_attack(self, world_data):
    # 新しい攻撃パターン
    # ...
```

変更後、Minecraftを再起動すれば反映されます。

## データ構造

### Java → Python（ワールドデータ）

```json
{
  "current_time": 1234567.89,
  "nearest_enemy_position": {
    "x": 100.0,
    "y": 64.0,
    "z": 200.0
  },
  "nearest_enemy_distance": 5.3
}
```

### Java → Python（エンティティデータ）

```json
{
  "health": 20.0,
  "max_health": 20.0,
  "position": {
    "x": 95.0,
    "y": 64.0,
    "z": 195.0
  }
}
```

### Python → Java（アクション）

```json
{
  "action": "attack",
  "data": {
    "type": "slash",
    "combo": 1,
    "damage_multiplier": 1.5,
    "speed": 0.3,
    "direction": [1.0, 0.0, 0.0],
    "look_at": {"x": 100, "y": 64, "z": 200}
  }
}
```

## アクションタイプ

### 移動系
- **idle** - 待機
- **move** - 移動
- **move_to_target** - ターゲットに向かって移動
- **move_away** - ターゲットから離れる
- **strafe** - 横移動

### 戦闘系
- **attack** - 攻撃
- **charge_attack** - チャージ攻撃
- **use_weapon_skill** - 武器スキル使用

### 回避系
- **dodge** - 回避

### 探索系
- **search** - 探索
- **patrol** - 巡回

## エラーハンドリング

### フォールバック機能

Python AIが失敗した場合、自動的に既存のJava AIにフォールバックします。

```java
// Pythonが失敗しても動作し続ける
ALifeAIBridge.AIAction action = ai.update();
// ↑ Pythonエラー時は自動的にJava AIが使われる
```

### Python無効化（デバッグ用）

```java
// Pythonを無効化してJava AIのみ使用
PythonALifeAIBridge.disablePython();

// Pythonを再度有効化
PythonALifeAIBridge.enablePython();

// Pythonが有効か確認
boolean isPythonEnabled = PythonALifeAIBridge.isPythonEnabled();
```

## トラブルシューティング

### Pythonが起動しない

1. **Pythonがインストールされているか確認**
   ```bash
   python --version
   # または
   python3 --version
   ```

2. **必要なライブラリをインストール**
   ```bash
   # 特別なライブラリは不要（標準ライブラリのみ）
   ```

3. **ログを確認**
   ```
   Python AI Error: ...
   ```
   というログが出力されます。

### Pythonは起動しているがAIが動かない

1. **ai_bridge_wrapper.py のパスを確認**
   - `src/main/python/ai/ai_bridge_wrapper.py` が存在するか

2. **CommonSoldier.py のインポートエラーを確認**
   - `base_alife_ai.py` が同じディレクトリにあるか

3. **JSON通信のログを確認**
   - コンソールに "Python AI initialized" と表示されるか

### AI動作がおかしい

1. **Python側のデバッグ**
   - `CommonSoldier.py` の最後の `if __name__ == "__main__":` 部分で単体テスト可能

   ```bash
   cd src/main/python/ai
   python CommonSoldier.py
   ```

2. **Java側でフォールバックを強制**
   ```java
   PythonALifeAIBridge.disablePython();
   ```
   これでJava AIのみで動作確認できます。

## パフォーマンス

- **レイテンシ**: 約1-5ms/更新（JSON通信のオーバーヘッド）
- **メモリ**: Pythonプロセス + AI数 × 数KB
- **CPU**: Pythonが別プロセスなので並列実行可能

### 最適化のヒント

1. **Python側でキャッシュを使う**
   ```python
   @functools.lru_cache(maxsize=128)
   def expensive_calculation(self, ...):
       # ...
   ```

2. **不要なデータを送らない**
   - 必要最小限のワールドデータのみ送信

3. **更新頻度を調整**
   ```java
   // 2tickに1回更新
   if (entity.tickCount % 2 == 0) {
       action = ai.update();
   }
   ```

## 将来の拡張

### 新しいティアを追加

1. Python側で新しいAIクラスを作成
2. `ai_bridge_wrapper.py` のマッピングに追加
3. Javaから `new PythonALifeAIBridge(mob, newTier)` で使用

### 複雑なワールド情報を追加

1. `PythonALifeAIBridge.createWorldDataJson()` を拡張
2. Python側で新しいデータを使用

```java
// Java側
private JsonObject createWorldDataJson() {
    // ...

    // 新しいデータを追加
    data.addProperty("time_of_day", entity.level.getDayTime());
    data.addProperty("weather", entity.level.isRaining() ? "rain" : "clear");

    return data;
}
```

```python
# Python側
def update(self, delta_time, world_data):
    time_of_day = world_data.get("time_of_day", 0)
    weather = world_data.get("weather", "clear")

    # 時間や天気に応じて行動を変える
    if weather == "rain":
        return self._seek_shelter()
    # ...
```

## まとめ

このシステムの利点:
- **Pythonを変更するだけ**でAIを変更できる
- **Javaのコードは触らない**
- **フォールバック機能**でエラー時も安全
- **柔軟な拡張性**

Pythonで自由にAIを開発してください！

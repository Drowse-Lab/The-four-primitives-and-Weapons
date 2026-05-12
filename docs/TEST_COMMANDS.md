# /test コマンド一覧

MOD のテスト/デバッグ用コマンド集。権限レベル 2 以上が必要 (OP)。
実装: [TestCommand.java](../src/main/java/the_four_primitives_and_weapons/command/TestCommand.java)

## 概要

| カテゴリ | サブコマンド |
|---|---|
| Mobスポーン | `trait`, `traitall`, `debugmob` |
| 属性武器 | `element`, `elementall` |
| プレイヤー | `heal`, `god` |
| 難易度 | `difficulty`, `info` |
| ダメージテスト | `damage`, `damageall`, `dps` |
| ワールド整理 | `clear` |
| レンダリング調整 | `gaterot` |
| 戦闘ログ | `log` ( `on` / `off` / `status` / `tail` / `analyze` / `refresh` / `path` / `clear` ) |

---

## Mob スポーン

### `/test trait <trait>`
指定した特性を持つゾンビを1体スポーン。
- `<trait>` — 特性名 (タブ補完対応)。`MobTrait` enum の値

### `/test traitall`
全特性のゾンビを自分の周囲に円形配置でスポーン。

### `/test debugmob`
デバッグMob(サンドバッグ) を足元にスポーン。
- カスタムエンティティ `the_four_primitives_and_weapons:debug_mob`

---

## 属性武器

### `/test element <element> [level]`
手持ち武器に属性を付与。
- `<element>` — 属性名 (タブ補完対応)。`NONE` と `ERROR` 以外
- `[level]` — 属性レベル 1〜100 (既定: 1)

### `/test elementall [level]`
全属性のダイヤ剣をインベントリに付与。
- `[level]` — 属性レベル 1〜100 (既定: 5)

---

## プレイヤー

### `/test heal`
自分を全回復: HP・食料・彩度を全快、全エフェクト削除、満腹感付与。

### `/test god`
無敵モードを切り替え (`setInvulnerable`)。

---

## 難易度

### `/test difficulty <name>`
MOD難易度を即変更。
- `<name>` — 難易度名 (タブ補完対応)。`CustomDifficulty` enum の値

### `/test info`
現在の MOD 設定を表示: 難易度・AIレベル・特性確率・エリート確率・バフ確率・ブロック設置/破壊・TrueCrafter有効状態。

---

## ダメージテスト

### `/test damage <amount> <element> [level]`
半径20ブロック以内の最寄りMobに属性ダメージを1発。
- `<amount>` — ダメージ量 (最低 0.1)
- `<element>` — 属性名
- `[level]` — 属性レベル 1〜100 (既定: 1)

### `/test damageall <amount> [level]`
最寄りMobに全属性のダメージを順次与える (デバッグMob用)。
- `<amount>` — 各属性のダメージ量
- `[level]` — 属性レベル 1〜100 (既定: 1)

### `/test dps <element> <level> <seconds>`
DPS計測: 指定秒数間、0.25秒間隔でダメージを与えて合計/DPSを表示。
- `<element>` — 属性名
- `<level>` — 属性レベル 1〜100
- `<seconds>` — 計測秒数 1〜30

---

## ワールド整理

### `/test clear [radius]`
自分の周囲のMob(`Monster`継承) を全削除。
- `[radius]` — 半径 1〜200 ブロック (既定: 50)

---

## レンダリング調整

### `/test gaterot`
Gate剣 projectile の回転/スケールパラメータを表示。

### `/test gaterot <yaw> <pitch> <roll>`
### `/test gaterot <yaw> <pitch> <roll> <sx> <sy> <sz>`
Gate剣の向きとスケールをランタイム調整。
- `<yaw>` `<pitch>` `<roll>` — 度数
- `<sx>` `<sy>` `<sz>` — スケール 0.1〜3.0 (省略時: 全て 0.8)

---

## 戦闘ログ (`/test log ...`)

JSONL 形式の戦闘イベントログを制御/閲覧。
出力先: `.minecraft/logs/combat_ai/combat_events_YYYY-MM-DD.jsonl`

### `/test log` または `/test log status`
有効/無効・今日のファイル名・サイズ・イベント数を表示。

### `/test log on` / `/test log off`
戦闘ログの出力を有効化/無効化 ([CombatLogger.java](../src/main/java/the_four_primitives_and_weapons/ai/lisp/CombatLogger.java))。

### `/test log tail [n]`
今日のログから最新 n 件を表示。
- `[n]` — 件数 1〜200 (既定: 10)

### `/test log path`
ログディレクトリと今日のファイルの絶対パスを表示。Claude Code に読ませる際などに使用。

### `/test log clear`
今日のログファイルを削除。

### `/test log analyze [topN]`
集計統計を表示 ([CombatLogAnalyzer.java](../src/main/java/the_four_primitives_and_weapons/ai/lisp/CombatLogAnalyzer.java))。
Mobタイプ別・プレイヤー別の攻撃回数・平均ダメ・勝率・平均距離を出力。
- `[topN]` — 上位N件 1〜20 (既定: 5)

### `/test log refresh`
戦闘ログキャッシュを即時再解析 (既定は30秒キャッシュ)。

---

## Lisp AI への注入

`CombatLogAnalyzer` は S式AI ([MobAIBrain.java](../src/main/java/the_four_primitives_and_weapons/ai/lisp/MobAIBrain.java)) に以下の変数を注入する。

| 変数 | 意味 |
|---|---|
| `log-events-total` | 解析したイベント総数 |
| `log-mob-type-hit-count` | 同タイプMobの攻撃総数 |
| `log-mob-type-avg-damage` | 同タイプMobの平均与ダメ |
| `log-mob-type-kill-count` | 同タイプMobのキル数 |
| `log-mob-type-death-count` | 同タイプMobの死亡数 |
| `log-mob-type-winrate` | 同タイプMobの勝率 (kill / (kill + death)) |
| `log-player-hit-count` | ターゲットプレイヤーの攻撃総数 |
| `log-player-avg-damage` | ターゲットプレイヤーの平均ダメ |
| `log-player-avg-distance` | ターゲットプレイヤーの攻撃時平均距離 |
| `log-player-melee-rate` | 近接 (< 4m) 比率 |
| `log-player-ranged-rate` | 遠距離 (> 8m) 比率 |
| `log-player-kill-count` | ターゲットプレイヤーのキル数 |

S式ゲノムでの利用例:
```lisp
(if (> log-player-melee-rate 0.7)
    (retreat)
    (if (< log-mob-type-winrate 0.3)
        (charge-attack)
        (attack)))
```

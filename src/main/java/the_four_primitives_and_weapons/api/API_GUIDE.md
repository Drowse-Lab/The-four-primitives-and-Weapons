# the_four_primitives_and_weapons API ガイド

外部modからこのmodを前提modとして利用するためのAPIドキュメント。

## 1. スキル（技）の追加

### 基本的な流れ

```java
import the_four_primitives_and_weapons.api.ISkillAction;
import the_four_primitives_and_weapons.api.ChargeHelper;
import the_four_primitives_and_weapons.skill.SkillRegistry;
import the_four_primitives_and_weapons.skill.SkillRegistry.MotionCategory;
import the_four_primitives_and_weapons.skill.PlayerSkillData.AttackSlot;

// FMLCommonSetupEvent などで登録
public void setup(FMLCommonSetupEvent event) {
    // 特殊スキル: 特定の武器クラスに紐づく
    SkillRegistry.register(
        "my_mod:fire_slash",          // ユニークID（mod名:スキル名）
        "炎斬り",                      // 表示名
        "炎を纏った広範囲斬撃",         // 説明文
        MotionCategory.SPECIAL,        // SPECIAL = 武器紐づき
        EnumSet.allOf(AttackSlot.class), // 使用可能スロット
        "MyFireSwordItem",             // 対応する武器クラス名
        (player, chargePercent) -> {   // ISkillAction実装
            float damage = ChargeHelper.scaleDamage(12.0f, chargePercent, 2.0f);
            ChargeHelper.damageEntitiesInFront(player, 5.0, 3.0, damage);
            ChargeHelper.playSound(player, SoundEvents.BLAZE_SHOOT, 1.0f, 1.0f);
        }
    );

    // 汎用スキル: どの武器でも使える
    SkillRegistry.register(
        "my_mod:ground_pound",
        "地面叩き",
        "地面を叩いて周囲にダメージ",
        MotionCategory.UNIVERSAL,
        EnumSet.of(AttackSlot.CHARGED),  // チャージスロットのみ
        null,                            // UNIVERSAL は null
        (player, chargePercent) -> {
            float damage = ChargeHelper.scaleDamage(8.0f, chargePercent, 3.0f);
            double range = ChargeHelper.scaleValue(3.0, chargePercent, 2.0);
            ChargeHelper.damageEntitiesAround(player, range, damage);
        }
    );
}
```

### AttackSlot 一覧

| スロット | 説明 |
|---------|------|
| `FIRST_HIT` | 一撃目 |
| `SECOND_HIT` | 二撃目 |
| `THIRD_HIT` | 三撃目 |
| `CHARGED` | チャージ攻撃 |
| `DASH` | ダッシュ攻撃 |

### MotionCategory

| カテゴリ | 説明 |
|---------|------|
| `UNIVERSAL` | 全武器で使用可能。`requiredWeaponClass` は `null` |
| `SPECIAL` | 特定の武器クラスでのみ使用可能 |

## 2. ChargeHelper ユーティリティ

チャージ技を簡単に作るためのヘルパークラス。

### ダメージ計算

```java
// チャージ率に応じたダメージスケーリング
// baseDamage=10, chargePercent=1.0, multiplier=2.0 → 30.0
float damage = ChargeHelper.scaleDamage(10.0f, chargePercent, 2.0f);

// 汎用スケーリング（距離、範囲など）
double range = ChargeHelper.scaleValue(3.0, chargePercent, 2.0);

// チャージ判定
if (ChargeHelper.isCharged(chargePercent)) { ... }
if (ChargeHelper.isFullyCharged(chargePercent)) { ... }
```

### ターゲット検索

```java
// 前方の敵を取得（範囲, 横幅）
List<LivingEntity> targets = ChargeHelper.getEntitiesInFront(player, 5.0, 3.0);

// 前方の敵を取得（視線の内積指定: 0.8=狭い, -0.3=広い）
List<LivingEntity> targets = ChargeHelper.getEntitiesInFront(player, 5.0, 3.0, 0.5);

// 周囲の敵を取得
List<LivingEntity> targets = ChargeHelper.getEntitiesAround(player, 4.0);
```

### ダメージ適用

```java
// 前方にダメージ（戻り値: ヒット数）
int hits = ChargeHelper.damageEntitiesInFront(player, 5.0, 3.0, damage);

// 周囲にダメージ
int hits = ChargeHelper.damageEntitiesAround(player, 4.0, damage);

// ターゲットリストにダメージ+ノックバック
ChargeHelper.damageAndKnockback(player, targets, damage, 0.5);
```

### エフェクト

```java
// 前方にパーティクルライン
ChargeHelper.spawnParticleLine(player, ParticleTypes.FLAME, 5.0, 0.3, 3);

// 周囲にパーティクルリング
ChargeHelper.spawnParticleRing(player, ParticleTypes.FLAME, 4.0, 36, 2);

// サウンド再生
ChargeHelper.playSound(player, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 1.0f);
```

## 3. レシピの追加

`data/<mod_id>/rarity_forge_recipes/` にJSONを配置するだけで追加可能。

```json
{
  "pattern": ["1", "1", "2"],
  "key": {
    "1": "minecraft:diamond",
    "2": "minecraft:stick"
  },
  "result": "my_mod:my_weapon"
}
```

- `pattern`: 3x3グリッドの3行（上から順）。各文字がkeyに対応
- `key`: 文字→アイテムIDのマッピング。スペースは空スロット
- `result`: 完成品のアイテムID

## 4. 素材ティアの追加

`data/<mod_id>/rarity_config/material_tiers.json` でティアを追加可能。

```json
{
  "cursed": ["my_mod:cursed_gem"],
  "nether_star": ["my_mod:super_rare_item"],
  "diamond": ["my_mod:rare_crystal"],
  "iron": ["my_mod:common_ore"]
}
```

## 5. 確率テーブルのカスタマイズ

`data/<mod_id>/rarity_config/weights.json` で上書き可能。

```json
{
  "weights_by_level": [
    [100, 0, 0, 0, 0],
    [50, 35, 12, 3, 0],
    ...
  ],
  "tier_bonuses": {
    "none": 0.0,
    "basic": 0.5,
    "iron": 1.0,
    "diamond": 2.0,
    "nether_star": 3.0,
    "cursed": 4.0
  }
}
```

## 6. chargePercent について

| 値 | 意味 |
|----|------|
| `0.0` | 通常攻撃（チャージなし） |
| `0.0 < x < 1.0` | 部分チャージ |
| `1.0` | フルチャージ（3秒） |

- 最小チャージ時間: 1秒（20tick）
- 最大チャージ時間: 3秒（60tick）

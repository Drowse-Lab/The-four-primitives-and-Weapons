# 属性ダメージシステム - 実装完了

## 概要

Minecraft 1.20.1 Forgeモッド用の属性ダメージシステムです。**NBTタグベース**なので、すべてのアイテムに属性を付与できます。

## 実装済み機能

### ✅ 属性
1. **氷属性 (Ice)** - 独自凍結状態への追加ダメージ、attribute modifierによる移動速度低下
2. **電気/雷属性 (Electric)** - 水中AOE、導体装備時ボーナス
3. **侵食/闇属性 (Corrosion/Dark)** - 防御力低下、攻撃力低下、独自DoT
4. **聖属性 (Holy)** - アンデッド特効(2.5倍)、独自glowing tag、高レベルで炎上
5. **魂属性 (Soul)** - 残り体力が低い対象への追加倍率、専用DamageType
6. **燐火属性 (Soul Fire)** - 炎+魂の合成属性、青白い炎上、専用DamageType

### ✅ システム機能
- **NBTタグベース**: どのアイテムにも属性付与可能
- **自動適用**: 通常攻撃、チャージ攻撃、すべての攻撃に自動で属性ダメージが適用
- **ツールチップ表示**: エンチャント風に色付きで属性レベルを表示（ローマ数字）
- **Mixin統合**: DamageSourceとLivingEntityに介入し、透過的に動作

## ファイル構成

### コアファイル
```
src/main/java/the_four_primitives_and_weapons/damage/
├── ElementType.java                    # 属性列挙型
├── IElementalDamageSource.java         # DamageSourceインターフェース
├── ElementalDamageUtils.java           # NBTタグユーティリティ
├── IceElementDamageHandler.java        # 氷属性ハンドラー
├── ElectricElementDamageHandler.java   # 電気属性ハンドラー
├── CorrosionElementDamageHandler.java  # 侵食属性ハンドラー
├── HolyElementDamageHandler.java       # 聖属性ハンドラー
├── SoulElementDamageHandler.java       # 魂属性ハンドラー
├── BloodElementDamageHandler.java      # 血属性ハンドラー (出血DoT + 吸血)
└── SoulFireElementDamageHandler.java   # 燐火属性ハンドラー

src/main/java/the_four_primitives_and_weapons/mixin/
├── DamageSourceMixin.java              # DamageSourceへの属性データ追加
├── LivingEntityDamageMixin.java        # ダメージ計算時の属性適用
└── ItemStackTooltipMixin.java          # ツールチップへの属性表示

src/main/resources/
├── the_four_primitives_and_weapons.mixins.json  # Mixin設定
└── META-INF/mods.toml                  # Mixin読み込み設定
```

### テストアイテム
```
src/main/java/the_four_primitives_and_weapons/item/
├── IceTestKatanaItem.java              # 氷属性テスト刀
├── ElectricTestKatanaItem.java         # 電気属性テスト刀
├── CorrosionTestKatanaItem.java        # 侵食属性テスト刀
└── HolyTestKatanaItem.java             # 聖属性テスト刀
```

## 使用方法

### 既存アイテムに属性を付与
```java
ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
ElementalDamageUtils.setElement(sword, ElementType.ICE, 3);
```

### 新しい属性武器を作成
```java
public class MyCustomSwordItem extends SwordItem {
    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        // 属性を確実に設定
        if (!ElementalDamageUtils.hasElement(stack)) {
            ElementalDamageUtils.setElement(stack, ElementType.HOLY, 2);
        }
    }
}
```

### 属性情報の取得
```java
ItemStack weapon = player.getMainHandItem();
if (ElementalDamageUtils.hasElement(weapon)) {
    ElementType type = ElementalDamageUtils.getElementType(weapon);
    int level = ElementalDamageUtils.getElementLevel(weapon);
}
```

## 技術仕様

### Mixin統合
1. **DamageSourceMixin**: DamageSourceクラスに属性データを追加
2. **LivingEntityDamageMixin**: hurt()メソッドでダメージを計算・変更
3. **ItemStackTooltipMixin**: getTooltipLines()で属性情報を表示

### NBTタグ構造
```
{
  "ElementType": "ice",    // 属性タイプ
  "ElementLevel": 2        // 属性レベル
}
```

### ダメージ計算

#### 氷属性
- 基礎倍率: 1.5x
- レベル倍率: +0.25x/レベル
- 時間ボーナス: 最大+0.5x (独自凍結状態の残り時間)
- 移動速度低下はMobEffectではなく `Attributes.MOVEMENT_SPEED` の一時modifier

#### 電気/雷属性
- 基礎倍率: 1.2x
- 水中倍率: 1.5x
- 導体倍率: +0.3x/導体アイテム
- 雷命中時の硬直はMobEffectではなく独自スロー + 水平減速

#### 侵食/闇属性
- 基礎倍率: 1.1x
- 防御力減少: 2.0 + (ダメージ × 0.5)
- 侵食は `Attributes.ARMOR` の一時modifierで防御力低下
- 闇は `Attributes.ATTACK_DAMAGE` の一時modifierで攻撃力低下、Lv3以上で独自DoT

#### 聖属性
- 基礎倍率: 1.1x
- アンデッド倍率: 2.5x
- レベル倍率: +0.3x/レベル
- MobEffectではなく独自時間管理のglowing tagを付与
- 高レベルで炎上

#### 魂属性
- 基礎倍率: 1.05x
- レベル倍率: +0.04x/レベル(最大+0.40x)
- 対象の残り体力が少ないほど最大+0.35x追加
- `the_four_primitives_and_weapons:soul` の専用DamageTypeを使用

#### 燐火属性
- 炎 + 魂が同じレベルで 1:1 になった時の合成表示属性
- 合成後のNBTは `ElementType` / `ElementType2` に `fire` と `soul` を保持し、表示と実効処理だけが燐火になる
- 基礎倍率: 1.08x
- レベル倍率: +0.035x/レベル(最大+0.35x)
- 対象の残り体力が少ないほど最大+0.25x追加
- 青白い魂の炎で炎上させる
- `the_four_primitives_and_weapons:soul_fire` の専用DamageTypeを使用

## ビルドと実行

### ビルド
```bash
./gradlew build
```

### 開発環境で実行
```bash
./gradlew runClient
```

### テスト方法

#### テストコマンド一覧 (`/test`)

| コマンド | 説明 |
|---|---|
| `/test element <属性> [レベル]` | 手持ち武器に属性を付与 |
| `/test elementall [レベル]` | 全属性のダイヤ剣をインベントリに追加 |
| `/test damage <ダメージ量> <属性> [レベル]` | 最寄りMobに属性ダメージ |
| `/test damageall <ダメージ量> [レベル]` | 最寄りMobに全属性ダメージを順番に |
| `/test dps <属性> <レベル> <秒数>` | DPSテスト |
| `/test debugmob` | デバッグMob（サンドバッグ）をスポーン |
| `/test trait <特性名>` | 特性付きゾンビをスポーン |
| `/test traitall` | 全13特性のゾンビを円形にスポーン |
| `/test heal` | 自分を全回復（HP/空腹/デバフ解除） |
| `/test god` | 無敵モード切替 |
| `/test difficulty <難易度>` | 難易度変更 |
| `/test info` | 現在のMOD設定を表示 |
| `/test clear [半径]` | 周囲のMobを全削除 |

#### 属性ダメージコマンド (`/damage`)

```
/damage <対象> <ダメージ量> <属性名> [レベル]
```

例:
```
/damage @e[type=zombie,limit=1] 20 holy 5
/damage @p 10 ice 3
/damage @e[type=skeleton] 50 electric 10
```

#### 属性武器の入手

手持ち武器に属性を付与:
```
/test element holy 10
/test element ice 5
/test element electric 3
```

全属性の剣を一括入手:
```
/test elementall 5
```

giveコマンドでNBT指定:
```
/give @p minecraft:diamond_sword{ElementType:"HOLY",ElementLevel:10}
/give @p the_four_primitives_and_weapons:old_katana{ElementType:"ICE",ElementLevel:5}
```

#### 属性ダメージの確認方法

1. `/test debugmob` でサンドバッグMobをスポーン
2. 属性武器で殴る → チャットに以下が表示される:
   - `[Debug] ダメージ: 10.0 (種類: player) HP: 1014.0/1024`
   - `[Debug] 属性: HOLY Lv.10 (武器NBT)`
   - `[Debug] 属性DmgSrc: HOLY Lv.10 5.0ダメージ`
3. または `/test damage 20 holy 5` で直接属性ダメージを与えて確認

#### 不死特性の貫通テスト

1. `/test trait undying` で不死ゾンビをスポーン
2. `/test element holy 10` で手持ち武器にHOLYを付与
3. 殴ってトーテムが発動せず死亡すれば成功

#### 使用可能な属性名
`ice`, `electric`, `thunder`, `corrosion`, `holy`, `dark`, `fire`, `wind`, `water`, `miasma`, `blood`, `erasure`, `soul`, `soul_fire`
(大文字小文字どちらでもOK)

## 属性持ち歩きデバフと適性attribute

属性または呪を持つ武器を直接持っている場合、または `Feyn:"sigiled"` ではない鞘に入れて持ち歩く場合、プレイヤーに属性デバフが発生します。
封付き鞘 (`Feyn:"sigiled"`) は中の武器の属性/呪を遮断します。

適性は player attribute で管理されます。属性デバフの実効レベルは次の式です。

```text
実効デバフLv = max(0, ceil(属性Lv - 対応する適性attribute値))
```

適性値が属性Lv以上なら、その属性の持ち歩きデバフは発生しません。適性値が途中まである場合は、その分だけデバフLvが下がります。
呪は `curse_aptitude >= 1` で無効化されます。
持ち歩きデバフはMobEffectではなく、attribute modifier、独自tick処理、独自DamageSource、パーティクルで実装します。

例:

```mcfunction
/attribute @s the_four_primitives_and_weapons:fire_aptitude base set 5
```

| Attribute ID | 対応するデバフ |
|---|---|
| `the_four_primitives_and_weapons:fire_aptitude` | 炎: 被ダメージ時の微量Fire DoT |
| `the_four_primitives_and_weapons:water_aptitude` | 水: 水中での追加酸素減少 |
| `the_four_primitives_and_weapons:wind_aptitude` | 風: 満腹度exhaustion増加 |
| `the_four_primitives_and_weapons:ice_aptitude` | 氷: 移動速度低下 |
| `the_four_primitives_and_weapons:thunder_aptitude` | 雷: 防具強度低下 |
| `the_four_primitives_and_weapons:electric_aptitude` | 電気: 攻撃速度低下、導体防具装備時の感電ダメージ |
| `the_four_primitives_and_weapons:corrosion_aptitude` | 侵食: 防御力低下 |
| `the_four_primitives_and_weapons:holy_aptitude` | 聖: 満腹度exhaustion増加 + アンデッドから見つかりやすくなる |
| `the_four_primitives_and_weapons:dark_aptitude` | 闇: 攻撃力低下 + 黒霧粒子 |
| `the_four_primitives_and_weapons:miasma_aptitude` | 瘴気: 回復量低下 |
| `the_four_primitives_and_weapons:blood_aptitude` | 血: 被ダメージ増加 |
| `the_four_primitives_and_weapons:erasure_aptitude` | 消滅: 独自の操作揺らし + 消滅粒子 |
| `the_four_primitives_and_weapons:soul_aptitude` | 魂: 最大体力低下 + 魂粒子 |
| `the_four_primitives_and_weapons:soul_fire_aptitude` | 燐火: 青白い炎反動 + 最大体力低下 |
| `the_four_primitives_and_weapons:curse_aptitude` | 呪: 体力低下/攻撃上昇 |

## 属性合成

レアリティ強化台で中央アイテムと触媒枠のどちらかが同レベルの `FIRE` + `SOUL` になると、出力は燐火として表示されます。
内部NBTは `SOUL_FIRE` に置き換えず、`ElementType: fire`, `ElementLevel: N`, `ElementType2: soul`, `ElementLevel2: N` のように炎と魂を1:1で保持します。
NBTで属性が付いた武器、魔導書、`book_elements.json` に登録されたaddon魔導書、`SoulBookItem` / `SoulFireBookItem` クラス名のaddon本を判定します。

アドオン装備は通常の attribute modifier でこれらの適性を付与できます。

## 既存システムとの統合

### DamageCalculatorとの統合
既存の`DamageCalculator.dealDamage()`を使用する場合、自動的に属性ダメージが適用されます：

```java
// DamageCalculatorで攻撃
float damage = DamageCalculator.dealDamage(player, target, 10.0f, weapon);
// ↑ weaponに属性NBTがあれば自動的に属性ダメージが追加される
```

### ChargedAttackHandler/DodgeAndBattouHandlerとの統合
これらのハンドラーが`target.hurt()`を呼び出すと、LivingEntityDamageMixinが自動的に介入し、属性ダメージを計算します。コード変更は不要です。

## トラブルシューティング

### 属性が表示されない
1. `inventoryTick()`でNBTを設定していることを確認
2. Mixinが正しく読み込まれているか確認 (mods.tomlに`[[mixins]]`セクションがあるか)
3. ビルド時にMixin refmapが生成されているか確認

### 属性ダメージが適用されない
1. LivingEntityDamageMixinが正しく動作しているか確認
2. 武器にNBTタグが設定されているか確認 (`/data get entity @s SelectedItem`)
3. ログに"Elemental damage applied"などのメッセージがあるか確認

### ビルドエラー
1. `./gradlew clean build`でクリーンビルド
2. Mixin annotation processorが正しく設定されているか確認
3. Java 17を使用しているか確認

## 今後の拡張予定

- [ ] Config GUIでの設定変更
- [ ] 属性耐性システム
- [ ] 属性の相互作用（氷×炎など）
- [ ] パーティクルエフェクトの追加
- [ ] サウンドエフェクトの追加
- [ ] 属性レベルの上限拡張

## ライセンス
Academic Free License v3.0

## 作者
hiromichi nagase(hrmcngs)

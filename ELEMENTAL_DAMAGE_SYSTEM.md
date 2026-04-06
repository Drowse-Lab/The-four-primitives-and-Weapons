# 属性ダメージシステム - 実装完了

## 概要

Minecraft 1.20.1 Forgeモッド用の属性ダメージシステムです。**NBTタグベース**なので、すべてのアイテムに属性を付与できます。

## 実装済み機能

### ✅ 4つの属性
1. **氷属性 (Ice)** - 凍結エネミーへの追加ダメージ、Slowness効果
2. **電気/雷属性 (Electric)** - 水中AOE、導体装備時ボーナス
3. **侵食/闇属性 (Corrosion)** - 防御力減少、Weakness効果
4. **聖属性 (Holy)** - アンデッド特効(2.5倍)、高レベルで炎上

### ✅ システム機能
- **NBTタグベース**: どのアイテムにも属性付与可能
- **自動適用**: 通常攻撃、チャージ攻撃、すべての攻撃に自動で属性ダメージが適用
- **ツールチップ表示**: エンチャント風に色付きで属性レベルを表示（ローマ数字）
- **Mixin統合**: DamageSourceとLivingEntityに介入し、透過的に動作

## ファイル構成

### コアファイル
```
src/main/java/minecraftarmorweapon/damage/
├── ElementType.java                    # 属性列挙型
├── IElementalDamageSource.java         # DamageSourceインターフェース
├── ElementalDamageUtils.java           # NBTタグユーティリティ
├── IceElementDamageHandler.java        # 氷属性ハンドラー
├── ElectricElementDamageHandler.java   # 電気属性ハンドラー
├── CorrosionElementDamageHandler.java  # 侵食属性ハンドラー
└── HolyElementDamageHandler.java       # 聖属性ハンドラー

src/main/java/minecraftarmorweapon/mixin/
├── DamageSourceMixin.java              # DamageSourceへの属性データ追加
├── LivingEntityDamageMixin.java        # ダメージ計算時の属性適用
└── ItemStackTooltipMixin.java          # ツールチップへの属性表示

src/main/resources/
├── minecraft_armor_weapon.mixins.json  # Mixin設定
└── META-INF/mods.toml                  # Mixin読み込み設定
```

### テストアイテム
```
src/main/java/minecraftarmorweapon/item/
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
- 時間ボーナス: 最大+0.5x (Slowness効果持続時間)

#### 電気/雷属性
- 基礎倍率: 1.2x
- 水中倍率: 1.5x
- 導体倍率: +0.3x/導体アイテム

#### 侵食/闇属性
- 基礎倍率: 1.1x
- 防御力減少: 2.0 + (ダメージ × 0.5)
- Weakness効果、高レベルでWither効果

#### 聖属性
- 基礎倍率: 1.1x
- アンデッド倍率: 2.5x
- レベル倍率: +0.3x/レベル
- 高レベルで炎上効果

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
1. ゲーム内でテストアイテムを入手: `/give @p minecraft_armor_weapon:ice_test_katana`
2. アイテムのツールチップに「氷属性 II」と表示されることを確認
3. モブを攻撃して属性ダメージが適用されることを確認

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

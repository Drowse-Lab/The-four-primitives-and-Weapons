# 属性ダメージシステム

Minecraftに4つの属性ダメージシステムを追加します。

**NBTタグベースのシステムなので、すべてのアイテムに属性を付与できます！**

## 実装された属性

### 1. 氷属性 (Ice Element)
- **効果**: 氷で固まっている時に攻撃をすると多くダメージが入る
- **特徴**:
  - 時間経過でダメージが大きくなる
  - レベルによってダメージが変わる
  - Slowness効果を付与・延長
- **実装クラス**: `IceElementDamageHandler`, `IceDamageSource`

#### 使用例
```java
// 氷属性ダメージを与える
IceElementDamageHandler.applyIceDamage(target, 10.0f, attacker, 2);
```

---

### 2. 電気/雷属性 (Electric Element)
- **効果**: 水中にいるとその周りにもダメージが入る
- **特徴**:
  - 鉄防具などの導体を身につけているとダメージが増加
  - Configファイルで導体アイテムを追加可能
  - 水中では範囲ダメージ
- **実装クラス**: `ElectricElementDamageHandler`, `ElectricDamageSource`

#### 使用例
```java
// 電気属性ダメージを与える
ElectricElementDamageHandler.applyElectricDamage(target, 10.0f, attacker, 2);

// 導体アイテムを追加
List<String> conductorItems = Arrays.asList(
    "minecraft:golden_helmet",
    "minecraft:golden_chestplate"
);
ElectricElementDamageHandler.addConductorItems(conductorItems);
```

---

### 3. 侵食/闇属性 (Corrosion Element)
- **効果**: 防御力を一時的に落とす
- **特徴**:
  - ダメージに比例して防御力減少
  - Weakness効果を付与
  - 高レベルでWither効果も付与
- **実装クラス**: `CorrosionElementDamageHandler`, `CorrosionDamageSource`

#### 使用例
```java
// 侵食属性ダメージを与える
CorrosionElementDamageHandler.applyCorrosionDamage(target, 10.0f, attacker, 2);
```

---

### 4. 聖属性 (Holy Element)
- **効果**: アンデットに多くダメージが入る
- **特徴**:
  - アンデット系モブへ2.5倍ダメージ
  - 高レベルで炎上効果
  - 発光効果を付与
- **実装クラス**: `HolyElementDamageHandler`, `HolyDamageSource`

#### 使用例
```java
// 聖属性ダメージを与える
HolyElementDamageHandler.applyHolyDamage(target, 10.0f, attacker, 2);
```

---

## 技術仕様

### 特徴
- ✅ **通常攻撃と強化攻撃に自動適用**: 攻撃時に自動的に属性ダメージが計算されます
- ✅ **エンチャント風の表示**: ツールチップにエンチャントのように色付きで表示されます
- ✅ **すべてのアイテムに対応**: どんな武器でも属性を付与できます

### Mixinの使用
このシステムはMixinを使用してMinecraftのコアクラスに介入しています。

- `DamageSourceMixin`: DamageSourceに属性データを追加
- `LivingEntityDamageMixin`: ダメージ計算時にNBTタグから属性を読み取り、属性ダメージを適用
- `ItemStackTooltipMixin`: すべてのアイテムのツールチップに属性情報を自動表示

### 属性タイプ
```java
public enum ElementType {
    NONE,       // 無属性
    ICE,        // 氷属性
    ELECTRIC,   // 電気/雷属性
    CORROSION,  // 侵食/闇属性
    HOLY        // 聖属性
}
```

### カスタムダメージソースの作成
```java
// 例: 氷属性ダメージソースの作成
IElementalDamageSource elementalSource = (IElementalDamageSource) new IceDamageSource("ice");
elementalSource.setElementType(ElementType.ICE);
elementalSource.setElementLevel(2);

// ダメージを適用
target.hurt((DamageSource) elementalSource, 10.0f);
```

---

## 武器への実装例

属性を持つ武器を作成する場合の実装例：

```java
public class IceKatanaItem extends SwordItem {

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // 氷属性ダメージを適用
        IceElementDamageHandler.applyIceDamage(target, 5.0f, attacker, 2);
        return super.hurtEnemy(stack, target, attacker);
    }
}
```

---

## ダメージ倍率の設定

各属性のダメージ倍率は対応するHandlerクラスで定義されています：

### 氷属性
- 基礎倍率: 1.5x
- レベル倍率: +0.25x/レベル
- 時間ボーナス: 最大+0.5x

### 電気/雷属性
- 基礎倍率: 1.2x
- 水中倍率: 1.5x
- 導体倍率: +0.3x/導体

### 侵食/闇属性
- 基礎倍率: 1.1x
- 防御力減少: 2.0 + (ダメージ × 0.5)

### 聖属性
- 基礎倍率: 1.1x
- アンデット倍率: 2.5x
- レベル倍率: +0.3x/レベル

---

## 設定ファイル

電気属性の導体アイテムは、プログラム内で動的に追加できます：

```java
ElectricElementDamageHandler.addConductorItems(Arrays.asList(
    "modid:custom_armor",
    "modid:metal_sword"
));
```

---

## 注意事項

1. Mixinの設定は`minecraft_armor_weapon.mixins.json`で管理されています
2. `mods.toml`にMixin設定が追加されていることを確認してください
3. 属性ダメージは既存のダメージ計算に加算されます
4. 各属性のエフェクトは重複適用される場合があります

---

## 今後の拡張案

- [ ] Config GUIの追加
- [ ] 属性耐性システムの実装
- [ ] 属性の相互作用（氷と炎など）
- [ ] パーティクルエフェクトの追加
- [ ] サウンドエフェクトの追加

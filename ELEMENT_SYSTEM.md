# 属性システム (Elemental System)

## 属性一覧

| 属性 | ElementType | 対応魔導書 | カウンター(弱点) |
|------|-------------|-----------|----------------|
| 火 | FIRE | FireballItem | WATER |
| 氷 | ICE | IceBookItem | FIRE |
| 水 | WATER | BubbleshotItem | THUNDER |
| 風 | WIND | WindStepItem / StormItem | ICE |
| 雷 | THUNDER | ThunderboltItem | WIND |
| 電気 | ELECTRIC | ElectricBookItem | WIND |
| 腐食 | CORROSION | CorrosionBookItem | WATER |
| 聖 | HOLY | HolyBookItem | DARK |
| 闇 | DARK | DarknessItem | HOLY |
| 瘴気 | MIASMA | MiasmaBookItem | HOLY |
| 消滅 | ERASURE | ErasureBookItem | NONE |

---

## 各属性の特徴

### 火 (FIRE)
- 基本倍率: 1.0x
- 燃焼効果: 60tick(3秒) + レベルごとに+40tick(2秒)
- シンプルな継続燃焼ダメージ

### 氷 (ICE)
- 基本倍率: 1.5x
- レベルごとに+0.25x追加倍率
- 移動速度低下(Slowness)を付与(60tick)
- 既存のSlownessを増幅
- Slowness持続時間に応じて最大+0.5xボーナス
- 凍結エフェクト表示

### 水 (WATER)
- 基本倍率: 1.0x
- 移動速度低下II(Slowness II)を80tick(4秒)付与
- レベルごとに+40tick(2秒)追加

### 風 (WIND)
- 基本倍率: 1.0x
- 攻撃ブースト: 基本+20%、レベルごとに+10%
- 攻撃者にStrength効果を100tick付与
- レベルごとに+40tick追加、Lv2以上でStrength II

### 雷 (THUNDER)
- 基本倍率: 1.2x
- 水中ボーナス: 1.5x倍率
- 水中AoE: 半径3ブロック内の敵に50%ダメージ
- 導体防具ボーナス: 鉄/金防具1個につき+0.3x

### 電気 (ELECTRIC)
- 基本倍率: 1.2x
- 水中ボーナス: 1.5x倍率
- 連鎖ダメージ: 半径5ブロック内の敵に50%ダメージ(雷ダメージ)
- 導体ボーナス: 鉄/チェーン防具1個につき+0.3x

### 腐食 (CORROSION)
- 基本倍率: 1.1x
- 防御力削減: 2.0 + (ダメージ x 0.5) x (1.0 + レベル x 0.2)
- 削減持続: 100tick(5秒)
- Weakness付与(増幅: min(レベル, 3))
- Lv2以上でDoT(継続ダメージ): 60tick + レベルごとに+40tick、0.5 + レベルごとに+0.5ダメージ

### 聖 (HOLY)
- 基本倍率: 1.1x
- 対アンデッド: 2.5x + (レベル x 0.3x)
- アンデッドに発光・燃焼効果(Lv2以上)
- 対象アンデッド: ゾンビ、スケルトン、ウィザスケ、ストレイ、ハスク、ファントム、ドラウンド、村人ゾンビ、ゾンビピグリン、ウィザー
- トーテム貫通: 聖ダメージを2tick追跡

### 闇 (DARK)
- 基本倍率: 1.1x
- 暗所ボーナス(光レベル7以下): 1.4x倍率
- 盲目付与: 60tick + レベルごとに+40tick
- Lv3以上でDoT: 60tick + レベルごとに+40tick、0.5 + レベルごとに+0.5ダメージ
- 既存デバフを増幅: レベルごとに+40tick延長

### 瘴気 (MIASMA)
- 基本倍率: 1.1x
- 回復阻害: レベルごとに25%回復量削減(Lv4以上で完全阻害)
- 阻害持続: 100tick + レベルごとに+60tick(5秒 + 3秒/レベル)
- DoT(常に発動): 60tick + レベルごとに+40tick、0.5 + レベルごとに+0.5ダメージ
- Lv4以上で回復完全ブロック

### 消滅 (ERASURE)
- 攻撃時は通常攻撃として扱う
- 防御側が消滅属性を持つ場合、遠距離攻撃、属性攻撃、属性武器/魔導書を持つ相手からの攻撃を無効化する
- カウンター属性はなし

---

## 特殊メカニクス

### Storm(嵐の魔導書) - キメラ属性
- 風+水+雷の複合属性
- カウンター対象: ELECTRIC, THUNDER, FIRE, CORROSION, WATER
- 1冊で複数属性を防げる特殊な魔導書

### カウンターボーナス
- カウンター属性で攻撃時: +3基本ダメージ + (レベル x 1.0)

### 聖属性デバフ自動クレンズ
- Lv1: 200tickごと → Lv10: 毎tick

---

## ダメージ倍率まとめ

| 属性 | 基本倍率 | 条件 | 条件倍率 | 最大倍率目安 |
|------|---------|------|---------|------------|
| FIRE | 1.0x | - | - | 1.0x |
| ICE | 1.5x | Slowness中 | +0.5x | 2.0x+ |
| WATER | 1.0x | - | - | 1.0x |
| WIND | 1.0x | ブースト | +0.2x~1.2x | 2.2x |
| THUNDER | 1.2x | 水中 | 1.5x | 1.8x+ |
| ELECTRIC | 1.2x | 水中 | 1.5x | 1.8x+ |
| CORROSION | 1.1x | - | - | 1.1x |
| HOLY | 1.1x | 対アンデッド | 2.5x | 3.4x |
| DARK | 1.1x | 暗所 | 1.4x | 1.54x |
| MIASMA | 1.1x | - | - | 1.1x |
| ERASURE | 1.0x | 防御側で属性/遠距離攻撃を無効化 | - | 1.0x |

---

## 武器タイプ

| タイプ | 日本語名 |
|-------|---------|
| katana | 刀 |
| rapier | レイピア |
| straight_sword | 直刀 |
| sword | 剣 |
| greatsword | 大剣 |
| dagger | 短剣 |
| shield | 盾 |

---

## Curiosスロット

魔導書は**bookスロット**(Curios)に装備して使用する。
装備するとプレイヤーの攻撃に対応する属性ダメージが付与される。

---

## 属性持ち歩きデバフと適性attribute

属性または呪を持つ武器は、直接持っている場合、または `Feyn:"sigiled"` ではない鞘に納刀されている場合に持ち歩きデバフを発生させる。
封付き鞘 (`Feyn:"sigiled"`) は中の武器の属性/呪を遮断する。

プレイヤーの適性は attribute で管理する。属性デバフの実効レベルは次の式で決まる。

```text
実効デバフLv = max(0, ceil(属性Lv - 対応する適性attribute値))
```

適性値が属性Lv以上なら、その属性の持ち歩きデバフは発生しない。適性値が途中まである場合は、その分だけデバフLvが下がる。呪は `curse_aptitude >= 1` で無効化する。

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
| `the_four_primitives_and_weapons:electric_aptitude` | 電気: 攻撃速度低下 |
| `the_four_primitives_and_weapons:corrosion_aptitude` | 侵食: 防御力低下 |
| `the_four_primitives_and_weapons:holy_aptitude` | 聖: 発光 |
| `the_four_primitives_and_weapons:dark_aptitude` | 闇: Darkness付与 |
| `the_four_primitives_and_weapons:miasma_aptitude` | 瘴気: 回復量低下 |
| `the_four_primitives_and_weapons:blood_aptitude` | 血: 微量DoT |
| `the_four_primitives_and_weapons:erasure_aptitude` | 消滅: 混乱 |
| `the_four_primitives_and_weapons:curse_aptitude` | 呪: 体力低下/攻撃上昇 |

アドオン装備は通常の attribute modifier でこれらの適性を付与できる。

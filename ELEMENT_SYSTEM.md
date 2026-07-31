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
| 血 | BLOOD | (なし / NBT・血属性武器) | HOLY |
| 消滅 | ERASURE | ErasureBookItem | NONE |
| 魂 | SOUL | SoulBookItem / datapack | HOLY |
| 燐火 | SOUL_FIRE | SoulFireBookItem / datapack | WATER |

---

## 各属性の特徴

### 火 (FIRE)
- 基本倍率: 1.0x
- 燃焼効果: 60tick(3秒) + レベルごとに+40tick(2秒)
- シンプルな継続燃焼ダメージ

### 氷 (ICE)
- 基本倍率: 1.5x
- レベルごとに+0.25x追加倍率
- MobEffectを使わない独自凍結状態を付与(60tick)
- 移動速度低下は `Attributes.MOVEMENT_SPEED` の一時modifierで管理
- 独自凍結状態の残り時間に応じて最大+0.5xボーナス
- 凍結エフェクト表示

### 水 (WATER)
- 基本倍率: 1.0x
- MobEffectを使わない移動速度低下を80tick(4秒)付与
- レベルごとに+40tick(2秒)追加

### 風 (WIND)
- 基本倍率: 1.0x
- MobEffectを使わず、命中ダメージに直接追加ダメージを加算
- Lv1 = +1.0、Lv5 = +5.0、Lv10 = +10.0

### 雷 (THUNDER)
- 基本倍率: 1.2x
- 水中ボーナス: 1.5x倍率
- 水中AoE: 半径3ブロック内の敵に50%ダメージ
- 導体防具ボーナス: 鉄/金防具1個につき+0.3x
- 命中対象にMobEffectを使わない短時間の移動速度低下と水平減速を付与
- 擬似落雷(雨/雷雨の日限定): 与えた雷ダメージを攻撃者ごとに蓄積し、一定量で命中対象へ落雷
  - 必要蓄積量: 雨=40 / 雷雨=20、レベルごとに-1.5(下限8)
  - 落雷ダメージ: 5 + レベルx1.0(上限20)、雷雨は1.5倍。半径3ブロックに50%の余波
  - バニラの落雷は `setVisualOnly(true)` で見た目と音のみ(着火/変身なし)、実ダメージはthunder DamageType

### 電気 (ELECTRIC)
- 基本倍率: 1.2x
- 水中ボーナス: 1.5x倍率
- 連鎖ダメージ: 半径5ブロック内の敵に50%ダメージ(雷ダメージ)
- 導体ボーナス: 鉄/チェーン防具1個につき+0.3x

### 腐食 (CORROSION)
- 基本倍率: 1.1x
- 防御力削減: 2.0 + (ダメージ x 0.5) x (1.0 + レベル x 0.2)
- 削減持続: 100tick(5秒)
- MobEffectを使わず `Attributes.ARMOR` の一時modifierで防御力を下げる
- 現行仕様では防御力低下のみ

### 聖 (HOLY)
- 基本倍率: 1.1x
- 対アンデッド: 2.5x + (レベル x 0.3x)
- アンデッドに独自時間管理のglowing tagを付与、Lv2以上で炎上
- 対象アンデッド: ゾンビ、スケルトン、ウィザスケ、ストレイ、ハスク、ファントム、ドラウンド、村人ゾンビ、ゾンビピグリン、ウィザー
- トーテム貫通: 聖ダメージを2tick追跡
- 回復阻害(聖なる裁き): レベルごとに12%回復量削減(上限60%、完全阻害はしない)
- 阻害持続: 80tick + レベルごとに+40tick(上限240tick)
- 瘴気と同じ回復阻害stateを共有(`MiasmaHealMixin`)。瘴気の方が強い場合は弱めない

### 闇 (DARK)
- 基本倍率: 1.1x
- 暗所ボーナス(光レベル7以下): 1.4x倍率
- MobEffectを使わない攻撃力低下: 60tick + レベルごとに+40tick
- Lv3以上で独自DoT: 60tick + レベルごとに+40tick、0.5 + レベルごとに+0.5ダメージ
- 黒霧パーティクルを表示

### 瘴気 (MIASMA)
- 基本倍率: 1.1x
- 回復阻害: レベルごとに25%回復量削減(Lv4以上で完全阻害)
- 阻害持続: 100tick + レベルごとに+60tick(5秒 + 3秒/レベル)
- MobEffectを使わず `LivingEntity#heal` への独自介入で回復量を減らす
- Lv4以上で回復完全ブロック
- 魔導書経由だけでなく、NBTで瘴気属性を付けた武器の通常命中でも発動する

### 血 (BLOOD)
- 基本倍率: 1.1x / 既に出血中の対象へは 1.15x
- 出血(Bleed): `blood_dot` の独自DoT。60tick + レベルごとに+20tick(最大200tick)、0.5 + レベルごとに+0.15ダメージ/tick
- 出血は連撃で加算されるが 2.5ダメージ/tick で打ち止め (`ElementalDoTHandler.applyCapped`)
- 吸血: 与ダメージの4%/レベル(最大30%)を攻撃者に還元。1撃あたり最大4.0回復
- 血の無い対象(`MobType.UNDEAD`)には出血・吸血が乗らず、倍率も0.9xに下がる
- MobEffectは使わない。持ち歩きデバフ(被ダメージ増加)は `ElementalCarryDebuffHandler` 側

### 魂 (SOUL)
- 基本倍率: 1.05x + レベルごとに最大+0.40x
- 対象の残り体力が少ないほど最大+0.35xまで追加
- `the_four_primitives_and_weapons:soul` の独自DamageTypeを使う
- 魂系パーティクルを表示
- カウンター属性は聖

### 燐火 (SOUL_FIRE)
- 炎 + 魂が同じレベルで 1:1 になった時の合成表示属性
- 合成後のNBTは `ElementType` / `ElementType2` に `fire` と `soul` を保持し、表示と実効処理だけが燐火になる
- 基本倍率: 1.08x + レベルごとに最大+0.35x
- 対象を青白い魂の炎で燃やす
- 対象の残り体力が少ないほど最大+0.25xまで追加
- `the_four_primitives_and_weapons:soul_fire` の独自DamageTypeを使う
- カウンター属性は水

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
| ICE | 1.5x | 独自凍結中 | +0.5x | 2.0x+ |
| WATER | 1.0x | - | - | 1.0x |
| WIND | 1.0x | ブースト | +0.2x~1.2x | 2.2x |
| THUNDER | 1.2x | 水中 | 1.5x | 1.8x+ |
| ELECTRIC | 1.2x | 水中 | 1.5x | 1.8x+ |
| CORROSION | 1.1x | - | - | 1.1x |
| HOLY | 1.1x | 対アンデッド | 2.5x | 3.4x |
| DARK | 1.1x | 暗所 | 1.4x | 1.54x |
| MIASMA | 1.1x | - | - | 1.1x |
| BLOOD | 1.1x | 出血中の対象 / 対アンデッド | 1.15x / 0.9x | 1.15x |
| ERASURE | 1.0x | 防御側で属性/遠距離攻撃を無効化 | - | 1.0x |
| SOUL | 1.05x | 対象の体力低下 | +0.35x | 1.8x |
| SOUL_FIRE | 1.08x | 対象の体力低下 + 青白い炎上 | +0.25x | 1.68x |

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
持ち歩きデバフはMobEffectではなく、attribute modifier、独自tick処理、独自DamageSource、パーティクルで実装する。

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

アドオン装備は通常の attribute modifier でこれらの適性を付与できる。

## 属性合成

レアリティ強化台で中央のアイテムと触媒枠の片方が同レベルの `FIRE` + `SOUL` の組み合わせになると、出力は燐火として表示される。
内部NBTは `SOUL_FIRE` に置き換えず、`ElementType: fire`, `ElementLevel: N`, `ElementType2: soul`, `ElementLevel2: N` のように炎と魂を1:1で保持する。
武器・魔導書・NBTで属性が付いたaddonアイテムのどれでも判定される。

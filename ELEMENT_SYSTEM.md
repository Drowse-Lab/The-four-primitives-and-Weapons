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
| エラー | ERROR | ErrorBookItem | NONE |

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

### エラー (ERROR)
- 防御貫通: レベルごとに10%(Lv10以上で100%貫通)
- Lv10超過ボーナス: 超過レベルごとに+15%ダメージ(例: Lv15 = 100%貫通 + 75%追加)
- 全防御を無視: 防具、エンチャント、耐性、他MODの保護
- 対象のバフを除去

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
| ERROR | 1.0x | 防御貫通 | Lv10+で+15%/Lv | 無制限 |

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

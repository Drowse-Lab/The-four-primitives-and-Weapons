# 属性ダメージシステム / Elemental Damage System

## 概要 / Overview

NBTタグベースの属性ダメージシステムです。すべてのアイテムに属性を付与できます。
ツールチップにはエンチャント風にローマ数字でレベルが表示されます。

An NBT-tag-based elemental damage system. Any item can be given an elemental attribute.
The attribute level is shown in the item tooltip in Roman numerals, similar to enchantments.

---

## 属性一覧 / Element List

### 氷属性 / Ice

凍結中の敵に追加ダメージを与え、MobEffectを使わない独自鈍化を付与します。  
Deals bonus damage to frozen enemies and applies custom slowing without MobEffect.

| パラメータ | 値 |
|---|---|
| 基礎倍率 / Base multiplier | 1.5× |
| レベル倍率 / Per level | +0.25× |
| 独自凍結中ボーナス / Frozen-state bonus | 最大 +0.5× / Up to +0.5× |
| 付与効果 / Applied effect | 独自凍結 + 移動速度attribute低下 / Custom freeze + movement-speed attribute reduction |
| 弱点属性 / Weak against | Fire |

---

### 火属性 / Fire

バニラの炎と同じ挙動で敵を炎上させます。  
Behaves like vanilla fire — ignites the target.

| パラメータ | 値 |
|---|---|
| 基礎倍率 / Base multiplier | 1.0× |
| 付与効果 / Applied effect | 炎上 / On Fire |
| 弱点属性 / Weak against |  Water |

---

### 電気属性 / Electric

水中で範囲攻撃（AOE）が発生します。導体装備の敵ほど威力が上がります。  
Triggers AOE damage in water. Bonus damage against enemies wearing conductive armor.

| パラメータ | 値 |
|---|---|
| 基礎倍率 / Base multiplier | 1.2× |
| 水中倍率 / In water | 1.5× |
| 導体装備ボーナス / Per conductive item | +0.3× |
| 弱点属性 / Weak against | Wind |

---

### 雷属性 / Thunder

電気属性と同じ挙動です。  
Behaves the same as the Electric element.

| パラメータ | 値 |
|---|---|
| 基礎倍率 / Base multiplier | 1.2× |
| 水中倍率 / In water | 1.5× |
| 導体装備ボーナス / Per conductive item | +0.3× |
| 弱点属性 / Weak against | 🌬️ Wind |

---

###  侵食属性 / Corrosion

攻撃するたびに敵の防御力を削り取ります。MobEffectは使わずattribute modifierで管理します。  
Reduces enemy armor on every hit. It uses attribute modifiers instead of MobEffect.

| パラメータ | 値 |
|---|---|
| 基礎倍率 / Base multiplier | 1.1× |
| 防御力減少 / Armor reduction | −(2.0 + ダメージ × 0.5) |
| 付与効果 / Applied effect | 防御力attribute低下 / Armor attribute reduction |
| 弱点属性 / Weak against | Water |

---

### 聖属性 / Holy

アンデッドに強力な特攻ダメージを与えます。高レベルでは炎上効果も付与します。  
Deals massive bonus damage to undead. Higher levels also set enemies on fire.

| パラメータ | 値 |
|---|---|
| 基礎倍率 / Base multiplier | 1.1× |
| アンデッド特攻 / vs. Undead | 2.5× |
| レベル倍率 / Per level | +0.3× |
| 付与効果 / Applied effect | 独自glowing tag、炎上（高Lv） / Custom glowing tag, on fire (high lv) |
| 弱点属性 / Weak against | Dark |

---

### 闇属性 / Dark

暗所で威力が上がり、MobEffectを使わない攻撃力低下と黒霧パーティクルを付与します。Lv3以上では独自DoTも付与します。  
Deals more damage in darkness, applies custom attack-damage reduction and black mist particles without MobEffect. Level 3+ also applies custom DoT.

| パラメータ | 値 |
|---|---|
| 基礎倍率 / Base multiplier | 1.1× |
| 暗所倍率 / In darkness | 1.4× |
| 付与効果 / Applied effect | 攻撃力attribute低下、独自DoT(Lv3+) / Attack-damage attribute reduction, custom DoT (Lv3+) |
| 弱点属性 / Weak against | Holy |

---

### 水属性 / Water

敵の移動速度をMobEffectなしで低下させ、周囲の火を消します。  
Slows the target with custom attribute logic and extinguishes nearby fire without MobEffect.

| パラメータ | 値 |
|---|---|
| 基礎倍率 / Base multiplier | 1.0× |
| 付与効果 / Applied effect | 移動速度attribute低下 / Movement-speed attribute reduction |
| 弱点属性 / Weak against | Thunder |

---

### 風属性 / Wind

MobEffectを使わず、命中ダメージに直接追加ダメージを加算します。  
Adds direct hit damage without using MobEffect.

| パラメータ | 値 |
|---|---|
| 基礎倍率 / Base multiplier | 1.0× |
| 効果 / Effect | レベルごとの直接追加ダメージ / Direct bonus damage per level |
| 弱点属性 / Weak against | Ice |

---

### 魂属性 / Soul

対象の残り体力が少ないほど威力が上がる、専用DamageTypeの属性ダメージです。  
Uses a dedicated DamageType and gains a small bonus against targets with lower remaining health.

| パラメータ | 値 |
|---|---|
| 基礎倍率 / Base multiplier | 1.05× |
| レベル倍率 / Per level | +0.04× (最大 +0.40× / max +0.40×) |
| 体力低下ボーナス / Missing-health bonus | 最大 +0.35× / Up to +0.35× |
| DamageType | `the_four_primitives_and_weapons:soul` |
| 弱点属性 / Weak against | Holy |

---

### 燐火属性 / Soul Fire

炎と魂を合成した、青白い炎属性です。対象を魂の炎で燃やし、残り体力が少ない相手へ少し威力が伸びます。  
A blue-white fire element fused from Fire and Soul. It burns the target with soul fire and gains a small bonus against wounded targets.

| パラメータ | 値 |
|---|---|
| 基礎倍率 / Base multiplier | 1.08× |
| レベル倍率 / Per level | +0.035× (最大 +0.35× / max +0.35×) |
| 体力低下ボーナス / Missing-health bonus | 最大 +0.25× / Up to +0.25× |
| 付与効果 / Applied effect | 青白い炎上 / Blue-white soul fire |
| DamageType | `the_four_primitives_and_weapons:soul_fire` |
| 弱点属性 / Weak against | Water |

---

## 属性相性表 / Counter Chart

属性には「弱点属性」が設定されており、弱点属性の攻撃を受けるとダメージが増加します。  
Each element has a counter element that deals increased damage against it.

| 属性 / Element | 弱点 / Weak against |
|---|---|---|
| Ice | Fire |
| Fire | Water |
| Electric | Wind |
| Thunder | Wind |
| Corrosion | Water |
| Holy | Dark |
| Dark | Holy |
| Water | Thunder |
| Wind | Ice |
| Erasure | — |
| Soul | Holy |
| Soul Fire | Water |

---

## NBTタグ構造 / NBT Tag Structure

```json
{
  "ElementType": "ice",
  "ElementLevel": 2
}
```

---

## 属性持ち歩きデバフと適性attribute / Carry Debuffs and Aptitude Attributes

属性または呪を持つ武器を直接持っている場合、または `Feyn:"sigiled"` ではない鞘に入れて持ち歩く場合、プレイヤーに属性デバフが発生します。
封付き鞘 (`Feyn:"sigiled"`) は中の武器の属性/呪を遮断します。

When carrying an elemental or cursed weapon directly, or inside a scabbard that is not `Feyn:"sigiled"`, the player receives a carry debuff.
A sigiled scabbard blocks the stored weapon's element/curse.

適性は player attribute で管理されます。

```text
Effective debuff level = max(0, ceil(ElementLevel - aptitude attribute value))
```

適性値が属性Lv以上なら、その属性の持ち歩きデバフは発生しません。呪は `curse_aptitude >= 1` で無効化されます。

If aptitude is equal to or greater than the element level, that carry debuff is disabled. Curse is disabled when `curse_aptitude >= 1`.

Carry debuffs do not apply MobEffect instances. They use attribute modifiers, custom tick logic, custom DamageSource damage, and particles.

Electric carry debuff also shocks the player every few seconds while wearing conductive armor.
Conductive armor is controlled by the `the_four_primitives_and_weapons:electric_conductive_armor` item tag, with iron, chainmail, and golden armor included by default.

Example:

```mcfunction
/attribute @s the_four_primitives_and_weapons:fire_aptitude base set 5
```

| Attribute ID | Debuff |
|---|---|
| `the_four_primitives_and_weapons:fire_aptitude` | Fire: tiny Fire DoT after taking damage |
| `the_four_primitives_and_weapons:water_aptitude` | Water: extra air loss underwater |
| `the_four_primitives_and_weapons:wind_aptitude` | Wind: increased food exhaustion |
| `the_four_primitives_and_weapons:ice_aptitude` | Ice: movement speed down |
| `the_four_primitives_and_weapons:thunder_aptitude` | Thunder: armor toughness down |
| `the_four_primitives_and_weapons:electric_aptitude` | Electric: attack speed down, shock damage with conductive armor |
| `the_four_primitives_and_weapons:corrosion_aptitude` | Corrosion: armor down |
| `the_four_primitives_and_weapons:holy_aptitude` | Holy: increased food exhaustion + easier detection by undead |
| `the_four_primitives_and_weapons:dark_aptitude` | Dark: attack damage down + black mist particles |
| `the_four_primitives_and_weapons:miasma_aptitude` | Miasma: healing reduction |
| `the_four_primitives_and_weapons:blood_aptitude` | Blood: tiny DoT |
| `the_four_primitives_and_weapons:erasure_aptitude` | Erasure: custom movement instability + erasure particles |
| `the_four_primitives_and_weapons:soul_aptitude` | Soul: max health down + soul particles |
| `the_four_primitives_and_weapons:soul_fire_aptitude` | Soul Fire: blue-white fire backlash + max health down |
| `the_four_primitives_and_weapons:curse_aptitude` | Curse: max health down / attack up |

---
## 属性合成 / Element Fusion

レアリティ強化台で中央アイテムと触媒枠のどちらかが `FIRE` + `SOUL` の組み合わせになると、出力は `SOUL_FIRE` (燐火) になります。  
When the Rarity Forge sees `FIRE` + `SOUL` between the medium item and a catalyst slot, the output becomes `SOUL_FIRE`.

## Test command
/give @s the_four_primitives_and_weapons:kurikarakenutigatana{ElementType:"ELECTRIC",ElementLevel:1}
---

# 属性ダメージシステム / Elemental Damage System

## 概要 / Overview

NBTタグベースの属性ダメージシステムです。すべてのアイテムに属性を付与できます。
ツールチップにはエンチャント風にローマ数字でレベルが表示されます。

An NBT-tag-based elemental damage system. Any item can be given an elemental attribute.
The attribute level is shown in the item tooltip in Roman numerals, similar to enchantments.

---

## 属性一覧 / Element List

### 氷属性 / Ice

凍結中の敵に追加ダメージを与え、Slowness（鈍化）効果を付与します。  
Deals bonus damage to frozen enemies and applies Slowness.

| パラメータ | 値 |
|---|---|
| 基礎倍率 / Base multiplier | 1.5× |
| レベル倍率 / Per level | +0.25× |
| Slowness継続中ボーナス / Slowness bonus | 最大 +0.5× / Up to +0.5× |
| 付与効果 / Applied effect | Slowness |
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

攻撃するたびに敵の防御力を削り取ります。高レベルでは Wither 効果も付与します。  
Reduces enemy armor on every hit. Higher levels also apply Wither.

| パラメータ | 値 |
|---|---|
| 基礎倍率 / Base multiplier | 1.1× |
| 防御力減少 / Armor reduction | −(2.0 + ダメージ × 0.5) |
| 付与効果 / Applied effect | Weakness（高Lvで Wither） |
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
| 付与効果 / Applied effect | 炎上（高Lv） / On Fire (high lv) |
| 弱点属性 / Weak against | Dark |

---

### 闇属性 / Dark

> ⚠️ **未実装 / Not yet implemented**

仕様は現在検討中です。  
Details are currently being planned.

| パラメータ | 値 |
|---|---|
| 弱点属性 / Weak against | Holy |

---

### 水属性 / Water

敵の移動速度を低下させます（Slowness 付与）。  
Slows the target's movement speed by applying Slowness.

| パラメータ | 値 |
|---|---|
| 基礎倍率 / Base multiplier | 1.0× |
| 付与効果 / Applied effect | Slowness |
| 弱点属性 / Weak against | Thunder |

---

### 風属性 / Wind

攻撃力を上昇させます。  
Increases attack damage.

| パラメータ | 値 |
|---|---|
| 基礎倍率 / Base multiplier | 1.0× |
| 効果 / Effect | 攻撃力上昇 / Attack damage boost |
| 弱点属性 / Weak against | Ice |

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
| `the_four_primitives_and_weapons:electric_aptitude` | Electric: attack speed down |
| `the_four_primitives_and_weapons:corrosion_aptitude` | Corrosion: armor down |
| `the_four_primitives_and_weapons:holy_aptitude` | Holy: glowing |
| `the_four_primitives_and_weapons:dark_aptitude` | Dark: Darkness effect |
| `the_four_primitives_and_weapons:miasma_aptitude` | Miasma: healing reduction |
| `the_four_primitives_and_weapons:blood_aptitude` | Blood: tiny DoT |
| `the_four_primitives_and_weapons:erasure_aptitude` | Erasure: confusion |
| `the_four_primitives_and_weapons:curse_aptitude` | Curse: max health down / attack up |

---
## Test command
/give @s the_four_primitives_and_weapons:kurikarakenutigatana{ElementType:"ELECTRIC",ElementLevel:1}
---

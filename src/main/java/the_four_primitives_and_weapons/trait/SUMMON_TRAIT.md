# Summon Trait Mob - 特性付きMobスポーンガイド

## /summon コマンド (NBT)

ForgeDataの `TraitName` に特性名を指定する。

```
/summon minecraft:zombie ~ ~ ~ {ForgeData:{TraitName:"explosive"}}
/summon minecraft:skeleton ~ ~ ~ {ForgeData:{TraitName:"undying"}}
/summon minecraft:spider ~ ~ ~ {ForgeData:{TraitName:"iron_wall"}}
/summon minecraft:creeper ~ ~ ~ {ForgeData:{TraitName:"swift"}}
```

日本語表示名でも可:
```
/summon minecraft:zombie ~ ~ ~ {ForgeData:{TraitName:"爆裂"}}
/summon minecraft:zombie ~ ~ ~ {ForgeData:{TraitName:"不死"}}
```

## /spawntrait コマンド

```
/spawntrait <entity> <trait>
```

例:
```
/spawntrait minecraft:zombie explosive
/spawntrait minecraft:skeleton undying
/spawntrait minecraft:spider iron_wall
```

※ OP権限レベル2以上が必要。タブ補完対応。

## 特性一覧

| TraitName      | 表示名  | レアリティ  |
|----------------|---------|------------|
| iron_wall      | 鉄壁    | COMMON     |
| berserker      | 狂戦士   | COMMON     |
| swift          | 迅速    | UNCOMMON   |
| regenerator    | 再生者   | UNCOMMON   |
| web_spinner    | 蜘蛛糸   | UNCOMMON   |
| reflector      | 反射    | UNCOMMON   |
| arrow_guard    | 矢盾    | RARE       |
| poisoner       | 猛毒    | UNCOMMON   |
| blinder        | 盲目    | RARE       |
| nauseator      | 吐き気   | UNCOMMON   |
| weakener       | 弱体    | RARE       |
| explosive      | 爆裂    | RARE       |
| undying        | 不死    | LEGENDARY  |

## 備考

- モンスター(Monster)のみ対応。村人や動物には適用不可。
- 特性はNBT(ForgeData)に保存されるため、ワールド再読み込み後も維持される。
- TraitNameは大文字小文字を区別しない (`EXPLOSIVE` = `explosive`)。

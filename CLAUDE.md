# CLAUDE.md — Issue #199「連携させたいmod」実装ガイド

Minecraft 1.20.1 / Forge MOD `the_four_primitives_and_weapons` に、**Ice and Fire**（ドラゴンMOD）と **Mekanism** の連携を実装するための作業指針。
対象 issue: https://github.com/Drowse-Lab/The-four-primitives-and-Weapons/issues/199

> このリポジトリは MCreator 生成コードをベースに手書き拡張された大規模MODです。まず既存の属性システム（`ELEMENT_SYSTEM.md` / `ELEMENTAL_DAMAGE_SYSTEM.md`）を読んでから着手すること。

---

## 0. 用語

- **D** = ドラゴン（Dragon, Ice and Fire）。「Dの武器」= ドラゴン素材製の武器、「Dの素材」= dragonbone / dragonsteel / dragonscale 等。
- **属性 (Element)** = 本MODの `ElementType`（火・氷・雷・電気・侵食/腐食・聖・闇・瘴気・消滅・魂・燐火・風・水・血）。

---

## 1. 作業前提・ルール

- **返答は日本語**。
- **勝手にビルド／version bump しない**。コード変更のみで完了とし、`gradlew build` はユーザーが明示要請したときだけ実行する。
- 連携コードは **オプショナル依存**（相手MODが無くてもロード失敗しない）にすること。方式は §5 を参照。
- 既存の命名・登録パターンに合わせる（新しい抽象化を勝手に導入しない）。

---

## 2. issue #199 タスク分解

### A. Ice and Fire 連携 — ドラゴン素材武器
- [ ] 追加武器: **刀 / 直刀(tyokuto) / レイピア / ダガー**（ドラゴン素材版）
- [ ] 3Dモデル編集: **剣**（既存の剣モデルを3D化）
- [ ] 属性ごとの「D」武器を追加: 侵食・瘴気・電気・魂・燐火・風・水・血・闇・聖・消滅
- [ ] ドラゴンの属性攻撃 ↔ 本MOD属性の紐付け: **氷 / 炎 / 雷**

### B. Mekanism 連携
- [ ] **電気・雷属性で発電**できるようにする（Mekanismのエネルギー系へ供給）

---

## 3. 既存の属性システム（最重要・再利用する）

属性は **アイテムのNBTタグベース**。どのアイテムにも属性を付与でき、攻撃時に Mixin が透過的にダメージへ反映する。

### コアファイル
| 役割 | パス |
|------|------|
| 属性列挙型 | `src/main/java/the_four_primitives_and_weapons/damage/ElementType.java` |
| NBTユーティリティ | `src/main/java/the_four_primitives_and_weapons/damage/ElementalDamageUtils.java` |
| 各属性ハンドラ | `damage/*ElementDamageHandler.java`（Ice/Electric/Corrosion/Holy/Soul/SoulFire …） |
| ダメージ介入 | `mixin/LivingEntityDamageMixin.java`（`applyElementalDamage`） |
| DamageSource属性付与 | `mixin/DamageSourceMixin.java`（`IElementalDamageSource`） |
| ツールチップ表示 | `mixin/ItemStackTooltipMixin.java` |
| DamageType登録 | `init/TheFourPrimitivesAndWeaponsModDamageTypes.java` |

### `ElementType`（既存の値）
`NONE / ICE / ELECTRIC / CORROSION / HOLY / DARK / FIRE / WIND / THUNDER / WATER / MIASMA / BLOOD / ERASURE / SOUL / SOUL_FIRE`
→ **issue #199 の属性はすべて既存**。`ElementType` への追加は基本不要（新属性が要る場合のみ enum + counter + handler + tooltip を追加）。

### `ElementalDamageUtils` 主要API
```java
setElement(ItemStack, ElementType, int level)          // 単一属性を付与
setElementPair(ItemStack, primary, plv, secondary, slv) // 複合属性（燐火=炎+魂 等）
getEffectiveElementType(ItemStack) / getEffectiveElementLevel(ItemStack)
hasElement(ItemStack) / removeElement(ItemStack)
applyElementalDamage(attacker, target, damage, ...)     // ダメージ計算本体
```

**属性武器は「NBTで属性を焼き込んだアイテム」として作る**のが定石（新クラスを属性ごとに増やさなくてよい）。デフォルトNBT付与は各 `*BookItem` の実装（例: `item/IceBookItem.java`）や `RarityForge` 系を参考にする。

---

## 4. アイテム／モデル追加の手順（既存規約）

### 4-1. アイテムクラス
- 場所: `src/main/java/the_four_primitives_and_weapons/item/`
- 参考: `IronKatanaItem.java`（刀）, `IronRapierItem.java`（レイピア）, `IronDaggerItem.java`（ダガー）, `*TyokutoItem`（直刀）, `AbstractTieredRapierItem.java`（tier共通化の例）
- 武器は `SwordItem` 継承 + 独自 `Tier`（無名クラスで `getUses/getSpeed/getAttackDamageBonus/getLevel/getEnchantmentValue/getRepairIngredient`）。
- **ドラゴン素材版**は「新Tier（dragonbone/dragonsteel相当のステータス）」を定義した刀/直刀/レイピア/ダガークラスを追加する。属性版は §3 のNBT付与で表現する。

### 4-2. 登録
- `init/TheFourPrimitivesAndWeaponsModItems.java` に
  ```java
  public static final RegistryObject<Item> DRAGONBONE_KATANA =
      REGISTRY.register("dragonbone_katana", () -> new DragonboneKatanaItem());
  ```
  形式で追記（`DeferredRegister<Item> REGISTRY`）。
- 必要に応じてクリエイティブタブ: `init/TheFourPrimitivesAndWeaponsModTabs.java` / `CreativeTabPopulator.java`。

### 4-3. モデル・テクスチャ・言語
- アイテムモデル JSON: `src/main/resources/assets/the_four_primitives_and_weapons/models/item/<id>.json`
  - 既存の3D刀モデル（`iron_katana.json` 等）を流用/複製して素材差分を作る。
- テクスチャ: `.../textures/item/`
- 翻訳: `.../lang/ja_jp.json`, `en_us.json`
- **「剣の3Dモデル編集」** は該当する既存 `models/item/*sword*.json`（または `assets/minecraft/models/item/*_sword.json` のオーバーライド）を編集。IDEで開かれている `assets/minecraft/models/item/diamond_sword.json` がヒント。

---

## 5. 相手MODとの連携方式（ロード失敗させない）

### 5-1. 依存宣言
- `src/main/resources/META-INF/mods.toml` の依存は現状すべて `mandatory=false`。Ice and Fire / Mekanism も **`mandatory=false`** で追記する。
  ```toml
  [[dependencies.the_four_primitives_and_weapons]]
      modId="iceandfire"      # ← 実際のmodIdは要確認（iceandfire / mekanism / mekanismgenerators 等）
      mandatory=false
      ordering="AFTER"
  ```
- gradle 依存（開発時 compileOnly）が必要なら `build.gradle` に追加するが、**リフレクション方式なら compileOnly も不要**。

### 5-2. 実装パターン
- 既存の連携実装 `compat/SpellbooksCompat.java` を **手本**にする。要点:
  - `ModList.get().isLoaded(MOD_ID)` でロード判定を1回キャッシュ。
  - 相手APIには **リフレクション**でアクセス（compile依存ゼロ、API変更でもロード失敗しない）。
  - 連携クラスは `compat/` に置く（例: `IceAndFireCompat.java`, `MekanismCompat.java`）。
- compile依存を張ってよいなら `implementation fg.deobf(...)` + 直接API呼び出しでも可。**どちらにするかは着手時にユーザーへ確認**。

---

## 6. サブタスク別・実装方針

### A-1. ドラゴン素材武器（刀/直刀/レイピア/ダガー）
新Tierの武器クラス4種を `item/` に追加 → `ModItems` 登録 → モデル/テクスチャ/lang。レシピは Ice and Fire の素材（dragonbone 等）を `Ingredient` にする（相手MODのアイテムは `ForgeRegistries.ITEMS.getValue(new ResourceLocation("iceandfire","dragonbone"))` 等で取得、未ロード時はレシピ無効化）。

### A-2. 剣の3D化
既存剣モデル JSON を3D（`gui_light: front` / `display` 変換 / elements 定義）に編集。`blockbench/` や `item-template-3d/` に既存テンプレがあるので流用。

### A-3. 属性ごとの「D」武器
基本は **ベース武器 + NBT属性**（§3）。クラフト or クリエイティブ配布時に `ElementalDamageUtils.setElement(stack, ElementType.MIASMA, lv)` 等で焼き込む。属性ごとにクラスを量産しない。

### A-4. ドラゴン攻撃 ↔ 属性の紐付け（氷/炎/雷）
- Ice and Fire のドラゴン炎/氷ブレスや攻撃 `DamageSource` を判定し、本MODの属性ダメージ処理へ橋渡しする。
- フック候補: `LivingEntityDamageMixin` / Forge の `LivingHurtEvent`（`events/` or `event/` パッケージ）で、`source` が Ice and Fire 由来（`msg` / 攻撃者エンティティ型をリフレクション判定）なら対応 `ElementType`（火ドラゴン→FIRE、氷ドラゴン→ICE、雷系→THUNDER）として扱う。
- 逆方向（本MOD属性攻撃がドラゴンへ効く弱点関係）も `getCounterElement()` を活かせるなら検討。

### B. Mekanism 発電
- 電気/雷属性の発生（攻撃ヒット/雷ブロック等）を Mekanism のエネルギー受け入れ先へ供給。
- 方式: Forge Energy (`IEnergyStorage`) 経由が最も疎結合。Mekanism は FE 互換なので、**発電ブロック/機構を FE で実装 → Mekanismのケーブル/機械へ流す**のが安全。専用Joule APIはリフレクション必須。
- ブロックが必要なら `block/` + `block/entity/` + `init/TheFourPrimitivesAndWeaponsModBlocks.java` / `...ModBlockEntities.java` に追加。

---

## 7. 主要ディレクトリ早見

```
src/main/java/the_four_primitives_and_weapons/
├── item/              # 武器・道具アイテム（145+クラス）
├── damage/            # 属性システム（ElementType, *Handler, ElementalDamageUtils）
├── mixin/             # ダメージ/ツールチップ介入
├── compat/            # 他MOD連携（SpellbooksCompat = 手本）★ここに追加
├── init/              # DeferredRegister 登録（Items/Blocks/BlockEntities/DamageTypes…）
├── block/ block/entity/  # ブロック（Mekanism発電で使う可能性）
├── events/ event/     # Forgeイベントハンドラ
└── procedures/        # MCreator手続き
src/main/resources/
├── assets/the_four_primitives_and_weapons/  # models/item, textures, lang
├── assets/minecraft/models/item/            # バニラアイテムモデルのオーバーライド
└── META-INF/mods.toml                       # 依存宣言（optional追加）
```

---

## 8. 着手時にユーザーへ確認すべきこと

1. Ice and Fire / Mekanism の **正確な modId とバージョン**、開発環境に deobf jar を入れるか（compileOnly依存 vs 完全リフレクション）。
2. ドラゴン武器のステータス・素材（dragonbone/dragonsteel/ice/fire/lightning系のどれを使うか）。
3. 属性ごとの「D武器」はクラフト可能にするか、クリエイティブ配布のみか。
4. Mekanism発電は「新ブロック」か「既存の攻撃/効果から発電」か。

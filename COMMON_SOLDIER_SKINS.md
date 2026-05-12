# Common Soldier スキンシステム

## 概要

Common Soldierエンティティは、複数のスキンバリエーションをサポートしています。
スポーン時にランダムで見た目が選択されますが、NBTタグで特定のスキンを指定することも可能です。

**新機能**: `wide/`と`slim/`フォルダに分けてスキンファイルを管理できます！

## スキンフォルダ構造

```
src/main/resources/assets/the_four_primitives_and_weapons/textures/entity/
├── wide/               # 通常モデル（Steve型）用スキン
│   ├── README.md
│   ├── soldier_1.png
│   ├── soldier_2.png
│   └── ... (最大20個)
└── slim/               # スリムモデル（Alex型）用スキン
    ├── README.md
    ├── soldier_1.png
    ├── soldier_2.png
    └── ... (最大20個)
```

## スキンの種類

### Wide Model (通常モデル) - 70%の確率
- `textures/entity/steve.png` (デフォルト)
- `the_four_primitives_and_weapons:textures/entity/wide/soldier_1.png`
- `the_four_primitives_and_weapons:textures/entity/wide/soldier_2.png`
- ... (最大20個のカスタムスキン)

### Slim Model (スリムモデル) - 30%の確率
- `textures/entity/alex.png` (デフォルト)
- `the_four_primitives_and_weapons:textures/entity/slim/soldier_1.png`
- `the_four_primitives_and_weapons:textures/entity/slim/soldier_2.png`
- ... (最大20個のカスタムスキン)

## ランダムスポーン

通常の召喚コマンドを使用すると、ランダムでスキンが選択されます：

```mcfunction
/summon the_four_primitives_and_weapons:common_soldier ~ ~ ~
```

- スリムモデル: 30%の確率
- 通常モデル: 70%の確率
- スキンは各モデルタイプ内でUUIDベースでランダムに選択

## NBTタグで指定

特定のスキンを指定して召喚することもできます：

### スリムモデルを指定

```mcfunction
# スリムモデル、スキン0 (デフォルトAlex)
/summon the_four_primitives_and_weapons:common_soldier ~ ~ ~ {IsSlim:1,SkinIndex:0}

# スリムモデル、スキン1 (カスタムスリム1)
/summon the_four_primitives_and_weapons:common_soldier ~ ~ ~ {IsSlim:1,SkinIndex:1}

# スリムモデル、スキン2 (カスタムスリム2)
/summon the_four_primitives_and_weapons:common_soldier ~ ~ ~ {IsSlim:1,SkinIndex:2}
```

### 通常モデルを指定

```mcfunction
# 通常モデル、スキン0 (デフォルトSteve)
/summon the_four_primitives_and_weapons:common_soldier ~ ~ ~ {IsSlim:0,SkinIndex:0}

# 通常モデル、スキン1 (カスタム1)
/summon the_four_primitives_and_weapons:common_soldier ~ ~ ~ {IsSlim:0,SkinIndex:1}

# 通常モデル、スキン2 (カスタム2)
/summon the_four_primitives_and_weapons:common_soldier ~ ~ ~ {IsSlim:0,SkinIndex:2}

# 通常モデル、スキン3 (カスタム3)
/summon the_four_primitives_and_weapons:common_soldier ~ ~ ~ {IsSlim:0,SkinIndex:3}
```

### ランダムスキンで通常/スリムのみ指定

```mcfunction
# 通常モデル、ランダムスキン
/summon the_four_primitives_and_weapons:common_soldier ~ ~ ~ {IsSlim:0,SkinIndex:-1}

# スリムモデル、ランダムスキン
/summon the_four_primitives_and_weapons:common_soldier ~ ~ ~ {IsSlim:1,SkinIndex:-1}
```

## カスタムスキンファイルの作成

カスタムスキンを追加する方法：

### 1. スキンファイルを作成

Minecraftプレイヤースキン形式（64x64 PNG）で作成します。
以下のツールを使用できます：
- [Minecraft Skin Editor](https://www.minecraftskins.com/skin-editor/)
- [NovaSkin](https://minecraft.novaskin.me/)
- [Planet Minecraft Skin Editor](https://www.planetminecraft.com/skin-editor/)

### 2. ファイルを配置

作成したスキンファイルを適切なフォルダに配置：

#### Wide Model (通常モデル)用
```
src/main/resources/assets/the_four_primitives_and_weapons/textures/entity/wide/
├── soldier_1.png
├── soldier_2.png
├── soldier_3.png
└── ... (最大soldier_20.png)
```

#### Slim Model (スリムモデル)用
```
src/main/resources/assets/the_four_primitives_and_weapons/textures/entity/slim/
├── soldier_1.png
├── soldier_2.png
├── soldier_3.png
└── ... (最大soldier_20.png)
```

### 3. 命名規則

- **必須**: `soldier_X.png` の形式（Xは1-20の数字）
- ファイル名は正確に（大文字小文字を区別）
- 番号は連番でなくてもOK（例: soldier_1.png, soldier_5.png, soldier_10.pngのみ配置可能）

### 4. スキンの推奨デザイン

- **Wide Model**: 武装した兵士風のデザイン
  - 重装備の鎧や防具
  - 厳つい戦闘服
  - 様々な肌の色

- **Slim Model**: より軽装な兵士風のデザイン
  - 軽い鎧
  - 斥候風の装備
  - スリムな体型に合うデザイン
  - 様々な肌の色

## 技術詳細

### ファイル構造

- **CommonSoldierEntity.java**: NBTタグの読み書き処理
  - `IsSlim`: -1 (ランダム), 0 (通常), 1 (スリム)
  - `SkinIndex`: -1 (ランダム), 0以上 (指定インデックス)

- **CommonSoldierRenderer.java**: スキン選択とレンダリング
  - NBTタグを優先、未設定時はUUIDベースでランダム選択
  - スリムモデルは全体を95%に縮小（より細い体型）

### モデル比率

- **通常モデル**: 70% (UUIDの最上位ビット % 10 が 3-9)
- **スリムモデル**: 30% (UUIDの最上位ビット % 10 が 0-2)

### スキン選択

スキンインデックスはUUIDの最下位ビットから計算されるため、
同じエンティティは常に同じスキンを使用します（NBTタグで上書きしない限り）。

## 例: バラエティ豊かな部隊を召喚

```mcfunction
# ランダムな見た目の部隊 (5体)
/summon the_four_primitives_and_weapons:common_soldier ~1 ~ ~
/summon the_four_primitives_and_weapons:common_soldier ~-1 ~ ~
/summon the_four_primitives_and_weapons:common_soldier ~ ~ ~1
/summon the_four_primitives_and_weapons:common_soldier ~ ~ ~-1
/summon the_four_primitives_and_weapons:common_soldier ~ ~ ~

# 特定のスキンで統一された部隊
/summon the_four_primitives_and_weapons:common_soldier ~2 ~ ~ {IsSlim:0,SkinIndex:1}
/summon the_four_primitives_and_weapons:common_soldier ~4 ~ ~ {IsSlim:0,SkinIndex:1}
/summon the_four_primitives_and_weapons:common_soldier ~6 ~ ~ {IsSlim:0,SkinIndex:1}

# 斥候部隊（全員スリムモデル）
/summon the_four_primitives_and_weapons:common_soldier ~-2 ~ ~ {IsSlim:1}
/summon the_four_primitives_and_weapons:common_soldier ~-4 ~ ~ {IsSlim:1}
/summon the_four_primitives_and_weapons:common_soldier ~-6 ~ ~ {IsSlim:1}
```

## トラブルシューティング

### スキンが表示されない場合

1. スキンファイルが正しいディレクトリ（`wide/`または`slim/`）にあるか確認
2. ファイル名が正確か確認（`soldier_X.png`の形式、大文字小文字を区別）
3. PNGファイルが破損していないか確認
4. Minecraftを再起動
5. ログを確認: `[CommonSoldier] Initialized skins: X wide, Y slim` のメッセージを探す

### すべて同じスキンになる場合

- カスタムスキンファイルがまだ作成されていない可能性があります
- デフォルトのSteve/Alexスキンのみが使用されます
- カスタムスキンを追加すると自動的にバリエーションが増えます

### スキン数の確認

Minecraftのログで以下のメッセージを確認できます：
```
[CommonSoldier] Initialized skins: 21 wide, 21 slim
```
（デフォルトのSteve/Alexスキン + カスタムスキン）

## 利点

### wide/とslim/フォルダ分けの利点

1. **整理しやすい**: モデルタイプごとにスキンを分類
2. **管理が簡単**: それぞれのフォルダに専用のREADME
3. **拡張性が高い**: 各タイプ最大20個のカスタムスキン
4. **わかりやすい**: ファイル名が統一（soldier_X.png）
5. **柔軟性**: 番号が連番でなくてもOK

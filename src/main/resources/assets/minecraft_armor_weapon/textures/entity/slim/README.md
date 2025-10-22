# Slim Model Skins (スリムモデル用スキン)

このフォルダには、Common Soldierのスリムモデル（Alex型）用のスキンファイルを配置します。

## スキンファイルの命名規則

ファイル名は以下の形式で作成してください：

```
soldier_1.png
soldier_2.png
soldier_3.png
...
soldier_20.png
```

- 番号は1から20まで使用できます
- ファイル形式はPNG（64x64ピクセル）
- Minecraftプレイヤースキンの標準フォーマット（Alex型 = 細い腕）

## スキンの作成方法

### オンラインエディタ
- [Minecraft Skin Editor](https://www.minecraftskins.com/skin-editor/)（Slimモデルを選択）
- [NovaSkin](https://minecraft.novaskin.me/)（Alex/Slimモデル）
- [Planet Minecraft Skin Editor](https://www.planetminecraft.com/skin-editor/)

### ローカルツール
- Paint.NET（プラグイン使用）
- GIMP
- Photoshop

## スキンデザインのヒント

### 推奨デザイン
- 軽装備の兵士
- 斥候・アーチャー風
- 俊敏そうな体格のキャラクター
- スリムな体型に合うデザイン
- 様々な肌の色でバリエーションを作成

### 配色例
- **soldier_1.png**: 紫の鎧、白い服
- **soldier_2.png**: オレンジの鎧、茶色の服
- **soldier_3.png**: 水色の鎧、黒い服
- **soldier_4.png**: ピンクの鎧、灰色の服
- **soldier_5.png**: 黄色の鎧、緑の服

### Slimモデルの特徴
- 腕が3ピクセル幅（通常は4ピクセル）
- より華奢な見た目
- ゲーム内でさらに5%縮小される

## ファイル構造

```
slim/
├── README.md           (このファイル)
├── soldier_1.png       (スキン1)
├── soldier_2.png       (スキン2)
├── soldier_3.png       (スキン3)
└── ...
```

## 動作確認

1. スキンファイルをこのフォルダに配置
2. Minecraftを再起動
3. Common Soldierを召喚してスキンを確認

```mcfunction
# スリムモデルをランダムスキンでスポーン
/summon minecraft_armor_weapon:common_soldier ~ ~ ~ {IsSlim:1}

# 特定のスキンを指定（soldier_1.png = インデックス1）
/summon minecraft_armor_weapon:common_soldier ~ ~ ~ {IsSlim:1,SkinIndex:1}

# スリムモデルをランダムで複数スポーン
/summon minecraft_armor_weapon:common_soldier ~1 ~ ~ {IsSlim:1}
/summon minecraft_armor_weapon:common_soldier ~-1 ~ ~ {IsSlim:1}
/summon minecraft_armor_weapon:common_soldier ~ ~ ~1 {IsSlim:1}
```

## スポーン確率

NBTタグを指定しない場合：
- スリムモデル: 30%の確率
- 通常モデル: 70%の確率

```mcfunction
# ランダムスポーン（30%の確率でスリムモデル）
/summon minecraft_armor_weapon:common_soldier ~ ~ ~
```

## 注意事項

- スキンファイルが存在しない番号は自動的にスキップされます
- 最低1つのスキンファイルがない場合、デフォルトのAlexスキンが使用されます
- ファイル名は正確に（大文字小文字を区別）
- 最大20個のスキンをサポート
- **重要**: Slimモデル専用のスキンを作成すること（腕が3ピクセル幅）

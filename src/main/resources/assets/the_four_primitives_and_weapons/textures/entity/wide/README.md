# Wide Model Skins (通常モデル用スキン)

このフォルダには、Common Soldierの通常モデル（Steve型）用のスキンファイルを配置します。

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
- Minecraftプレイヤースキンの標準フォーマット

## スキンの作成方法

### オンラインエディタ
- [Minecraft Skin Editor](https://www.minecraftskins.com/skin-editor/)
- [NovaSkin](https://minecraft.novaskin.me/)
- [Planet Minecraft Skin Editor](https://www.planetminecraft.com/skin-editor/)

### ローカルツール
- Paint.NET（プラグイン使用）
- GIMP
- Photoshop

## スキンデザインのヒント

### 推奨デザイン
- 重装備の兵士
- 鎧を着た戦士
- 厳つい体格のキャラクター
- 様々な肌の色でバリエーションを作成

### 配色例
- **soldier_1.png**: 青い鎧、茶色の服
- **soldier_2.png**: 赤い鎧、灰色の服
- **soldier_3.png**: 緑の鎧、黒い服
- **soldier_4.png**: 金色の鎧、白い服
- **soldier_5.png**: 銀色の鎧、紫の服

## ファイル構造

```
wide/
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
# ランダムスキンでスポーン
/summon the_four_primitives_and_weapons:common_soldier ~ ~ ~

# 特定のスキンを指定（soldier_1.png = インデックス1）
/summon the_four_primitives_and_weapons:common_soldier ~ ~ ~ {IsSlim:0,SkinIndex:1}
```

## 注意事項

- スキンファイルが存在しない番号は自動的にスキップされます
- 最低1つのスキンファイルがない場合、デフォルトのSteveスキンが使用されます
- ファイル名は正確に（大文字小文字を区別）
- 最大20個のスキンをサポート

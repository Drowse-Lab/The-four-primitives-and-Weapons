# 軍服(明治制服)の3Dモデルを Blockbench で編集する

以下の3ファイルは Java の `ModelMeijiUniform.createBodyLayer()` に対応する
Blockbench プロジェクトです（ジオメトリ共通、 各制服のテクスチャ＋手袋を合成して埋め込み済み）。

- `meiji_uniform_imperial_army.bbmodel` … 大日本軍 軍服
- `meiji_uniform_battoutai.bbmodel` … 抜刀隊 制服
- `meiji_uniform_meiji_police.bbmodel` … 明治警官 制服

3制服は同じジオメトリを共有しているので、 形を変えたら **3ファイルすべて同じ編集**を反映するか、
1つを編集して `createBodyLayer()` を共通の `ModelMeijiUniform.java` に貼り替えれば全制服に反映されます。

## 開き方
1. Blockbench でいずれかの `.bbmodel` を開く（File → Open Model）。
2. 形式は **Modded Entity**、解像度 128×128。

## ボーン構成（armor スロットへの割当）
- `head` … 兜（クラウン/バンド/つば/天面ボタン）= HELMET
- `body` + `right_arm`/`left_arm` … 上着（チュニック/襟/ベルト/肩章/袖口/手袋）= CHESTPLATE
- `right_leg`/`left_leg` … ズボン = LEGGINGS
- `right_boot`/`left_boot` … 革ブーツ = BOOTS（脚スロット）

※ `glove_r`/`glove_l` キューブは「手袋」。 染色される層なので、 UV は本体テクスチャでは透明、
  `*_gloves.png` 側に白で描かれています。

## 反映方法（編集後）
1. **ボーン名は変えない**（head/body/right_arm/left_arm/right_leg/left_leg/right_boot/left_boot）。
2. File → Export → **Java/Modded Entity** で `createBodyLayer()` を書き出す。
3. 中身を `src/main/java/.../client/model/ModelMeijiUniform.java` の
   `createBodyLayer()` に貼り替える（クラス名・LAYER_LOCATION・フィールド宣言・
   renderToBuffer の各 part.render はそのまま残す）。
4. UV(texOffs) を変えた場合は `textures/entities/<制服>.png` 側も合わせて描き直す。

## 注意
- 座標は Java→Blockbench 変換（Y反転, bbY = 24 − javaY）で生成しています。
  Blockbench 上で多少ズレて見える場合は位置を微調整してください。
- 3制服はこの1モデルを共有し、 色はテクスチャだけ差し替えています。

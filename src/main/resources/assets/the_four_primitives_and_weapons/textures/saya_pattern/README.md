# 鞘の機織り模様テクスチャ (`saya_pattern`)

機織り機で旗模様 `<id>` を付けると、鞘の種類に応じて以下を鞘に重ねて表示します。

| 優先 | 参照先 | 備考 |
|---|---|---|
| 1 | `saya_pattern/<type>/<category>/<id>.png` | `<type>` = `katana`/`sword`/`tyokuto`/`rapier`、`<category>` は下表 |
| 2 | `saya_pattern/<type>/<id>.png` | 種類別・カテゴリ無し |
| 3 | `saya_pattern/<category>/<id>.png` | 種類共通・カテゴリ別 |
| 4 | `saya_pattern/<id>.png` | 種類共通 |
| 5 | （無ければ）**表示しない** | その模様レイヤーはスキップ（地色のみ） |

> **用意した模様だけ鞘に出ます。** バニラ旗模様へのフォールバックは無し。
> 模様を**増やしたい**→ その `<id>.png` を該当カテゴリに追加。**減らしたい**→ その png を消すだけ。
> 既定では下記の「特定の模様」だけ用意してあります（仮テクスチャ）:
> `base` `stripe_center` `stripe_middle` `small_stripes` `cross` `straight_cross` `border` `circle` `rhombus` `half_vertical` `half_horizontal`

ファイルは形ごとに `<category>` フォルダへ整理してあります（四角は `square/`、三角は `triangle/`…）。

| category | 含まれる id |
|---|---|
| `square` | square_top_left, square_top_right, square_bottom_left, square_bottom_right |
| `triangle` | triangle_top, triangle_bottom, triangles_top, triangles_bottom, diagonal_left, diagonal_right, diagonal_up_left, diagonal_up_right |
| `stripe` | stripe_top, stripe_bottom, stripe_middle, stripe_left, stripe_right, stripe_center, small_stripes, stripe_downleft, stripe_downright |
| `half` | half_horizontal, half_horizontal_bottom, half_vertical, half_vertical_right |
| `cross` | cross, straight_cross |
| `border` | border, curly_border |
| `gradient` | gradient, gradient_up |
| `shape` | circle, rhombus |
| `fill` | base, bricks |
| `figure` | creeper, skull, flower, mojang, globe, piglin |

## 描き方

- 各 `type` フォルダの png は、その鞘の **wrap テクスチャと同じ UV レイアウト**で描く（同じ場所が鞘の同じ面に対応）。下絵にすると位置が合う：

  | type | 下絵にする wrap テクスチャ |
  |---|---|
  | `katana` | `item/saya.png` |
  | `sword` | `item/sword_saya.png` |
  | `tyokuto` | `item/saya_tyokuto.png` |
  | `rapier` | `item/saya/saya_rapier.png` |

- 模様にしたい所を **白 (255,255,255)**、模様でない所は **透明**に。染料色は実行時に乗算で着くので、白く塗った所がその色になる。
- **上面（刀を差す口）・底面**には出ません（コード側で除外）。

> 現在の `<type>/<id>.png` は、模様の形ごとに分けた仮テクスチャ（四角→四角、三角→三角、縞→縞、丸→丸…を各鞘の不透明領域内に白で配置）。あくまで土台なので、各 `id` を清書して実際の模様にしてください。位置（上下左右）は wrap テクスチャ上の向きに合わせて調整を。

## `<id>` と形の対応（バニラ旗模様の形）

### 帯・縞
| id | 形 |
|---|---|
| `base` | 全面（地全体） |
| `stripe_top` / `stripe_bottom` | 上／下の横帯 |
| `stripe_middle` | 中央の横帯（横一文字） |
| `stripe_left` / `stripe_right` | 左／右の縦帯 |
| `stripe_center` | 中央の縦帯（縦一文字） |
| `small_stripes` | 細い縦縞（多数） |

### 半分・四角・縁・地
| id | 形 |
|---|---|
| `half_horizontal` / `half_horizontal_bottom` | 上半分／下半分 |
| `half_vertical` / `half_vertical_right` | 左半分／右半分 |
| `square_top_left` / `square_top_right` | 左上／右上の四角 |
| `square_bottom_left` / `square_bottom_right` | 左下／右下の四角 |
| `border` / `curly_border` | 縁取り／波打つ縁取り |
| `bricks` | レンガ模様（全面） |
| `gradient` / `gradient_up` | グラデーション 上→下／下→上 |

### 斜め・十字・三角
| id | 形 |
|---|---|
| `stripe_downright` / `stripe_downleft` | 斜め帯 ＼／／ |
| `cross` / `straight_cross` | 斜め十字 ✕／十字 ＋ |
| `diagonal_right` / `diagonal_left` | 右下／左下の三角（斜め半分） |
| `diagonal_up_right` / `diagonal_up_left` | 右上／左上の三角 |
| `triangle_top` / `triangle_bottom` | 上向き／下向き三角 |
| `triangles_top` / `triangles_bottom` | 上辺／下辺のギザギザ（三角の並び） |

### 図柄
| id | 形 |
|---|---|
| `circle` / `rhombus` | 中央の丸／ひし形 |
| `flower` | 花（オックスアイデイジー） |
| `creeper` / `skull` / `piglin` | クリーパー顔／ドクロ／ピグリンの鼻 |
| `mojang` / `globe` | Mojang ロゴ／地球儀 |

> 「上/下/左/右」は**旗での形**の意味です。鞘では wrap テクスチャ上の位置として描いてください。

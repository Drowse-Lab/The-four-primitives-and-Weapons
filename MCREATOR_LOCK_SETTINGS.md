# MCreatorでファイルの自動上書きを防ぐ方法

## 手順

### 1. MCreatorでベースMod要素ファイルをロック

1. MCreatorを開く
2. メニューから「**Workspace → Workspace settings**」を選択
3. 「**Lock base mod element files**」にチェックを入れる
4. 「**OK**」をクリック

これにより、以下のファイルが自動再生成から保護されます：
- `TheFourPrimitivesAndWeaponsMod.java`（メインクラス）
- `TheFourPrimitivesAndWeaponsModEntities.java`
- `TheFourPrimitivesAndWeaponsModEntityRenderers.java`
- その他の基本的なModファイル

### 2. 個別要素のロック（実装済み）

`auto_lock_codes.py`スクリプトが既に実行されており、すべての個別要素が`locked_code: true`に設定されています。

### 3. カスタムエンティティの安全な実装（実装済み）

MCreatorによって上書きされないカスタムファイル：
- `/src/main/java/the_four_primitives_and_weapons/init/CustomEntityInit.java`
- `/src/main/java/the_four_primitives_and_weapons/client/init/CustomEntityRenderers.java`
- `/src/main/java/the_four_primitives_and_weapons/entity/DarkProjectileEntity.java`
- `/src/main/java/the_four_primitives_and_weapons/client/renderer/DarkProjectileRenderer.java`

これらのファイルは独立しているため、MCreatorの自動生成システムの影響を受けません。

## 重要な注意点

⚠️ **必ず「Lock base mod element files」を有効にしてください**
これをしないと、ビルドのたびに`TheFourPrimitivesAndWeaponsMod.java`が上書きされ、カスタムエンティティの登録が消えてしまいます。

## 確認方法

ロックが成功している場合、`TheFourPrimitivesAndWeaponsMod.java`の最初のコメントが以下のように表示されます：

```
/*
 *    MCreator note:
 *
 *    If you lock base mod element files, you can edit this file and it won't get overwritten.
 *    ...
 */
```

ロックされていない場合は以下のように表示されます：

```
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
```

## トラブルシューティング

もし再び自動上書きされる場合：
1. MCreatorの「Workspace settings」で「Lock base mod element files」が有効になっているか確認
2. `lock_codes.bat`または`lock_codes.sh`を再実行
3. MCreatorを再起動

これで、DarkProjectileEntityやその他のカスタムコードが保護されます！
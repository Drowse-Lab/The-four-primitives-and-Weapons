# TaCZ銃弾偏向テスト方法

## 🎯 2段階実行方式（これなら動く！）

### ステップ1: modをビルドして出力
1. **TaCZなしでビルド**
   ```bash
   # run/mods/が空の状態で
   # MCreatorで「Build」→「Export mod」
   # または「Build」→「Build mod JAR」
   ```

2. **生成されたmodを取得**
   ```
   build/libs/minecraft_armor_weapon-1.0.0.jar
   （または類似の名前）
   ```

### ステップ2: 通常のMinecraftで実行

1. **通常のMinecraft Forgeをセットアップ**
   - Minecraft 1.19.2
   - Forge 43.2.0
   - `.minecraft/mods/`フォルダ

2. **modを配置**
   ```
   .minecraft/mods/
   ├── minecraft_armor_weapon-1.0.0.jar （あなたのmod）
   └── tacz-1.19.2-1.1.4-hotfix-all.jar （TaCZ）
   ```

3. **Minecraftを起動**
   - 通常のMinecraftランチャーから
   - Forge 1.19.2プロファイルで起動

4. **テスト！**
   - FlyingAttackerEntityをスポーン
   - TaCZの銃で撃つ
   - 弾が偏向されるか確認！

## 🔧 なぜこれなら動くか

- **開発環境の問題回避**: MCreatorの開発環境ではなく、実際のゲーム環境で実行
- **Mixin競合なし**: 通常のForgeは`-all`版を正しく処理
- **完全な機能テスト**: TaCZ銃弾偏向が実際に動作するか確認可能

## 📝 FlyingAttackerEntityのTaCZ対応コード（確認）

```java
// すでに実装済み
private void deflectProjectiles() {
    // TaCZ弾の検出
    if (isTaczProjectile(projectile)) {
        deflectTaczBullet(projectile);
    }
}

private boolean isTaczProjectile(Entity entity) {
    try {
        Class<?> taczClass = Class.forName("com.tacz.guns.entity.EntityBullet");
        return taczClass.isInstance(entity);
    } catch (ClassNotFoundException e) {
        return false;
    }
}
```

## ✅ この方法のメリット

1. **確実に動作**: 実際のゲーム環境なので競合なし
2. **完全なテスト**: TaCZの全機能が使える
3. **配布版と同じ**: 実際にユーザーが使う環境と同じ

## 🚀 今すぐ試すには

1. MCreatorで「**Build → Export mod**」
2. 通常のMinecraft 1.19.2 + Forgeを用意
3. 両方のmodを入れて起動
4. TaCZ銃弾偏向をテスト！

**これなら確実にTaCZの銃弾を弾けるかテストできます！**
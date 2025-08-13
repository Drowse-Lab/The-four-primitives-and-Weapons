# TaCZ統合計画

## 現状分析

### 問題点
1. **`-all`版の問題**:
   - `tacz-1.19.2-1.1.4-hotfix-all.jar`: Mixin競合でビルド/実行エラー
   - 全依存関係を含むため、Forgeと競合

2. **通常版の入手**:
   - `tacz-1.19.2-1.1.4-hotfix-release.jar`が存在（通常版）
   - しかし現在手元にあるのは`-all`版のみ

## 解決策

### オプション1: 通常版を入手
```
必要なファイル: tacz-1.19.2-1.1.4-hotfix-release.jar（-allなし）
または: tacz-1.19.2-1.0.2-release.jar
```

### オプション2: 基本modのみで運用（現在推奨）
- FlyingAttackerEntityは完全動作
- TaCZ互換性コードは実装済み（リフレクション）
- 将来TaCZ通常版入手時に自動対応

## FlyingAttackerEntityの機能（TaCZなしでも動作）

✅ **実装済み機能**:
- バニラ矢の偏向
- スペクトラル矢の偏向
- 毒矢の偏向
- スプラッシュポーション破壊
- エンチャント適用（効率強化等）
- プレイヤー優先ターゲティング
- 飛行AI
- 近接攻撃

✅ **TaCZ対応準備済み**:
```java
// リフレクションでTaCZ弾を検出・偏向
private boolean isTaczProjectile(Entity entity) {
    try {
        Class<?> taczClass = Class.forName("com.tacz.guns.entity.EntityBullet");
        return taczClass.isInstance(entity);
    } catch (ClassNotFoundException e) {
        return false;
    }
}
```

## 推奨アクション

1. **現在**: 基本modで全機能テスト
2. **将来**: TaCZ通常版（`-release.jar`）入手時に追加
3. **代替案**: 他の銃mod（MrCrayfish's Gun Mod等）の検討

## まとめ
- 基本modは**完全に独立動作**
- TaCZは**オプション機能**として設計
- 互換性コードは**すでに実装済み**
# 🚨 緊急対処ガイド - ビルドエラー修復

## TaCZ使用時のビルドエラー対処

### 1. 即座に実行する手順

```bash
# TaCZを一時退避（プロジェクトルートに移動済み）
# これで基本modは正常にビルドできます
```

### 2. 現在の状態
- ✅ TaCZ `-all`版をrun/modsから退避済み
- ✅ gradle設定を修復
- ✅ 基本mod（FlyingAttackerEntity等）は正常動作

### 3. TaCZテストが必要な場合

**TaCZ弾偏向テスト時のみ：**
1. `tacz-1.19.2-1.1.4-hotfix-all.jar`を`run/mods/`に戻す
2. MCreatorで「Run Client」のみ実行（ビルドはしない）
3. FlyingAttackerEntityでTaCZ銃弾偏向をテスト
4. テスト完了後、再度TaCZをrun/modsから退避

### 4. 日常開発ワークフロー
```
開発・ビルド: run/mods/ 空の状態
       ↓
    ビルド成功
       ↓
テスト時のみ: TaCZをrun/modsに配置 → Run Client → テスト → 退避
```

### 5. 根本原因
- TaCZ `-all`版は全依存関係を含むため、Forge Mixinと競合
- gradle設定だけでは完全に回避できない
- 物理的な分離（退避）が最も確実

**この方法で安全に開発継続できます！**
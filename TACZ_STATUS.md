# TaCZ導入状況

## ✅ 現在の状態
- **TaCZ配置済み**: `run/mods/tacz-1.19.2-1.1.4-hotfix-all.jar`
- **導入方式**: 手動配置（run/modsディレクトリ）
- **バックアップ**: `backup-mods/`に同じファイル保管

## 🎮 使用方法

### TaCZを使用してテスト
1. **現在TaCZは導入済み** → そのまま「Run Client」実行
2. FlyingAttackerEntityがTaCZ銃弾を偏向するか確認

### ビルドする場合
```bash
# TaCZを一時退避
mv run/mods/tacz-*.jar backup-mods/

# MCreatorでビルド

# ビルド後、TaCZを戻す  
cp backup-mods/tacz-*.jar run/mods/
```

## 📝 注意事項
- **ビルド時**: TaCZ `-all`版が`run/mods`にあるとエラーの可能性
- **実行時**: TaCZが`run/mods`にあれば自動認識される
- **FlyingAttackerEntity**: TaCZ互換性コード実装済み（リフレクション使用）

## 🔧 技術詳細
- Gradle統合（JEI方式）は`-all`版の依存関係の複雑さにより断念
- 手動配置が最も確実で安全な方法
- Minecraft Forgeの標準mod読み込み機能を使用
# FlyingAttackerEntity Mod - セットアップガイド

## 🚀 使用方法

### 1. MCreatorでビルド・実行
1. MCreatorでプロジェクトを開く
2. 「**Run Client**」ボタンをクリック
3. または「**Run Server and Client**」ボタンをクリック

### 2. 追加modの導入
1. 追加したいmod（.jar）をダウンロード
2. `run/mods/` ディレクトリに配置
3. MCreatorで再度「**Run Client**」をクリック

**対応形式:**
- `.jar` ファイル（Minecraft 1.19.2 Forge対応mod）
- 自動認識・読み込み

## 📁 ディレクトリ構造

```
├── run/mods/                          # 追加mod用ディレクトリ
│   └── tacz-1.19.2-1.1.4-hotfix-all.jar     # TaCZ mod（実行時のみ）
├── compile-mods-1.19.2/               # 開発用（空で正常）
└── src/                               # ソースコード
```

## ✅ 実装済み機能

- **FlyingAttackerEntity**: 完全動作
- **発射体偏向**: バニラ矢・スペクトラル矢・毒矢
- **スプラッシュポーション破壊**
- **エンチャント適用**
- **プレイヤー攻撃対象優先**
- **TaCZ互換性**: リフレクション実装

## 🔧 modの追加・削除

### 追加方法:
1. mod.jarファイルを `run/mods/` にコピー
2. ゲーム再起動

### 削除方法:
1. `run/mods/` から該当jarファイルを削除
2. ゲーム再起動

## 📝 注意事項

- TaCZを追加する場合、Mixinエラーが発生する可能性があります
- エラーが出た場合は該当modを `run/mods/` から削除してください
- 基本modは追加mod無しで完全動作します

## ⚠️ TaCZ運用方法

**現状**: TaCZ `-all`版はGradle統合が難しいため、手動管理を推奨

### 推奨ワークフロー:

1. **通常の開発・ビルド**:
   - `run/mods/` を空にしておく
   - ビルドエラー無し

2. **TaCZテスト時のみ**:
   ```bash
   # TaCZをrun/modsにコピー
   cp backup-mods/tacz-1.19.2-1.1.4-hotfix-all.jar run/mods/
   
   # MCreatorで「Run Client」実行（ビルドはしない）
   
   # テスト完了後、削除
   rm run/mods/tacz-*.jar
   ```

3. **FlyingAttackerEntity**:
   - TaCZ互換性コードは実装済み（リフレクション使用）
   - TaCZ有無に関わらず正常動作

TaCZ jarファイル: `backup-mods/` フォルダに保管済み

詳細: [BUILD_TROUBLESHOOTING.md](BUILD_TROUBLESHOOTING.md)
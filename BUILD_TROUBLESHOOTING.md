# ビルドトラブルシューティング

## run/modsにmodを入れるとビルドできない問題

### 問題
- `run/mods/`ディレクトリにmodのjarファイルを配置するとビルドが失敗する
- 特に`-all`版のmodで発生しやすい

### 解決方法

#### 1. `-all`版modの対処法
- 現在入手可能: `tacz-1.19.2-1.1.4-hotfix-all.jar`
- ❌ この版は依存関係が全て含まれ、**ビルド時にMixinエラー**を引き起こします
- ✅ **解決策**: 実行専用として使用し、ビルド時は退避する

#### 2. gradle設定による分離
mcreator.gradleで以下の設定を追加：

```gradle
// run/modsディレクトリをビルドから除外
sourceSets {
    main {
        resources {
            exclude 'run/**'
        }
    }
}

// 実行時のみrun/modsを読み込み
client {
    if (file('run/mods').exists()) {
        jvmArgs '-Dfml.modsDir=' + file('run/mods').absolutePath
    }
}
```

### 🎉 NEW: JEI方式によるTaCZ統合

**解決済み**: TaCZをJEI同様にGradle依存関係として統合

```gradle
repositories {
  maven {
    name = "Curse Maven"
    url = "https://www.cursemaven.com"
  }
}

dependencies {
  // 公式CurseForge Snippetの最新版ID使用
  compileOnly fg.deobf("curse.maven:timeless-and-classics-zero-1028108:6069349")
  runtimeOnly fg.deobf("curse.maven:timeless-and-classics-zero-1028108:6069349")
}
```

**利点:**
- ✅ ビルドエラー無し（API分離）
- ✅ 自動ダウンロード（手動管理不要）
- ✅ FlyingAttackerEntity弾偏向機能完全対応

### 従来方式（backup）
手動管理が必要な場合：
1. **開発・ビルド用**: `compile-mods-1.19.2/`ディレクトリを使用
2. **実行・テスト用**: `run/mods/`ディレクトリを使用
3. 旧TaCZ jarファイル: `backup-mods/`に保管

**現在は新方式（Gradle統合）が推奨です。**
# マルチバージョン対応ビルドガイド

このプロジェクトは Minecraft 1.19.2 と 1.20.1 の両方に対応したモッドです。
Java 17でビルドする場合は1.20.1がターゲットになります。

## クイックスタート

### デフォルト（1.20.1 - Java 17 推奨）
```bash
./gradlew build
```

このコマンドで 1.20.1 向けのモッドファイル（`build/libs/minecraft_armor_weapon-1.0.jar`）が生成されます。

### 1.19.2 向けビルド（実験的）
```bash
./gradlew build -DMC_VERSION=1.19.2 -DMAPPING_VERSION=1.19.2 -DFORGE_VERSION=<version>
```

**注意**: 1.19.2 用の正確な Forge バージョンは、プロジェクトのテストが必要です。

## ビルド設定の詳細

### build.gradle - バージョン設定
以下のプロパティでビルドターゲットを制御します：

- `MC_VERSION`: ターゲット Minecraft バージョン  
  - デフォルト: `1.20.1`
  - 1.19.2の場合: `1.19.2`
  
- `MAPPING_VERSION`: Mojang マッピングのバージョン  
  - Minecraft バージョンと一致させる必要があります
  
- `FORGE_VERSION`: Forge のバージョン  
  - デフォルト: `1.20.1-47.1.0`
  - サポート対象版は FORGE_VERSION テーブルを参照

### mcreator.gradle - 自動バージョン切り替え
`MC_VERSION` に基づいて以下が自動的に選択されます：

- **compile-mods ディレクトリ**
  - 1.19.2 → `compile-mods-1.19.2/`
  - 1.20.1 → `compile-mods-1.20.1/`

- **依存関係パッケージ**
  - Curios、GeckoLib、JEI など、各バージョン対応のライブラリが自動選択されます

## サポート対象バージョン

| Minecraft | Forge | Java | Status |
|-----------|-------|------|--------|
| 1.20.1 | 47.1.0+ | 17 | ✅ デフォルト |
| 1.19.2 | ⚠️ TBD | 8/11 | 🔧 要テスト |

### 依存関係バージョン表

現在の設定：

| ライブラリ | 1.19.2 | 1.20.1 |
|----------|--------|--------|
| Curios | 5.1.1.0 | (要確認) |
| GeckoLib | geckolib-forge-1.19 3.1.40 | geckolib-forge-1.20 4.4.7+ |
| JEI | jei-1.19.2-forge 11.4.0.286 | jei-1.20.1-forge 14.6.13.56+ |

**注意**: 1.20.1 の Curios、GeckoLib、JEI バージョンはプロジェクト内で確認・テストが必要です。

## クライアント実行

### 1.20.1 用（デフォルト）
```bash
./gradlew runClient
```

### 1.19.2 用
```bash
./gradlew runClient \
  -DMC_VERSION=1.19.2 \
  -DMAPPING_VERSION=1.19.2 \
  -DFORGE_VERSION=<version>
```

## サーバー実行

### 1.20.1 用（デフォルト）
```bash
./gradlew runServer
```

### 1.19.2 用
```bash
./gradlew runServer \
  -DMC_VERSION=1.19.2 \
  -DMAPPING_VERSION=1.19.2 \
  -DFORGE_VERSION=<version>
```

## トラブルシューティング

### ビルド失敗時

1. **依存関係解決エラー**
   ```bash
   ./gradlew clean
   ./gradlew build
   ```

2. **JAR キャッシュクリア**
   ```bash
   rm -rf ~/.gradle/caches
   ```

3. **環境変数の確認**
   ```bash
   echo $JAVA_HOME
   java -version
   ```

### 1.19.2 ビルド時の注意

- `compile-mods-1.19.2/` ディレクトリに必要な JAR ファイルが存在することを確認
- Forge バージョンを正確に指定（スペースなし）
- `./gradlew clean` でキャッシュをクリア

## 推奨環境

- **OS**: Linux / macOS / Windows
- **Java**: OpenJDK 17 以上
- **Gradle**: 7.5+ （wrapper 同梱）
- **ディスク空き容量**: 最低 5GB

## 詳細設定

### gradle.properties でデフォルト変更

`gradle.properties` ファイルでデフォルトバージョンを永続的に変更できます：

```properties
MC_VERSION=1.19.2
MAPPING_VERSION=1.19.2
FORGE_VERSION=<version>
```

### カスタム Gradle オプション

```bash
./gradlew build \
  -DMC_VERSION=1.20.1 \
  -DMAPPING_VERSION=1.20.1 \
  -DFORGE_VERSION=1.20.1-47.1.0 \
  --no-daemon
```

## 参考リンク

- [Minecraft Forge Documentation](https://docs.minecraftforge.net/)
- [ForgeGradle チュートリアル](https://github.com/MinecraftForge/ForgeGradle/wiki)
- [このプロジェクトの copilot-instructions](/.github/copilot-instructions.md)

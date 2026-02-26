# The four primitives and Weapons - Build Guide

Minecraft Forge 1.20.1 Mod (Java 17)

## .jar ビルド手順

### 1. ビルド実行
```bash
./gradlew build
```
Windows (PowerShell/cmd) の場合:
```cmd
.\gradlew.bat build
```

### 2. 出力先
```
build/libs/The four primitives and Weapons-forge-1.20.1--1.0.jar
```

### 3. Minecraftに導入
1. 上記の `.jar` ファイルをコピー
2. Minecraftの `mods` フォルダに配置
   - デフォルト: `%APPDATA%\.minecraft\mods\`
3. Forge 1.20.1 の入ったプロファイルで起動

### クリーンビルド（キャッシュ問題時）
```bash
./gradlew clean build
```

### コンパイルだけ（エラーチェック）
```bash
./gradlew compileJava
```

### デバッグ付きビルド
```bash
./gradlew build --stacktrace
```

## Run (Test Play)

### Terminal
```bash
./gradlew runClient
```

### VSCode (F5 Debug)
1. `F5` or Run > Start Debugging
2. Select **runClient**
3. Minecraft launches with mod loaded + breakpoint support

### VSCode Task (Ctrl+Shift+B)
- **Build Project** - `./gradlew build`
- **Run Client** - `./gradlew runClient`
- **Run Server** - `./gradlew runServer`

### VSCode Run Config Regeneration
```bash
./gradlew genVSCodeRuns
```

## Server
```bash
./gradlew runServer
```

## Environment

| Item | Value |
|------|-------|
| Minecraft | 1.20.1 |
| Forge | 47.1.0 |
| Java | JDK 17 (`C:\Program Files\Java\jdk-17`) |
| Gradle | 7.5 (wrapper) |

### Dependencies

| Library | Version |
|---------|---------|
| GeckoLib | 4.7.3 (forge-1.20.1) |
| Curios | 5.x (forge-1.20.1) |
| JEI | 15.x (forge-1.20.1) |

## Troubleshooting

### ビルドが通らない場合
```bash
./gradlew clean
./gradlew build --stacktrace
```

### Gradle Cache Clear
```bash
rm -rf ~/.gradle/caches/forge_gradle
./gradlew build
```

### deprecation警告が出る
Curios APIの`SlotTypeMessage`等の警告はエラーではないので無視してOK。jarは正常に生成される。

### WSL (Linux) でビルドする場合

#### 初回セットアップ（Java 17インストール）
```bash
sudo apt update && sudo apt install -y openjdk-17-jdk
```

#### ビルド
```bash
bash build.sh          # 通常ビルド
bash build.sh clean    # クリーンビルド
```

詳細は [WSL_BUILD.md](WSL_BUILD.md) を参照。

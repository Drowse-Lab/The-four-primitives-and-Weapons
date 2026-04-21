# ビルド手順 (WSL / macOS)

## 初回セットアップ

### WSL (Windows)

#### 1. Java 17 インストール

```bash
sudo apt update && sudo apt install -y openjdk-17-jdk
```

確認：

```bash
java -version
# openjdk version "17.x.x" と表示されればOK
```

#### 2. JAVA_HOME 設定（永続化）

```bash
echo 'export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64' >> ~/.bashrc
echo 'export PATH="$JAVA_HOME/bin:$PATH"' >> ~/.bashrc
source ~/.bashrc
```

### macOS

#### 1. Java 17 インストール (Homebrew)

```bash
brew install --cask temurin@17
```

確認：

```bash
java -version
# openjdk version "17.x.x" と表示されればOK
```

#### 2. JAVA_HOME 設定（永続化）

```bash
echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 17)' >> ~/.zshrc
echo 'export PATH="$JAVA_HOME/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
```

## ビルド

### WSL (Windows)

```bash
bash build_win.sh                   # 通常ビルド
bash build_win.sh clean             # クリーンビルド
bash build_win.sh offline           # オフラインビルド
bash build_win.sh clean offline     # クリーン＋オフライン
```

### macOS

```bash
bash build_mac.sh                   # 通常ビルド
bash build_mac.sh clean             # クリーンビルド
bash build_mac.sh offline           # オフラインビルド
bash build_mac.sh clean offline     # クリーン＋オフライン
```

### 手動で実行 (共通)

```bash
# WSL:
cd /mnt/c/Users/hrmcn/MCreatorWorkspaces/minecraft_armor_weapon
# mac: ローカルにcloneしたパスへ
./gradlew build
```

## 出力先

```
build/libs/The four primitives and Weapons-forge-1.20.1--1.0.jar
```

## テストプレイ

### WSL (Windows)

```bash
bash run_client_win.sh              # 通常起動
bash run_client_win.sh offline      # オフライン起動
```

### macOS

```bash
bash run_client_mac.sh              # 通常起動
bash run_client_mac.sh offline      # オフライン起動
```

## トラブルシューティング

### JAVA_HOME is not set エラー

WSL:
```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
```

macOS:
```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
```

### Permission denied

WSL:
```bash
chmod +x gradlew
chmod +x build_win.sh run_client_win.sh
```

macOS:
```bash
chmod +x gradlew
chmod +x build_mac.sh run_client_mac.sh
```

### 改行コードエラー (`$'\r': command not found` など)

Windowsで編集した `.sh` がCRLFになっている場合:

```bash
sed -i 's/\r$//' build_win.sh run_client_win.sh
# mac (BSD sed) の場合:
sed -i '' 's/\r$//' build_mac.sh run_client_mac.sh
```

### キャッシュ問題

```bash
# WSL
bash build_win.sh clean
# mac
bash build_mac.sh clean
```

それでもダメなら：

```bash
rm -rf ~/.gradle/caches/forge_gradle
# 上記ビルドを再実行
```

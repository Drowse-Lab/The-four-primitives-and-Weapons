# WSL ビルド手順

## 初回セットアップ

### 1. Java 17 インストール

```bash
sudo apt update && sudo apt install -y openjdk-17-jdk
```

確認：

```bash
java -version
# openjdk version "17.x.x" と表示されればOK
```

### 2. JAVA_HOME 設定（永続化）

```bash
echo 'export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64' >> ~/.bashrc
source ~/.bashrc
```

## ビルド

### スクリプトで実行

```bash
bash build.sh                   # 通常ビルド
bash build.sh clean             # クリーンビルド
bash build.sh offline	        # オフラインビルド
bash build.sh clean offline	    # クリーン＋オフライン
```

### 手動で実行

```bash
cd /mnt/c/Users/hrmcn/MCreatorWorkspaces/minecraft_armor_weapon
./gradlew build
```

## 出力先

```
build/libs/The four primitives and Weapons-forge-1.20.1--1.0.jar
```

## テストプレイ

```bash
bash run_client.sh	            # 通常起動
bash run_client.sh offline	    # オフライン起動
```

## トラブルシューティング

### JAVA_HOME is not set エラー

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
```

### Permission denied

```bash
chmod +x gradlew
chmod +x build.sh
```

### キャッシュ問題

```bash
bash build.sh clean
```

それでもダメなら：

```bash
rm -rf ~/.gradle/caches/forge_gradle
bash build.sh
```

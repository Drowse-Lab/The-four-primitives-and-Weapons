#!/bin/bash
# jarをビルドするスクリプト（mac / WSL / Windows Git Bash 対応）
#
# 使い方:
#   bash build.sh                  通常ビルド
#   bash build.sh clean            クリーンビルド
#   bash build.sh offline          オフラインビルド（キャッシュ済み依存のみ）
#   bash build.sh clean offline    クリーン + オフライン
#   bash build.sh offline clean    同上（順不同）

cd "$(dirname "$0")"

TASKS="build"
GRADLE_ARGS=""
LABEL="Build"
DO_CLEAN=0
for arg in "$@"; do
    case "$arg" in
        clean)
            DO_CLEAN=1
            LABEL="Clean Build"
            ;;
        offline)
            GRADLE_ARGS="$GRADLE_ARGS --offline -Dnet.minecraftforge.gradle.check.certs=false"
            LABEL="$LABEL (Offline)"
            ;;
    esac
done

echo "=== $LABEL ==="

# クリーン: build/ を掃除するが build/fg_cache ( 再コンパイル済み Minecraft 依存 ) は残す。
#   gradle の `clean` タスクは fg_cache も消すため、 同一実行の compileJava が
#   「設定時にマップ済み MC jar が未生成」 となり net.minecraft.* を解決できず
#   "package net.minecraft.* does not exist" で大量に失敗する ( 2 回目以降は通る )。
#   依存キャッシュを残せば 1 回の build で通り、 対話プロンプトも 1 回で済む。
#   ※ Forge バージョン変更等で MC を完全に再生成したい時だけ手動で `rm -rf build` を。
if [ "$DO_CLEAN" = "1" ] && [ -d build ]; then
    find build -mindepth 1 -maxdepth 1 ! -name fg_cache -exec rm -rf {} +
fi

case "$(uname -s)" in
    MINGW*|CYGWIN*|MSYS*)
        ./gradlew.bat $TASKS $GRADLE_ARGS
        ;;
    *)
        ./gradlew $TASKS $GRADLE_ARGS
        ;;
esac
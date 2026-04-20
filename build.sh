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
for arg in "$@"; do
    case "$arg" in
        clean)
            TASKS="clean build"
            LABEL="Clean Build"
            ;;
        offline)
            GRADLE_ARGS="$GRADLE_ARGS --offline -Dnet.minecraftforge.gradle.check.certs=false"
            LABEL="$LABEL (Offline)"
            ;;
    esac
done

echo "=== $LABEL ==="

case "$(uname -s)" in
    MINGW*|CYGWIN*|MSYS*)
        ./gradlew.bat $TASKS $GRADLE_ARGS
        ;;
    *)
        ./gradlew $TASKS $GRADLE_ARGS
        ;;
esac

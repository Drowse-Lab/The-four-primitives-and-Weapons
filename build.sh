#!/bin/bash
# jarをビルドするスクリプト（mac / WSL 両対応）
#
# 使い方:
#   bash build.sh          通常ビルド
#   bash build.sh clean    クリーンビルド

cd "$(dirname "$0")"

case "$(uname -s)" in
    Darwin|Linux)
        # mac または Linux（ネイティブ）
        if [ "$1" = "clean" ]; then
            echo "=== Clean Build ==="
            ./gradlew clean build
        else
            echo "=== Build ==="
            ./gradlew build
        fi
        ;;
    MINGW*|CYGWIN*|MSYS*)
        # Windows Git Bash 等
        if [ "$1" = "clean" ]; then
            echo "=== Clean Build ==="
            ./gradlew.bat clean build
        else
            echo "=== Build ==="
            ./gradlew.bat build
        fi
        ;;
    *)
        # WSL（uname は Linux だが cmd.exe が使える場合はネイティブ gradlew で OK）
        if [ "$1" = "clean" ]; then
            echo "=== Clean Build ==="
            ./gradlew clean build
        else
            echo "=== Build ==="
            ./gradlew build
        fi
        ;;
esac

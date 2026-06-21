#!/bin/bash
# jarをビルドするスクリプト（mac / WSL / Windows Git Bash 対応）
#
# 使い方:
#   bash build.sh                              通常ビルド (Gradle が version / release_type をプロンプト)
#   bash build.sh clean                        クリーンビルド
#   bash build.sh offline                      オフラインビルド (キャッシュ済み依存のみ)
#   bash build.sh -Pmod_version_override=1.2.3 プロンプトを skip して 1.2.3 で即ビルド
#   bash build.sh -Prelease_type=release       version は対話、release type だけ指定
#
# 対話プロンプトは build.gradle 側 (Backpack-Arsenal と同じ仕組み):
#   1) version を聞く (空 Enter で gradle.properties の mod_version)
#   2) release type を聞く (b/a/r/rc/t)
#   最終的に "<version><suffix>" が jar 名と mods.toml の version になる。

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
        *)
            # それ以外は Gradle にそのまま渡す (-Pmod_version_override=X 等)
            GRADLE_ARGS="$GRADLE_ARGS $arg"
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

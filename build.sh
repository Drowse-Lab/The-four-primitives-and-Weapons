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
        *)
            # それ以外は Gradle にそのまま渡す (-Pmod_version_override=X 等)
            GRADLE_ARGS="$GRADLE_ARGS $arg"
            ;;
    esac
done

echo "=== $LABEL ==="

# クリーン: build/ を掃除するが build/fg_cache ( 再コンパイル済み Minecraft 依存 ) は残す。
#   gradle の `clean` タスクは fg_cache も消すため、 同一実行の compileJava が
#   "package net.minecraft.* does not exist" で大量に失敗する ( 設定時にマップ済み MC jar が未生成のため )。
#   依存キャッシュを残せば 1 回の build で通る。 MC を完全再生成したい時のみ手動で `rm -rf build`。
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

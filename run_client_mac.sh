#!/bin/bash
# Minecraftクライアントを起動するスクリプト（mac / WSL / Windows Git Bash 対応）
#
# 使い方:
#   bash run_client.sh            通常（オンライン）
#   bash run_client.sh offline    オフライン（キャッシュ済み依存のみで起動）

cd "$(dirname "$0")"

GRADLE_ARGS="runClient"
if [ "$1" = "offline" ]; then
    # --offline: ネットワーク経由の依存解決を禁止
    # -Dnet.minecraftforge.gradle.check.certs=false: プラグイン適用時の証明書検証を無効化
    #   （オフラインでは maven.minecraftforge.net に接続できないため必要）
    GRADLE_ARGS="$GRADLE_ARGS --offline -Dnet.minecraftforge.gradle.check.certs=false"
    echo "=== Offline mode (using cached dependencies) ==="
fi

case "$(uname -s)" in
    MINGW*|CYGWIN*|MSYS*)
        ./gradlew.bat $GRADLE_ARGS
        ;;
    *)
        ./gradlew $GRADLE_ARGS
        ;;
esac
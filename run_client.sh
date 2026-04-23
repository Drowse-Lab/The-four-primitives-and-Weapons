#!/bin/bash
# Minecraftクライアントを起動するスクリプト（mac / WSL / Windows Git Bash 対応）
#
# 使い方:
#   bash run_client.sh            通常（オンライン）
#   bash run_client.sh offline    オフライン（キャッシュ済み依存のみで起動）
#
# 起動前に Iron's Spells 'n Spellbooks を入れるか対話で尋ねる (y/N)。
# 環境変数で上書き: WITH_SPELLBOOKS=1 強制 / SKIP_SPELLBOOKS_PROMPT=1 対話なし。

cd "$(dirname "$0")"

GRADLE_ARGS="runClient"
if [ "$1" = "offline" ]; then
    GRADLE_ARGS="$GRADLE_ARGS --offline -Dnet.minecraftforge.gradle.check.certs=false"
    echo "=== Offline mode (using cached dependencies) ==="
fi

# Iron's Spellbooks プロンプト
if [ -n "$WITH_SPELLBOOKS" ]; then
    USE_SPELLBOOKS="yes"
elif [ -n "$SKIP_SPELLBOOKS_PROMPT" ]; then
    USE_SPELLBOOKS="no"
elif [ -t 0 ]; then
    printf "Iron's Spells 'n Spellbooks を入れてビルドしますか? [y/N]: "
    read ANSWER
    case "$ANSWER" in
        y|Y|yes|YES|Yes) USE_SPELLBOOKS="yes" ;;
        *)               USE_SPELLBOOKS="no"  ;;
    esac
else
    USE_SPELLBOOKS="no"
fi

if [ "$USE_SPELLBOOKS" = "yes" ]; then
    GRADLE_ARGS="$GRADLE_ARGS -PwithSpellbooks=true"
    echo "=== with Iron's Spellbooks ==="
    if [ -f "libs/ironsspellbooks-1.20.1.jar" ]; then
        echo "  → libs/ironsspellbooks-1.20.1.jar を検出、同梱します"
    elif ls run/mods/ironsspellbooks*.jar >/dev/null 2>&1; then
        echo "  → run/mods/ 内の Iron's Spellbooks JAR を使います"
    else
        echo "  ! libs/ にも run/mods/ にも Iron's Spellbooks JAR が見つかりません。"
        echo "    https://www.curseforge.com/minecraft/mc-mods/irons-spells-n-spellbooks/files"
    fi
fi

case "$(uname -s)" in
    MINGW*|CYGWIN*|MSYS*)
        ./gradlew.bat $GRADLE_ARGS
        ;;
    *)
        ./gradlew $GRADLE_ARGS
        ;;
esac

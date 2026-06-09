#!/bin/bash
# Minecraftクライアントを起動するスクリプト（mac / WSL / Windows Git Bash 対応）
#
# 使い方:
#   bash run_client_mac.sh            通常（オンライン）
#   bash run_client_mac.sh offline    オフライン（キャッシュ済み依存のみで起動）
#
# 起動前に Iron's Spells 'n Spellbooks を入れるか対話で尋ねる (y/N)。
# y/yes を選ぶと -PwithSpellbooks=true を gradle に渡し、
#   libs/ironsspellbooks-1.20.1.jar または run/mods/ への JAR 配置で連携ビルドできる。

cd "$(dirname "$0")"

GRADLE_ARGS="runClient"
if [ "$1" = "offline" ]; then
    # downloadAssets は --offline を尊重せず piston-meta.mojang.com に毎回検証 HTTP を送る。
    # アセットは ~/.gradle/caches/forge_gradle/assets/ にキャッシュ済みなので
    # offline モードでは -x で明示的に除外する (SSL handshake failure 回避)。
    #
    # ForgeGradle 5.x の MCPRepo.findVersion も同様に --offline を完全には尊重せず、
    # maven.minecraftforge.net や dvs1.progwml6.com に HEAD 投げる。Java 17 のデフォルト
    # TLS 設定だと特定 cipher suite で handshake_failure を起こすことが報告されている。
    # TLS 1.2 を明示して TLS 1.3 関連の互換性問題を回避し、握手だけは通るようにする。
    # （実通信は --offline でほぼスキップされるので、SSL handshake さえ通れば OK）
    GRADLE_ARGS="$GRADLE_ARGS --offline -Dnet.minecraftforge.gradle.check.certs=false -x downloadAssets"
    GRADLE_ARGS="$GRADLE_ARGS -Dhttps.protocols=TLSv1.2,TLSv1.3 -Djdk.tls.client.protocols=TLSv1.2,TLSv1.3"
    GRADLE_ARGS="$GRADLE_ARGS -Djava.security.egd=file:/dev/./urandom"
    echo "=== Offline mode (using cached dependencies, skipping downloadAssets) ==="
fi

# --- Iron's Spellbooks 同梱の対話確認 ---
# 非対話環境 (CI等) での誤検出を防ぐため、標準入力が TTY の時のみ問う。
# 環境変数で明示指定もできる: WITH_SPELLBOOKS=1 / SKIP_SPELLBOOKS_PROMPT=1
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
        echo "  ! libs/ironsspellbooks-1.20.1.jar も run/mods/ にも見つからない。"
        echo "    CurseForge からダウンロードして libs/ またはプロジェクトの run/mods/ に配置してください。"
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

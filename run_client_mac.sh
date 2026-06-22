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
    GRADLE_ARGS="$GRADLE_ARGS --offline -x downloadAssets"
    echo "=== Offline mode (using cached dependencies, skipping downloadAssets) ==="
fi

# --- TLS workaround for Cisco Umbrella SSL inspection ---
# 開発端末のネット (マンション ISP) が Cisco Umbrella の透過 SSL 検査を経由しており、
# 中間 Proxy が古くて TLS 1.3 と ECDHE/DHE 系 cipher を理解できない。
# JDK 17 が ClientHello を送ると handshake_failure になるため、
# TLS 1.2 + RSA key exchange cipher のみに限定する。
# 該当 cipher は JDK 17 でデフォルト無効化されているので
# tls_workaround.properties で再有効化。
#
# JAVA_TOOL_OPTIONS は JVM 起動時に自動で picked up されるので
# Gradle daemon / worker 含む全 JVM に伝播する (org.gradle.jvmargs より確実)。
TLS_WORKAROUND_FILE="$(pwd)/tls_workaround.properties"
if [ -f "$TLS_WORKAROUND_FILE" ]; then
    export JAVA_TOOL_OPTIONS="-Djava.security.properties=${TLS_WORKAROUND_FILE} -Djdk.tls.client.protocols=TLSv1.2 -Dhttps.protocols=TLSv1.2 -Djdk.tls.client.cipherSuites=TLS_RSA_WITH_AES_256_GCM_SHA384,TLS_RSA_WITH_AES_128_GCM_SHA256,TLS_RSA_WITH_AES_256_CBC_SHA256,TLS_RSA_WITH_AES_128_CBC_SHA256"
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

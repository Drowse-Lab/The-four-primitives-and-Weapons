#!/bin/bash
# Minecraftクライアントを起動するスクリプト（mac / WSL / Windows Git Bash 対応）
#
# 使い方:
#   bash run_client_mac.sh                通常 ( オンライン、 TLS workaround は回線を実測して自動判定 )
#   bash run_client_mac.sh offline        オフライン ( キャッシュ済み依存のみ )
#   bash run_client_mac.sh notls          テザリング等 ( workaround off を強制 — 素の TLS )
#   bash run_client_mac.sh tls            Cisco Umbrella 検査回線 ( workaround on を強制 )
#   bash run_client_mac.sh offline notls  併用も可
#   bash run_client_mac.sh keepdaemon     gradle daemon を kill しない ( デフォルトは kill )
#
# 起動前に外部 mod ( libs/local/ 配下 ) を含めるか対話で尋ねる ( y/N )。
# y を選ぶと -PwithExternalMods=true を gradle に渡し、 libs/local/*.jar が自動取り込み。

cd "$(dirname "$0")"

GRADLE_ARGS="runClient"
USE_TLS_WORKAROUND="auto"   # auto = 回線を実測して自動判定 ( notls / tls で明示上書き可 )
KILL_DAEMON="yes"   # JAVA_TOOL_OPTIONS / gradle.properties 変更が daemon に反映されない問題対策
for arg in "$@"; do
    case "$arg" in
        offline)
            GRADLE_ARGS="$GRADLE_ARGS --offline -x downloadAssets"
            echo "=== Offline mode (using cached dependencies, skipping downloadAssets) ==="
            ;;
        notls|no-tls)
            USE_TLS_WORKAROUND="no"
            echo "=== TLS workaround OFF (デフォルト TLS で接続 — テザリング等 直接回線向け) ==="
            ;;
        tls|force-tls|forcetls)
            USE_TLS_WORKAROUND="yes"
            echo "=== TLS workaround ON (強制 — Cisco Umbrella 検査回線向け) ==="
            ;;
        keepdaemon|keep-daemon)
            KILL_DAEMON="no"
            ;;
    esac
done

# --- TLS workaround 自動判定 ---
# 引数で notls / tls を明示しなかった場合、実際に素の TLS で Mojang の
# メタサーバへ到達できるかを curl で 1 回だけ試す。
#   到達できる ( テザリング等 直接回線 )        → workaround OFF
#   到達できない ( Cisco Umbrella 検査回線など ) → workaround ON
# これで `bash run_client_mac.sh` を引数なしで両方の回線に対応させる。
if [ "$USE_TLS_WORKAROUND" = "auto" ]; then
    echo "=== TLS 回線を自動判定中 (piston-meta.mojang.com へ素の TLS で接続テスト) ==="
    if curl -s -o /dev/null --max-time 8 https://piston-meta.mojang.com/mc/game/version_manifest_v2.json 2>/dev/null; then
        USE_TLS_WORKAROUND="no"
        echo "=== 判定: 素の TLS で到達可能 → TLS workaround OFF ==="
    else
        USE_TLS_WORKAROUND="yes"
        echo "=== 判定: 素の TLS が不通 → TLS workaround ON (Cisco Umbrella 検査回線とみなす) ==="
    fi
fi

# --- gradle daemon を停止 ( JVM 引数 / 環境変数の変更を確実に反映させる ) ---
# 起動毎に 5 秒前後余分にかかるが、 「daemon が古い JAVA_TOOL_OPTIONS を保持していて
# notls / TLS workaround の切り替えが効かない」 問題を完全に潰せる。
# どうしても daemon を残したい時は引数に `keepdaemon` を追加 ( 既存 daemon が起動中ならそのまま使う )。
if [ "$KILL_DAEMON" = "yes" ]; then
    if [ -x ./gradlew ]; then
        echo "=== Stopping any running gradle daemon (--stop) ==="
        ./gradlew --stop > /dev/null 2>&1 || true
    fi
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
#
# `bash run_client_mac.sh notls` で一時的に workaround を無効化できる
# ( テザリング等 で 直接 maven.minecraftforge.net に到達できる時に使用 )。
TLS_WORKAROUND_FILE="$(pwd)/tls_workaround.properties"
if [ "$USE_TLS_WORKAROUND" = "yes" ] && [ -f "$TLS_WORKAROUND_FILE" ]; then
    export JAVA_TOOL_OPTIONS="-Djava.security.properties=${TLS_WORKAROUND_FILE} -Djdk.tls.client.protocols=TLSv1.2 -Dhttps.protocols=TLSv1.2 -Djdk.tls.client.cipherSuites=TLS_RSA_WITH_AES_256_GCM_SHA384,TLS_RSA_WITH_AES_128_GCM_SHA256,TLS_RSA_WITH_AES_256_CBC_SHA256,TLS_RSA_WITH_AES_128_CBC_SHA256"
fi

# --- 外部 mod 同梱の対話確認 ---
# 非対話環境 (CI等) での誤検出を防ぐため、標準入力が TTY の時のみ問う。
# 環境変数で明示指定もできる: WITH_EXTERNAL_MODS=1 / SKIP_EXTERNAL_MODS_PROMPT=1
# ( 後方互換 ) WITH_SPELLBOOKS=1 / SKIP_SPELLBOOKS_PROMPT=1 も従来通り受け付ける。
if [ -n "$WITH_EXTERNAL_MODS" ] || [ -n "$WITH_SPELLBOOKS" ]; then
    USE_EXTERNAL_MODS="yes"
elif [ -n "$SKIP_EXTERNAL_MODS_PROMPT" ] || [ -n "$SKIP_SPELLBOOKS_PROMPT" ]; then
    USE_EXTERNAL_MODS="no"
elif [ -t 0 ]; then
    printf "外部 mod をオンにしますか? [y/N]: "
    read ANSWER
    case "$ANSWER" in
        y|Y|yes|YES|Yes) USE_EXTERNAL_MODS="yes" ;;
        *)               USE_EXTERNAL_MODS="no"  ;;
    esac
else
    USE_EXTERNAL_MODS="no"
fi

if [ "$USE_EXTERNAL_MODS" = "yes" ]; then
    GRADLE_ARGS="$GRADLE_ARGS -PwithExternalMods=true"
    echo "=== 外部 mod ON ( libs/local/ 配下の .jar を自動ロード ) ==="
    if [ -d libs/local ]; then
        found=0
        while IFS= read -r jar; do
            echo "  → $jar"
            found=$((found + 1))
        done < <(find libs/local -type f -name "*.jar" 2>/dev/null | sort)
        if [ "$found" = "0" ]; then
            echo "  ! libs/local/ に .jar がありません ( ここに置けば自動取り込み )"
        fi
    else
        echo "  ! libs/local/ ディレクトリが存在しません — 作成して .jar を置いてください"
    fi
else
    echo "=== 外部 mod OFF ==="
fi

case "$(uname -s)" in
    MINGW*|CYGWIN*|MSYS*)
        ./gradlew.bat $GRADLE_ARGS
        ;;
    *)
        ./gradlew $GRADLE_ARGS
        ;;
esac

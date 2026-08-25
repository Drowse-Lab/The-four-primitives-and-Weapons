#!/bin/bash
# Minecraftクライアントを起動するスクリプト（mac / WSL / Windows Git Bash 対応）
#
# 使い方:
#   bash run_client_mac.sh                オフライン ( 学校Wi-Fi向けのデフォルト )
#   bash run_client_mac.sh online         オンライン ( TLS workaround は回線を実測して自動判定 )
#   bash run_client_mac.sh offline        明示的なオフライン (従来互換)
#   bash run_client_mac.sh notls          テザリング等 ( workaround off を強制 — 素の TLS )
#   bash run_client_mac.sh tls            Cisco Umbrella 検査回線 ( workaround on を強制 )
#   bash run_client_mac.sh offline notls  併用も可
#   bash run_client_mac.sh keepdaemon     gradle daemon を kill しない ( デフォルトは kill )
#   bash run_client_mac.sh help           この説明を表示して終了
#
# 起動時の質問:
#   1. 外部 mod をオンにしますか?
#      y = 下記の外部MODを同期して起動 / Enter または n = 本体MODだけで起動
#   2. ラグを減らす軽量モードにしますか?
#      Enter または y = 軽い3個だけ / n = 全部入り9個
#
# 軽量モードで入るMOD:
#   - chuzume-addon
#   - extra_video_settings
#   - RPGish-HPDisplay (mh_rpgish)
#
# 全部入りで追加されるMOD:
#   - gun_and_weapon + TACZ
#   - Backpack Arsenal
#   - Mekanism
#   - Sophisticated Core + Sophisticated Backpacks
#
# 質問を省略する例:
#   WITH_EXTERNAL_MODS=1 LIGHT_EXTERNAL_MODS=1 bash run_client_mac.sh offline  # 軽量
#   WITH_EXTERNAL_MODS=1 LIGHT_EXTERNAL_MODS=0 bash run_client_mac.sh offline  # 全部入り
#
# 起動前に外部MODを含めるか対話で尋ねる。選択したJARは
# libs/runtime_selected/ へ同期し、-PwithExternalMods=true で読み込む。

cd "$(dirname "$0")"

show_help() {
    sed -n '2,/^$/p' "$0" | sed 's/^# \{0,1\}//'
}

for arg in "$@"; do
    case "$arg" in
        help|--help|-h)
            show_help
            exit 0
            ;;
    esac
done

GRADLE_ARGS="runClient --offline -x downloadAssets"
USE_TLS_WORKAROUND="auto"   # auto = 回線を実測して自動判定 ( notls / tls で明示上書き可 )
KILL_DAEMON="yes"   # JAVA_TOOL_OPTIONS / gradle.properties 変更が daemon に反映されない問題対策
OFFLINE_MODE="yes"
for arg in "$@"; do
    case "$arg" in
        offline)
            OFFLINE_MODE="yes"
            GRADLE_ARGS="runClient --offline -x downloadAssets"
            echo "=== Offline mode (using cached dependencies, skipping downloadAssets) ==="
            ;;
        online)
            OFFLINE_MODE="no"
            GRADLE_ARGS="runClient"
            echo "=== Online mode ==="
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
if [ "$OFFLINE_MODE" = "yes" ] && [ "$USE_TLS_WORKAROUND" = "auto" ]; then
    USE_TLS_WORKAROUND="no"
    echo "=== Offline mode: TLS 回線判定をスキップ ==="
elif [ "$USE_TLS_WORKAROUND" = "auto" ]; then
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
if [ "$OFFLINE_MODE" = "yes" ]; then
    # Gradle/Minecraftが認証・スキン・アセット取得を試みても、学校Wi-Fiへ出さず
    # localhost の閉じたポートで即時遮断する。Gradleは --offline なのでローカル依存のみ使う。
    export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Djava.net.useSystemProxies=false -Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=9 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=9 -Dhttp.nonProxyHosts=localhost\|127.*\|[::1]"
    echo "=== Offline network block ON (HTTP/HTTPS を localhost で遮断) ==="
elif [ "$USE_TLS_WORKAROUND" = "yes" ] && [ -f "$TLS_WORKAROUND_FILE" ]; then
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
    SYNC_ARGS=""
    case " $GRADLE_ARGS " in
        *" --offline "*) SYNC_ARGS="--offline" ;;
    esac
    # 大型依存 (TACZ 50MB、Mekanism一式) は必要な時だけ読み込めるようにする。
    # LIGHT_EXTERNAL_MODS=0 なら確認せず全部入り、=1 なら確認せず軽量版。
    if [ "${LIGHT_EXTERNAL_MODS:-}" = "0" ]; then
        USE_LIGHT_EXTERNAL_MODS="no"
    elif [ "${LIGHT_EXTERNAL_MODS:-}" = "1" ]; then
        USE_LIGHT_EXTERNAL_MODS="yes"
    elif [ -t 0 ]; then
        printf "ラグを減らす軽量モードにしますか? (TACZ/Mekanism系を除外) [Y/n]: "
        read LIGHT_ANSWER
        case "$LIGHT_ANSWER" in
            n|N|no|NO|No) USE_LIGHT_EXTERNAL_MODS="no" ;;
            *)             USE_LIGHT_EXTERNAL_MODS="yes" ;;
        esac
    else
        USE_LIGHT_EXTERNAL_MODS="yes"
    fi
    if [ "$USE_LIGHT_EXTERNAL_MODS" = "yes" ]; then
        SYNC_ARGS="$SYNC_ARGS --light"
    fi
    bash scripts/sync-selected-external-mods.sh $SYNC_ARGS
    GRADLE_ARGS="$GRADLE_ARGS -PwithExternalMods=true -PexternalModsGroup=runtime_selected"
    if [ "$USE_LIGHT_EXTERNAL_MODS" = "yes" ]; then
        echo "=== 外部 mod ON / 軽量モード ==="
    else
        echo "=== 外部 mod ON / 全部入り ==="
    fi
    if [ -d libs/runtime_selected ]; then
        found=0
        while IFS= read -r jar; do
            echo "  → $jar"
            found=$((found + 1))
        done < <(find libs/runtime_selected -type f -name "*.jar" 2>/dev/null | sort)
        if [ "$found" = "0" ]; then
            echo "  ! 外部 mod jar がありません"
        fi
    else
        echo "  ! libs/runtime_selected/ の生成に失敗しました"
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

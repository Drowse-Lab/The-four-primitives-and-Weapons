#!/bin/bash
# 指定した外部MODの最新JARを libs/runtime_selected へ集めるスクリプト。
#
# 使い方:
#   bash scripts/sync-selected-external-mods.sh                 全部入り
#   bash scripts/sync-selected-external-mods.sh --offline       全部入り・オフライン
#   bash scripts/sync-selected-external-mods.sh --light         軽量3MOD
#   bash scripts/sync-selected-external-mods.sh --offline --light
#
# --light で残すもの:
#   chuzume-addon / extra_video_settings / RPGish-HPDisplay
#
# --light で除外する重いもの:
#   gun_and_weapon / TACZ / Backpack Arsenal / Mekanism /
#   Sophisticated Core / Sophisticated Backpacks
#
# 任意で取り込むもの ( libs/external/ に jar を置いたときだけ ):
#   DuMmmMmmy ( ターゲットダミー ) と前提の Moonlight Lib。
#   属性ダメージの計測に使う。 公式 ( Modrinth / CurseForge ) から落とした
#   jar を libs/external/ に置くと、 軽量モードでも自動で取り込む。
#     https://modrinth.com/mod/mmmmmmmmmmmm
#     https://modrinth.com/mod/moonlight
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MODS_ROOT="/Users/hiromichi/Documents/github/mods"
DATAPACK_ROOT="/Users/hiromichi/Documents/github/datapack"
DEST="$ROOT/libs/runtime_selected"
# 自前ビルドではない、手で置く外部 mod jar ( DuMmmMmmy 等 ) の置き場
EXTERNAL_DIR="$ROOT/libs/external"
OFFLINE_ARG=""
LIGHT_MODE="no"

for arg in "$@"; do
    case "$arg" in
        --offline|-o|offline) OFFLINE_ARG="--offline" ;;
        --light|light) LIGHT_MODE="yes" ;;
    esac
done

mkdir -p "$DEST"
find "$DEST" -mindepth 1 -maxdepth 1 -exec rm -rf {} +

latest_jar() {
    find "$1" -maxdepth 3 -type f -name '*.jar' ! -name '*-sources.jar' ! -name '*-dev.jar' -print0 2>/dev/null \
        | xargs -0 ls -1t 2>/dev/null \
        | sed -n '1p'
}

install_jar() {
    local source_jar="$1"
    local base artifact version target_dir
    base="$(basename "$source_jar" .jar)"
    artifact="${base%%-[0-9]*}"
    version="${base#"$artifact"-}"
    if [ -z "$artifact" ] || [ "$version" = "$base" ]; then
        echo "[error] jar名からバージョンを判定できません: $source_jar" >&2
        exit 1
    fi
    target_dir="$DEST/$artifact/$version"
    mkdir -p "$target_dir"
    cp "$source_jar" "$target_dir/$artifact-$version.jar"
    echo "  + $artifact-$version.jar"
}

echo "=== 指定外部Modを同期 ==="

CHUZUME_JAR="$(latest_jar "$MODS_ROOT/chuzume-addon/build/libs")"
if [ -z "$CHUZUME_JAR" ] \
        || find "$MODS_ROOT/chuzume-addon/src" -type f -newer "$CHUZUME_JAR" -print -quit | grep -q .; then
    echo "==> Chuzume Addon のソース更新を検出: ビルド"
    (cd "$MODS_ROOT/chuzume-addon" && ./build.sh $OFFLINE_ARG)
    CHUZUME_JAR="$(latest_jar "$MODS_ROOT/chuzume-addon/build/libs")"
else
    echo "==> Chuzume Addon はビルド済みjarを使用"
fi

JARS=(
    "$(latest_jar "$MODS_ROOT/extra_video_settings/forge/forge/build/libs")"
    "$(latest_jar "$DATAPACK_ROOT/RPGish-HPDisplay/mod-forge/build/libs")"
    "$CHUZUME_JAR"
)

if [ "$LIGHT_MODE" = "yes" ]; then
    echo "==> 軽量モード: TACZ / Gun and Weapon / Backpack Arsenal / Mekanism / Sophisticated を除外"
else
    JARS+=(
        "$(latest_jar "$MODS_ROOT/gun_and_weapon/build/libs")"
        "$(latest_jar "$MODS_ROOT/Backpack-Arsenal/build/libs")"
        "$MODS_ROOT/gun_and_weapon/libs/local/tacz/1.20.1-1.1.7/tacz-1.20.1-1.1.7.jar"
        "$MODS_ROOT/Backpack-Arsenal/libs/local/mekanism/1.20.1-10.4.16.80/mekanism-1.20.1-10.4.16.80.jar"
        "$(latest_jar "/Users/hiromichi/.gradle/caches/modules-2/files-2.1/curse.maven/sophisticated-core-618298/8143884")"
        "$(latest_jar "/Users/hiromichi/.gradle/caches/modules-2/files-2.1/curse.maven/sophisticated-backpacks-422301/8136850")"
    )
fi

for jar in "${JARS[@]}"; do
    if [ -z "$jar" ] || [ ! -f "$jar" ]; then
        echo "[error] 必要な外部Modのjarが見つかりません" >&2
        exit 1
    fi
    install_jar "$jar"
done

# --- 任意: libs/external/ に置かれた jar ( DuMmmMmmy / Moonlight など ) ---
# 無ければ黙って飛ばす。 これらは自前ビルドではないので必須にしない。
if [ -d "$EXTERNAL_DIR" ]; then
    OPTIONAL_COUNT=0
    while IFS= read -r jar; do
        [ -f "$jar" ] || continue
        install_jar "$jar"
        OPTIONAL_COUNT=$((OPTIONAL_COUNT + 1))
    done < <(find "$EXTERNAL_DIR" -maxdepth 2 -type f -name '*.jar' \
                ! -name '*-sources.jar' ! -name '*-dev.jar' 2>/dev/null | sort)
    if [ "$OPTIONAL_COUNT" -gt 0 ]; then
        echo "==> libs/external/ から ${OPTIONAL_COUNT} 個を追加で取り込み"
    fi
else
    echo "==> libs/external/ は未作成 ( DuMmmMmmy を使うならここに jar を置く )"
fi

#!/bin/bash
# WSL から Windows 側の gradlew.bat を呼び出して Minecraft クライアントを起動する。
#
# 使い方:
#   bash run_client_win.sh            通常
#   bash run_client_win.sh offline    オフライン (キャッシュ済み依存のみ)
#
# 起動前に Iron's Spells 'n Spellbooks を入れるか対話で尋ねる (y/N)。
# 環境変数で上書き: WITH_SPELLBOOKS=1 強制 / SKIP_SPELLBOOKS_PROMPT=1 対話なし。

# Windows 側プロジェクトパス (必要に応じて編集)
PROJECT_WIN_PATH='C:\Users\hrmcn\MCreatorWorkspaces\the_four_primitives_and_weapons'

GRADLE_ARGS="runClient"
if [ "$1" = "offline" ]; then
    GRADLE_ARGS="$GRADLE_ARGS --offline -Dnet.minecraftforge.gradle.check.certs=false"
    echo "=== Offline mode (using cached dependencies) ==="
fi

# Iron's Spellbooks プロンプト (mac 版と同ロジック)
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
    echo "  (プロジェクトの libs/ironsspellbooks-1.20.1.jar もしくは run/mods/ に JAR を配置)"
fi

# cmd.exe 経由で gradlew.bat を呼び出す。引数は cd /d と && で連結。
# 二重引用の escape 問題を避けるため、全体を 1 文字列で渡す。
cmd.exe /c "cd /d $PROJECT_WIN_PATH && gradlew.bat $GRADLE_ARGS"

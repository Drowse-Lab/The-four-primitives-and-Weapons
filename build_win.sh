#!/bin/bash
# WSLからjarをビルドするスクリプト（Windows側のJavaを使用）
#
# 使い方:
#   bash build_win.sh          通常ビルド
#   bash build_win.sh clean    クリーンビルド

if [ "$1" = "clean" ]; then
    echo "=== Clean Build ==="
    cmd.exe /c "cd /d C:\Users\hrmcn\MCreatorWorkspaces\minecraft_armor_weapon && gradlew.bat clean build"
else
    echo "=== Build ==="
    cmd.exe /c "cd /d C:\Users\hrmcn\MCreatorWorkspaces\minecraft_armor_weapon && gradlew.bat build"
fi

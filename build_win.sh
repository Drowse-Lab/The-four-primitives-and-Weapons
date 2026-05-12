#!/bin/bash
# WSLからjarをビルドするスクリプト（Windows側のJavaを使用）
#
# 使い方:
#   bash build.sh          通常ビルド
#   bash build.sh clean    クリーンビルド

if [ "$1" = "clean" ]; then
    echo "=== Clean Build ==="
    cmd.exe /c "cd /d C:\Users\hrmcn\MCreatorWorkspaces\the_four_primitives_and_weapons && gradlew.bat clean build"
else
    echo "=== Build ==="
    cmd.exe /c "cd /d C:\Users\hrmcn\MCreatorWorkspaces\the_four_primitives_and_weapons && gradlew.bat build"
fi
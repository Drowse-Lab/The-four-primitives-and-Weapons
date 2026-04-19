#!/bin/bash
# Minecraftクライアントを起動するスクリプト（mac / WSL 両対応）

cd "$(dirname "$0")"

case "$(uname -s)" in
    Darwin|Linux)
        ./gradlew runClient
        ;;
    MINGW*|CYGWIN*|MSYS*)
        ./gradlew.bat runClient
        ;;
    *)
        ./gradlew runClient
        ;;
esac

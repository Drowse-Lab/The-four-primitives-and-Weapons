@echo off
echo TaCZ Mod付きでMinecraftを起動しています...
echo.
echo TaCZ jarファイルが compile-mods-1.19.2/ ディレクトリに配置されていることを確認してください。
echo.

REM TaCZファイルの存在確認
if exist "compile-mods-1.19.2\tacz-1.1.4.jar" (
    echo TaCZ mod found! ✓
) else (
    echo 警告: tacz-1.1.4.jar が compile-mods-1.19.2 ディレクトリに見つかりません！
    echo CurseForgeからダウンロードして配置してください。
    echo.
    pause
    exit /b 1
)

echo Gradleビルドを実行しています...
call gradlew.bat build

if %ERRORLEVEL% NEQ 0 (
    echo ビルドに失敗しました。
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo クライアントを起動しています...
call gradlew.bat runClient

pause
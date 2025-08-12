@echo off
title MCreator + TaCZ Test Environment Setup

echo ===========================================
echo MCreator Mod Test Environment with TaCZ
echo ===========================================
echo.

REM TaCZファイルの存在確認
if exist "compile-mods-1.19.2\tacz-1.1.4.jar" (
    echo [OK] TaCZ mod found: compile-mods-1.19.2\tacz-1.1.4.jar
) else (
    echo [ERROR] TaCZ mod not found!
    echo Please download TaCZ from: https://www.curseforge.com/minecraft/mc-mods/timeless-and-classics-zero/files/6069349
    echo And place it as: compile-mods-1.19.2\tacz-1.1.4.jar
    pause
    exit /b 1
)

REM Java環境の確認
echo.
echo Checking Java environment...
java -version
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Java not found! Please ensure Java is installed and in PATH.
    pause
    exit /b 1
) else (
    echo [OK] Java is available
)

REM gradlewの確認と実行権限設定（Unix環境用）
echo.
echo Setting up build environment...
if exist "gradlew" (
    wsl chmod +x gradlew 2>nul
    echo [OK] gradlew permissions set
) else (
    echo [WARNING] gradlew not found
)

REM ビルド実行
echo.
echo Starting build process...
echo This may take several minutes...
echo.

REM WSLでのビルド実行を試す
wsl bash -c "cd /mnt/c/Users/hrmcn/MCreatorWorkspaces/minecraft_armor_weapon && export PATH=\"/mnt/c/Program Files/Common Files/Oracle/Java/javapath:/mnt/c/Users/hrmcn/AppData/Local/Java/jdk-19.0.2/bin:\$PATH\" && ./gradlew build"

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Build failed! Trying alternative approach...
    echo.
    echo Attempting to run client directly...
    wsl bash -c "cd /mnt/c/Users/hrmcn/MCreatorWorkspaces/minecraft_armor_weapon && export PATH=\"/mnt/c/Program Files/Common Files/Oracle/Java/javapath:/mnt/c/Users/hrmcn/AppData/Local/Java/jdk-19.0.2/bin:\$PATH\" && ./gradlew runClient"
    
    if %ERRORLEVEL% NEQ 0 (
        echo [ERROR] Unable to run client. Please check Java installation and PATH.
        pause
        exit /b 1
    )
) else (
    echo.
    echo [OK] Build successful!
    echo.
    echo Starting Minecraft client with TaCZ...
    echo.
    wsl bash -c "cd /mnt/c/Users/hrmcn/MCreatorWorkspaces/minecraft_armor_weapon && export PATH=\"/mnt/c/Program Files/Common Files/Oracle/Java/javapath:/mnt/c/Users/hrmcn/AppData/Local/Java/jdk-19.0.2/bin:\$PATH\" && ./gradlew runClient"
)

echo.
echo Test completed!
pause
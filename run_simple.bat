@echo off
echo TaCZ Test Environment - Simple Version
echo ======================================
echo.

REM TaCZ確認
if exist "compile-mods-1.19.2\tacz-1.1.4.jar" (
    echo [OK] TaCZ found
) else (
    echo [ERROR] TaCZ not found
    pause
    exit /b 1
)

REM Java 8を使用してWSLでgradlew実行
echo Using Java 8 for Minecraft 1.19.2...
echo.

set "JAVA8_PATH=C:\Program Files (x86)\Common Files\Oracle\Java\java8path"
wsl bash -c "cd /mnt/c/Users/hrmcn/MCreatorWorkspaces/minecraft_armor_weapon && export JAVA_HOME='/mnt/c/Program Files (x86)/Common Files/Oracle/Java/java8path' && export PATH='/mnt/c/Program Files (x86)/Common Files/Oracle/Java/java8path:\$PATH' && ./gradlew --no-daemon runClient"

echo.
echo Finished!
pause
@echo off
echo Simple TaCZ Test Run
echo ===================
echo.

echo TaCZ Status:
if exist "compile-mods-1.19.2\tacz-1.1.4.jar" (
    echo [OK] TaCZ mod found
) else (
    echo [ERROR] TaCZ mod not found!
    exit /b 1
)

echo.
echo Java Version:
java -version
echo.

echo Starting Minecraft with TaCZ...
echo (This will download Gradle and dependencies on first run)
echo.

java -jar gradle\wrapper\gradle-wrapper.jar runClient --no-daemon

pause
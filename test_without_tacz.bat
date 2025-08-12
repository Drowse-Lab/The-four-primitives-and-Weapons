@echo off
echo Testing MCreator mod WITHOUT TaCZ
echo ==================================
echo.

echo TaCZ Status:
if exist "compile-mods-1.19.2\tacz-1.1.4.jar" (
    echo [WARNING] TaCZ mod found - should be disabled for this test
) else (
    echo [OK] TaCZ mod disabled for compatibility test
)

echo.
echo Java Version:
java -version
echo.

echo Starting Minecraft WITHOUT TaCZ...
echo This will test if the base mod works correctly.
echo.

java -jar gradle\wrapper\gradle-wrapper.jar runClient --no-daemon

pause
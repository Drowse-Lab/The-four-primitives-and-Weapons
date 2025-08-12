@echo off
echo Testing with TaCZ Regular Version
echo ==================================
echo.

echo TaCZ Status:
if exist "compile-mods-1.19.2\tacz-1.1.4.jar" (
    echo [OK] TaCZ regular version found
    for %%A in ("compile-mods-1.19.2\tacz-1.1.4.jar") do echo File size: %%~zA bytes
) else (
    echo [ERROR] TaCZ not found
    pause
    exit /b 1
)

echo.
echo Starting Minecraft with TaCZ regular version...
echo This will test FlyingAttackerEntity WITH TaCZ support.
echo.
echo Expected features:
echo - All basic mod functions (working from previous test)
echo - TaCZ bullet deflection (via reflection)
echo - TaCZ compatibility without Mixin errors
echo.
echo If Mixin errors occur, we'll try an older TaCZ version.
echo.

pause

"C:\Program Files\Pylo\MCreator\jdk\bin\java.exe" -jar gradle\wrapper\gradle-wrapper.jar runClient --no-daemon

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Failed to run with TaCZ regular version
    echo This likely indicates Mixin compatibility issues
    echo.
    echo Recommendations:
    echo 1. Use mod without TaCZ - all features work
    echo 2. Try TaCZ 1.0.3 (older, more stable)
    echo 3. Wait for TaCZ update compatible with your Forge version
    echo.
)

pause
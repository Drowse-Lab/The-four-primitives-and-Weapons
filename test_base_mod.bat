@echo off
echo Testing Base Minecraft Armor Weapon Mod
echo =======================================
echo.

echo Checking compile-mods directory:
dir compile-mods-1.19.2\
echo.

if exist "compile-mods-1.19.2\tacz*.jar" (
    echo [WARNING] TaCZ files found - this may cause compatibility issues
    echo Moving TaCZ files to backup...
    move "compile-mods-1.19.2\tacz*.jar" "compile-mods-1.19.2\backup_tacz.jar" 2>nul
) else (
    echo [OK] No TaCZ files found - testing base mod only
)

echo.
echo Starting base mod test...
echo This will verify FlyingAttackerEntity and other features work correctly.
echo.

REM Direct Gradle execution with proper Java
"C:\Program Files\Pylo\MCreator\jdk\bin\java.exe" -jar gradle\wrapper\gradle-wrapper.jar runClient --no-daemon

pause
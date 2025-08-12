@echo off
echo Testing Basic Minecraft Armor Weapon Mod
echo ========================================
echo.

echo Checking mod environment:
echo - compile-mods-1.19.2 directory: 
dir compile-mods-1.19.2\
echo.

if exist "compile-mods-1.19.2\*.jar" (
    echo [WARNING] JAR files found in compile-mods-1.19.2:
    dir compile-mods-1.19.2\*.jar
    echo Moving to backup...
    if not exist "backup\" mkdir backup
    move compile-mods-1.19.2\*.jar backup\ 2>nul
) else (
    echo [OK] No mod dependencies - testing base mod only
)

echo.
echo Starting Minecraft with base mod only...
echo This will test FlyingAttackerEntity without TaCZ.
echo.
echo Expected features:
echo - FlyingAttackerEntity spawning and behavior
echo - Vanilla arrow deflection
echo - Splash potion destruction
echo - Enchantment support
echo - Player attack targeting
echo.

pause

"C:\Program Files\Pylo\MCreator\jdk\bin\java.exe" -jar gradle\wrapper\gradle-wrapper.jar runClient --no-daemon --stacktrace

pause
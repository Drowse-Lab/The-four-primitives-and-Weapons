@echo off
REM 保護されたカスタムファイルを復元するスクリプト

echo ================================================
echo Protected Files Restoration
echo ================================================
echo.

set "PROTECTED_DIR=.protected_custom_files"

if not exist "%PROTECTED_DIR%" (
    echo Error: %PROTECTED_DIR% not found
    echo Please run lock_all.bat first
    pause
    exit /b 1
)

echo Restoring protected files...
echo.

REM TheFourPrimitivesAndWeaponsModModels.javaを復元
if exist "%PROTECTED_DIR%\TheFourPrimitivesAndWeaponsModModels.java" (
    copy /Y "%PROTECTED_DIR%\TheFourPrimitivesAndWeaponsModModels.java" "src\main\java\the_four_primitives_and_weapons\init\TheFourPrimitivesAndWeaponsModModels.java"
    echo [OK] TheFourPrimitivesAndWeaponsModModels.java restored
) else (
    echo [SKIP] TheFourPrimitivesAndWeaponsModModels.java not in protected files
)

REM その他のファイルも復元
for %%f in (%PROTECTED_DIR%\*.java) do (
    set "filename=%%~nxf"
    echo Processing: %%~nxf
)

echo.
echo ================================================
echo Restoration complete!
echo ================================================
echo.
echo You can now rebuild in MCreator
echo.
pause

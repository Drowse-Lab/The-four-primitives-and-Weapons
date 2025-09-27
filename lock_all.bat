@echo off
echo ================================================
echo MCreator Complete Lock System
echo ================================================
echo.

REM Python3またはPythonを探す
where python3 >nul 2>1
if %errorlevel%==0 (
    python3 auto_lock_all.py
) else (
    where python >nul 2>1
    if %errorlevel%==0 (
        python auto_lock_all.py
    ) else (
        echo Error: Python not found. Please install Python 3.
        pause
        exit /b 1
    )
)

echo.
echo ================================================
echo Process completed!
echo ================================================
echo.
echo Next steps:
echo 1. Open MCreator
echo 2. Go to Workspace - Workspace settings
echo 3. Check "Lock base mod element files"
echo 4. Click OK
echo.
pause
@echo off
REM Windows用バッチファイル - MCreatorのコードを自動ロック

echo ========================================
echo MCreator Code Auto-Lock Script
echo ========================================
echo.

REM Pythonスクリプトを実行
python auto_lock_codes.py

if %ERRORLEVEL% EQU 0 (
    echo.
    echo [成功] すべてのコードがロックされました
    echo MCreatorを安全に起動できます
) else (
    echo.
    echo [エラー] コードのロックに失敗しました
)

echo.
pause
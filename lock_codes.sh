#!/bin/bash
# Linux/Mac用スクリプト - MCreatorのコードを自動ロック

echo "========================================"
echo "MCreator Code Auto-Lock Script"
echo "========================================"
echo ""

# Pythonスクリプトを実行
python3 auto_lock_codes.py

if [ $? -eq 0 ]; then
    echo ""
    echo "[成功] すべてのコードがロックされました"
    echo "MCreatorを安全に起動できます"
else
    echo ""
    echo "[エラー] コードのロックに失敗しました"
fi

echo ""
read -p "Press Enter to continue..."
#!/usr/bin/env python3
"""
自動的にMCreatorファイルのlocked_codeをtrueに設定するスクリプト
MCreatorを開く前に実行することで、すべてのコードがロックされます
"""

import json
import sys
import os

def lock_all_codes(filepath="the_four_primitives_and_weapons.mcreator"):
    """すべてのmod要素のlocked_codeをtrueに設定"""
    
    if not os.path.exists(filepath):
        print(f"エラー: {filepath} が見つかりません")
        return False
    
    # バックアップを作成
    backup_path = filepath + ".backup_auto"
    
    try:
        # JSONファイルを読み込み
        with open(filepath, 'r', encoding='utf-8') as f:
            data = json.load(f)
        
        # バックアップを保存
        with open(backup_path, 'w', encoding='utf-8') as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
        
        # すべてのmod_elementsのlocked_codeをtrueに設定
        locked_count = 0
        for element in data.get('mod_elements', []):
            if 'locked_code' in element and not element['locked_code']:
                element['locked_code'] = True
                locked_count += 1
        
        # 変更を保存
        with open(filepath, 'w', encoding='utf-8') as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
        
        print(f"✓ {locked_count}個の要素のコードをロックしました")
        print(f"✓ バックアップを {backup_path} に保存しました")
        return True
        
    except json.JSONDecodeError as e:
        print(f"エラー: JSONの解析に失敗しました - {e}")
        return False
    except Exception as e:
        print(f"エラー: {e}")
        return False

if __name__ == "__main__":
    # コマンドライン引数でファイルパスを指定可能
    filepath = sys.argv[1] if len(sys.argv) > 1 else "the_four_primitives_and_weapons.mcreator"
    
    success = lock_all_codes(filepath)
    sys.exit(0 if success else 1)
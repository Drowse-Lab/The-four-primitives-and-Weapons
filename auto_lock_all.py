#!/usr/bin/env python3
"""
MCreatorのすべてのコードと設定を完全にロックするスクリプト
これによりMCreatorの自動再生成を完全に防ぎます
"""

import json
import sys
import os
import shutil
from datetime import datetime

def lock_all_elements(filepath="the_four_primitives_and_weapons.mcreator"):
    """すべてのmod要素のlocked_codeをtrueに設定"""

    if not os.path.exists(filepath):
        print(f"❌ エラー: {filepath} が見つかりません")
        return False

    # バックアップを作成
    backup_path = filepath + f".backup_{datetime.now().strftime('%Y%m%d_%H%M%S')}"

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
            if 'locked_code' not in element or not element['locked_code']:
                element['locked_code'] = True
                locked_count += 1

        # ワークスペース設定でベースファイルのロックも有効化
        if 'workspaceSettings' not in data:
            data['workspaceSettings'] = {}

        # ベースファイルロック設定を追加
        data['workspaceSettings']['lockBaseModFiles'] = True

        # 変更を保存
        with open(filepath, 'w', encoding='utf-8') as f:
            json.dump(data, f, indent=2, ensure_ascii=False)

        print(f"✅ {locked_count}個の要素のコードをロックしました")
        print(f"✅ ベースModファイルのロックを有効化しました")
        print(f"✅ バックアップを {backup_path} に保存しました")
        return True

    except json.JSONDecodeError as e:
        print(f"❌ エラー: JSONの解析に失敗しました - {e}")
        return False
    except Exception as e:
        print(f"❌ エラー: {e}")
        return False

def create_lock_marker_files():
    """ロックマーカーファイルを作成してMCreatorに通知"""

    lock_files = [
        "src/main/java/the_four_primitives_and_weapons/.mcreator_lock",
        "src/main/java/the_four_primitives_and_weapons/init/.mcreator_lock",
        "src/main/java/the_four_primitives_and_weapons/entity/.mcreator_lock",
        "src/main/java/the_four_primitives_and_weapons/client/init/.mcreator_lock",
        "src/main/java/the_four_primitives_and_weapons/client/renderer/.mcreator_lock"
    ]

    for lock_file in lock_files:
        os.makedirs(os.path.dirname(lock_file), exist_ok=True)
        with open(lock_file, 'w', encoding='utf-8') as f:
            f.write("# MCreator Lock File\n")
            f.write("# This file indicates that the directory contents should not be regenerated\n")
            f.write(f"# Locked at: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
        print(f"✅ ロックファイルを作成: {lock_file}")

def protect_custom_files():
    """カスタムファイルを保護"""

    custom_files = [
        # --- Rarity Forge システム ---
        "src/main/java/the_four_primitives_and_weapons/init/RarityForgeRegistration.java",
        "src/main/java/the_four_primitives_and_weapons/block/RarityForgeBlock.java",
        "src/main/java/the_four_primitives_and_weapons/block/entity/RarityForgeBlockEntity.java",
        "src/main/java/the_four_primitives_and_weapons/world/inventory/RarityForgeMenu.java",
        "src/main/java/the_four_primitives_and_weapons/client/gui/RarityForgeScreen.java",
        "src/main/java/the_four_primitives_and_weapons/item/rarity/WeaponRarity.java",
        "src/main/java/the_four_primitives_and_weapons/item/rarity/RarityCraftingLogic.java",
        "src/main/java/the_four_primitives_and_weapons/item/rarity/RarityForgeRecipe.java",
        "src/main/java/the_four_primitives_and_weapons/item/rarity/RarityForgeRecipes.java",
        "src/main/java/the_four_primitives_and_weapons/network/RarityForgeButtonMessage.java",
        "src/main/java/the_four_primitives_and_weapons/events/WeaponRarityEventHandler.java",
        "src/main/java/the_four_primitives_and_weapons/integration/RarityForgeRecipeCategory.java",
        "src/main/java/the_four_primitives_and_weapons/integration/RarityForgeJEIPlugin.java",
        # --- カスタムエンティティ ---
        "src/main/java/the_four_primitives_and_weapons/init/CustomEntityInit.java",
        "src/main/java/the_four_primitives_and_weapons/init/TheFourPrimitivesAndWeaponsModCustomEntities.java",
        "src/main/java/the_four_primitives_and_weapons/init/TheFourPrimitivesAndWeaponsModModels.java",
        "src/main/java/the_four_primitives_and_weapons/entity/DarkProjectileEntity.java",
        "src/main/java/the_four_primitives_and_weapons/entity/TornadoEntity.java",
        "src/main/java/the_four_primitives_and_weapons/entity/ai/CustomMeleeAttackGoal.java",
        "src/main/java/the_four_primitives_and_weapons/client/init/CustomEntityRenderers.java",
        "src/main/java/the_four_primitives_and_weapons/client/renderer/DarkProjectileRenderer.java",
        "src/main/java/the_four_primitives_and_weapons/client/model/Modelplayer_slim.java",
        # --- ユーティリティ・ネットワーク ---
        "src/main/java/the_four_primitives_and_weapons/util/DamageCalculator.java",
        "src/main/java/the_four_primitives_and_weapons/network/AttackPacket.java",
        "src/main/java/the_four_primitives_and_weapons/events/ChargedAttackHandler.java",
        # --- スキルシステム ---
        "src/main/java/the_four_primitives_and_weapons/skill/ElectricDischargeBurstSkill.java",
        "src/main/java/the_four_primitives_and_weapons/block/ElectricConductBlock.java",
        # --- その他カスタムコード ---
        "src/main/java/the_four_primitives_and_weapons/procedures/MagicKatanaSpecialChargeProcedure.java",
        "src/main/java/the_four_primitives_and_weapons/procedures/TyokutouThrustAttackProcedure.java",
        "src/main/java/the_four_primitives_and_weapons/procedures/SummonTriggerEffectEffectExpiresProcedure.java",
        "src/main/java/the_four_primitives_and_weapons/item/MagicalKatanaItem.java",
        "src/main/java/the_four_primitives_and_weapons/item/MagischesFeenKatanaItem.java",
        # --- メインModファイル ---
        "src/main/java/the_four_primitives_and_weapons/TheFourPrimitivesAndWeaponsMod.java",
    ]

    protected_dir = ".protected_custom_files"
    os.makedirs(protected_dir, exist_ok=True)

    for filepath in custom_files:
        if os.path.exists(filepath):
            backup_path = os.path.join(protected_dir, os.path.basename(filepath))
            shutil.copy2(filepath, backup_path)
            print(f"✅ カスタムファイルをバックアップ: {os.path.basename(filepath)}")

def add_to_gitignore():
    """MCreatorの自動生成を防ぐため.gitignoreに追加"""

    gitignore_entries = [
        "\n# MCreator Lock Files",
        "**/.mcreator_lock",
        ".protected_custom_files/",
        "*.backup_*",
        "\n# Prevent MCreator regeneration",
        "the_four_primitives_and_weapons.mcreator.lock"
    ]

    gitignore_path = ".gitignore"

    if os.path.exists(gitignore_path):
        with open(gitignore_path, 'r', encoding='utf-8') as f:
            content = f.read()

        for entry in gitignore_entries:
            if entry not in content:
                content += f"\n{entry}"

        with open(gitignore_path, 'w', encoding='utf-8') as f:
            f.write(content)

        print("✅ .gitignoreを更新しました")

def main():
    print("=" * 60)
    print("MCreator 完全ロックシステム")
    print("=" * 60)

    # 1. MCreatorファイルのロック
    print("\n1. MCreator要素をロック中...")
    success = lock_all_elements()

    if not success:
        print("❌ ロックに失敗しました")
        return False

    # 2. ロックマーカーファイルの作成
    print("\n2. ロックマーカーファイルを作成中...")
    create_lock_marker_files()

    # 3. カスタムファイルの保護
    print("\n3. カスタムファイルを保護中...")
    protect_custom_files()

    # 4. .gitignoreの更新
    print("\n4. .gitignoreを更新中...")
    add_to_gitignore()

    print("\n" + "=" * 60)
    print("✅ すべての処理が完了しました！")
    print("\n重要な注意事項:")
    print("1. MCreatorを開く際、「Workspace → Workspace settings」で")
    print("   「Lock base mod element files」にチェックを入れてください")
    print("2. カスタムファイルのバックアップは .protected_custom_files/ に保存されています")
    print("3. MCreatorがファイルを上書きした場合、バックアップから復元できます")
    print("=" * 60)

    return True

if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1)
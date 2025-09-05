# MCreator コード自動再生成の無効化方法

このプロジェクトでは、MCreatorによるコードの自動再生成を防ぐための複数の対策を実装しています。

## 実装済みの対策

### 1. 既存コードのロック (完了)
- `minecraft_armor_weapon.mcreator`内のすべての`locked_code`フラグを`true`に設定
- バックアップファイル: `minecraft_armor_weapon.mcreator.backup`

### 2. 自動ロックシステム

#### a) Gradleタスク (`lock_all_codes.gradle`)
- ビルド実行前に自動的にすべてのコードをロック
- `build.gradle`に統合済み

#### b) Pythonスクリプト (`auto_lock_codes.py`)
- MCreatorファイルのすべての要素を自動的にロック
- JSONフォーマットを保持しながら安全に更新

#### c) 実行用スクリプト
- **Windows**: `lock_codes.bat`を実行
- **Linux/Mac**: `./lock_codes.sh`を実行

## 使用方法

### MCreatorを開く前に（推奨）
```bash
# Windowsの場合
lock_codes.bat

# Linux/Macの場合
./lock_codes.sh
```

### ビルド時（自動）
Gradleビルドを実行すると、自動的にコードロックが適用されます：
```bash
./gradlew build
```

## 新しい要素を追加した後

1. MCreatorで新しい要素を作成
2. MCreatorを閉じる
3. `lock_codes.bat`（Windows）または`./lock_codes.sh`（Linux/Mac）を実行
4. MCreatorを再度開く

これにより、新しく追加した要素もコード再生成から保護されます。

## 元に戻す方法

コードロックを解除したい場合：
```bash
# バックアップから復元
cp minecraft_armor_weapon.mcreator.backup minecraft_armor_weapon.mcreator
```

## 注意事項

- MCreatorのGUI上では「Regenerate code and build」ボタンが表示されますが、コードがロックされているため実際には再生成されません
- 新しい要素を追加するたびにロックスクリプトを実行することをお勧めします
- バックアップファイルは定期的に作成されます（`.backup_auto`拡張子）
# TaCZ -all版を動作させる方法

## 方法1: 実行時のみ読み込み（推奨）

### 手順:
1. **ビルド時**: `run/mods/`を空にする
2. **ビルド完了後**: TaCZを配置
   ```bash
   cp backup-mods/tacz-1.19.2-1.1.4-hotfix-all.jar run/mods/
   ```
3. **MCreatorから直接実行しない**
4. **代わりにコマンドラインで実行**:
   ```bash
   cd run
   java -Xmx4G -Xms4G \
        -Dmixin.env.disableRefMap=true \
        -Dfml.earlyprogresswindow=false \
        -jar ../build/libs/[生成されたjar名].jar
   ```

## 方法2: Mixin設定を調整

`run/config/`に`mixins.properties`を作成:
```properties
mixin.checks.interfaces=false
mixin.debug.export=false
mixin.hotSwap=false
```

## 方法3: 起動引数を追加

MCreatorの設定で以下を追加:
- JVM引数: `-Dmixin.env.disableRefMap=true`
- プログラム引数: `--mixin.config=minecraftarmorweapon.mixins.json`

## 方法4: TaCZ設定ファイルを編集

`run/config/tacz/`フォルダを作成し、`tacz.toml`を配置:
```toml
[client]
  #Disable conflicting mixins
  disable_mixins = true
```

## 現実的な解決策

**開発フロー:**
1. 開発・ビルド → TaCZなし
2. 最終テスト → TaCZを`run/mods/`に配置
3. 配布版作成 → TaCZは別途ダウンロード推奨

これらの方法で`-all`版でも動作する可能性があります。
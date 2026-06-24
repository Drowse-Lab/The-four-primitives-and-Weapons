# 開発用クライアント起動ガイド

開発用 Minecraft クライアント (`runClient`) を起動するためのスクリプトと、各種フラグ / 環境変数 / トラブルシュート手順をまとめます。

## スクリプト

| 環境 | スクリプト |
|---|---|
| macOS / Linux / WSL / Git Bash | `bash run_client_mac.sh` |
| Windows ( cmd / PowerShell ) | `run_client_windows.bat` |

両方とも内部処理は等価です。引数も共通。

## 引数

複数併用可 ( 順不同 )。

| 引数 | 意味 |
|---|---|
| ( なし ) | 通常: オンライン + TLS workaround on + daemon stop |
| `offline` | `--offline -x downloadAssets` 付き ( キャッシュのみで起動 ) |
| `notls` / `no-tls` | TLS workaround を切る ( テザリング等で素の TLS を使う ) |
| `keepdaemon` / `keep-daemon` | gradle daemon を `--stop` で kill しない ( 2 回目以降の高速起動用 ) |

例:
```bash
# macOS / Linux
bash run_client_mac.sh offline
bash run_client_mac.sh notls
bash run_client_mac.sh offline notls
bash run_client_mac.sh offline keepdaemon

:: Windows
run_client_windows.bat offline
run_client_windows.bat notls
run_client_windows.bat offline keepdaemon
```

## 起動フロー

```
1. 引数パース
2. gradle daemon を --stop ( keepdaemon が無ければ )
   → JAVA_TOOL_OPTIONS / gradle.properties の変更を daemon に確実反映
3. TLS workaround を環境変数 JAVA_TOOL_OPTIONS に設定 ( notls 指定時はスキップ )
4. 外部 mod 取り込みを対話で確認 ( y → -PwithExternalMods=true を gradle に渡す )
5. gradlew runClient を起動
```

## 外部 mod ( libs/local/ )

任意の `.jar` を `libs/local/` 配下に置くと、 起動時に自動取り込みされます。

- フラット配置 ( `libs/local/foo-1.2.3.jar` ) でも
- Maven 階層 ( `libs/local/foo/1.2.3/foo-1.2.3.jar` ) でも OK

スクリプトの起動時にプロンプトが出るので、 `y` を入力すると `-PwithExternalMods=true` が gradle に渡され、 `build.gradle` 側で:

1. ファイル名から artifact / version を自動推定
2. Maven 階層に自動配置 ( フラット配置の場合 )
3. `fg.deobf("local:<artifact>:<version>")` で取り込み ( SRG → official のリマップが効く )

mod 名 / version を build.gradle に書き込む必要は無し。

## 環境変数 ( CI / 非対話用 )

| 環境変数 | 効果 |
|---|---|
| `WITH_EXTERNAL_MODS=1` | プロンプトを `y` 扱いにする |
| `SKIP_EXTERNAL_MODS_PROMPT=1` | プロンプトを `N` 扱いにする |
| ( 後方互換 ) `WITH_SPELLBOOKS=1` | 同上 ( WITH_EXTERNAL_MODS と等価 ) |
| ( 後方互換 ) `SKIP_SPELLBOOKS_PROMPT=1` | 同上 ( SKIP_EXTERNAL_MODS_PROMPT と等価 ) |

## TLS workaround について

開発端末の ISP ( マンション ) が Cisco Umbrella の透過 SSL 検査を経由しており、 中間 Proxy が古くて TLS 1.3 と ECDHE / DHE 系 cipher を理解できないことがあります。 そのままだと `maven.minecraftforge.net` への接続が handshake_failure で落ちるため、 `JAVA_TOOL_OPTIONS` で以下を強制しています:

- TLS 1.2 のみ ( `-Djdk.tls.client.protocols=TLSv1.2` )
- RSA 系 cipher のみ ( `-Djdk.tls.client.cipherSuites=...` )
- 該当 cipher は JDK 17 でデフォルト無効化されているので `tls_workaround.properties` で再有効化

逆にテザリング等 で `maven.minecraftforge.net` の現代 TLS を直接叩ける場合は、 RSA-only 制約が邪魔になります。 その時は `notls` 引数を指定して workaround を切ってください。

## キャッシュ場所 ( オフライン起動の前提 )

すべて gradle のキャッシュに乗っていれば `offline` で起動できます。

- Forge userdev jar: `~/.gradle/caches/modules-2/files-2.1/net.minecraftforge/forge/<version>/<hash>/`
- mappings / patches: `~/.gradle/caches/forge_gradle/`
- deobf 済み依存 ( Curios / JEI 等 ): `~/.gradle/caches/forge_gradle/deobf_dependencies/`
- libs/local の deobf 結果: 上と同じ場所にキャッシュされる

一度オンラインで `runClient` を完走すれば、 上記キャッシュが全部揃って以後 `offline` で動きます。

## オフライン環境で何を準備すれば良いか

1. **Forge userdev** を一度オンラインで取得 ( gradle が自動 DL )
2. **libs/local/** に使いたい外部 mod jar を全部入れる
3. ⇒ あとは `bash run_client_mac.sh offline` ( または `run_client_windows.bat offline` ) でずっと オフライン起動可能

## トラブルシュート

### `handshake_failure` で落ちる
TLS workaround の cipher と Forge maven が噛み合っていない。 テザリングなど別回線で:
```bash
bash run_client_mac.sh notls
```

### `Cannot deobfuscate dependency of type DefaultSelfResolvingDependency_Decorated`
旧版の `fg.deobf(files(...))` が混ざっている。 現行 build.gradle は Maven 階層 + `fg.deobf("local:...")` で取り込むので発生しないはず。 もし出たら build.gradle が古い可能性。

### 「変更したフラグ ( notls 等 ) が効いていない」
gradle daemon に古い JAVA_TOOL_OPTIONS が残っている。 デフォルトでスクリプトが `--stop` を打つので通常は発生しないが、 `keepdaemon` を付けて起動した場合は次回 `keepdaemon` 抜きで起動するか、 手動で `./gradlew --stop` を打つ。

### `ResourceLocation.fromNamespaceAndPath` で NoSuchMethodError
外部 mod が Forge 47.4.0 以降の API を使っている。 `FORGE_VERSION` を 1.20.1-47.4.0 に bump 必要 ( `gradle.properties` 参照 )。

### `Mod ID 'irons_spellbooks' requires forge [47.4.0,)`
同上。 Forge のバージョンを bump して再キャッシュ。

### Spellbooks を使いたいが Forge 47.4.0 を引けない
- テザリングで `bash run_client_mac.sh notls` を一度実行して Forge 47.4.0 をキャッシュ
- もしくは Spellbooks の旧版 ( 47.1.0 互換 ) を CurseForge から DL して libs/local/ に差し替え

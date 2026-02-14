# Copilot 指示（リポジトリ固有）

このファイルは、このリポジトリ（The-four-primitives-and-Weapons）でAI支援エージェントが素早く生産的になるための要点をまとめたランブックです。短く具体的に、探索と変更で役立つ情報を中心に記載します。

---

## 概要（大局観）
- このプロジェクトは Minecraft Forge 用のモッドで、MCreator から生成された要素群（`elements/` の多くの `*.mod.json`）とカスタムコード（`src/`、`models/`、`resources/`）で構成されています。
- Mod ID / アーカイブ名: `minecraft_armor_weapon`（`build.gradle` の `archivesBaseName` を参照）。パッケージ群は `com.hrmcngs.minecraft_armor_weapon` です。
- ビルドは ForgeGradle を使用した Gradle ベース（`build.gradle` と `mcreator.gradle` を参照）。Java 17 をターゲットにしています。

## 重要ファイル／ディレクトリ（すぐ見る場所）
- [build.gradle](build.gradle): Forge の設定、`runs`（`runClient` / `runServer`）と `mappings` の指定。
- [mcreator.gradle](mcreator.gradle): MCreator 関連のビルドスクリプト（自動生成や変換処理を含む）。
- [elements/](elements/): MCreator が生成する `*.mod.json` 要素が多数ある場所。新しいアイテム・ブロック・エフェクトはまずここに表現される。
- `src/`: カスタム Java ソース。MCreator 生成物と手書きコードが混在するため、クラス命名やパッケージを壊さないよう注意。
- `src/main/resources/META-INF/accesstransformer.cfg`: アクセストランスフォーマー設定（`build.gradle` で参照）。

## 開発ワークフロー（コマンド）
- 依存解決とビルド（ヘッドレス）:
  - `./gradlew build`
- クライアントをローカルで起動（実行時リソースが `run/` に生成される）:
  - `./gradlew runClient`
- サーバーをローカルで起動:
  - `./gradlew runServer`
- 注意: Gradle タスクは `build.gradle` の `runs` ブロックで `workingDirectory project.file('run')` を使うため、実行後に `run/` 配下を確認してください。

## コード変更時の注意点（プロジェクト特有の規約）
- MCreator による自動生成ファイルと手書きコードが混在します。`elements/` の JSON を直接編集すると MCreator 側で上書きされる可能性があるため、意図する変更先を確認してください。
- パッケージ名や `archivesBaseName` を変えるとリソースの解決が壊れる場合があります。既存の命名を維持すること。
- リソース（テクスチャ・モデル）は `src/main/resources` や `models/` に配置されています。リソースパスが正しいかをビルド前に確認してください。

## 典型的な変更パターン／例（素早く理解するための参照）
- 新しいアイテムを追加する場合:
  1. `elements/` にある該当する `*.mod.json` を確認（既存のアイテム定義をコピーすることが多い）。
  2. 必要なら `src/` にカスタムクラスを追加し、MCreator の生成方式に合わせて登録する。
  3. `./gradlew build` → `./gradlew runClient` で動作確認。
- アクセス修正が必要な場合: [src/main/resources/META-INF/accesstransformer.cfg](src/main/resources/META-INF/accesstransformer.cfg) を参照。

## テストとデバッグ
- 自動テストは特に用意されていません。動作確認は `runClient` / `runServer` を用いた実機起動が基本です。
- ログレベルやデバッグ出力は `build.gradle` の `runs` セクションで `forge.logging.console.level` を `debug` に設定しています。追加ログは `LOGGER` を使って出力してください。

## 依存と外部連携
- Forge: `net.minecraftforge:forge:1.20.1-47.1.0`（`build.gradle` の dependencies を参照）。
- MCreator: プロジェクトは MCreator ベースで、`minecraft_armor_weapon.mcreator` 等のメタファイルを含みます。MCreator を使う場合はエクスポートや同期の仕組みに注意。

## 変更を行う際のチェックリスト（簡潔）
 - ビルド: `./gradlew build` が通ること
 - 実機起動: `./gradlew runClient` でクラッシュしないこと
 - `elements/` と `src/` の責務を分けること（自動生成 vs 手作業）
 - リソースパスと `accesstransformer` の整合性を確認

---

フィードバック: ここに書かれている箇所で不明点や追加したいルールはありますか？明確化すべき箇所を教えてください。次はテスト手順や典型的な変更の具体例（ファイル/行番号リンク付き）を追加できます。

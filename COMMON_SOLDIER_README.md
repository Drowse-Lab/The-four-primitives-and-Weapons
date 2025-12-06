# 一般兵（Common Soldier）Mobの使い方

## 概要

**一般兵**は、プレイヤーのような動作をするティア1のAI Mobです。プレイヤーと同じように回避、チャージ攻撃、武器スキルを使用できます。

## 特徴

### 基本ステータス
- **HP**: 40 (プレイヤーの2倍)
- **攻撃力**: 5
- **防御力**: 4 (鉄防具相当)
- **移動速度**: 0.28 (やや遅い)
- **ノックバック耐性**: 20%

### AI能力（ティア1）
- **回避成功率**: 30%
- **戦術認識度**: 30%
- **チャージ率**: 50-80%
- **回避クールダウン**: 2秒
- **チャージ攻撃クールダウン**: 2秒
- **武器スキルクールダウン**: 3秒

### プレイヤーのような動作
1. **回避（右クリック相当）**
   - 敵が2ブロック以内に近づくと30%の確率で回避
   - 回避時に煙エフェクトとサウンド
   - 1.5秒間落下ダメージ無効

2. **チャージ攻撃（左クリック長押し相当）**
   - 敵が3～7ブロックの距離にいると自動的にチャージ
   - 電撃エフェクトで直線上の敵全てにダメージ
   - チャージ率に応じたダメージ倍率

3. **武器スキル（右クリック相当）**
   - 敵が5ブロック以内にいると防御スキルを使用
   - GUARDエフェクトを5秒間付与
   - エンチャントエフェクトとサウンド

### 装備
- **武器**: 鉄の刀（IRON_KATANA）
- 武器は落とさない設定

## 使い方

### 1. スポーンエッグで召喚

クリエイティブモードの「その他」タブから「一般兵のスポーンエッグ」を入手して使用します。

### 2. コマンドで召喚

```
/summon minecraft_armor_weapon:common_soldier ~ ~ ~
```

### 3. コードで召喚

```java
import minecraftarmorweapon.entity.CommonSoldierEntity;
import minecraftarmorweapon.init.MinecraftArmorWeaponModEntities;

// エンティティタイプから作成
CommonSoldierEntity soldier = MinecraftArmorWeaponModEntities.COMMON_SOLDIER.get().create(level);
soldier.moveTo(x, y, z, 0, 0);
level.addFreshEntity(soldier);
```

## 戦闘の様子

一般兵は次のような戦闘スタイルを取ります：

1. **敵を発見** → 32ブロック以内のプレイヤーを検知
2. **接近** → 敵に向かって移動（速度: 0.3）
3. **回避判定** → 敵が近づくと回避を試みる
4. **チャージ攻撃** → 適切な距離でチャージ攻撃を準備
5. **通常攻撃** → 3ブロック以内で近接攻撃
6. **撤退** → HP30%以下で逃走

## 技術情報

### ファイル構成
```
src/main/java/minecraftarmorweapon/
├── entity/
│   └── CommonSoldierEntity.java         # エンティティ本体
├── client/renderer/
│   └── CommonSoldierRenderer.java       # レンダラー（プレイヤーモデル）
├── ai/
│   ├── PlayerLikeAIGoal.java           # AI動作
│   └── ALifeAIBridge.java              # AI判断
└── init/
    ├── MinecraftArmorWeaponModEntities.java    # エンティティ登録
    ├── MinecraftArmorWeaponModEntityRenderers.java  # レンダラー登録
    └── MinecraftArmorWeaponModItems.java       # スポーンエッグ登録
```

### カスタマイズ

ステータスをカスタマイズする場合は、`CommonSoldierEntity.java`の`createAttributes()`メソッドを編集します：

```java
public static AttributeSupplier.Builder createAttributes() {
    return Monster.createMonsterAttributes()
        .add(Attributes.MAX_HEALTH, 40.0)    // HP
        .add(Attributes.MOVEMENT_SPEED, 0.28) // 移動速度
        .add(Attributes.ATTACK_DAMAGE, 5.0)   // 攻撃力
        .add(Attributes.ARMOR, 4.0);          // 防御力
}
```

AIの挙動をカスタマイズする場合は、`src/main/python/ai/CommonSoldier.py`を編集します（現在はJavaで直接実装）。

## ビルドと実行

### 1. プロジェクトをビルド

```bash
./gradlew build
```

### 2. Minecraftを起動

MCreatorから「クライアントを実行」を選択するか、手動でMinecraftを起動します。

### 3. 動作確認

1. クリエイティブモードで「一般兵のスポーンエッグ」を取得
2. 地面に設置して召喚
3. サバイバルモードに切り替えて戦闘

## トラブルシューティング

### エンティティが表示されない
- ビルドが正常に完了しているか確認
- レンダラーが正しく登録されているか確認
- MCreatorのワークスペースを再読み込み

### AIが動作しない
- `PlayerLikeAIGoal`が正しく登録されているか確認
- ティアが正しく設定されているか確認（ティア1）
- コンソールログでエラーを確認

### 回避しない
- 敵が2ブロック以内にいるか確認
- クールダウン（2秒）が経過しているか確認
- 回避成功率は30%（低ティア）

### チャージ攻撃しない
- 敵が3～7ブロックの距離にいるか確認
- クールダウン（2秒）が経過しているか確認

## 今後の拡張

さらに強力なティアのMobを追加できます：

- **ティア2**: エリート兵（回避率50%）
- **ティア3**: 特異点（回避率70%）
- **ティア4**: 英雄級（回避率80%）
- **ティア5**: 神話級（回避率90%）
- **ティア6**: 天使級（回避率95%）
- **ティア7**: 神聖級（回避率99%）

`CommonSoldierEntity.java`をコピーして、`AI_TIER`を変更するだけで別のティアのMobを作成できます。

## 参考資料

- `PLAYER_LIKE_AI_README.md` - AI システムの詳細
- `ExamplePlayerLikeMob.java` - 実装例
- `src/main/python/ai/` - Python AIファイル（将来の拡張用）

---

**作成日**: 2025年10月14日
**Minecraft バージョン**: 1.19.2
**Forge バージョン**: 43.x.x

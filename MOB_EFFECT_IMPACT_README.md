# Mobへのステータス効果影響システム

## 概要

このシステムは、**すべてのMob**に盲目などのステータス効果が付与されたときに、自動的に影響を与えます。プレイヤーには影響しません。

## サポートされるステータス効果

### 1. 盲目（Blindness）

**影響:**
- 視界範囲を70%縮小（元の30%にする）
- 移動速度を30%低下
- 攻撃力を2低下

**AI動作への影響:**
- 回避が70%の確率で失敗
- チャージ攻撃が80%の確率で失敗
- 武器スキルが60%の確率で失敗

**コマンド例:**
```
/effect give @e[type=minecraft:zombie] minecraft:blindness 30 0
```

### 2. 混乱（Nausea）

**影響:**
- 移動速度を20%低下

**AI動作への影響:**
- 回避方向がランダムになる（どこに逃げるか分からない）
- チャージ攻撃の方向が±45度ランダムにずれる
- 武器スキルが40%の確率で失敗

**コマンド例:**
```
/effect give @e[type=minecraft:skeleton] minecraft:nausea 30 0
```

### 3. 弱体化（Weakness）

**影響:**
- 攻撃力をさらに4低下（バニラの効果に追加）

**コマンド例:**
```
/effect give @e[type=minecraft_armor_weapon:common_soldier] minecraft:weakness 30 0
```

### 4. 暗闇（Darkness）

**影響:**
- 視界範囲を50%縮小
- 移動速度を15%低下

**AI動作への影響:**
- チャージ攻撃の範囲が50%減少

**コマンド例:**
```
/effect give @e[type=minecraft:creeper] minecraft:darkness 30 0
```

### 5. 発光（Glowing）

**影響:**
- 敵に発見されやすくなる（視覚的効果のみ）

**コマンド例:**
```
/effect give @e[type=minecraft:zombie] minecraft:glowing 30 0
```

## 技術情報

### ファイル構成

```
src/main/java/minecraftarmorweapon/
├── events/
│   └── MobEffectEventHandler.java     # ステータス効果イベントハンドラー
└── ai/
    └── PlayerLikeAIGoal.java          # AI動作（効果に応じた制限を含む）
```

### イベント処理

#### 効果が追加されたとき
```java
@SubscribeEvent
public static void onMobEffectAdded(MobEffectEvent.Added event) {
    // 効果の種類を判定
    if (effectInstance.getEffect() == MobEffects.BLINDNESS) {
        applyBlindnessEffect(mob, effectInstance);
    }
    // ...
}
```

#### 効果が切れたとき
```java
@SubscribeEvent
public static void onMobEffectRemoved(MobEffectEvent.Remove event) {
    // 元の状態に戻す
    if (event.getEffect() == MobEffects.BLINDNESS) {
        removeBlindnessEffect(mob);
    }
    // ...
}
```

### 属性修正

各効果は固有のUUIDを持つ`AttributeModifier`を使用して、Mobの属性を動的に変更します：

```java
AttributeModifier modifier = new AttributeModifier(
    BLINDNESS_SPEED_UUID,
    "blindness_movement_speed",
    -0.3,  // 30%減少
    AttributeModifier.Operation.MULTIPLY_TOTAL
);
movementSpeed.addTransientModifier(modifier);
```

効果が切れたときは、同じUUIDを使用して修正を削除します：

```java
movementSpeed.removeModifier(BLINDNESS_SPEED_UUID);
```

## 使い方

### 1. 特定のMobに効果を付与

```
# ゾンビに盲目を付与（30秒）
/effect give @e[type=minecraft:zombie,limit=1] minecraft:blindness 30 0

# スケルトンに混乱を付与（20秒）
/effect give @e[type=minecraft:skeleton,limit=1] minecraft:nausea 20 0

# 一般兵に弱体化を付与（60秒）
/effect give @e[type=minecraft_armor_weapon:common_soldier] minecraft:weakness 60 0
```

### 2. 範囲内の全てのMobに効果を付与

```
# 10ブロック以内の全Mobに盲目を付与
/effect give @e[type=!minecraft:player,distance=..10] minecraft:blindness 30 0

# 近くの敵対Mobに暗闇を付与
/effect give @e[type=#minecraft:hostile,distance=..20] minecraft:darkness 30 0
```

### 3. 効果の解除

```
# 特定のMobから盲目を解除
/effect clear @e[type=minecraft:zombie,limit=1] minecraft:blindness

# 全Mobから全ての効果を解除
/effect clear @e[type=!minecraft:player]
```

## 動作確認

### テスト手順

1. **一般兵を召喚**
   ```
   /summon minecraft_armor_weapon:common_soldier ~ ~ ~
   ```

2. **盲目を付与**
   ```
   /effect give @e[type=minecraft_armor_weapon:common_soldier,limit=1] minecraft:blindness 30 0
   ```

3. **動作確認**
   - 一般兵の移動速度が遅くなる
   - 視界範囲が狭くなり、遠くのプレイヤーを検知できない
   - 攻撃力が低下する
   - 回避、チャージ攻撃、スキルがほぼ使えなくなる

4. **混乱を付与**
   ```
   /effect give @e[type=minecraft_armor_weapon:common_soldier,limit=1] minecraft:nausea 30 0
   ```

5. **動作確認**
   - 回避方向がランダムになる
   - チャージ攻撃の方向がずれる
   - スキルの成功率が低下

## 効果の組み合わせ

複数の効果を同時に付与すると、それぞれの影響が重複します：

```
# 盲目+混乱+弱体化を同時に付与
/effect give @e[type=minecraft:zombie,limit=1] minecraft:blindness 30 0
/effect give @e[type=minecraft:zombie,limit=1] minecraft:nausea 30 0
/effect give @e[type=minecraft:zombie,limit=1] minecraft:weakness 30 0
```

**結果:**
- 視界範囲が70%減少（盲目）
- 移動速度が30%+20%=50%減少（盲目+混乱）
- 攻撃力が2+4=6低下（盲目+弱体化）
- AI動作がほぼ不能になる

## カスタマイズ

### 効果の強度を変更

`MobEffectEventHandler.java`で各効果の影響度を調整できます：

```java
// 盲目の視界減少を50%に変更
AttributeModifier modifier = new AttributeModifier(
    BLINDNESS_ATTACK_UUID,
    "blindness_follow_range",
    -0.5,  // -0.7から-0.5に変更
    AttributeModifier.Operation.MULTIPLY_TOTAL
);
```

### AI動作の制限を変更

`PlayerLikeAIGoal.java`で失敗率を調整できます：

```java
// 盲目時の回避失敗率を50%に変更
if (entity.hasEffect(MobEffects.BLINDNESS)) {
    if (random.nextDouble() < 0.5) {  // 0.7から0.5に変更
        return;  // 回避失敗
    }
}
```

### 新しい効果への対応を追加

1. **MobEffectEventHandler.javaに処理を追加:**

```java
@SubscribeEvent
public static void onMobEffectAdded(MobEffectEvent.Added event) {
    // ...既存のコード...

    // 新しい効果への対応
    else if (effectInstance.getEffect() == MobEffects.POISON) {
        applyPoisonEffect(mob, effectInstance);
    }
}

private static void applyPoisonEffect(Mob mob, MobEffectInstance effect) {
    // 毒効果時の処理
    System.out.println("[MobEffect] " + mob.getName().getString() +
                      " に毒効果を適用しました");
}
```

2. **PlayerLikeAIGoal.javaにAI制限を追加:**

```java
private void executeDodge(ALifeAIBridge.AIAction action) {
    // 毒効果時：回避速度が低下
    double speedMultiplier = 1.0;
    if (entity.hasEffect(MobEffects.POISON)) {
        speedMultiplier = 0.7;
    }

    Vec3 dodgeVec = dodgeDirection.scale(action.speed * speedMultiplier);
    // ...
}
```

## トラブルシューティング

### 効果が適用されない

- イベントハンドラーが登録されているか確認
- サーバー側でのみ処理されるため、シングルプレイヤーで確認
- コンソールログで`[MobEffect]`のメッセージを確認

### 効果が解除されない

- `MobEffectEvent.Remove`イベントが正しく処理されているか確認
- 属性修正のUUIDが一致しているか確認

### Mobの動作がおかしい

- 効果の強度が強すぎないか確認（移動速度-100%など）
- 複数の効果が重複していないか確認
- コンソールログでエラーを確認

## パフォーマンス

- 属性修正は軽量な操作です
- `originalFollowRanges`マップは1000エントリーを超えると自動的にクリアされます
- 効果が切れると自動的に元の状態に戻ります

## 互換性

- **Minecraft**: 1.19.2
- **Forge**: 43.x.x
- **他のMod**: バニラのステータス効果システムを使用しているため、他のModと互換性があります

## 今後の拡張

追加できる効果：
- **毒（Poison）**: HP回復速度低下、移動速度低下
- **ウィザー（Wither）**: 攻撃力低下、防御力低下
- **採掘速度低下（Mining Fatigue）**: 攻撃速度低下
- **空腹（Hunger）**: スタミナ減少（カスタム実装）
- **浮遊（Levitation）**: AI動作の混乱
- **鈍化（Slowness）**: 移動速度低下（バニラで既に実装済み）

## 参考資料

- `PLAYER_LIKE_AI_README.md` - プレイヤーのようなAIシステムの詳細
- `COMMON_SOLDIER_README.md` - 一般兵Mobの使い方
- Minecraft Wiki - [Status Effects](https://minecraft.fandom.com/wiki/Effect)

---

**作成日**: 2025年10月14日
**Minecraft バージョン**: 1.19.2
**Forge バージョン**: 43.x.x

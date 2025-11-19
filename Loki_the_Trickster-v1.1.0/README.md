# Loki the Trickster - Datapack

詐欺と策略の武器「Loki the Trickster」のデータパック実装

## インストール

1. このフォルダを`.minecraft/saves/[ワールド名]/datapacks/`にコピー
2. ワールドに入って `/reload` を実行

## 武器の入手方法

以下のコマンドで入手できます:

### Disarmモード (デフォルト)
```
/give @s minecraft:iron_sword{display:{Name:'{"text":"Loki the Trickster <Disarm>","color":"gold","italic":false}',Lore:['{"text":" "}','{"text":"[Tap Sneak: Mode Change]","color":"white","italic":false}','{"text":"[Hold Sneak: Disarm Wind]","color":"white","italic":false}','{"text":" "}','{"text":"敵を欺き、翻弄せよ。","color":"white","italic":false}','{"text":" "}','{"text":"Damage +6","color":"blue","italic":false}']},HideFlags:3,Unbreakable:1b,ItemMode:Loki_Disarm,ItemName:Loki_the_Trickster,Enchantments:[{id:"minecraft:unbreaking",lvl:0s}],AttributeModifiers:[{AttributeName:"generic.attack_damage",Name:"generic.attack_damage",Amount:5.5d,Operation:0,UUID:[I; 0, 48839, 0, 283256],Slot:"mainhand"},{AttributeName:"generic.attack_speed",Name:"generic.attack_speed",Amount:-2,Operation:0,UUID:[I;1774358129,-1670168079,-1476587264,259033975],Slot:"mainhand"}]}
```

### Decoyモード
```
/give @s minecraft:iron_sword{display:{Name:'{"text":"Loki the Trickster <Decoy>","color":"gold","italic":false}',Lore:['{"text":" "}','{"text":"[Tap Sneak: Mode Change]","color":"white","italic":false}','{"text":"[Hold Sneak: Create Decoy]","color":"white","italic":false}','{"text":" "}','{"text":"敵を欺き、翻弄せよ。","color":"white","italic":false}','{"text":" "}','{"text":"Damage +6","color":"blue","italic":false}']},HideFlags:3,Unbreakable:1b,ItemMode:Loki_Decoy,ItemName:Loki_the_Trickster,Enchantments:[{id:"minecraft:unbreaking",lvl:0s}],AttributeModifiers:[{AttributeName:"generic.attack_damage",Name:"generic.attack_damage",Amount:5.5d,Operation:0,UUID:[I; 0, 48839, 0, 283256],Slot:"mainhand"},{AttributeName:"generic.attack_speed",Name:"generic.attack_speed",Amount:-2,Operation:0,UUID:[I;1774358129,-1670168079,-1476587264,259033975],Slot:"mainhand"}]}
```

## 使い方

### モード切り替え
- **短くスニーク** (10tick以下): モードが切り替わります
  - Disarm ⇄ Decoy

### Disarmモード
- **長くスニーク** (10tick以上): 武装解除Windを発射
  - エンティティの武器を奪い取る
  - ヒット後、近くのエンティティを2秒間追跡
  - その後、プレイヤーに戻ってくる
  - 満腹度が3ゲージ以上必要

### Decoyモード  
- **長くスニーク** (20tick以上): デコイボールを発射
  - 着弾地点にデコイ(アーマースタンド)を召喚
  - デコイは敵を挑発し、攻撃を引き付ける
  - 9秒後に爆発する
  - 満腹度が3ゲージ以上必要

## 特徴

1. **Disarm Wind (武装解除)**
   - プレイヤーの視線方向に発射
   - エンティティにヒットすると武器をドロップさせる
   - ヒット後、周囲8ブロック内のエンティティを2秒間追跡
   - 最終的にプレイヤーに戻ってくる
   - スロウネスとウィザー効果を付与

2. **Decoy (デコイ)**
   - 放物線を描いて飛ぶボール
   - 着地点にデコイを召喚
   - デコイは定期的に周囲のMobを挑発
   - 9秒後に爆発して消滅

## 技術仕様

- 満腹度消費: 1ゲージ (hunger effect level 200)
- Disarmチャージ: 10tick (0.5秒)
- Decoyチャージ: 20tick (1秒)
- Disarm追跡時間: 40tick (2秒)
- Disarm最大寿命: 60tick (3秒)
- Decoy自爆タイマー: 180tick (9秒)

## ファイル構成

```
Loki_the_Trickster-v1.1.0/
├── pack.mcmeta
├── README.md
└── data/
    ├── loki_weapon/
    │   └── functions/
    │       ├── load.mcfunction              # 初期化
    │       ├── main.mcfunction              # メインループ
    │       ├── move.mcfunction              # 移動処理
    │       ├── disarm.mcfunction            # 武装解除処理
    │       ├── item/
    │       │   └── loki_the_trickster/
    │       │       ├── decoy_mode.mcfunction
    │       │       └── disarm_mode.mcfunction
    │       ├── projectile/
    │       │   ├── decoy_ball.mcfunction
    │       │   └── disarm_wind.mcfunction
    │       └── entity/
    │           └── loki_decoy.mcfunction
    └── minecraft/
        └── tags/
            └── functions/
                ├── load.json
                └── tick.json
```

## クレジット

Original Datapack: Blade of Shade - Loki the Trickster
Adapted for MCreator Workspace

# Disarm Wind - 武装解除弾丸処理

# 初期化: プレイヤーの向きに合わせる
scoreboard players set @s[tag=Loki_Disarm0] Motion_Speed 5
execute if entity @s[tag=Loki_Disarm0] at @s store result entity @s Rotation[0] float 1 run data get entity @p Rotation[0]
execute if entity @s[tag=Loki_Disarm0] at @s store result entity @s Rotation[1] float 1 run data get entity @p Rotation[1]
tag @s[tag=Loki_Disarm0] remove Loki_Disarm0

# エフェクトとサウンド
scoreboard players add @s BulletRemain 1
playsound minecraft:entity.horse.breathe neutral @a ~ ~ ~ 2 2
particle minecraft:sweep_attack ~ ~ ~ 0 0 0 1 0 force
particle minecraft:cloud ~ ~ ~ 0.5 0 0.5 0 2

# 移動処理
execute at @s[tag=!Loki_Disarm0,tag=!Loki_Disarm_Return,tag=!Loki_Disarm_Chase] run summon area_effect_cloud ^ ^ ^0.1 {Duration:1,Radius:0f,Tags:[pos]}
execute at @s[tag=!Loki_Disarm0,tag=!Loki_Disarm_Return,tag=!Loki_Disarm_Chase] run function loki_weapon:move

# アイテムを引き寄せる
execute as @e[type=item,distance=..2] at @s run tp @s @e[tag=Loki_Disarm,limit=1,sort=nearest]

# エンティティにヒットしたらチェイスモードに入る
execute if entity @s[tag=!Loki_Disarm_Return,tag=!Loki_Disarm_Chase,scores={BulletRemain=5..}] if entity @e[distance=..2,type=!item,type=!armor_stand,type=!area_effect_cloud,type=!player] run tag @s add Loki_Disarm_Chase
scoreboard players set @s[tag=Loki_Disarm_Chase] BulletRemain 0

# チェイスモード処理 (40tick = 2秒間)
execute if entity @s[tag=Loki_Disarm_Chase] run scoreboard players add @s BulletRemain 1
execute if entity @s[tag=Loki_Disarm_Chase,scores={BulletRemain=..40}] facing entity @e[distance=..8,type=!item,type=!armor_stand,type=!area_effect_cloud,type=!player,limit=1,sort=nearest] feet run tp @s ^ ^ ^0.6
execute if entity @s[tag=Loki_Disarm_Chase,scores={BulletRemain=40..}] run tag @s add Loki_Disarm_Return
execute if entity @s[tag=Loki_Disarm_Chase,scores={BulletRemain=40..}] run tag @s remove Loki_Disarm_Chase

# ヒット時の武装解除とエフェクト
execute if entity @s[tag=!Loki_Disarm_Return,tag=!Loki_Disarm_Chase,scores={BulletRemain=5..}] as @e[distance=..2,type=!item,tag=!Loki_Disarm,type=!area_effect_cloud] at @s run function loki_weapon:disarm
execute if entity @s[tag=!Loki_Disarm_Return,scores={BulletRemain=5..}] run effect give @e[distance=..2] slowness 2 10
execute if entity @s[tag=!Loki_Disarm_Return,scores={BulletRemain=5..}] run effect give @e[distance=..2] wither 1 2
execute if entity @s[tag=!Loki_Disarm_Return,scores={BulletRemain=5..}] if entity @e[distance=..2,type=!item,tag=!Loki_Disarm,type=!area_effect_cloud] run particle minecraft:crit ~ ~ ~ 0 0 0 0.5 5

# 戻ってくる条件
tag @s[scores={BulletRemain=60..}] add Loki_Disarm_Return
execute if entity @s[tag=Loki_Disarm_Return] at @s if entity @a[distance=..1.25] run kill @s

# Enter the function code here

#向きをプレイヤーと同じに
scoreboard players set @s[tag=Loki_Disarm0] Motion_Speed 5
execute if entity @s[tag=Loki_Disarm0] at @s store result entity @s Rotation[0] float 1 run data get entity @p Rotation[0]
execute if entity @s[tag=Loki_Disarm0] at @s store result entity @s Rotation[1] float 1 run data get entity @p Rotation[1]
tag @s[tag=Loki_Disarm0] remove Loki_Disarm0

#武装解除攻撃
scoreboard players add @s BulletRemain 1
playsound minecraft:entity.horse.breathe neutral @a ~ ~ ~ 2 2
particle minecraft:sweep_attack ~ ~ ~ 0 0 0 1 0 force
particle minecraft:cloud ~ ~ ~ 0.5 0 0.5 0 2
execute at @s[tag=!Loki_Disarm0,tag=!Loki_Disarm_Return] run summon area_effect_cloud ^ ^ ^0.1 {Duration:1,Radius:0f,Tags:[pos]}
execute at @s[tag=!Loki_Disarm0,tag=!Loki_Disarm_Return] run function the_four_primitives_and_weapons:disarm_move
execute as @e[type=item,distance=..2] at @s run tp @s @e[tag=Loki_Disarm,limit=1,sort=nearest]

#戻ってくる
tag @s[scores={BulletRemain=15..}] add Loki_Disarm_Return
#execute if entity @s[tag=Loki_Disarm_Return] at @s if entity @a[distance=..1.25] run kill @s

#ヒット
execute if entity @s[tag=!Loki_Disarm_Return,scores={BulletRemain=5..}] as @e[distance=..2,type=!item,tag=!Loki_Disarm,type=!area_effect_cloud] at @s run function the_four_primitives_and_weapons:disarm_disarm
execute if entity @s[tag=!Loki_Disarm_Return,scores={BulletRemain=5..}] run effect give @e[distance=..2] slowness 2 10
execute if entity @s[tag=!Loki_Disarm_Return,scores={BulletRemain=5..}] run effect give @e[distance=..2] wither 1 2
execute if entity @s[tag=!Loki_Disarm_Return,scores={BulletRemain=5..}] if entity @e[distance=..2,type=!item,tag=!Loki_Disarm,type=!area_effect_cloud] run particle minecraft:crit ~ ~ ~ 0 0 0 0.5 5
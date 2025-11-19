# 移動処理 - Motion_Speedの値に応じてスピード調整

# X軸の移動
execute as @s store result score @s Motion_Power run data get entity @s Pos[0] 300
execute as @e[tag=pos] store result score @s Motion_Power run data get entity @s Pos[0] 300
execute as @s at @s positioned ^ ^ ^0.1 run scoreboard players operation @s Motion_Power -= @e[tag=pos,limit=1,sort=nearest] Motion_Power
scoreboard players operation @s Motion_Power *= @s Motion_Speed
execute as @s store result entity @s Motion[0] double -0.01 run scoreboard players get @s Motion_Power

# Y軸の移動
execute as @s store result score @s Motion_Power run data get entity @s Pos[1] 300
execute as @e[tag=pos] store result score @s Motion_Power run data get entity @s Pos[1] 300
execute as @s at @s positioned ^ ^ ^0.1 run scoreboard players operation @s Motion_Power -= @e[tag=pos,limit=1,sort=nearest] Motion_Power
scoreboard players operation @s Motion_Power *= @s Motion_Speed
execute as @s store result entity @s Motion[1] double -0.01 run scoreboard players get @s Motion_Power

# Z軸の移動
execute as @s store result score @s Motion_Power run data get entity @s Pos[2] 300
execute as @e[tag=pos] store result score @s Motion_Power run data get entity @s Pos[2] 300
execute as @s at @s positioned ^ ^ ^0.1 run scoreboard players operation @s Motion_Power -= @e[tag=pos,limit=1,sort=nearest] Motion_Power
scoreboard players operation @s Motion_Power *= @s Motion_Speed
execute as @s store result entity @s Motion[2] double -0.01 run scoreboard players get @s Motion_Power

# 位置マーカーを削除
kill @e[tag=pos]

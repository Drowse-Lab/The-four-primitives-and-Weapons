# Loki Decoy - デコイアーマースタンドの挙動

# 挑発攻撃
execute if entity @s[tag=Loki_Decoy_Taunt] run tp @s ~ ~ ~ facing entity @e[distance=0.1..,sort=nearest,limit=1,type=!snowball,tag=!Loki_Decoy_Target] feet
execute if entity @s[tag=Loki_Decoy_Taunt] run data modify entity @s Motion[1] set value 0.5
execute if entity @s[tag=Loki_Decoy_Taunt] run particle minecraft:poof ~ ~1 ~ 0 0 0 0.25 25
execute if entity @s[tag=Loki_Decoy_Taunt] run playsound minecraft:entity.wither.shoot neutral @a ~ ~ ~ 2 2
execute if entity @s[tag=Loki_Decoy_Taunt] run playsound minecraft:entity.chicken.egg neutral @a ~ ~ ~ 2 0
execute if entity @s[tag=Loki_Decoy_Taunt] at @e[distance=0.1..5,tag=!Loki_Decoy_Target] run summon minecraft:snowball ~ ~-0.31 ~ {Owner:{},Motion:[0.0,10.0,0.0],Tags:["Loki_Decoy_Attack"],Item:{id:"minecraft:gray_stained_glass",Count:1b}}
data modify entity @e[type=minecraft:snowball,tag=Loki_Decoy_Attack,limit=1,sort=nearest] Owner set from entity @s UUID
tag @s[tag=Loki_Decoy_Taunt] remove Loki_Decoy_Taunt

# 挑発タイミング制御 (Invulnerable切り替えで敵対させる)
scoreboard players add @s Decoy_Action 1
execute if entity @s[scores={Decoy_Action=3}] run data modify entity @s Invulnerable set value 1
execute if entity @s[scores={Decoy_Action=59}] run data modify entity @s Invulnerable set value 0
tag @s[scores={Decoy_Action=60}] add Loki_Decoy_Taunt
execute if entity @s[scores={Decoy_Action=61}] run data modify entity @s Invulnerable set value 1
execute if entity @s[scores={Decoy_Action=119}] run data modify entity @s Invulnerable set value 0
tag @s[scores={Decoy_Action=120}] add Loki_Decoy_Taunt
execute if entity @s[scores={Decoy_Action=121}] run data modify entity @s Invulnerable set value 1

# 自爆カウントダウン
tag @s[scores={Decoy_Action=150}] add Decoy_Spin1
execute if entity @s[scores={Decoy_Action=150..}] run particle minecraft:smoke ~ ~1.8 ~ 0 0 0 0 2
execute if entity @s[scores={Decoy_Action=150}] run playsound minecraft:block.note_block.pling master @a ~ ~ ~ 2 1

tag @s[scores={Decoy_Action=160}] remove Decoy_Spin1
tag @s[scores={Decoy_Action=160}] add Decoy_Spin2
execute if entity @s[scores={Decoy_Action=160}] run playsound minecraft:block.note_block.pling master @a ~ ~ ~ 2 1.5

tag @s[scores={Decoy_Action=170}] remove Decoy_Spin2
tag @s[scores={Decoy_Action=170}] add Decoy_Spin3
execute if entity @s[scores={Decoy_Action=170..}] run playsound minecraft:block.note_block.pling master @a ~ ~ ~ 2 2

# 爆発
execute if entity @s[scores={Decoy_Action=180}] run summon creeper ~ ~ ~ {Fuse:0s}
execute if entity @s[scores={Decoy_Action=180}] run kill @e[tag=Loki_Decoy_Target,limit=1,sort=nearest]
kill @s[scores={Decoy_Action=180}]

# 頭を回転させる演出
execute if entity @s[tag=Decoy_Spin1] store result entity @s Pose.Head[1] float -1 run scoreboard players add @s Decoy_Spin 20
execute if entity @s[tag=Decoy_Spin2] store result entity @s Pose.Head[1] float -1 run scoreboard players add @s Decoy_Spin 40
execute if entity @s[tag=Decoy_Spin3] store result entity @s Pose.Head[1] float -1 run scoreboard players add @s Decoy_Spin 60
scoreboard players set @s[scores={Decoy_Spin=360..}] Decoy_Spin 0

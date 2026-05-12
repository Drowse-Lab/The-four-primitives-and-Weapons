# スコアボード初期化
scoreboard players add @s Decoy_Action 1
scoreboard players add @s Decoy_Spin 0

# 挑発機能 - 近くの敵MOBを引き寄せる
execute if entity @s[tag=Loki_Decoy_Taunt] run effect give @e[type=!minecraft:armor_stand,type=!minecraft:player,distance=..10] minecraft:glowing 1 0 true
execute if entity @s[tag=Loki_Decoy_Taunt] as @e[type=!minecraft:armor_stand,type=!minecraft:player,distance=..10] at @s facing entity @e[tag=Loki_Decoy,limit=1,sort=nearest] feet run tp @s ^ ^ ^0.1

# 爆発前の演出
tag @s[scores={Decoy_Action=150}] add Decoy_Spin1
execute if entity @s[scores={Decoy_Action=150..}] run particle minecraft:smoke ~ ~1.8 ~ 0 0 0 0 2
execute if entity @s[scores={Decoy_Action=150}] run playsound minecraft:block.note_block.pling master @a ~ ~ ~ 2 1
tag @s[scores={Decoy_Action=160}] remove Decoy_Spin1
tag @s[scores={Decoy_Action=160}] add Decoy_Spin2
execute if entity @s[scores={Decoy_Action=160}] run playsound minecraft:block.note_block.pling master @a ~ ~ ~ 2 1.5
tag @s[scores={Decoy_Action=170}] remove Decoy_Spin2
tag @s[scores={Decoy_Action=170}] add Decoy_Spin3
execute if entity @s[scores={Decoy_Action=170}] run playsound minecraft:block.note_block.pling master @a ~ ~ ~ 2 2

# 爆発処理
execute if entity @s[scores={Decoy_Action=180}] run summon creeper ~ ~ ~ {Fuse:0s}
execute if entity @s[scores={Decoy_Action=180}] run kill @e[tag=Loki_Decoy_Target,limit=1,sort=nearest]
kill @s[scores={Decoy_Action=180}]

# 頭くるくる
execute if entity @s[tag=Decoy_Spin1] store result entity @s Pose.Head[1] float -1 run scoreboard players add @s Decoy_Spin 20
execute if entity @s[tag=Decoy_Spin2] store result entity @s Pose.Head[1] float -1 run scoreboard players add @s Decoy_Spin 40
execute if entity @s[tag=Decoy_Spin3] store result entity @s Pose.Head[1] float -1 run scoreboard players add @s Decoy_Spin 60
scoreboard players set @s[scores={Decoy_Spin=360..}] Decoy_Spin 0

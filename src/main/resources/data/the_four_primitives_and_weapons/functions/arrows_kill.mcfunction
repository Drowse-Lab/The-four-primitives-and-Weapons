# Enter the function code here
execute if entity @a[nbt=limit=1,sort=nearest] run kill @e[name=b]
execute at @s[scores={playerHit=1..}] run execute at @e[nbt={HurtTime:10s}] run tag @e[type=!player,nbt=!distance=..5,type=!item,type=!minecraft:wolf,type=!minecraft:armor_stand,type=!arrow,type=!experience_orb] add kill
execute as @a[scores={no=1},limit=1] at @s run scoreboard players add @s no 1
execute as @e[tag=kill] at @s run attribute @s minecraft:generic.max_health base set 1
execute as @e[tag=kill] at @s run playsound minecraft:entity.drowned.shoot voice @a ~ ~ ~ 1 1
execute as @e[tag=kill] at @s run particle minecraft:sweep_attack ~ ~1 ~ 0.5 0.5 0.5 1 1 normal @a
execute as @e[tag=kill,type=!minecraft:skeletons] at @s run particle minecraft:item redstone ~ ~1.5 ~ 0.2 0.3 0.2 0.05 10 force @a

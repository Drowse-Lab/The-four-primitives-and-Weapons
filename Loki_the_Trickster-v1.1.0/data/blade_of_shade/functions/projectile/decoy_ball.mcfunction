scoreboard players set @s[tag=Loki_Decoy_Shoot] Motion_Speed 4
execute if entity @s[tag=Loki_Decoy_Shoot] run function blade_of_shade:move
tag @s[tag=Loki_Decoy_Shoot] remove Loki_Decoy_Shoot
particle minecraft:smoke ~ ~ ~ 0.1 0.1 0.1 0 2
particle minecraft:crit ~ ~ ~ 0 0 0 0 1
execute if entity @s[nbt={OnGround:1b}] run summon minecraft:armor_stand ~ ~ ~ {Invulnerable:1b,ShowArms:1b,NoBasePlate:1b,Tags:["Loki_Decoy","Loki_Decoy_Taunt"],Pose:{LeftArm:[0f,5f,-90f],RightArm:[0f,-5f,90f],LeftLeg:[0f,-5f,0f],RightLeg:[0f,5f,0f],Head:[0f,0f,0.1f]},DisabledSlots:4144959,ArmorItems:[{id:"minecraft:leather_boots",Count:1b,tag:{display:{color:11573855}}},{id:"minecraft:leather_leggings",Count:1b},{id:"minecraft:leather_chestplate",Count:1b,tag:{display:{color:11573855}}},{id:"minecraft:player_head",Count:1b,tag:{SkullOwner:{Id:[I;-2006638556,212421221,-1198641951,-1500527049],Properties:{textures:[{Value:"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTg3YjhlYjEzZDk5MjU3NTlhMmU1ZjkwOTRhNWU1YzUwZDJlZWI0YWQ2ZWJiMjk0YjVjNzA2NDU5OTA3NmQwOCJ9fX0="}]}}}}]}
execute if entity @s[nbt={OnGround:1b}] run kill @s


#ドロッパーに上向き額縁貼り付けて、その中に作業台ぶちこむとカスタム作業台が登場！
execute as @e[type=minecraft:item_frame,nbt={Facing:1b,Item:{id:"minecraft:crafting_table"}}] at @s if block ~ ~-1 ~ minecraft:dropper{Items:[]} run playsound minecraft:block.anvil.place block @a ~ ~ ~ 1 2
execute as @e[type=minecraft:item_frame,nbt={Facing:1b,Item:{id:"minecraft:crafting_table"}}] at @s if block ~ ~-1 ~ minecraft:dropper{Items:[]} run playsound minecraft:entity.player.levelup block @a ~ ~ ~ 1 2
execute as @e[type=minecraft:item_frame,nbt={Facing:1b,Item:{id:"minecraft:crafting_table"}}] at @s if block ~ ~-1 ~ minecraft:dropper{Items:[]} run particle minecraft:crit ~ ~ ~ 0 0 0 0.5 10
execute as @e[type=minecraft:item_frame,nbt={Facing:1b,Item:{id:"minecraft:crafting_table"}}] at @s if block ~ ~-1 ~ minecraft:dropper{Items:[]} run setblock ~ ~-1 ~ minecraft:dropper[facing=up]{CustomName:"{\"text\":\"Custom Crafter\",\"color\":\"dark_gray\",\"italic\":false}"}
execute as @e[type=minecraft:item_frame,nbt={Facing:1b,Item:{id:"minecraft:crafting_table"}}] at @s if block ~ ~-1 ~ minecraft:dropper{Items:[]} run data merge block ~ ~-1 ~ {CustomName:"{\"text\":\"Custom Crafter\",\"color\":\"dark_gray\",\"italic\":false}"}
execute as @e[type=minecraft:item_frame,nbt={Facing:1b,Item:{id:"minecraft:crafting_table"}}] at @s if block ~ ~-1 ~ minecraft:dropper{Items:[]} run summon minecraft:item_frame ~ ~ ~ {Facing:1b,Invulnerable:1b,Tags:["Custom_Crafter_Core"],Item:{id:"minecraft:white_stained_glass_pane",Count:1b,tag:{display:{Name:'{"text":"Right Click Here"}'},CustomModelData:1}}}
execute as @e[type=minecraft:item_frame,nbt={Facing:1b,Item:{id:"minecraft:crafting_table"}}] at @s if block ~ ~-1 ~ minecraft:dropper{Items:[]} run kill @s
#流石に地味だし…
execute at @e[tag=Custom_Crafter_Core] run particle minecraft:dust 1 1 1 0.5 ~0.3 ~0.05 ~0.3 0 0 0 0 1
execute at @e[tag=Custom_Crafter_Core] run particle minecraft:dust 1 1 1 0.5 ~0.3 ~0.05 ~-0.3 0 0 0 0 1
execute at @e[tag=Custom_Crafter_Core] run particle minecraft:dust 1 1 1 0.5 ~-0.3 ~0.05 ~0.3 0 0 0 0 1
execute at @e[tag=Custom_Crafter_Core] run particle minecraft:dust 1 1 1 0.5 ~-0.3 ~0.05 ~-0.3 0 0 0 0 1

#上部右クリックでクラフト
execute at @e[tag=Custom_Crafter_Core,nbt={ItemRotation:1b}] run playsound minecraft:block.stone_button.click_on block @a ~ ~ ~ 1 0.7
execute at @e[tag=Custom_Crafter_Core,nbt={ItemRotation:1b}] run summon minecraft:area_effect_cloud ~ ~ ~ {Particle:"smoke",Radius:0.1f,Duration:2,Tags:["Custom_Crafter_Crafting"]}
execute at @e[tag=Custom_Crafter_Succes] run playsound minecraft:block.anvil.use block @a ~ ~ ~ 1 2
execute at @e[tag=Custom_Crafter_Succes] run playsound minecraft:block.iron_door.open block @a ~ ~ ~ 1 2
execute at @e[tag=Custom_Crafter_Succes] run particle minecraft:crit ~ ~ ~ 0 0 0 0.5 10
execute at @e[tag=Custom_Crafter_Succes] run particle minecraft:smoke ~ ~ ~ 0 0 0 0.05 5

tag @e[tag=Custom_Crafter_Succes] remove Custom_Crafter_Succes
execute as @e[tag=Custom_Crafter_Core,nbt={ItemRotation:1b}] run data modify entity @s ItemRotation set value 0b

#コアの下がドロッパーでなければ破壊される
execute as @e[tag=Custom_Crafter_Core] at @s unless block ~ ~-1 ~ dropper run kill @e[type=item,nbt={Age:0s,Item:{id:"minecraft:dropper",Count:1b}}]
execute as @e[tag=Custom_Crafter_Core] at @s unless block ~ ~-1 ~ dropper run summon minecraft:item ~ ~ ~ {Item:{id:"minecraft:dropper",Count:1b}}
execute as @e[tag=Custom_Crafter_Core] at @s unless block ~ ~-1 ~ dropper run summon minecraft:item ~ ~ ~ {Item:{id:"minecraft:item_frame",Count:1b}}
execute as @e[tag=Custom_Crafter_Core] at @s unless block ~ ~-1 ~ dropper run summon minecraft:item ~ ~ ~ {Item:{id:"minecraft:crafting_table",Count:1b}}
execute as @e[tag=Custom_Crafter_Core] at @s unless block ~ ~-1 ~ dropper run kill @s

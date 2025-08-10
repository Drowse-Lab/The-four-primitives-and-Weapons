summon minecraft:stray ~ ~ ~ {Tags:["hardentity_skeleton"],ArmorDropChances:[1f,1f,1f,1f],HandDropChances:[1f,1f]}
particle minecraft:crit ~ ~0.2 ~ 0 0 0 0.2 10
playsound minecraft:block.anvil.use neutral @a ~ ~ ~ 1 2
playsound minecraft:block.iron_door.open neutral @a ~ ~ ~ 1 2
kill @e[sort=nearest,limit=1,type=item,nbt={Item:{id:"minecraft:nether_star",Count:1b}}]
kill @e[sort=nearest,limit=1,type=item,nbt={Item:{id:"minecraft_armor_weapon:stray_bone",Count:16b}}]
kill @e[sort=nearest,limit=1,type=item,nbt={Item:{id:"minecraft:iron_block",Count:64b}}]
effect give @e[sort=nearest,limit=1,type=minecraft:stray,tag=hardentity_skeleton] minecraft_armor_weapon:hardentity 999999 4


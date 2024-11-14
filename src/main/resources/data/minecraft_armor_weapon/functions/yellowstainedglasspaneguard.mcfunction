summon minecraft:armor_stand ~ ~ ~ {NoGravity:1b,Invisible:1b,Tags:["minecraft_armor_weapon_guard_bind"],Pose:{LeftArm:[0f,90f,-90f],RightArm:[0f,-90f,90f]},DisabledSlots:4144959,HandItems:[{id:"minecraft:yellow_stained_glass_pane",Count:1b},{id:"minecraft:yellow_stained_glass_pane",Count:1b}]}
playsound minecraft:block.enchantment_table.use player @s ~ ~ ~ 2 2
playsound minecraft:item.armor.equip_gold player @s ~ ~ ~ 1 1
particle minecraft:dust 1 1 0.5 0.5 ~ ~1 ~ 0.25 0.25 0.25 1 35

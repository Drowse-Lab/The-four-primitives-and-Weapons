summon minecraft:armor_stand ~-0.3 ~-0.5 ~0.025 {Invisible:true,Invulnerable:true,PersistenceRequired:true,NoBasePlate:true,NoGravity:true,ShowArms:true,DisabledSlots:65537,Pose:{RightArm:[90f,90f,0f]},HandItems:[{id:"the_four_primitives_and_weapons:luna",Count:1},{}],Tags:["the_four_primitives_and_weapons_item_stand_amor_stand_luna"]}
kill @e[sort=nearest,limit=1,type=item,nbt={Item:{id:"the_four_primitives_and_weapons:iron_katana",Count:1b}}]
kill @e[sort=nearest,limit=1,type=item,nbt={Item:{id:"minecraft:beacon",Count:16b}}]
kill @e[sort=nearest,limit=1,type=item,nbt={Item:{id:"minecraft:iron_block",Count:64b}}]
playsound minecraft:block.beacon.activate block @a ~ ~ ~ 5 2
particle minecraft:cloud ~ ~1.5 ~ 1 1 1 0.1 200 normal
particle minecraft:end_rod ~ ~1.5 ~ 0.4 5 0.4 0.0 200 normal
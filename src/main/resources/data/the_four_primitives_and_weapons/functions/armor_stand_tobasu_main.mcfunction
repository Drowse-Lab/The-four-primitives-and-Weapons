# Enter the function code here# say [main]
#########################################################
# ③の処理
#########################################################
execute store result score @s the_four_primitives_and_weapons_armor_stand_tobasu_main_AS2_X0 run data get entity @s Pos[0] 1000
execute store result score @s the_four_primitives_and_weapons_armor_stand_tobasu_main_AS2_Y0 run data get entity @s Pos[1] 1000
execute store result score @s the_four_primitives_and_weapons_armor_stand_tobasu_main_AS2_Z0 run data get entity @s Pos[2] 1000
#########################################################

#########################################################
# ②の処理
#########################################################
execute rotated as @s run summon minecraft:area_effect_cloud ^ ^ ^1 {Tags:[the_four_primitives_and_weapons_armor_stand_tobasu_main_AS2_AEC],Duration:0}
#########################################################
# ④の処理
#########################################################
execute store result score @s the_four_primitives_and_weapons_armor_stand_tobasu_main_AS2_X1 run data get entity @e[tag=the_four_primitives_and_weapons_armor_stand_tobasu_main_AS2_AEC,limit=1] Pos[0] 1000
execute store result score @s the_four_primitives_and_weapons_armor_stand_tobasu_main_AS2_Y1 run data get entity @e[tag=the_four_primitives_and_weapons_armor_stand_tobasu_main_AS2_AEC,limit=1] Pos[1] 1000
execute store result score @s the_four_primitives_and_weapons_armor_stand_tobasu_main_AS2_Z1 run data get entity @e[tag=the_four_primitives_and_weapons_armor_stand_tobasu_main_AS2_AEC,limit=1] Pos[2] 1000
#########################################################
kill @e[tag=the_four_primitives_and_weapons_armor_stand_tobasu_main_AS2_AEC,limit=1]
#########################################################

#########################################################
# ⑤の処理
#########################################################
scoreboard players operation @s the_four_primitives_and_weapons_armor_stand_tobasu_main_AS2_X0 -= @s the_four_primitives_and_weapons_armor_stand_tobasu_main_AS2_X1
scoreboard players operation @s the_four_primitives_and_weapons_armor_stand_tobasu_main_AS2_Y0 -= @s the_four_primitives_and_weapons_armor_stand_tobasu_main_AS2_Y1
scoreboard players operation @s the_four_primitives_and_weapons_armor_stand_tobasu_main_AS2_Z0 -= @s the_four_primitives_and_weapons_armor_stand_tobasu_main_AS2_Z1
#########################################################

#########################################################
# ⑥の処理
#########################################################
execute store result entity @s Motion[0] double -0.001 run scoreboard players get @s the_four_primitives_and_weapons_armor_stand_tobasu_main_AS2_X0
execute store result entity @s Motion[1] double -0.001 run scoreboard players get @s the_four_primitives_and_weapons_armor_stand_tobasu_main_AS2_Y0
execute store result entity @s Motion[2] double -0.001 run scoreboard players get @s the_four_primitives_and_weapons_armor_stand_tobasu_main_AS2_Z0
#########################################################


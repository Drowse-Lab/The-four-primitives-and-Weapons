#########################################################
execute as @a[scores={the_four_primitives_and_weapons_armor_stand_tobasu_main_AS2_R=1..}] at @s run function the_four_primitives_and_weapons:armor_stand_tobasu_start_kill
scoreboard players set @a[scores={the_four_primitives_and_weapons_armor_stand_tobasu_main_AS2_R=1..}] the_four_primitives_and_weapons_armor_stand_tobasu_main_AS2_R 0
#########################################################

#########################################################
scoreboard players remove @e[scores={the_four_primitives_and_weapons_armor_stand_tobasu_main_AS2_D=1..}] the_four_primitives_and_weapons_armor_stand_tobasu_main_AS2_D 1
kill @e[scores={the_four_primitives_and_weapons_armor_stand_tobasu_main_AS2_D=0}]
#########################################################

#########################################################
execute as @e[tag=the_four_primitives_and_weapons_armor_stand_tobasu_main_AS2] at @s run function the_four_primitives_and_weapons:armor_stand_tobasu_main
#########################################################


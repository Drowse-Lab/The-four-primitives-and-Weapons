# Enter the function code here
# say [start]
#########################################################
# ①の処理
#########################################################
#summon minecraft:armor_stand ~ ~1 ~ {Tags:[the_four_primitives_and_weapons_armor_stand_tobasu_enma_main_AS2,the_four_primitives_and_weapons_armor_stand_tobasu_enma_main_AS2_INI],Small:1b,Invisible:1b,Invulnerable:1b,NoBasePlate:1b}
#execute anchored eyes as @e[tag=the_four_primitives_and_weapons_armor_stand_tobasu_enma_main_AS2_INI,limit=1,sort=nearest] run tp @s ^ ^ ^ ~ ~
#execute anchored eyes as @e[tag=the_four_primitives_and_weapons_armor_stand_tobasu_enma_main_AS2_INI,limit=1,sort=nearest] run effect give @s the_four_primitives_and_weapons:armorstandtobasuenmaeffectkill 10 1 true
##########################################################
#scoreboard players set @e[tag=AS2_INI,limit=1,sort=nearest] the_four_primitives_and_weapons_armor_stand_tobasu_enma_main_AS2_D 80
#tag @e[tag=the_four_primitives_and_weapons_armor_stand_tobasu_enma_main_AS2_INI,limit=1,sort=nearest] remove the_four_primitives_and_weapons_armor_stand_tobasu_enma_main_AS2_INI
##########################################################
# say [start]
#########################################################
# ①の処理
#########################################################
#summon minecraft:armor_stand ~ ~1 ~ {Tags:[the_four_primitives_and_weapons_armor_stand_tobasu_main_AS2,the_four_primitives_and_weapons_armor_stand_tobasu_main_AS2_INI],Small:1b,Invisible:1b,Invulnerable:1b,NoBasePlate:1b}
summon minecraft:armor_stand ~ ~1 ~ {Tags:[the_four_primitives_and_weapons_armor_stand_tobasu_main_AS2,the_four_primitives_and_weapons_armor_stand_tobasu_main_AS2_INI],Small:1b,Invisible:1b,NoBasePlate:1b}
execute anchored eyes as @e[tag=the_four_primitives_and_weapons_armor_stand_tobasu_main_AS2_INI,limit=1,sort=nearest] run tp @s ^ ^ ^ ~ ~
#execute anchored eyes as @e[tag=the_four_primitives_and_weapons_armor_stand_tobasu_main_AS2_INI,limit=1,sort=nearest] run fill ^-30 ^ ^5 ^30 ^3 ^5 air
#execute anchored eyes as @e[tag=the_four_primitives_and_weapons_armor_stand_tobasu_main_AS2_INI,limit=1,sort=nearest] run particle sweep_attack ^ ^ ^ 10 0 0 5 100 normal @a
#execute anchored eyes as @e[tag=the_four_primitives_and_weapons_armor_stand_tobasu_main_AS2_INI,limit=1,sort=nearest] run particle witch ^ ^ ^ 10 1 0 10 100 normal @a
execute anchored eyes as @e[tag=the_four_primitives_and_weapons_armor_stand_tobasu_main_AS2_INI,limit=1,sort=nearest] run effect give @s the_four_primitives_and_weapons:armorstandtobasuenmaeffectkill 10 1 true
#########################################################
scoreboard players set @e[tag=the_four_primitives_and_weapons_armor_stand_tobasu_main_AS2_INI,limit=1,sort=nearest] the_four_primitives_and_weapons_armor_stand_tobasu_main_AS2_D 80
tag @e[tag=the_four_primitives_and_weapons_armor_stand_tobasu_main_AS2_INI,limit=1,sort=nearest] remove the_four_primitives_and_weapons_armor_stand_tobasu_main_AS2_INI
#########################################################

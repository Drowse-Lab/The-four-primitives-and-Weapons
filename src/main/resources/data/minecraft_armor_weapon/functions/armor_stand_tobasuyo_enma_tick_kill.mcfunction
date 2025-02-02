#########################################################
execute as @a[scores={minecraft_armor_weapon_armor_stand_tobasu_main_AS2_R=1..}] at @s run function minecraft_armor_weapon:armor_stand_tobasuyo_enma_start_kill
scoreboard players set @a[scores={minecraft_armor_weapon_armor_stand_tobasu_main_AS2_R=1..}] minecraft_armor_weapon_armor_stand_tobasu_main_AS2_R 0
#########################################################

#########################################################
scoreboard players remove @e[scores={minecraft_armor_weapon_armor_stand_tobasu_main_AS2_D=1..}] minecraft_armor_weapon_armor_stand_tobasu_main_AS2_D 1
kill @e[scores={minecraft_armor_weapon_armor_stand_tobasu_main_AS2_D=0}]
#########################################################

#########################################################
execute as @e[tag=minecraft_armor_weapon_armor_stand_tobasu_main_AS2] at @s run function minecraft_armor_weapon:armor_stand_tobasu_main
#########################################################


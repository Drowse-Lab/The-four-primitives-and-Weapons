# Enter the function code here
# say [start]
#########################################################
# ①の処理
#########################################################
summon minecraft:armor_stand ~ ~1 ~ {Tags:[minecraft_armor_weapon_armor_stand_tobasu_enma_enmamain_AS2,minecraft_armor_weapon_armor_stand_tobasu_enma_main_AS2_INI],NoGravity:1b,Small:1b,Invisible:1b,Invulnerable:1b,NoBasePlate:1b}
execute anchored eyes as @e[tag=minecraft_armor_weapon_armor_stand_tobasu_enma_main_AS2_INI,limit=1,sort=nearest] run tp @s ^ ^ ^ ~ ~
execute anchored eyes as @e[tag=minecraft_armor_weapon_armor_stand_tobasu_enma_main_AS2_INI,limit=1,sort=nearest] run effect give @s minecraft_armor_weapon:armorstandtobasuenmaeffectkill 2 1 true
#########################################################
scoreboard players set @e[tag=AS2_INI,limit=1,sort=nearest] minecraft_armor_weapon_armor_stand_tobasu_enma_main_AS2_D 80
tag @e[tag=minecraft_armor_weapon_armor_stand_tobasu_enma_main_AS2_INI,limit=1,sort=nearest] remove minecraft_armor_weapon_armor_stand_tobasu_enma_main_AS2_INI
#########################################################

# Enter the function code here
#武装解除ショット
execute as @e[tag=Loki_Disarm] at @s run function the_four_primitives_and_weapons:disarm_wimd

#戻ってこい
execute as @a at @e[tag=Loki_Disarm_Return] if score @s PlayerID = @e[tag=Loki_Disarm,limit=1,sort=nearest] ScoreID2 facing entity @s feet run teleport @e[tag=Loki_Disarm,limit=1,sort=nearest] ^ ^ ^1.5 facing entity @s

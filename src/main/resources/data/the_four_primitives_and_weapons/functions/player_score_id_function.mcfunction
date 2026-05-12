# Enter the function code here
#プレイヤーのID
execute if entity @a[tag=!HasID_Player,limit=1] run scoreboard players add #PlayerID_Core PlayerID_Core 1
execute as @a[tag=!HasID_Player,limit=1] run scoreboard players operation @s PlayerID = #PlayerID_Core PlayerID_Core
tag @a[tag=!HasID_Player,limit=1,scores={PlayerID=0..}] add HasID_Player

#"NeedID"タグ持ちにプレイヤーと同じIDを付与
#"ScoreID2"はプレイヤー由来のエンティティ限定
execute as @e[tag=NeedID] at @s run scoreboard players operation @s ScoreID2 = @p PlayerID
tag @e[tag=NeedID] remove NeedID

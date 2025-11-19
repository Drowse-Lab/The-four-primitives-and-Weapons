# メインループ処理

# Decoyモード処理
execute as @a[nbt={SelectedItem:{tag:{ItemName:Loki_the_Trickster,ItemMode:Loki_Decoy}}}] at @s run function loki_weapon:item/loki_the_trickster/decoy_mode

# Disarmモード処理
execute as @a[nbt={SelectedItem:{tag:{ItemName:Loki_the_Trickster,ItemMode:Loki_Disarm}}}] at @s run function loki_weapon:item/loki_the_trickster/disarm_mode

# デコイボール処理
execute as @e[tag=Loki_Decoy_Ball] at @s run function loki_weapon:projectile/decoy_ball

# デコイアーマースタンド処理
execute as @e[tag=Loki_Decoy] at @s run function loki_weapon:entity/loki_decoy

# Disarm Wind処理
execute as @e[tag=Loki_Disarm] at @s run function loki_weapon:projectile/disarm_wind

# Disarm Windの帰還処理
execute as @a at @e[tag=Loki_Disarm_Return] if score @s PlayerID = @e[tag=Loki_Disarm,limit=1,sort=nearest] ScoreID2 facing entity @s feet run teleport @e[tag=Loki_Disarm,limit=1,sort=nearest] ^ ^ ^1.5 facing entity @s

# スニークチャージ処理
scoreboard players add @a[nbt={SelectedItem:{tag:{ItemName:Loki_the_Trickster}}},scores={Loki_Sneak=1..}] Loki_Charge 1
scoreboard players reset @a[scores={Loki_Sneak=0}] Loki_Charge
scoreboard players set @a[scores={Loki_Sneak=1..}] Loki_Sneak 0
scoreboard players reset @a[nbt=!{SelectedItem:{tag:{ItemName:Loki_the_Trickster}}}] Loki_Charge

# 満腹度をスコア化
execute as @a store result score @s FoodLevel run data get entity @s foodLevel

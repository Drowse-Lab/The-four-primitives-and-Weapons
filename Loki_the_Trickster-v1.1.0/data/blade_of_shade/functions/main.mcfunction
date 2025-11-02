#動作ファンクション
execute as @a[nbt={SelectedItem:{tag:{ItemName:Loki_the_Trickster,ItemMode:Loki_Decoy}}}] at @s run function blade_of_shade:item/loki_the_trickster/decoy_mode
execute as @a[nbt={SelectedItem:{tag:{ItemName:Loki_the_Trickster,ItemMode:Loki_Disarm}}}] at @s run function blade_of_shade:item/loki_the_trickster/disarm_mode

#デコイを出すボール
execute as @e[tag=Loki_Decoy_Ball] at @s run function blade_of_shade:projectile/decoy_ball

#デコイ
execute as @e[tag=Loki_Decoy] at @s run function blade_of_shade:entity/loki_decoy

#武装解除ショット
execute as @e[tag=Loki_Disarm] at @s run function blade_of_shade:projectile/disarm_wimd

#戻ってこい
execute as @a at @e[tag=Loki_Disarm_Return] if score @s PlayerID = @e[tag=Loki_Disarm,limit=1,sort=nearest] ScoreID2 facing entity @s feet run teleport @e[tag=Loki_Disarm,limit=1,sort=nearest] ^ ^ ^1.5 facing entity @s

#クラフト
execute as @e[tag=Custom_Crafter_Crafting] at @s if block ~ ~-0.5 ~ minecraft:dropper{Items:[{Slot:0b,id:"minecraft:iron_ingot",Count:1b},{Slot:1b,id:"minecraft:end_crystal",Count:1b},{Slot:2b,id:"minecraft:iron_ingot",Count:1b},{Slot:3b,id:"minecraft:iron_ingot",Count:1b},{Slot:4b,id:"minecraft:clock",Count:1b},{Slot:5b,id:"minecraft:iron_ingot",Count:1b},{Slot:7b,id:"minecraft:stick",Count:1b}]} run function blade_of_shade:c.crafter/loki_the_trickster

#スニーク処理
scoreboard players add @a[nbt={SelectedItem:{tag:{ItemName:Loki_the_Trickster}}},scores={Loki_Sneak=1..}] Loki_Charge 1
scoreboard players reset @a[scores={Loki_Sneak=0}] Loki_Charge
scoreboard players set @a[scores={Loki_Sneak=1..}] Loki_Sneak 0
scoreboard players reset @a[nbt=!{SelectedItem:{tag:{ItemName:Loki_the_Trickster}}}] Loki_Charge

#満腹ゲージをスコア化
execute as @a store result score @s FoodLevel run data get entity @s foodLevel
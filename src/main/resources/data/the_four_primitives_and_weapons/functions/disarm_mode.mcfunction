# Enter the function code here
#タツマキ発動
execute if entity @s[scores={Loki_Charge=10..,Loki_Sneak=0,FoodLevel=6..}] run playsound minecraft:entity.bat.takeoff neutral @a ~ ~ ~ 2 1
execute if entity @s[scores={Loki_Charge=10..,Loki_Sneak=0,FoodLevel=6..}] run playsound minecraft:entity.wither.shoot player @a ~ ~ ~ 2 2
execute if entity @s[scores={Loki_Charge=10..,Loki_Sneak=0,FoodLevel=6..}] positioned ~ ~1 ~ run summon minecraft:armor_stand ^ ^ ^1 {Small:1b,Invisible:1b,Tags:["Loki_Disarm","Loki_Disarm0","NeedID"]}
effect give @s[scores={Loki_Charge=10..,Loki_Sneak=0,FoodLevel=6..}] minecraft:hunger 1 200 true

#空腹で発動失敗
execute if entity @s[scores={Loki_Charge=10..,Loki_Sneak=0,FoodLevel=..5}] run playsound minecraft:block.note_block.bass player @a ~ ~ ~ 1.5 0

#チャージ維持
execute if entity @s[scores={Loki_Charge=10}] run playsound minecraft:entity.guardian.death player @a ~ ~ ~ 1.5 2
execute if entity @s[scores={Loki_Charge=10}] run playsound minecraft:entity.experience_orb.pickup player @a ~ ~ ~ 1.5 1
execute if entity @s[scores={Loki_Charge=10..}] run particle minecraft:smoke ~ ~1 ~ 0.25 0.5 0.25 0 2

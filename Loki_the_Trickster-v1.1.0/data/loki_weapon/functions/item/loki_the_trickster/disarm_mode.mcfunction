# Disarmモード - 武装解除Wind発射

# Disarm Wind発射 (チャージ10tick以上、満腹度6以上)
execute if entity @s[scores={Loki_Charge=10..,Loki_Sneak=0,FoodLevel=6..}] run playsound minecraft:entity.bat.takeoff neutral @a ~ ~ ~ 2 1
execute if entity @s[scores={Loki_Charge=10..,Loki_Sneak=0,FoodLevel=6..}] run playsound minecraft:entity.wither.shoot player @a ~ ~ ~ 2 2
execute if entity @s[scores={Loki_Charge=10..,Loki_Sneak=0,FoodLevel=6..}] positioned ~ ~1 ~ run summon minecraft:armor_stand ^ ^ ^1 {Small:1b,Invisible:1b,Tags:["Loki_Disarm","Loki_Disarm0","NeedID"]}
effect give @s[scores={Loki_Charge=10..,Loki_Sneak=0,FoodLevel=6..}] minecraft:hunger 1 200 true

# 満腹度不足で失敗
execute if entity @s[scores={Loki_Charge=10..,Loki_Sneak=0,FoodLevel=..5}] run playsound minecraft:block.note_block.bass player @a ~ ~ ~ 1.5 0

# チャージ完了音
execute if entity @s[scores={Loki_Charge=10}] run playsound minecraft:entity.guardian.death player @a ~ ~ ~ 1.5 2
execute if entity @s[scores={Loki_Charge=10}] run playsound minecraft:entity.experience_orb.pickup player @a ~ ~ ~ 1.5 1
execute if entity @s[scores={Loki_Charge=10..}] run particle minecraft:smoke ~ ~1 ~ 0.25 0.5 0.25 0 2

# モードチェンジ (短押し10tick以下でスニーク解除)
execute if entity @s[scores={Loki_Charge=0..10,Loki_Sneak=0}] run playsound minecraft:ui.button.click player @a ~ ~ ~ 1 2
execute if entity @s[scores={Loki_Charge=0..10,Loki_Sneak=0}] run item replace entity @s weapon.mainhand with minecraft:iron_sword{display:{Name:'{"text":"Loki the Trickster <Decoy>","color":"gold","italic":false}',Lore:['{"text":" "}','{"text":"[Tap Sneak: Mode Change]","color":"white","italic":false}','{"text":"[Hold Sneak: Create Decoy]","color":"white","italic":false}','{"text":" "}','{"text":"敵を欺き、翻弄せよ。","color":"white","italic":false}','{"text":" "}','{"text":"Damage +6","color":"blue","italic":false}']},HideFlags:3,Unbreakable:1b,ItemMode:Loki_Decoy,ItemName:Loki_the_Trickster,Enchantments:[{id:"minecraft:unbreaking",lvl:0s}],AttributeModifiers:[{AttributeName:"generic.attack_damage",Name:"generic.attack_damage",Amount:5.5d,Operation:0,UUID:[I; 0, 48839, 0, 283256],Slot:"mainhand"},{AttributeName:"generic.attack_speed",Name:"generic.attack_speed",Amount:-2,Operation:0,UUID:[I;1774358129,-1670168079,-1476587264,259033975],Slot:"mainhand"}]} 1
scoreboard players reset @s[scores={Loki_Charge=0..10,Loki_Sneak=0}] Loki_Charge

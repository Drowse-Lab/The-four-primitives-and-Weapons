
#デコイボールチャージ
execute if entity @s[scores={Loki_Charge=20..,Loki_Sneak=0,FoodLevel=6..}] run playsound minecraft:entity.arrow.shoot player @a ~ ~ ~ 1 0
execute if entity @s[scores={Loki_Charge=20..,Loki_Sneak=0,FoodLevel=6..}] run summon minecraft:armor_stand ~ ~1.67 ~ {Small:1b,Invisible:1b,Tags:["Loki_Decoy_Shoot","Loki_Decoy_Ball"],DisabledSlots:4144959}
execute if entity @s[scores={Loki_Charge=20..,Loki_Sneak=0,FoodLevel=6..}] positioned ~ ~1.67 ~ run summon area_effect_cloud ^ ^ ^0.1 {Duration:1,Radius:0f,Tags:[pos]}
effect give @s[scores={Loki_Charge=20..,Loki_Sneak=0,FoodLevel=6..}] minecraft:hunger 1 200 true

#空腹で発動失敗
execute if entity @s[scores={Loki_Charge=20..,Loki_Sneak=0,FoodLevel=..5}] run playsound minecraft:block.note_block.bass player @a ~ ~ ~ 1.5 0

#チャージ維持
execute if entity @s[scores={Loki_Charge=20}] run playsound minecraft:entity.guardian.death player @a ~ ~ ~ 1.5 2
execute if entity @s[scores={Loki_Charge=20}] run playsound minecraft:entity.experience_orb.pickup player @a ~ ~ ~ 1.5 1
execute if entity @s[scores={Loki_Charge=20..}] run particle minecraft:smoke ~ ~1 ~ 0.25 0.5 0.25 0 2

#モードチェンジ
execute if entity @s[scores={Loki_Charge=0..10,Loki_Sneak=0}] run playsound minecraft:ui.button.click player @a ~ ~ ~ 1 2
execute if entity @s[scores={Loki_Charge=0..10,Loki_Sneak=0}] run replaceitem entity @s weapon.mainhand minecraft:iron_sword{display:{Name:'{"text":"Loki the Trickster <Disarm>","color":"gold","italic":false}',Lore:['{"text":" "}','{"text":"[Tap Sneak: Mode Change]","color":"white","italic":false}','{"text":"[Hold Sneak: Disarm Wind]","color":"white","italic":false}','{"text":" ","color":"blue"}','{"text":"敵を欺き、翻弄せよ。","color":"white","italic":false}','{"text":" "}','{"text":"Damage +6","color":"blue","italic":false}']},HideFlags:3,Unbreakable:1b,ItemMode:Loki_Disarm,ItemName:Loki_the_Trickster,Enchantments:[{id:"minecraft:unbreaking",lvl:0s}],AttributeModifiers:[{AttributeName:"generic.attack_damage",Name:"generic.attack_damage",Amount:5.5d,Operation:0,UUID:[I; 0, 48839, 0, 283256],Slot:"mainhand"},{AttributeName:"generic.attack_speed",Name:"generic.attack_speed",Amount:-2,Operation:0,UUID:[I;1774358129,-1670168079,-1476587264,259033975],Slot:"mainhand"}]} 1
scoreboard players reset @s[scores={Loki_Charge=0..10,Loki_Sneak=0}] Loki_Charge

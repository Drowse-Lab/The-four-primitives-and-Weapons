# 武装解除処理

# 一時アイテムエンティティを作成
summon item ~ ~ ~ {PickupDelay:10s,Tags:["DisarmedItem"],Item:{id:"minecraft:structure_void",Count:1b}}

# NPCの場合はHandItems[0]から取得
execute if entity @s[type=!player] run data modify entity @e[tag=DisarmedItem,limit=1,sort=nearest] Item set from entity @s HandItems[0]

# プレイヤーの場合はSelectedItemから取得
execute if entity @s[type=player] run data modify entity @e[tag=DisarmedItem,limit=1,sort=nearest] Item set from entity @s SelectedItem

# メインハンドを空にする
item replace entity @s weapon.mainhand with air

# 空のアイテムは削除
kill @e[limit=1,sort=nearest,tag=DisarmedItem,nbt={Item:{id:"minecraft:structure_void",Count:1b}}]

# サウンド再生
playsound minecraft:entity.item.break neutral @a ~ ~ ~ 1 1

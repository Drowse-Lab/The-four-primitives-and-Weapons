# make_friendly.mcfunction

# すべてのゾンビのターゲットをリセット
execute as @e[type=zombie,tag=friendly] run data modify entity @s Brain.memories.minecraft:anger_time set value 0
execute as @e[type=zombie,tag=friendly] run data remove entity @s Brain.memories.minecraft:angry_at

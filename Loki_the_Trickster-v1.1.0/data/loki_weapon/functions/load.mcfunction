# Loki the Trickster - Load Function
# スコアボード初期化

scoreboard objectives add Loki_Charge dummy
scoreboard objectives add Loki_Sneak minecraft.custom:minecraft.sneak_time
scoreboard objectives add FoodLevel dummy
scoreboard objectives add BulletRemain dummy
scoreboard objectives add Motion_Speed dummy
scoreboard objectives add Motion_Power dummy
scoreboard objectives add PlayerID dummy
scoreboard objectives add ScoreID2 dummy
scoreboard objectives add Decoy_Action dummy
scoreboard objectives add Decoy_Spin dummy

tellraw @a {"text":"[Loki the Trickster] Datapack loaded successfully!","color":"gold"}

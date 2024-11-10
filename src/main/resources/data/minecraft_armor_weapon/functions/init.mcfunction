# 初期化: 名前が「Friendly」のゾンビに"friendly" タグを付与
tag @e[type=zombie, name=Friendly] add friendly

# 毎tickで "make_friendly" を実行して味方ゾンビのターゲットをリセット
function minecraftarmorweapon:make_friendly
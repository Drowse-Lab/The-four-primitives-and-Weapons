#リソースパックを導入したのちに実行するといろんなことが出来るコマンドたち。

#・カスタムアイコンをどん、と画面に表示

/title @p title {"text":"\uE010\uE011\uE012\uE013\uE014\uE015\uE016\uE017\uE018\uE019\uE01A"}

#・チェストを使ったカスタムGUI

/give @p chest{display:{Name:'[{"text":"\\uF808\\uE200\\uF80C\\uF80A\\uF808\\uF801","color":"#FFFFFF"},{"text":"カスタムGUI","italic":"false","color":"#3F3F3F"}]'}} 1

#・「やあ。」ふきだし村人

/summon villager ~ ~ ~ {Passengers:[{id:"minecraft:armor_stand",CustomNameVisible:1b,Invulnerable:1b,Small:1b,Invisible:1b,CustomName:'{"text":"\\uE003"}'}],CustomName:'{"text":"村人A"}'}

#・暗転　　/title @p time 5 50 5 (左のコマンドと併せて使えると良いかも。)

/title @p title {"text":"\uE001"}

# ・詳細なページを持つ本（textures/font/book_area128やbook_area256.pngをいじれば自分だけのものも作れるよ。）

/give @p written_book{title:"",author:"",pages:['{"text":"\\uE102","color":white}']}

#・出血ぽいパーティクル
/particle item redstone ~ ~1 ~ 0.5 0.5 0.5 0.3

;; ======================================================================
;; KnifeLauncher の計算値を Lisp で定義する。
;; 起動時に一度だけ評価され、結果はキャッシュされる。
;; リソースパックで上書きすることで Java 再コンパイル無しに調整可能。
;; ======================================================================

;; --- モード別の最大投擲数 (本) ---
(define max-normal 10)
(define max-stun   3)
(define max-screw  5)

;; --- モード別のクールダウン (tick, 20tick=1秒) ---
(define cooldown-normal 8)
(define cooldown-stun   16)
(define cooldown-screw  10)

;; --- モード別の 1 本あたりの MP コスト ---
(define mana-normal 0)
(define mana-stun   25)
(define mana-screw  10)

;; --- スポーン位置のランダムオフセット最大 (ブロック単位) ---
;; プレイヤー視線に垂直な方向に左右/上下へこの値×±1 ぶんずれる
(define spawn-offset-h 1.2)
(define spawn-offset-v 0.6)

;; --- 投射初速 (shootFromRotation の velocity 引数) ---
(define shoot-velocity 1.6)

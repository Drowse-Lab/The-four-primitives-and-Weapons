;; ============================================
;; angel_trio.lisp — 天使の三人組AI
;; ============================================
;;
;; 基本的に攻撃しない。プレイヤーを観察する。
;; 怒り状態になると天使級の戦闘力で攻撃。
;; 回避99%。テレポート回避。

(let (close      3
      mid        10
      far        20
      has-skill  can-skill)

  (if (not has-target)
    ;; ターゲットなし → のんびり
    (idle)

    ;; === 怒り状態でないなら攻撃しない ===
    ;; （怒り判定はJava側で制御。ここに来る時は怒り状態）

    ;; 近接圏 → フルコンボ
    (if (< distance close)
      (if can-attack
        ;; プレイヤー対策
        (if target-is-player
          (seq
            (if (and player-is-aggressive can-block)
              (seq (parry) (combo-attack 16))
              (if (> player-hitrun-rate 0.5)
                (seq (skill "ice") (charge-attack))
                (if (> player-shield-rate 0.4)
                  (seq (skill "dark") (combo-attack 12))
                  (combo-attack 16)))))
          ;; Mob相手
          (combo-attack 16))
        ;; 攻撃クールダウン中
        (if (and can-dodge (chance 0.99))
          (dodge)
          (wait 1)))

      ;; 中距離 → 属性攻撃で先制
      (if (< distance mid)
        (if (and has-skill (chance 0.6))
          (if (chance 0.5) (skill "holy") (skill "lightning"))
          (charge-attack))

        ;; 遠距離 → 接近
        (approach)))))

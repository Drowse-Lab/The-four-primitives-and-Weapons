;; ============================================
;; elite_soldier.lisp — 精鋭兵AI（ティア2）
;; ============================================
;;
;; ダッシュ攻撃と回避を使いこなす。回避率50%。
;; プレイヤーの盾・ヒット＆ランに対応。

(let (close   3
      mid     8
      dash    13
      low-hp  (< health-ratio 0.3))

  (if (not has-target)
    (idle)

    (if low-hp
      (if can-dodge (dodge) (retreat))

      ;; 近接圏
      (if (< distance close)
        (if can-attack
          ;; コンボ可能なら3連撃
          (if (and (>= ai-level 4) (chance 0.25))
            (combo-attack 3)
            (attack))
          ;; 回避50%
          (if (and can-dodge (chance 0.5))
            (dodge)
            (wait 3)))

        ;; 中距離 → チャージ or ダッシュ
        (if (< distance mid)
          (if (and can-attack (chance 0.4))
            (charge-attack)

            ;; プレイヤー学習: ヒット＆ラン対策
            (if (and target-is-player (> player-hitrun-rate 0.5))
              (charge-attack)
              (approach)))

          ;; ダッシュ距離
          (if (< distance dash)
            ;; 盾多用プレイヤー → 回り込んでからダッシュ
            (if (and target-is-player (> player-shield-rate 0.4)
                     (>= ai-level 3))
              (seq (circle-strafe) (charge-attack))
              (approach))

            ;; 遠距離
            (approach)))))))

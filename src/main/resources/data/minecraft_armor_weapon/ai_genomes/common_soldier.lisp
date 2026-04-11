;; ============================================
;; common_soldier.lisp — 一般兵AI（ティア1）
;; ============================================
;;
;; 基本的な近接戦闘。回避率30%。
;; 学習能力は低いが、基本の攻防はできる。

(let (close   3
      mid     7
      low-hp  (< health-ratio 0.3))

  (if (not has-target)
    (idle)

    ;; 瀕死 → 撤退
    (if low-hp
      (retreat)

      ;; 近接圏
      (if (< distance close)
        (if can-attack
          (attack)
          ;; 低確率で回避
          (if (and can-dodge (chance 0.3))
            (dodge)
            (wait 5)))

        ;; 中距離
        (if (< distance mid)
          ;; プレイヤーの攻撃的な癖には少し対応
          (if (and target-is-player player-is-aggressive
                   (>= ai-level 2) can-dodge (chance 0.2))
            (dodge)
            (approach))

          ;; 遠距離
          (approach))))))

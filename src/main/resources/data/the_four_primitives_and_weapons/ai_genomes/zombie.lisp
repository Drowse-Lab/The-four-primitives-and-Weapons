;; ============================================
;; zombie.lisp — ゾンビ系AI
;; 適用: zombie, husk, drowned, wither_skeleton
;; ============================================
;;
;; 近接戦闘メイン。
;; プレイヤーの癖を読んで対策する。
;; HPが減ると狂暴化。

(let (close    3
      mid      8
      far      16
      berserk  (< health-ratio 0.3))

  (if (not has-target)
    (idle)

    ;; === 近接圏 ===
    (if (< distance close)
      (if berserk
        ;; 瀕死 → 捨て身の連撃
        (if can-attack
          (if (>= ai-level 4) (combo-attack 4) (attack))
          (attack))

        ;; -- プレイヤー学習対策 --
        (if (and target-is-player (>= ai-level 3))

          ;; ヒット＆ラン型 → 攻撃後に追撃
          (if (and (> player-hitrun-rate 0.5) can-attack)
            (seq (attack) (approach))

            ;; 盾多用 → 回り込み
            (if (and (> player-shield-rate 0.4) (>= ai-level 5))
              (circle-strafe)

              ;; 攻撃的 → 回避してから反撃
              (if (and player-is-aggressive can-dodge (chance 0.4))
                (dodge)

                ;; 通常攻撃
                (if can-attack
                  (attack)
                  (wait 5)))))

          ;; -- 低レベル or Mob相手 --
          (if can-attack
            (attack)
            (if (and can-dodge (>= ai-level 2) (chance 0.3))
              (dodge)
              (wait 5)))))

      ;; === 中距離 ===
      (if (< distance mid)
        (if (and (>= ai-level 3) can-attack (chance 0.4))
          (charge-attack)

          ;; 遠距離好きプレイヤー → 急いで詰める
          (if (and target-is-player player-prefers-long)
            (approach)

            ;; 盾構え → 回り込み
            (if (and target-is-blocking (>= ai-level 5))
              (circle-strafe)
              (approach))))

        ;; === 遠距離 ===
        (if (< distance far)
          (approach)
          (idle))))))

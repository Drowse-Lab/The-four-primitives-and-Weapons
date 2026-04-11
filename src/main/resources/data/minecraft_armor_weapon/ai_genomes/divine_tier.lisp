;; ============================================
;; divine_tier.lisp — 神聖級AI（ティア7 / 最強）
;; ============================================
;;
;; 20段コンボ。回避率99%。ダメージ5.0倍。
;; 全属性完全マスター: 火/氷/雷/聖/闇。
;; プレイヤーの全行動パターンを完全に把握し、
;; あらゆる状況で最適解を即座に実行する。
;; 事実上、倒すことが不可能に近い存在。

(let (close      3
      mid        15
      far        25
      god-range  30
      critical   (< health-ratio 0.05)
      low-hp     (< health-ratio 0.15)
      has-skill  can-skill
      many-foes  (> nearby-enemy-count 3)
      swarm      (> nearby-enemy-count 6)
      horde      (> nearby-enemy-count 10))

  (if (not has-target)
    (idle)

    ;; 瀕死 → 聖属性全回復 + 闇で空間制圧
    (if critical
      (if has-skill
        (seq (skill "holy") (skill "dark") (dodge))
        (if can-dodge (seq (dodge) (dodge) (retreat)) (retreat)))

      ;; HP低い → 属性連打で制圧しつつ回復
      (if low-hp
        (if has-skill
          (seq (skill "holy")
               (if (< distance close)
                 (seq (dodge) (skill "ice"))
                 (retreat)))
          (if (and can-dodge (chance 0.99)) (dodge) (retreat)))

        ;; === 近接圏 ===
        (if (< distance close)

          ;; 大群 → 属性連鎖で殲滅
          (if horde
            (if has-skill
              (seq (skill "fire") (skill "lightning") (skill "dark"))
              (combo-attack 20))

            (if swarm
              (if has-skill
                (seq (skill "fire") (skill "lightning"))
                (combo-attack 20))

              ;; 複数敵 → 範囲攻撃
              (if (and many-foes has-skill)
                (seq (skill "fire") (combo-attack 10))

                ;; === プレイヤー完全攻略 ===
                (if target-is-player
                  (seq
                    ;; 全パターンを同時に判定して最適解を即座に実行

                    ;; 攻撃的 + スプリント → パリィ→20連コンボ→属性追撃
                    (if (and player-is-aggressive (> player-sprint-rate 0.5))
                      (if can-block
                        (seq (parry) (combo-attack 20) (if has-skill (skill "fire") (wait 1)))
                        (seq (dodge) (combo-attack 20)))

                      ;; 攻撃的 → 回避→反撃→属性
                      (if player-is-aggressive
                        (seq (dodge) (combo-attack 12)
                             (if has-skill (skill "lightning") (wait 1)))

                        ;; ヒット＆ラン → 先読みチャージ + 氷で拘束
                        (if (> player-hitrun-rate 0.5)
                          (if has-skill
                            (seq (skill "ice") (charge-attack) (combo-attack 8))
                            (seq (charge-attack) (combo-attack 8)))

                          ;; 盾多用 → 闇属性でガード貫通 + コンボ
                          (if (> player-shield-rate 0.4)
                            (if has-skill
                              (seq (skill "dark") (combo-attack 16))
                              (seq (circle-strafe) (combo-attack 12)))

                            ;; 側面攻撃 → 即回避→背後取り→コンボ
                            (if (> player-flank-rate 0.5)
                              (seq (dodge) (circle-strafe)
                                   (if can-attack (combo-attack 12) (dodge)))

                              ;; 遠距離好き → 瞬間接近→コンボ
                              (if player-prefers-long
                                (seq (charge-attack)
                                     (if can-attack (combo-attack 16) (approach)))

                                ;; 通常 → 20連コンボ
                                (if can-attack
                                  (combo-attack 20)
                                  (if (and can-dodge (chance 0.99))
                                    (dodge)
                                    (wait 1))))))))))

                  ;; Mob相手 → 20連コンボ + 属性
                  (if can-attack
                    (seq (combo-attack 20)
                         (if has-skill (skill "fire") (wait 1)))
                    (if (and can-dodge (chance 0.99)) (dodge) (wait 1)))))))

          ;; === 中距離 ===
          (if (< distance mid)
            ;; 属性連鎖で先制
            (if (and has-skill (chance 0.7))
              (if (chance 0.2) (skill "fire")
                (if (chance 0.25) (skill "ice")
                  (if (chance 0.33) (skill "lightning")
                    (if (chance 0.5) (skill "holy") (skill "dark")))))
              ;; 瞬間チャージ突進
              (if can-attack
                (charge-attack)
                (approach)))

            ;; === 遠距離 ===
            (if (< distance god-range)
              ;; 神級の遠距離属性攻撃
              (if (and has-skill (chance 0.6))
                (if (chance 0.5) (skill "holy") (skill "lightning"))
                (approach))
              (approach))))))))

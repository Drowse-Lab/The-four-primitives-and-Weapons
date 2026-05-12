;; ============================================
;; angel_tier.lisp — 天使級AI（ティア6）
;; ============================================
;;
;; 16段コンボ。回避率95%。ダメージ3.0倍。
;; 全属性攻撃: 火/氷/雷/聖/闇。
;; プレイヤーの行動を2手先まで読む。
;; 攻撃と防御を同時にこなす神業級の戦闘。

(let (close      3
      mid        10
      far        20
      skill-far  25
      critical   (< health-ratio 0.08)
      low-hp     (< health-ratio 0.2)
      has-skill  can-skill
      many-foes  (> nearby-enemy-count 3)
      swarm      (> nearby-enemy-count 6))

  (if (not has-target)
    (idle)

    ;; 瀕死 → 闇属性で空間制圧しつつ離脱
    (if critical
      (if (and has-skill (chance 0.8))
        (seq (skill "dark") (dodge) (retreat))
        (if can-dodge (seq (dodge) (retreat)) (retreat)))

      ;; HP低い → 聖属性で回復しつつ戦闘継続
      (if low-hp
        (if (and has-skill (chance 0.5))
          (seq (skill "holy") (if can-dodge (dodge) (retreat)))
          (if (< distance close)
            ;; 近接でも回避しながら戦える
            (if (and can-dodge (chance 0.95))
              (seq (dodge) (if can-attack (combo-attack 8) (retreat)))
              (retreat))
            (retreat)))

        ;; === 近接圏 ===
        (if (< distance close)

          ;; 大群 → 範囲属性攻撃の連続
          (if swarm
            (if has-skill
              (seq (skill "fire") (skill "lightning"))
              (combo-attack 16))

            ;; 多数の敵 → 範囲攻撃
            (if (and many-foes has-skill)
              (skill "fire")

              ;; プレイヤー完全対策 — 2手先を読む
              (if target-is-player
                (seq
                  ;; 攻撃的 + スプリント多用 → 待ち構えてパリィ→16連
                  (if (and player-is-aggressive (> player-sprint-rate 0.5) can-block)
                    (seq (parry) (if can-attack (combo-attack 16) (wait 1)))

                    ;; 攻撃的だけど慎重 → 回避からの反撃
                    (if (and player-is-aggressive can-dodge)
                      (seq (dodge) (if can-attack (combo-attack 8) (approach)))

                      ;; ヒット＆ラン → チャージで追いかけ→属性攻撃
                      (if (> player-hitrun-rate 0.5)
                        (if has-skill
                          (seq (charge-attack) (skill "ice"))
                          (charge-attack))

                        ;; 盾多用 → 闇属性でガード無視
                        (if (and (> player-shield-rate 0.4) has-skill)
                          (seq (skill "dark") (combo-attack 8))

                          ;; 側面攻撃多い → 背後を取り返す
                          (if (> player-flank-rate 0.5)
                            (seq (circle-strafe) (circle-strafe)
                                 (if can-attack (combo-attack 8) (dodge)))

                            ;; 通常 → 16連コンボ
                            (if can-attack
                              (combo-attack 16)
                              (if (and can-dodge (chance 0.95))
                                (dodge)
                                (wait 1)))))))))

                ;; Mob相手 → 16連コンボ
                (if can-attack
                  (combo-attack 16)
                  (if (and can-dodge (chance 0.95)) (dodge) (wait 1))))))

          ;; === 中距離 ===
          (if (< distance mid)
            ;; 属性攻撃で先制（ランダム属性）
            (if (and has-skill (chance 0.6))
              (if (chance 0.25) (skill "fire")
                (if (chance 0.33) (skill "ice")
                  (if (chance 0.5) (skill "lightning") (skill "holy"))))
              ;; チャージ
              (if (and can-attack (chance 0.8))
                (charge-attack)
                (approach)))

            ;; === 遠距離 ===
            (if (< distance skill-far)
              ;; 遠距離属性攻撃
              (if (and has-skill (chance 0.5))
                (if (chance 0.5) (skill "holy") (skill "lightning"))
                (approach))
              (approach))))))))

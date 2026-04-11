;; ============================================
;; mythical_tier.lisp — 神話級AI（ティア5）
;; ============================================
;;
;; 12段コンボ。回避率90%。ダメージ2.5倍。
;; 属性攻撃: 火/氷/雷/聖。
;; プレイヤーの動きを高精度に読み、先読み行動。
;; 複数の敵にも対応し、最適なターゲットを選ぶ。

(let (close      3
      mid        8
      far        16
      skill-far  20
      critical   (< health-ratio 0.1)
      low-hp     (< health-ratio 0.25)
      has-skill  can-skill
      many-foes  (> nearby-enemy-count 3))

  (if (not has-target)
    (idle)

    ;; 瀕死 → 属性スキルで牽制しつつ撤退
    (if critical
      (if (and has-skill (chance 0.6))
        (seq (skill "holy") (retreat))
        (if can-dodge (dodge) (retreat)))

      ;; HP低い → 距離を取りつつ属性攻撃
      (if low-hp
        (if (< distance mid)
          (if can-dodge (dodge) (retreat))
          (if (and has-skill (chance 0.4))
            (skill "ice")
            (retreat)))

        ;; === 近接圏 ===
        (if (< distance close)

          ;; 多数の敵 → 範囲属性攻撃
          (if (and many-foes has-skill)
            (skill "fire")

            ;; プレイヤー完全対策
            (if target-is-player
              (seq
                ;; 攻撃的 → パリィからの12連コンボ
                (if (and player-is-aggressive can-block)
                  (seq (parry) (if can-attack (combo-attack 12) (wait 2)))

                  ;; ヒット＆ラン → 先読みチャージ
                  (if (> player-hitrun-rate 0.5)
                    (if can-attack (charge-attack) (approach))

                    ;; 盾多用 → 属性攻撃で崩す
                    (if (and (> player-shield-rate 0.4) has-skill)
                      (seq (skill "lightning") (circle-strafe))

                      ;; 側面攻撃 → 回避→反撃コンボ
                      (if (and (> player-flank-rate 0.5) can-dodge)
                        (seq (dodge) (if can-attack (combo-attack 6) (approach)))

                        ;; 通常 → フルコンボ
                        (if can-attack
                          (combo-attack 12)
                          (if (and can-dodge (chance 0.9))
                            (dodge)
                            (wait 2))))))))

              ;; Mob相手 → フルコンボ
              (if can-attack
                (combo-attack 12)
                (if (and can-dodge (chance 0.9)) (dodge) (wait 2)))))

          ;; === 中距離 ===
          (if (< distance mid)
            ;; 属性攻撃で先制
            (if (and has-skill (chance 0.5))
              (if (chance 0.33) (skill "fire")
                (if (chance 0.5) (skill "ice") (skill "lightning")))
              ;; チャージ突進
              (if (and can-attack (chance 0.7))
                (charge-attack)
                ;; 遠距離好きプレイヤー → 高速接近
                (if (and target-is-player player-prefers-long)
                  (charge-attack)
                  (approach))))

            ;; === 遠距離 ===
            (if (< distance skill-far)
              ;; 聖属性で遠距離攻撃
              (if (and has-skill (chance 0.4))
                (skill "holy")
                (approach))
              (approach))))))))

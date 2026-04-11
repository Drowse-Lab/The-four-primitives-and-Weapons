;; ============================================
;; heroic_tier.lisp — 英雄級AI（ティア4）
;; ============================================
;;
;; 最強の戦闘AI。8段コンボ。回避率80%。
;; 全属性攻撃（火/氷/雷）。2.0倍ダメージ。
;; プレイヤーの癖を完全分析して最適対策を取る。

(let (close      3
      mid        8
      far        16
      critical   (< health-ratio 0.15)
      low-hp     (< health-ratio 0.3)
      has-skill  (and (>= ai-level 4) can-skill)
      high-lv    (>= ai-level 7))

  (if (not has-target)
    (idle)

    ;; 瀕死 → 戦術的撤退
    (if critical
      (if (and has-skill (chance 0.5))
        ;; スキルで牽制しながら撤退
        (seq (skill "ice") (retreat))
        (if can-dodge (dodge) (retreat)))

      ;; HP低い → 慎重に
      (if low-hp
        (if (and can-dodge (chance 0.6))
          (dodge)
          (retreat))

        ;; === 近接圏 ===
        (if (< distance close)
          (if (and target-is-player (>= ai-level 5))

            ;; --- プレイヤー完全対策 ---

            ;; 攻撃的プレイヤー → パリィ→カウンター
            (if (and player-is-aggressive can-block)
              (parry)

              ;; ヒット＆ラン → 追撃チャージ
              (if (and (> player-hitrun-rate 0.5) can-attack)
                (charge-attack)

                ;; 盾多用 → 回り込み→コンボ
                (if (> player-shield-rate 0.4)
                  (seq (circle-strafe)
                       (if can-attack (combo-attack 5) (wait 3)))

                  ;; 側面攻撃多い → 回避→反撃
                  (if (and (> player-flank-rate 0.5) can-dodge)
                    (seq (dodge)
                         (if can-attack (combo-attack 3) (approach)))

                    ;; スプリント多用 → 待ち構え→パリィ
                    (if (and (> player-sprint-rate 0.6) can-block)
                      (block-shield)

                      ;; 通常 → 最大コンボ
                      (if can-attack
                        (if high-lv
                          (combo-attack 8)
                          (combo-attack 5))
                        (if (and can-dodge (chance 0.8))
                          (dodge)
                          (wait 2))))))))

            ;; Mob相手 → フルコンボ
            (if can-attack
              (if high-lv (combo-attack 8) (combo-attack 5))
              (if (and can-dodge (chance 0.8)) (dodge) (wait 2))))

          ;; === 中距離 ===
          (if (< distance mid)
            ;; スキルで先制
            (if (and has-skill (chance 0.4))
              (if (chance 0.5)
                (skill "fire")
                (skill "lightning"))

              ;; チャージ突進
              (if (and can-attack (chance 0.6))
                (charge-attack)

                ;; 遠距離好きプレイヤー → 一気に詰める
                (if (and target-is-player player-prefers-long)
                  (charge-attack)
                  (approach))))

            ;; === 遠距離 ===
            (if (< distance far)
              (if (and has-skill (chance 0.3))
                (skill "ranged")
                (approach))
              (approach))))))))

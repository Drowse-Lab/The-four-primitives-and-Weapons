;; ============================================
;; soldier.lisp — 兵Mob共通AI
;; 適用: common_soldier, elite_soldier,
;;       singularity, heroic_tier
;; ============================================
;;
;; プレイヤーの戦闘パターンを学習して対策する。
;; player-* 変数にプレイヤーの癖が入っている。

(let (close        3
      mid          8
      far          16
      low-hp       (< health-ratio 0.3)
      critical-hp  (< health-ratio 0.15)
      enemy-shield (and target-is-blocking (>= ai-level 5))
      high-level   (>= ai-level 6))

  (if (not has-target)
    (idle)

    ;; === 瀕死 → 撤退判断 ===
    (if critical-hp
      (if can-dodge (dodge) (retreat))

      ;; === 近接圏 ===
      (if (< distance close)

        ;; -- プレイヤー学習対策 --
        (if (and target-is-player (>= ai-level 4))

          ;; ヒット＆ラン型プレイヤー → 追い詰める
          (if (and (> player-hitrun-rate 0.5) can-attack)
            (seq (charge-attack))

            ;; 攻撃的プレイヤー → カウンター狙い
            (if (and player-is-aggressive (>= ai-level 5) can-block)
              (parry)

              ;; 盾多用プレイヤー → 回り込み
              (if (and (> player-shield-rate 0.4) (>= ai-level 5))
                (circle-strafe)

                ;; 側面攻撃してくるプレイヤー → 回避
                (if (and (> player-flank-rate 0.5) can-dodge)
                  (dodge)

                  ;; 通常攻撃
                  (if can-attack
                    (if (and (>= ai-level 7) (chance 0.3))
                      (combo-attack 5)
                      (if (and (>= ai-level 4) (chance 0.25))
                        (combo-attack 3)
                        (attack)))
                    (wait 3))))))

          ;; -- Mob相手 or 低レベル → 通常戦闘 --
          (if enemy-shield
            (seq (circle-strafe)
                 (if can-attack (attack) (wait 5)))
            (if can-attack
              (attack)
              (if (and can-dodge (chance 0.4))
                (dodge)
                (wait 3)))))

        ;; === 中距離 ===
        (if (< distance mid)
          (if low-hp
            (if can-dodge (dodge) (retreat))

            ;; 遠距離好きプレイヤー → 間合いを詰める
            (if (and target-is-player player-prefers-long (>= ai-level 4))
              (if (and (>= ai-level 3) can-attack (chance 0.6))
                (charge-attack)
                (approach))

              ;; スプリント多用プレイヤー → 待ち構える
              (if (and target-is-player (> player-sprint-rate 0.6)
                       (>= ai-level 5) can-block)
                (block-shield)

                ;; 通常接近
                (if (and (>= ai-level 3) can-attack (chance 0.5))
                  (charge-attack)
                  (approach)))))

          ;; === 遠距離 ===
          (if (< distance far)
            (if (and high-level can-skill (chance 0.2))
              (skill "ranged")
              (approach))
            (approach)))))))

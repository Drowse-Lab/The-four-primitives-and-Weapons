;; ============================================
;; spider.lisp — スパイダー系AI
;; 適用: spider, cave_spider
;; ============================================
;;
;; 飛びかかり攻撃が得意。
;; プレイヤーの回避パターンを見て跳ぶタイミングを変える。

(let (bite-range   3
      leap-range   8
      pack-range   12
      many-allies  (> nearby-enemy-count 2))

  (if (not has-target)
    (idle)

    ;; === 噛みつき圏 ===
    (if (< distance bite-range)
      (if can-attack
        (attack)
        ;; 攻撃後に離れるプレイヤー → 追撃
        (if (and target-is-player (> player-hitrun-rate 0.5)
                 (>= ai-level 4))
          (approach)
          ;; 少し離れて再突入
          (if (and (>= ai-level 4) (chance 0.3))
            (dodge)
            (wait 3))))

      ;; === 飛びかかり圏 ===
      (if (< distance leap-range)
        (if (and (>= ai-level 3) can-attack)
          ;; 盾構えてるプレイヤー → 回り込んでから跳ぶ
          (if (and target-is-player (> player-shield-rate 0.4)
                   (>= ai-level 5))
            (seq (circle-strafe) (leap))
            ;; 通常飛びかかり
            (leap))
          (approach))

        ;; === 遠距離 ===
        (if (< distance pack-range)
          ;; 仲間が多い → 包囲
          (if (and many-allies (>= ai-level 4))
            (circle-strafe)
            (approach))
          (approach))))))

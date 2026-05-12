;; ============================================
;; creeper.lisp — クリーパーAI
;; 適用: creeper
;; ============================================
;;
;; ひたすら接近して爆発。
;; レベルが上がると回り込みや待ち伏せが上手くなる。

(let (blast-range  3
      sneak-range  12
      can-see      can-see-target)

  (if (not has-target)
    (idle)

    ;; === 爆発圏 ===
    (if (< distance blast-range)
      (attack)

      ;; === 接近中 ===
      (if (< distance sneak-range)
        (if can-see
          ;; 見えている → 直進
          (approach)
          ;; 見えていない → 回り込み（高レベル）
          (if (>= ai-level 4)
            (circle-strafe)
            (approach)))

        ;; === 遠距離 ===
        (approach)))))

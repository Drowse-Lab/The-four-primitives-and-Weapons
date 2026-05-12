;; ============================================
;; skeleton.lisp — スケルトン系AI
;; 適用: skeleton, stray
;; ============================================
;;
;; 遠距離射撃メイン。
;; プレイヤーの接近パターンを学習して回避する。

(let (melee-danger  4
      shoot-range   16
      too-close     (< distance melee-danger)
      in-range      (and (< distance shoot-range) can-see-target))

  (if (not has-target)
    (idle)

    ;; === 近接圏（危険） ===
    (if too-close
      (if (and target-is-player (>= ai-level 3))

        ;; スプリント突進してくるプレイヤー → 早めに回避
        (if (and (> player-sprint-rate 0.5) can-dodge)
          (dodge)

          ;; 攻撃的プレイヤー → すぐ後退
          (if player-is-aggressive
            (retreat)

            ;; それ以外 → 回避 or 後退
            (if (and can-dodge (>= ai-level 2))
              (dodge)
              (retreat))))

        ;; 低レベル or Mob相手
        (if (and can-dodge (>= ai-level 2))
          (dodge)
          (retreat)))

    ;; === 射撃圏 ===
    (if in-range
      (if can-attack
        (ranged-attack)

        ;; クールダウン中
        (if (>= ai-level 3)
          ;; 近接好きプレイヤー → 離れながら撃つ
          (if (and target-is-player player-prefers-close)
            (retreat)
            (circle-strafe))
          (wait 5)))

      ;; === 射程外 ===
      (approach)))))

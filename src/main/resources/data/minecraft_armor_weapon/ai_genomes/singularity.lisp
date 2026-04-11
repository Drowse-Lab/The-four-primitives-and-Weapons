;; ============================================
;; singularity.lisp — 特異点AI（ティア3）
;; ============================================
;;
;; 属性攻撃（氷/雷）と高い回避率70%。
;; プレイヤーの戦闘スタイルを分析して対応。
;; 1.6倍ダメージ。

(let (close     3
      mid       8
      far       16
      low-hp    (< health-ratio 0.3)
      has-skill (and (>= ai-level 4) can-skill))

  (if (not has-target)
    (idle)

    (if low-hp
      ;; 撤退しつつスキルで牽制
      (if (and has-skill (chance 0.3))
        (skill "ice")
        (if can-dodge (dodge) (retreat)))

      ;; 近接圏
      (if (< distance close)
        (if (and target-is-player (>= ai-level 5))

          ;; プレイヤー学習対策
          (if (and player-is-aggressive can-block)
            ;; 攻撃的 → パリィ
            (parry)
            (if (and (> player-flank-rate 0.5) can-dodge)
              ;; 側面攻撃 → 回避
              (dodge)
              ;; 通常 → コンボ
              (if can-attack
                (if (chance 0.35) (combo-attack 4) (attack))
                (if (and can-dodge (chance 0.7)) (dodge) (wait 3)))))

          ;; Mob相手 or 低レベル
          (if can-attack
            (attack)
            (if (and can-dodge (chance 0.7)) (dodge) (wait 3))))

        ;; 中距離 → チャージ or スキル
        (if (< distance mid)
          (if (and has-skill (chance 0.3))
            (skill "electric")
            (if (and can-attack (chance 0.5))
              (charge-attack)
              ;; 遠距離好きプレイヤー → 急いで詰める
              (if (and target-is-player player-prefers-long)
                (seq (approach) (approach))
                (approach))))

          ;; 遠距離 → スキル or 接近
          (if (< distance far)
            (if (and has-skill (chance 0.2))
              (skill "ranged")
              (approach))
            (approach)))))))

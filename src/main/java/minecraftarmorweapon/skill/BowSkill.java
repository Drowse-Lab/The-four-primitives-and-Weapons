package minecraftarmorweapon.skill;

import minecraftarmorweapon.entity.TestBowEntity;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerPlayer;

/**
 * 弓のスキル定義。
 * INSTANT  = 左クリック即発動
 * CHARGE   = 左クリック長押し→離して発動 (チャージ量で威力変動)
 * PASSIVE  = 右クリック通常射撃にバフを付与
 */
public enum BowSkill {
    NONE         ("なし",       "§7", Type.PASSIVE, 0,   0),
    POWER_SHOT   ("集中射撃",   "§e", Type.INSTANT, 60,  1),
    EXPLOSIVE    ("爆裂矢",     "§c", Type.INSTANT, 100, 1),
    PIERCE       ("貫通矢",     "§b", Type.INSTANT, 60,  1),
    POTION_TIPPED("ポーション矢","§5", Type.INSTANT, 80,  1),
    RAPID_FIRE   ("連射",       "§a", Type.CHARGE,  80,  3),
    HOMING       ("追尾矢",     "§d", Type.CHARGE,  120, 1),
    QUICK_DRAW   ("速射",       "§f", Type.PASSIVE, 0,   0),
    HEAVY_BLOW   ("強撃",       "§6", Type.PASSIVE, 0,   0),
    WIND         ("風纏い",     "§9", Type.PASSIVE, 0,   0);

    public enum Type { INSTANT, CHARGE, PASSIVE }

    /** Arrowに付与するNBTタグキー (BowSkillArrowHandlerが見て挙動を決定) */
    public static final String NBT_EXPLOSIVE = "msw_bow_explosive";
    public static final String NBT_HOMING_POWER = "msw_bow_homing";

    public final String displayName;
    public final String colorCode;
    public final Type type;
    public final int cooldownTicks;
    public final int arrowCost;

    BowSkill(String displayName, String colorCode, Type type, int cooldownTicks, int arrowCost) {
        this.displayName = displayName;
        this.colorCode = colorCode;
        this.type = type;
        this.cooldownTicks = cooldownTicks;
        this.arrowCost = arrowCost;
    }

    public String coloredName() { return colorCode + displayName; }

    public static BowSkill byId(int id) {
        BowSkill[] vals = values();
        if (id < 0 || id >= vals.length) return NONE;
        return vals[id];
    }

    /* ===================== INSTANT / CHARGE 発動 ===================== */

    /** チャージ率 0.0〜1.0 で発動。INSTANTは1.0が渡される。 */
    public void execute(ServerPlayer player, float chargePercent) {
        Level level = player.level();
        switch (this) {
            case POWER_SHOT -> {
                TestBowEntity arrow = TestBowEntity.shoot(level, player, level.getRandom(), 1.5f, 4.0, 1);
                arrow.setBaseDamage(arrow.getBaseDamage() + 4.0);
                arrow.setCritArrow(true);
                arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
                level.playSound(null, player.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0f, 0.7f);
            }
            case EXPLOSIVE -> {
                Arrow arrow = new Arrow(level, player);
                Vec3 v = player.getLookAngle();
                arrow.shoot(v.x, v.y, v.z, 2.5f, 0.5f);
                arrow.getPersistentData().putBoolean(NBT_EXPLOSIVE, true);
                arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
                level.addFreshEntity(arrow);
                level.playSound(null, player.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0f, 1.2f);
            }
            case PIERCE -> {
                Arrow arrow = new Arrow(level, player);
                Vec3 v = player.getLookAngle();
                arrow.shoot(v.x, v.y, v.z, 3.0f, 0.3f);
                arrow.setPierceLevel((byte) 5);
                arrow.setBaseDamage(arrow.getBaseDamage() + 1.0);
                arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
                level.addFreshEntity(arrow);
                level.playSound(null, player.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0f, 1.4f);
            }
            case POTION_TIPPED -> {
                ItemStack tipped = findTippedArrow(player);
                Arrow arrow = new Arrow(level, player);
                if (!tipped.isEmpty()) {
                    arrow.setEffectsFromItem(tipped);
                    if (!player.getAbilities().instabuild) tipped.shrink(1);
                } else {
                    arrow.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
                }
                Vec3 v = player.getLookAngle();
                arrow.shoot(v.x, v.y, v.z, 2.5f, 0.5f);
                arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
                level.addFreshEntity(arrow);
                level.playSound(null, player.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0f, 1.0f);
            }
            case RAPID_FIRE -> {
                int count = Math.max(2, (int)(2 + chargePercent * 3));
                Vec3 base = player.getLookAngle();
                for (int i = 0; i < count; i++) {
                    Arrow arrow = new Arrow(level, player);
                    arrow.shoot(base.x, base.y, base.z, 2.5f, 4.0f);
                    arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
                    level.addFreshEntity(arrow);
                }
                level.playSound(null, player.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.2f, 1.3f);
            }
            case HOMING -> {
                Arrow arrow = new Arrow(level, player);
                Vec3 v = player.getLookAngle();
                arrow.shoot(v.x, v.y, v.z, 2.5f, 0.2f);
                arrow.getPersistentData().putFloat(NBT_HOMING_POWER, 0.05f + chargePercent * 0.15f);
                arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
                level.addFreshEntity(arrow);
                level.playSound(null, player.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0f, 0.9f);
            }
            default -> {}
        }
    }

    /* ===================== PASSIVE フック ===================== */

    /** 右クリック射撃時のチャージ補正 (1.0で変化なし) */
    public float chargeMultiplier() {
        return this == QUICK_DRAW ? 1.5f : 1.0f;
    }

    /** 右クリック射撃時の矢にバフ付与 */
    public void modifyArrow(TestBowEntity arrow) {
        switch (this) {
            case HEAVY_BLOW -> arrow.setBaseDamage(arrow.getBaseDamage() + 2.0);
            case WIND -> {
                Vec3 dm = arrow.getDeltaMovement();
                arrow.setDeltaMovement(dm.scale(1.5));
                arrow.setNoGravity(true);
            }
            default -> {}
        }
    }

    private static ItemStack findTippedArrow(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.getItem() == Items.TIPPED_ARROW) return s;
        }
        return ItemStack.EMPTY;
    }
}

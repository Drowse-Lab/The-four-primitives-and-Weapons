package the_four_primitives_and_weapons.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import the_four_primitives_and_weapons.skill.PlayerSkillData;
import the_four_primitives_and_weapons.skill.PlayerSkillData.AttackSlot;
import the_four_primitives_and_weapons.skill.PlayerSkillData.WeaponLoadout;
import the_four_primitives_and_weapons.skill.SkillRegistry;
import the_four_primitives_and_weapons.world.inventory.SkillSelectionMenu;
import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;

import java.util.function.Supplier;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class SkillSelectionPacket {

    // slotId = loadoutIndex ("-1" for default), motionId = motionId, itemId = AttackSlot id
    private final String slotId;
    private final String motionId;
    private final String itemId;

    public static SkillSelectionPacket selectLoadoutMotion(int loadoutIndex, AttackSlot attackSlot, String motionId) {
        return new SkillSelectionPacket(String.valueOf(loadoutIndex), motionId, attackSlot.getId());
    }

    public static SkillSelectionPacket setProficiency(String proficiencyId) {
        return new SkillSelectionPacket("proficiency", proficiencyId, null);
    }

    /**
     * 武器タイプ別モーション設定
     * slotId="type:<typeId>", motionId=motionId, itemId=attackSlot.getId()
     */
    public static SkillSelectionPacket setTypeMotion(String typeId, AttackSlot attackSlot, String motionId) {
        return new SkillSelectionPacket("type:" + typeId, motionId, attackSlot.getId());
    }

    /**
     * 技 ON/OFF トグル
     * slotId="motion_toggle", motionId=motionId, itemId="true"/"false"
     */
    public static SkillSelectionPacket toggleMotion(String motionId, boolean enabled) {
        return new SkillSelectionPacket("motion_toggle", motionId, enabled ? "true" : "false");
    }

    private SkillSelectionPacket(String slotId, String motionId, String itemId) {
        this.slotId = slotId;
        this.motionId = motionId;
        this.itemId = itemId;
    }

    public SkillSelectionPacket(FriendlyByteBuf buf) {
        this.slotId = buf.readBoolean() ? buf.readUtf() : null;
        this.motionId = buf.readBoolean() ? buf.readUtf() : null;
        this.itemId = buf.readBoolean() ? buf.readUtf() : null;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(slotId != null);
        if (slotId != null) buf.writeUtf(slotId);

        buf.writeBoolean(motionId != null);
        if (motionId != null) buf.writeUtf(motionId);

        buf.writeBoolean(itemId != null);
        if (itemId != null) buf.writeUtf(itemId);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            player.getCapability(PlayerSkillData.SKILL_CAPABILITY).ifPresent(sd -> {
                if ("proficiency".equals(slotId)) {
                    handleProficiency(sd);
                } else if ("motion_toggle".equals(slotId)) {
                    handleMotionToggle(player);
                } else if (slotId != null && slotId.startsWith("type:")) {
                    handleTypeMotion(sd, player);
                } else {
                    handleSelectLoadoutMotion(sd, player);
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }

    private void handleMotionToggle(ServerPlayer player) {
        if (motionId == null || itemId == null) return;
        boolean enabled = "true".equalsIgnoreCase(itemId);
        PlayerSkillData.setMotionEnabled(player, motionId, enabled);
    }

    private void handleTypeMotion(PlayerSkillData.SkillStorage skillData, ServerPlayer player) {
        if (slotId == null || motionId == null || itemId == null) return;
        String typeId = slotId.substring(5); // "type:" を除去
        AttackSlot attackSlot = AttackSlot.fromId(itemId);
        if (attackSlot == null) return;
        // ロードアウト側 ( handleSelectLoadoutMotion ) と同じく、 そのスロットに置けない技は弾く。
        // ここに検証が無いと 連撃 ( チャージ専用 ) を一撃目に入れられてしまう。
        SkillRegistry.MotionInfo motion = SkillRegistry.getById(motionId);
        if (motion == null || !motion.getCompatibleSlots().contains(attackSlot)) return;
        skillData.setTypeMotion(typeId, attackSlot, motionId);

        // タイプ別設定 ( 優先度2 ) より上位の 武器NBT ( 優先度0 ) / 武器スロット ( 優先度1 ) が
        // 残っていると、 ここで選び直しても何も起きない。 スキル画面で選んだものが必ず反映される
        // よう、 このタイプの武器についてだけ上位設定を落とす。
        skillData.clearLoadoutMotionsForType(typeId, attackSlot);
        clearNbtMotionForType(player, typeId, attackSlot);
    }

    /** プレイヤーの持ち物にある「指定タイプの武器」から、 該当スロットの NBT 技設定を消す。 */
    private static void clearNbtMotionForType(ServerPlayer player, String typeId, AttackSlot attackSlot) {
        net.minecraft.world.entity.player.Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            net.minecraft.world.item.ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            the_four_primitives_and_weapons.skill.WeaponTypeRegistry.WeaponTypeData type =
                    the_four_primitives_and_weapons.skill.WeaponTypeRegistry.getTypeForItem(stack);
            if (type != null && typeId.equals(type.getId())) {
                the_four_primitives_and_weapons.skill.WeaponSkillNBT.removeMotion(stack, attackSlot);
            }
        }
    }

    private void handleProficiency(PlayerSkillData.SkillStorage skillData) {
        if (motionId == null) return;
        skillData.setWeaponProficiency(PlayerSkillData.WeaponProficiency.fromId(motionId));
    }

    private void handleSelectLoadoutMotion(PlayerSkillData.SkillStorage skillData, ServerPlayer player) {
        if (slotId == null || motionId == null || itemId == null) return;

        AttackSlot attackSlot = AttackSlot.fromId(itemId);
        if (attackSlot == null) return;

        SkillRegistry.MotionInfo motion = SkillRegistry.getById(motionId);
        if (motion == null) return;
        if (!motion.getCompatibleSlots().contains(attackSlot)) return;

        int loadoutIndex;
        try {
            loadoutIndex = Integer.parseInt(slotId);
        } catch (NumberFormatException e) {
            return;
        }

        if (loadoutIndex == -1) {
            // デフォルト設定（UNIVERSAL のみ）
            if (motion.getCategory() != SkillRegistry.MotionCategory.UNIVERSAL) return;
            skillData.setMotion(attackSlot, motionId);
            // 手持ち武器のタイプにもtypeMotionを設定（JSONデフォルトを上書き）
            net.minecraft.world.item.ItemStack held = player.getMainHandItem();
            the_four_primitives_and_weapons.skill.WeaponTypeRegistry.WeaponTypeData wt =
                    the_four_primitives_and_weapons.skill.WeaponTypeRegistry.getTypeForItem(held);
            if (wt != null) {
                skillData.setTypeMotion(wt.getId(), attackSlot, motionId);
                // 上位設定 ( 武器NBT / 武器スロット ) が残っていると反映されないので落とす。
                skillData.clearLoadoutMotionsForType(wt.getId(), attackSlot);
                clearNbtMotionForType(player, wt.getId(), attackSlot);
            } else if (!held.isEmpty()) {
                // タイプ未登録の武器でも、 手持ちの武器の NBT / 武器スロット設定は落としておく。
                skillData.clearLoadoutMotionsForItem(held.getItem(), attackSlot);
                the_four_primitives_and_weapons.skill.WeaponSkillNBT.removeMotion(held, attackSlot);
            }
        } else {
            // 特定スロットの設定
            WeaponLoadout loadout = skillData.getLoadoutAt(loadoutIndex);

            // ロードアウトが未作成の場合、開いているメニューから武器を取得して作成
            if (loadout == null) {
                if (player.containerMenu instanceof SkillSelectionMenu) {
                    SkillSelectionMenu skillMenu = (SkillSelectionMenu) player.containerMenu;
                    net.minecraft.world.item.ItemStack weapon = skillMenu.getSlot(loadoutIndex).getItem();
                    if (!weapon.isEmpty()) {
                        skillData.setLoadoutAt(loadoutIndex, weapon);
                        loadout = skillData.getLoadoutAt(loadoutIndex);
                    }
                }
                if (loadout == null) return;
            }

            // SPECIALの場合、その武器クラスに対応しているか確認
            if (motion.getCategory() == SkillRegistry.MotionCategory.SPECIAL) {
                String weaponClass = loadout.getWeaponClass();
                if (!weaponClass.equals(motion.getRequiredWeaponClass())) return;
            }

            skillData.setLoadoutMotion(loadoutIndex, attackSlot, motionId);

            // 武器のNBTにも技設定を保存（アイテムと一緒に永続化）
            writeMotionToRealWeapons(player, loadout, loadoutIndex, attackSlot, motionId);
        }
    }

    /**
     * 技設定を「実物の武器」の NBT に書く。
     *
     * <p>GUI の武器スロットに入っている ItemStack は、 メニューを開き直すたびに
     * {@code SkillSelectionMenu#populateFromSkillData()} が
     * {@code WeaponLoadout#getWeapon()} ( = {@code weapon.copy()} ) を入れ直した
     * 「コピー」であることがある。 そこにだけ NBT を書くとメニューを閉じた時点で捨てられ、
     * 設定がまったく反映されない。 しかも一度でも実物をスロットに入れて設定した武器は
     * NBT が焼き付き、 それが {@code getMotionForWeapon} の最優先 ( 優先度0 ) なので、
     * 以後 GUI から変更できなくなっていた。</p>
     *
     * <p>そのため スロットの ItemStack ( 実物を入れている場合はこれ ) に加えて、
     * プレイヤーの持ち物にある同じアイテムにも書き込む。 ロードアウトの一致判定
     * ( {@code WeaponLoadout#matchesItem} ) がアイテム単位なので、 対象もアイテム単位で揃える。</p>
     */
    private static void writeMotionToRealWeapons(ServerPlayer player, WeaponLoadout loadout,
                                                 int loadoutIndex, AttackSlot attackSlot, String motionId) {
        net.minecraft.world.item.Item target = null;

        if (player.containerMenu instanceof SkillSelectionMenu) {
            SkillSelectionMenu skillMenu = (SkillSelectionMenu) player.containerMenu;
            net.minecraft.world.item.ItemStack weaponInSlot = skillMenu.getSlot(loadoutIndex).getItem();
            if (!weaponInSlot.isEmpty()) {
                // 実物を入れている場合はこれで実物に書ける ( コピーの場合は下のループが拾う )
                the_four_primitives_and_weapons.skill.WeaponSkillNBT.setMotion(weaponInSlot, attackSlot, motionId);
                target = weaponInSlot.getItem();
            }
        }
        if (target == null && loadout != null) {
            net.minecraft.world.item.ItemStack loadoutWeapon = loadout.getWeapon();
            if (!loadoutWeapon.isEmpty()) target = loadoutWeapon.getItem();
        }
        if (target == null) return;

        net.minecraft.world.entity.player.Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            net.minecraft.world.item.ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && stack.getItem() == target) {
                the_four_primitives_and_weapons.skill.WeaponSkillNBT.setMotion(stack, attackSlot, motionId);
            }
        }
    }

    @SubscribeEvent
    public static void registerMessage(FMLCommonSetupEvent event) {
        TheFourPrimitivesAndWeaponsMod.addNetworkMessage(SkillSelectionPacket.class,
            SkillSelectionPacket::encode,
            SkillSelectionPacket::new,
            SkillSelectionPacket::handle);
    }
}

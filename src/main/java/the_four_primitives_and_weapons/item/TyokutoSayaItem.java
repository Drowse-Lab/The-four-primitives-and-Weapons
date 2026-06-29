package the_four_primitives_and_weapons.item;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import top.theillusivec4.curios.api.type.capability.ICurioItem;
import top.theillusivec4.curios.api.SlotContext;

import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModTabs;
import the_four_primitives_and_weapons.procedures.TyokutouThrustAttackProcedure;
import the_four_primitives_and_weapons.util.SayaRegistry;

import java.util.List;

public class TyokutoSayaItem extends Item implements ICurioItem {
    public TyokutoSayaItem() {
        super(new Item.Properties()
            
            .stacksTo(1)
            .rarity(Rarity.UNCOMMON));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        // 抜刀は R キーのみで行う (鞘の右クリックでは何もしない)
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    @Override
    public void appendHoverText(ItemStack stack, Level world, List<Component> list, net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, world, list, flag);

        String sayaHex = the_four_primitives_and_weapons.util.SayaDesign.getBaseHex(stack);
        if (sayaHex != null) list.add(Component.translatable("tooltip.the_four_primitives_and_weapons.saya.base", sayaHex));
        Component sayaFinish = the_four_primitives_and_weapons.util.SayaStyles.finishName(
                the_four_primitives_and_weapons.util.SayaDesign.getStyle(stack),
                the_four_primitives_and_weapons.util.SayaDesign.getLacquer(stack));
        if (sayaFinish != null)
            list.add(Component.translatable("tooltip.the_four_primitives_and_weapons.saya.style", sayaFinish));

        if (stack.hasTag() && stack.getTag().contains("StoredSword")) {
            ItemStack storedSword = ItemStack.of(stack.getTag().getCompound("StoredSword"));
            list.add(Component.translatable("tooltip.the_four_primitives_and_weapons.saya.sheathed", storedSword.getHoverName()));
        } else {
            list.add(Component.translatable("tooltip.the_four_primitives_and_weapons.saya.empty"));
            list.add(Component.translatable("tooltip.the_four_primitives_and_weapons.saya.hint"));
        }
    }

    @Override
    public UseAnim getUseAnimation(ItemStack itemstack) {
        return UseAnim.NONE;
    }

    @Override
    public int getUseDuration(ItemStack itemstack) {
        return 0;
    }

    /**
     * 納刀可能な直刀かチェック
     */
    public static boolean canSheathe(ItemStack swordStack) {
        return TyokutouThrustAttackProcedure.isStraightSword(swordStack);
    }

    /**
     * 納刀処理
     */
    public static void sheatheSword(Player player, ItemStack swordStack, ItemStack sheathStack,
                                    InteractionHand swordHand, InteractionHand sheathHand) {
        if (swordHand == sheathHand) return; // 同じ手は不可
        CompoundTag sheathTag = sheathStack.getOrCreateTag();

        // 鞘が空の場合のみ納刀可能
        if (!sheathTag.contains("StoredSword")) {
            // 直刀鞘に登録されていない武器は納刀不可 (slot/model どちらでも登録扱い)
            if (!SayaRegistry.isRegistered(SayaRegistry.SayaType.TYOKUTO, swordStack)) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("§cこの武器はこの鞘に納刀できません"), true);
                return;
            }
            // 武器のNBTデータを保存
            CompoundTag swordData = swordStack.save(new CompoundTag());
            sheathTag.put("StoredSword", swordData);

            // 見た目は SayaModelWrapper が StoredSword NBT を読んで動的に解決する。
            sheathStack.setTag(sheathTag);

            // 武器を削除
            player.setItemInHand(swordHand, ItemStack.EMPTY);

            // 鞘を更新
            player.setItemInHand(sheathHand, sheathStack);

            // 納刀音
            player.playSound(SoundEvents.ARMOR_EQUIP_IRON, 1.0F, 0.8F);
        } else {
            player.displayClientMessage(Component.literal("§c鞘には既に刀が入っています"), true);
        }
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        String id = slotContext.identifier();
        return id.equals("belt") || id.equals("back");
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return false;
    }

    /**
     * 直刀ごとのモデルデータを返す（publicにしてCurios納刀から参照可能）。
     * data/&lt;namespace&gt;/maw_saya/*.json の "tyokuto" セクションから読み込まれる。
     *
     * BluepurgeItemは特例: CustomModelData==2のときのみ直刀扱い（NBT依存のためJSON不可）。
     */
    public static int getSwordModelData(ItemStack sword) {
        int registered = SayaRegistry.getModelData(SayaRegistry.SayaType.TYOKUTO, sword);
        if (registered != 0) return registered;

        // BluepurgeItem 特例（NBTの CustomModelData==2 で直刀化）
        if (sword.getItem().getClass().getSimpleName().equals("BluepurgeItem")) {
            if (sword.hasTag() && sword.getTag().contains("CustomModelData")
                    && sword.getTag().getInt("CustomModelData") == 2) {
                return 2;
            }
        }
        return 0;
    }
}

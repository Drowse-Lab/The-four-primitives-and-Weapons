package minecraftarmorweapon.item;

import net.minecraft.world.level().Level;
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

import minecraftarmorweapon.init.MinecraftArmorWeaponModTabs;
import minecraftarmorweapon.procedures.TyokutouThrustAttackProcedure;

import java.util.List;

public class TyokutouSayaItem extends Item {
    public TyokutouSayaItem() {
        super(new Item.Properties()
            .tab(MinecraftArmorWeaponModTabs.TAB_WEAPON)
            .stacksTo(1)
            .rarity(Rarity.UNCOMMON));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack sayaStack = player.getItemInHand(hand);

        if (sayaStack.hasTag() && sayaStack.getTag().contains("StoredSword")) {
            // 抜刀処理
            CompoundTag tag = sayaStack.getOrCreateTag();
            ItemStack swordStack = ItemStack.of(tag.getCompound("StoredSword"));

            // 反対の手にある武器を確認
            InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ?
                                       InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            ItemStack otherItem = player.getItemInHand(otherHand);

            // 反対の手が空いている場合のみ抜刀
            if (otherItem.isEmpty()) {
                // 刀を反対の手に配置
                player.setItemInHand(otherHand, swordStack);

                // 鞘から刀を削除
                tag.remove("StoredSword");
                tag.putInt("CustomModelData", 0); // 空の鞘のモデル
                sayaStack.setTag(tag);

                // 抜刀音
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 0.8f, 1.2f);

                // 抜刀エフェクト（必要に応じて追加）
                player.displayClientMessage(Component.literal("§e抜刀！"), true);

                return InteractionResultHolder.sidedSuccess(sayaStack, world.isClientSide());
            } else {
                player.displayClientMessage(Component.literal("§c反対の手が空いていません"), true);
                return InteractionResultHolder.fail(sayaStack);
            }
        }

        return InteractionResultHolder.pass(sayaStack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level world, List<Component> list, net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, world, list, flag);

        if (stack.hasTag() && stack.getTag().contains("StoredSword")) {
            ItemStack storedSword = ItemStack.of(stack.getTag().getCompound("StoredSword"));
            list.add(Component.literal("§7納刀中: §f" + storedSword.getHoverName().getString()));
        } else {
            list.add(Component.literal("§7空の鞘"));
            list.add(Component.literal("§8Shift+右クリックで納刀"));
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
        CompoundTag sheathTag = sheathStack.getOrCreateTag();

        // 鞘が空の場合のみ納刀可能
        if (!sheathTag.contains("StoredSword")) {
            // 武器のNBTデータを保存
            CompoundTag swordData = swordStack.save(new CompoundTag());
            sheathTag.put("StoredSword", swordData);

            // 鞘の見た目を更新（納刀状態）
            sheathTag.putInt("CustomModelData", getSwordModelData(swordStack));

            // 鞘にタグを適用
            sheathStack.setTag(sheathTag);

            // 武器を削除
            player.setItemInHand(swordHand, ItemStack.EMPTY);

            // 鞘を更新
            player.setItemInHand(sheathHand, sheathStack);

            // 納刀音
            player.playSound(SoundEvents.ARMOR_EQUIP_IRON, 1.0F, 0.8F);

            player.displayClientMessage(Component.literal("§7納刀"), true);
        }
    }

    /**
     * 直刀ごとのモデルデータを返す
     */
    private static int getSwordModelData(ItemStack sword) {
        String itemName = sword.getItem().getClass().getSimpleName();

        // 直刀ごとに異なるCustomModelDataを返す
        if (itemName.equals("LunaItem")) return 1;
        if (itemName.equals("BluepurgeTyokutouItem")) return 2;
        if (itemName.equals("KaminariKurikarakenTyokutouItem")) return 3;

        // BluepurgeItemの場合、custom_model_dataをチェック
        if (itemName.equals("BluepurgeItem")) {
            if (sword.hasTag() && sword.getTag().contains("CustomModelData")) {
                int customModelData = sword.getTag().getInt("CustomModelData");
                if (customModelData == 2) {
                    return 2; // 直刀モデル
                }
            }
        }

        return 0; // デフォルト（空の鞘）
    }
}
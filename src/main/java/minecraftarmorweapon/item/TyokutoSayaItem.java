package minecraftarmorweapon.item;

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

import minecraftarmorweapon.init.MinecraftArmorWeaponModTabs;
import minecraftarmorweapon.procedures.TyokutouThrustAttackProcedure;
import minecraftarmorweapon.util.SayaRegistry;

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

        if (stack.hasTag() && stack.getTag().contains("StoredSword")) {
            ItemStack storedSword = ItemStack.of(stack.getTag().getCompound("StoredSword"));
            list.add(Component.literal("§7納刀中: §f" + storedSword.getHoverName().getString()));
        } else {
            list.add(Component.literal("§7空の鞘"));
            list.add(Component.literal("§8Rキーで納刀"));
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
            // 直刀鞘に登録されていない武器は納刀不可
            int modelData = getSwordModelData(swordStack);
            if (modelData == 0) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("§cこの武器はこの鞘に納刀できません"), true);
                return;
            }
            // 武器のNBTデータを保存
            CompoundTag swordData = swordStack.save(new CompoundTag());
            sheathTag.put("StoredSword", swordData);

            // 鞘の見た目を更新（納刀状態）
            sheathTag.putInt("CustomModelData", modelData);

            // 鞘にタグを適用
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

package the_four_primitives_and_weapons.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.events.MagicalKatanaCrystalHandler;

import java.util.function.Supplier;

/**
 * Magical Katana 専用アクションを R ホイール経由で発動するパケット。
 *   - SPAWN_CRYSTAL : 結晶を生成 ( 特殊技 ) — unlocked 限定
 *   - SHATTER       : 具現化版を破壊 ( shatter ) — 手に持ってる Materialized が対象
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class MagicalKatanaActionPacket {

    public enum Action { SPAWN_CRYSTAL, SHATTER, DESTROY_ALL_OWNED }

    private final Action action;

    public MagicalKatanaActionPacket(Action action) {
        this.action = action;
    }

    public MagicalKatanaActionPacket(FriendlyByteBuf buf) {
        this.action = Action.values()[buf.readByte()];
    }

    public static void encode(MagicalKatanaActionPacket msg, FriendlyByteBuf buf) {
        buf.writeByte(msg.action.ordinal());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            switch (action) {
                case SPAWN_CRYSTAL:     handleSpawnCrystal(player); break;
                case SHATTER:           handleShatter(player); break;
                case DESTROY_ALL_OWNED: handleDestroyAllOwned(player); break;
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private void handleDestroyAllOwned(ServerPlayer player) {
        int n = MagicalKatanaCrystalHandler.destroyAllOwnedMaterialized(
                player.getServer(), player);
        if (n > 0) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "§7自分の具現化武器を §c" + n + " §7本破壊しました"), true);
        }
    }

    private void handleSpawnCrystal(ServerPlayer player) {
        ItemStack stack = pickMagicalKatana(player, false);
        if (stack.isEmpty()) {
            // インベ / 手 / saya に Magical Katana 物理的に存在しなくても、
            // 過去に結晶化したことがあれば player.persistentData の SavedMagicalKatanaNBT
            // から「虚空から復元」 して結晶化を発動する ( = backbone storage 風 別保管 )
            net.minecraft.nbt.CompoundTag pd = player.getPersistentData();
            ItemStack restored = new ItemStack(
                    the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModItems.MAGICAL_KATANA.get());
            if (pd.contains("SavedMagicalKatanaNBT", 10)) {
                // 過去に結晶化経験あり = saved NBT から復元 + unlock 強制
                net.minecraft.nbt.CompoundTag savedTag = pd.getCompound("SavedMagicalKatanaNBT").copy();
                savedTag.putBoolean("MagicalKatanaUnlocked", true);
                restored.setTag(savedTag);
            } else {
                // 完全 fresh start ( 初回 ) — unlocked のみセット
                restored.getOrCreateTag().putBoolean("MagicalKatanaUnlocked", true);
            }
            MagicalKatanaCrystalHandler.spawnCrystal(player, restored);
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "§5虚空から結晶化"), true);
            return;
        }
        if (!MagicalKatanaCrystalHandler.isUnlocked(stack)) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "§7封印された Magical Katana §8— §c具現化版を破壊§7、 または §6Lv12 (CORROSION) §7をセット"), true);
            return;
        }
        // 元 stack の NBT を player に保存して、 具現化版に引き継ぐ
        MagicalKatanaCrystalHandler.spawnCrystal(player, stack);
    }

    private void handleShatter(ServerPlayer player) {
        ItemStack main = player.getMainHandItem();
        if (MagicalKatanaCrystalHandler.isMaterialized(main)) {
            MagicalKatanaCrystalHandler.shatterOnSheathe(player, main, InteractionHand.MAIN_HAND);
            return;
        }
        ItemStack off = player.getOffhandItem();
        if (MagicalKatanaCrystalHandler.isMaterialized(off)) {
            MagicalKatanaCrystalHandler.shatterOnSheathe(player, off, InteractionHand.OFF_HAND);
        }
    }

    /**
     * Magical Katana を 手 → インベントリ全体 ( ホットバー含む ) の順で探して返す。
     * materializedOnly=true なら 具現化版のみ。
     */
    private static ItemStack pickMagicalKatana(ServerPlayer player, boolean materializedOnly) {
        ItemStack main = player.getMainHandItem();
        if (MagicalKatanaCrystalHandler.isMagicalKatana(main)
                && (!materializedOnly || MagicalKatanaCrystalHandler.isMaterialized(main))) return main;
        ItemStack off = player.getOffhandItem();
        if (MagicalKatanaCrystalHandler.isMagicalKatana(off)
                && (!materializedOnly || MagicalKatanaCrystalHandler.isMaterialized(off))) return off;
        // インベントリ全体 ( SPAWN_CRYSTAL では手に持っていない状態でも発動できるように )
        net.minecraft.world.entity.player.Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (MagicalKatanaCrystalHandler.isMagicalKatana(s)
                    && (!materializedOnly || MagicalKatanaCrystalHandler.isMaterialized(s))) return s;
            // saya 内に納刀された Magical Katana も探す
            if (the_four_primitives_and_weapons.util.CuriosScabbardHelper.isScabbard(s)
                    && the_four_primitives_and_weapons.util.CuriosScabbardHelper.hasStoredWeapon(s)) {
                ItemStack stored = the_four_primitives_and_weapons.util.CuriosScabbardHelper.extractWeaponFromScabbard(s);
                if (MagicalKatanaCrystalHandler.isMagicalKatana(stored)
                        && (!materializedOnly || MagicalKatanaCrystalHandler.isMaterialized(stored))) return stored;
            }
        }
        return ItemStack.EMPTY;
    }

    @SubscribeEvent
    public static void registerMessage(FMLCommonSetupEvent event) {
        TheFourPrimitivesAndWeaponsMod.addNetworkMessage(MagicalKatanaActionPacket.class,
                MagicalKatanaActionPacket::encode,
                MagicalKatanaActionPacket::new,
                (msg, ctx) -> msg.handle(ctx));
    }
}

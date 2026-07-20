package com.tacz_caliber_ammo.item;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.builder.AmmoItemBuilder;
import com.tacz_caliber_ammo.caliber.PatternEntry;
import com.tacz_caliber_ammo.menu.AmmoPouchMenu;
import com.tacz_caliber_ammo.nbt.NbtKeys;
import com.tacz_caliber_ammo.util.DebugLog;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.network.NetworkHooks;

/**
 * 弹药包：多弹种存储 + 压弹图案（Stage 6）。
 *
 * <p>存储 = {@code Map<ammoId,count>}（NBT 键 {@link NbtKeys#POUCH_STORE}，ListTag of {@code {id,c}}），
 * 容量上限 {@link #getCapacity()}（默认 540，子类可覆写以做不同容量的弹药包）。
 * 图案 = 有序 {@code List<PatternEntry>}（NBT 键 {@link NbtKeys#POUCH_PATTERN}，ListTag of {@code {id,n}}，
 * 最多 {@link #MAX_PATTERN}=5）；换弹时按图案循环组成逐发序列（Stage 6 T6.6）。GUI 见 Stage 3（后做）。
 */
public class AmmoPouchItem extends Item {

    /** 默认容量（总发数上限）。 */
    public static final int DEFAULT_CAPACITY = 540;
    /** 压弹图案最大项数。 */
    public static final int MAX_PATTERN = 5;

    private static final String ID = "id";
    private static final String COUNT = "c";
    private static final String PER_CYCLE = "n";

    public AmmoPouchItem(Properties properties) {
        super(properties);
    }

    /** 本弹药包容量（总发数上限）。子类覆写以提供不同容量。 */
    public int getCapacity() {
        return DEFAULT_CAPACITY;
    }

    // ==== 存储：Map<ammoId,count>（NBT 键 PouchStore） ====

    /** 读多弹种存储（有序）。 */
    public static Map<ResourceLocation, Integer> getStore(ItemStack pouch) {
        Map<ResourceLocation, Integer> map = new LinkedHashMap<>();
        CompoundTag tag = pouch.getTag();
        if (tag == null || !tag.contains(NbtKeys.POUCH_STORE, Tag.TAG_LIST)) {
            return map;
        }
        ListTag list = tag.getList(NbtKeys.POUCH_STORE, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag e = list.getCompound(i);
            ResourceLocation id = ResourceLocation.tryParse(e.getString(ID));
            int c = e.getInt(COUNT);
            if (id != null && c > 0) {
                map.merge(id, c, Integer::sum);
            }
        }
        return map;
    }

    /** 写多弹种存储（丢弃 count&lt;=0 的项）。 */
    public static void setStore(ItemStack pouch, Map<ResourceLocation, Integer> store) {
        ListTag list = new ListTag();
        for (Map.Entry<ResourceLocation, Integer> en : store.entrySet()) {
            if (en.getKey() == null || en.getValue() == null || en.getValue() <= 0) {
                continue;
            }
            CompoundTag e = new CompoundTag();
            e.putString(ID, en.getKey().toString());
            e.putInt(COUNT, en.getValue());
            list.add(e);
        }
        pouch.getOrCreateTag().put(NbtKeys.POUCH_STORE, list);
    }

    /** 存储总发数。 */
    public static int getTotalCount(ItemStack pouch) {
        int sum = 0;
        for (int v : getStore(pouch).values()) {
            sum += v;
        }
        return sum;
    }

    /** 存入 count 发 ammoId，受 {@link #getCapacity()} 限制；返回实际存入数。 */
    public int deposit(ItemStack pouch, ResourceLocation ammoId, int count) {
        if (ammoId == null || count <= 0) {
            return 0;
        }
        Map<ResourceLocation, Integer> store = getStore(pouch);
        int total = 0;
        for (int v : store.values()) {
            total += v;
        }
        int room = getCapacity() - total;
        int add = Math.min(count, Math.max(0, room));
        if (add > 0) {
            if (store.containsKey(ammoId)) {
                store.merge(ammoId, add, Integer::sum);
            } else {
                // New ammo type is placed at the FRONT of the store (LinkedHashMap order),
                // so it is preferred for no-pattern reload and for withdraw.
                Map<ResourceLocation, Integer> reordered = new LinkedHashMap<>();
                reordered.put(ammoId, add);
                reordered.putAll(store);
                store = reordered;
            }
            setStore(pouch, store);
        }
        return add;
    }

    /** 取出 count 发 ammoId；返回实际取出数。 */
    public static int withdraw(ItemStack pouch, ResourceLocation ammoId, int count) {
        if (ammoId == null || count <= 0) {
            return 0;
        }
        Map<ResourceLocation, Integer> store = getStore(pouch);
        int have = store.getOrDefault(ammoId, 0);
        int take = Math.min(count, have);
        if (take > 0) {
            int left = have - take;
            if (left > 0) {
                store.put(ammoId, left);
            } else {
                store.remove(ammoId);
            }
            setStore(pouch, store);
        }
        return take;
    }

    // ==== 图案：List<PatternEntry>（NBT 键 PouchPattern，最多 MAX_PATTERN） ====

    /** 读压弹图案（最多 MAX_PATTERN 项）。 */
    public static List<PatternEntry> getPattern(ItemStack pouch) {
        List<PatternEntry> out = new ArrayList<>();
        CompoundTag tag = pouch.getTag();
        if (tag == null || !tag.contains(NbtKeys.POUCH_PATTERN, Tag.TAG_LIST)) {
            return out;
        }
        ListTag list = tag.getList(NbtKeys.POUCH_PATTERN, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size() && out.size() < MAX_PATTERN; i++) {
            CompoundTag e = list.getCompound(i);
            ResourceLocation id = ResourceLocation.tryParse(e.getString(ID));
            int n = e.getInt(PER_CYCLE);
            if (id != null && n > 0) {
                out.add(new PatternEntry(id, n));
            }
        }
        return out;
    }

    /** 写压弹图案（截断到 MAX_PATTERN，丢弃非法项）。 */
    public static void setPattern(ItemStack pouch, List<PatternEntry> pattern) {
        ListTag list = new ListTag();
        int cnt = 0;
        for (PatternEntry pe : pattern) {
            if (cnt >= MAX_PATTERN) {
                break;
            }
            if (pe == null || pe.ammoId() == null || pe.perCycle() <= 0) {
                continue;
            }
            CompoundTag e = new CompoundTag();
            e.putString(ID, pe.ammoId().toString());
            e.putInt(PER_CYCLE, pe.perCycle());
            list.add(e);
            cnt++;
        }
        pouch.getOrCreateTag().put(NbtKeys.POUCH_PATTERN, list);
    }

    /**
     * DEV-ONLY test scaffold (Stage 3 GUI will replace this). Right-clicking the pouch in a dev
     * environment deposits every ammo stack from the player's inventory into this pouch, then sets a
     * pattern from the first {@link #MAX_PATTERN} distinct ammo types (5 rounds each per cycle), so a
     * data-filled pouch can be produced in-game to exercise the reload supply path. No-op in production.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack pouch = player.getItemInHand(hand);
        // Normal right-click opens the pouch GUI (server side, main hand). Sneak + right-click keeps
        // the dev-only fill scaffold below for testing.
        if (!player.isShiftKeyDown()) {
            if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                    && hand == InteractionHand.MAIN_HAND) {
                int pouchSlot = player.getInventory().selected;
                NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider(
                        (id, inv, p) -> new AmmoPouchMenu(id, inv, pouchSlot), pouch.getHoverName()),
                        buf -> buf.writeVarInt(pouchSlot));
            }
            return InteractionResultHolder.success(pouch);
        }
        if (level.isClientSide() || !DebugLog.ENABLED) {
            return InteractionResultHolder.success(pouch);
        }
        IItemHandler inv = player.getCapability(ForgeCapabilities.ITEM_HANDLER, null).resolve().orElse(null);
        if (inv == null) {
            return InteractionResultHolder.success(pouch);
        }
        List<ResourceLocation> order = new ArrayList<>();
        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack s = inv.getStackInSlot(i);
            if (s.getItem() instanceof IAmmo ammo) {
                ResourceLocation id = ammo.getAmmoId(s);
                if (id == null) {
                    continue;
                }
                int added = this.deposit(pouch, id, s.getCount());
                if (added > 0) {
                    s.shrink(added);
                    if (!order.contains(id)) {
                        order.add(id);
                    }
                }
            }
        }
        List<PatternEntry> pat = new ArrayList<>();
        for (int i = 0; i < order.size() && i < MAX_PATTERN; i++) {
            pat.add(new PatternEntry(order.get(i), 5));
        }
        if (!pat.isEmpty()) {
            setPattern(pouch, pat);
        }
        DebugLog.log("pouch dev-fill: store={} pattern={}", getStore(pouch), getPattern(pouch));
        return InteractionResultHolder.success(pouch);
    }

    @Override
    public java.util.Optional<TooltipComponent> getTooltipImage(ItemStack pouch) {
        List<PouchTooltipData.Entry> entries = new ArrayList<>();
        entries.add(new PouchTooltipData.Entry(true, 0,
                Component.translatable("tooltip.tacz_caliber_ammo.ammo_pouch.capacity_label")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(" " + getTotalCount(pouch)).withStyle(ChatFormatting.WHITE))
                        .append(Component.literal("/" + getCapacity()).withStyle(ChatFormatting.GOLD))));
        Map<ResourceLocation, Integer> store = getStore(pouch);
        if (store.isEmpty()) {
            entries.add(new PouchTooltipData.Entry(true, 0,
                    Component.translatable("tooltip.tacz_caliber_ammo.ammo_pouch.empty")
                            .withStyle(ChatFormatting.DARK_GRAY)));
        } else {
            entries.add(new PouchTooltipData.Entry(true, 0,
                    Component.translatable("tooltip.tacz_caliber_ammo.ammo_pouch.stored")
                            .withStyle(ChatFormatting.GRAY)));
            for (Map.Entry<ResourceLocation, Integer> e : store.entrySet()) {
                entries.add(new PouchTooltipData.Entry(false, e.getValue(), nameOf(e.getKey())));
            }
        }
        List<PatternEntry> pattern = getPattern(pouch);
        if (!pattern.isEmpty()) {
            entries.add(new PouchTooltipData.Entry(true, 0,
                    Component.translatable("tooltip.tacz_caliber_ammo.ammo_pouch.pattern")
                            .withStyle(ChatFormatting.GRAY)));
            for (PatternEntry pe : pattern) {
                entries.add(new PouchTooltipData.Entry(false, pe.perCycle(), nameOf(pe.ammoId())));
            }
        }
        return java.util.Optional.of(new PouchTooltipData(entries));
    }

    /** Ammo display name = the ammo item's hover name (built from ammoId via TacZ AmmoItemBuilder). */
    private static Component nameOf(ResourceLocation ammoId) {
        return AmmoItemBuilder.create().setId(ammoId).setCount(1).build().getHoverName();
    }

    // ==== Inventory right-click interaction (like TacZ AmmoBox): deposit / withdraw ====

    /**
     * Pouch is on the cursor, right-clicked onto {@code slot} (meOnOther).
     * Empty slot -> withdraw one stack of the first stored ammo; ammo slot -> deposit that ammo.
     */
    @Override
    public boolean overrideStackedOnOther(ItemStack pouch, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY) {
            return false;
        }
        ItemStack slotItem = slot.getItem();
        if (slotItem.isEmpty()) {
            return withdrawFirstToSlot(pouch, slot, player);
        }
        if (slotItem.getItem() instanceof IAmmo ammo) {
            ResourceLocation ammoId = ammo.getAmmoId(slotItem);
            if (ammoId == null || DefaultAssets.EMPTY_AMMO_ID.equals(ammoId)) {
                return false;
            }
            int room = getCapacity() - getTotalCount(pouch);
            if (room <= 0) {
                return false;
            }
            int want = Math.min(slotItem.getCount(), room);
            ItemStack taken = slot.safeTake(slotItem.getCount(), want, player);
            int added = this.deposit(pouch, ammoId, taken.getCount());
            if (added < taken.getCount()) {
                // Return the surplus that did not fit (defensive; want<=room should prevent this).
                taken.shrink(added);
                player.getInventory().placeItemBackInInventory(taken);
            }
            if (added > 0) {
                playInsertSound(player);
                return true;
            }
        }
        return false;
    }

    /**
     * An ammo stack ({@code other}, on the cursor) is right-clicked onto this pouch (otherOnMe).
     * Deposit that ammo into the pouch.
     */
    @Override
    public boolean overrideOtherStackedOnMe(ItemStack pouch, ItemStack other, Slot slot, ClickAction action,
            Player player, SlotAccess access) {
        if (action != ClickAction.SECONDARY || other.isEmpty()) {
            return false;
        }
        if (other.getItem() instanceof IAmmo ammo) {
            ResourceLocation ammoId = ammo.getAmmoId(other);
            if (ammoId == null || DefaultAssets.EMPTY_AMMO_ID.equals(ammoId)) {
                return false;
            }
            int added = this.deposit(pouch, ammoId, other.getCount());
            if (added > 0) {
                other.shrink(added);
                playInsertSound(player);
                return true;
            }
        }
        return false;
    }

    /** Withdraw one stack (ammo index stack-size) of the first stored ammo type into the empty slot. */
    private boolean withdrawFirstToSlot(ItemStack pouch, Slot slot, Player player) {
        Map<ResourceLocation, Integer> store = getStore(pouch);
        if (store.isEmpty()) {
            return false;
        }
        Map.Entry<ResourceLocation, Integer> first = store.entrySet().iterator().next();
        ResourceLocation ammoId = first.getKey();
        int have = first.getValue();
        int stackSize = TimelessAPI.getCommonAmmoIndex(ammoId).map(index -> index.getStackSize()).orElse(64);
        int takeCount = Math.min(stackSize, have);
        if (takeCount <= 0) {
            return false;
        }
        ItemStack takeAmmo = AmmoItemBuilder.create().setId(ammoId).setCount(takeCount).build();
        ItemStack remaining = slot.safeInsert(takeAmmo);
        int inserted = takeCount - remaining.getCount();
        if (inserted <= 0) {
            return false;
        }
        withdraw(pouch, ammoId, inserted);
        playRemoveSound(player);
        return true;
    }

    private void playInsertSound(Player player) {
        player.playSound(SoundEvents.BUNDLE_INSERT, 0.8f, 0.8f + player.level().getRandom().nextFloat() * 0.4f);
    }

    private void playRemoveSound(Player player) {
        player.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8f, 0.8f + player.level().getRandom().nextFloat() * 0.4f);
    }

    // ==== Durability bar shows capacity usage (like TacZ AmmoBox) ====

    @Override
    public boolean isBarVisible(ItemStack pouch) {
        return getTotalCount(pouch) > 0;
    }

    @Override
    public int getBarWidth(ItemStack pouch) {
        int cap = getCapacity();
        if (cap <= 0) {
            return 0;
        }
        double pct = (double) getTotalCount(pouch) / cap;
        return (int) Math.min(1.0 + 12.0 * pct, 13.0);
    }

    @Override
    public int getBarColor(ItemStack pouch) {
        return Mth.hsvToRgb(0.33333334f, 1.0f, 1.0f); // green, same as ammo box
    }
}

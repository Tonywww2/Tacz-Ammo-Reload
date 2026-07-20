package com.tacz_caliber_ammo.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.item.IAmmo;
import com.tacz_caliber_ammo.caliber.PatternEntry;
import com.tacz_caliber_ammo.caliber.Round;
import com.tacz_caliber_ammo.item.AmmoPouchItem;
import com.tacz_caliber_ammo.registry.ModMenus;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Ammo pouch container menu. Holds the player inventory slots plus a reference (by inventory-slot
 * index) to the pouch being edited. The pouch storage/pattern live as NBT on that ItemStack, drawn as
 * virtual slots by the screen. The pouch ItemStack syncs to the client through the container's
 * inventory slots, so the screen always reads fresh data from getPouch().
 */
public class AmmoPouchMenu extends AbstractContainerMenu {

    public static final int IMAGE_WIDTH = 176;
    public static final int IMAGE_HEIGHT = 244;

    private final Inventory playerInv;
    private final int pouchSlot;

    public AmmoPouchMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        this(id, playerInv, buf.readVarInt());
    }

    public AmmoPouchMenu(int id, Inventory playerInv, int pouchSlot) {
        super(ModMenus.AMMO_POUCH.get(), id);
        this.playerInv = playerInv;
        this.pouchSlot = pouchSlot;

        int backpackY = IMAGE_HEIGHT - 82;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, backpackY + row * 18));
            }
        }
        int hotbarY = IMAGE_HEIGHT - 24;
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 8 + col * 18, hotbarY));
        }
    }

    /** The pouch ItemStack, read live from the player's inventory slot the GUI was opened from. */
    public ItemStack getPouch() {
        return this.playerInv.getItem(this.pouchSlot);
    }

    public int getPouchSlot() {
        return this.pouchSlot;
    }

    /** Ordered storage list (ammoId + count) read live from the pouch NBT; for the screen to render. */
    public List<Round> getStorageList() {
        List<Round> list = new ArrayList<>();
        ItemStack pouch = getPouch();
        if (pouch.getItem() instanceof AmmoPouchItem) {
            for (Map.Entry<ResourceLocation, Integer> e : AmmoPouchItem.getStore(pouch).entrySet()) {
                list.add(new Round(e.getKey(), e.getValue()));
            }
        }
        return list;
    }

    /** Ordered reload pattern read live from the pouch NBT; for the screen to render. */
    public List<PatternEntry> getPatternList() {
        ItemStack pouch = getPouch();
        if (pouch.getItem() instanceof AmmoPouchItem) {
            return AmmoPouchItem.getPattern(pouch);
        }
        return new ArrayList<>();
    }

    @Override
    public boolean stillValid(Player player) {
        return this.getPouch().getItem() instanceof AmmoPouchItem;
    }

    /** Shift-click an inventory ammo stack to deposit it into the pouch (capacity-limited). */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        if (stack.getItem() instanceof IAmmo ammo
                && this.getPouch().getItem() instanceof AmmoPouchItem pouchItem) {
            ResourceLocation ammoId = ammo.getAmmoId(stack);
            if (ammoId != null && !DefaultAssets.EMPTY_AMMO_ID.equals(ammoId)) {
                int added = pouchItem.deposit(this.getPouch(), ammoId, stack.getCount());
                if (added > 0) {
                    stack.shrink(added);
                    slot.setChanged();
                }
            }
        }
        return ItemStack.EMPTY;
    }
}

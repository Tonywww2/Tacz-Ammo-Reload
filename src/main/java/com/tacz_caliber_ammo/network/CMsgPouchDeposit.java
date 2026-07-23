package com.tacz_caliber_ammo.network;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.item.IAmmo;
import com.tacz_caliber_ammo.item.AmmoPouchItem;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Deposit the ammo the player is holding on the cursor (the container's carried stack) into the pouch
 * storage (client -&gt; server). Sent when the player clicks a storage virtual slot while carrying an
 * ammo item. The carried stack is shrunk by however many rounds actually fit (capacity-limited).
 * The loader backend is registered by {@code PlatformNetwork}.
 */
public record CMsgPouchDeposit(int pouchSlot) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(this.pouchSlot);
    }

    public static CMsgPouchDeposit decode(FriendlyByteBuf buf) {
        return new CMsgPouchDeposit(buf.readVarInt());
    }

    public void handle(ServerPlayer player) {
        if (this.pouchSlot < 0 || this.pouchSlot >= player.getInventory().getContainerSize()) {
            return;
        }
        ItemStack pouch = player.getInventory().getItem(this.pouchSlot);
        if (!(pouch.getItem() instanceof AmmoPouchItem pouchItem)) {
            return;
        }
        ItemStack carried = player.containerMenu.getCarried();
        if (carried.isEmpty() || !(carried.getItem() instanceof IAmmo ammo)) {
            return;
        }
        ResourceLocation ammoId = ammo.getAmmoId(carried);
        if (ammoId == null || DefaultAssets.EMPTY_AMMO_ID.equals(ammoId)) {
            return;
        }
        int added = pouchItem.deposit(pouch, ammoId, carried.getCount());
        if (added > 0) {
            carried.shrink(added);
            player.containerMenu.setCarried(carried);
        }
    }
}

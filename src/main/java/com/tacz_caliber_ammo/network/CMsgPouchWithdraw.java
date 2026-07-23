package com.tacz_caliber_ammo.network;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.builder.AmmoItemBuilder;
import com.tacz_caliber_ammo.item.AmmoPouchItem;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Withdraw one stack (ammo index stack-size) of an ammo type from the pouch into the player
 * inventory (client -&gt; server). Sent when the player clicks a storage virtual slot in the pouch GUI.
 * The loader backend is registered by {@code PlatformNetwork}.
 */
public record CMsgPouchWithdraw(int pouchSlot, String ammoId) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(this.pouchSlot);
        buf.writeUtf(this.ammoId);
    }

    public static CMsgPouchWithdraw decode(FriendlyByteBuf buf) {
        return new CMsgPouchWithdraw(buf.readVarInt(), buf.readUtf());
    }

    public void handle(ServerPlayer player) {
        if (this.pouchSlot < 0 || this.pouchSlot >= player.getInventory().getContainerSize()) {
            return;
        }
        ItemStack pouch = player.getInventory().getItem(this.pouchSlot);
        if (!(pouch.getItem() instanceof AmmoPouchItem)) {
            return;
        }
        ResourceLocation id = ResourceLocation.tryParse(this.ammoId);
        if (id == null) {
            return;
        }
        int stackSize = TimelessAPI.getCommonAmmoIndex(id).map(index -> index.getStackSize()).orElse(64);
        int taken = AmmoPouchItem.withdraw(pouch, id, stackSize);
        if (taken > 0) {
            ItemStack ammo = AmmoItemBuilder.create().setId(id).setCount(taken).build();
            if (!player.getInventory().add(ammo)) {
                player.drop(ammo, false);
            }
        }
    }
}

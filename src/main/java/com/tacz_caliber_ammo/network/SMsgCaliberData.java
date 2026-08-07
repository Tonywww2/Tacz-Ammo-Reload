package com.tacz_caliber_ammo.network;

import com.tacz_caliber_ammo.caliber.CaliberManager;
import com.tacz_caliber_ammo.caliber.CaliberSyncSnapshot;

import net.minecraft.network.FriendlyByteBuf;

/** Server-authoritative caliber tables synchronized after login and datapack reload. */
public record SMsgCaliberData(CaliberSyncSnapshot snapshot) {

    public void encode(FriendlyByteBuf buffer) {
        this.snapshot.encode(buffer);
    }

    public static SMsgCaliberData decode(FriendlyByteBuf buffer) {
        return new SMsgCaliberData(CaliberSyncSnapshot.decode(buffer));
    }

    public void handle() {
        CaliberManager.applySyncSnapshot(this.snapshot);
    }
}
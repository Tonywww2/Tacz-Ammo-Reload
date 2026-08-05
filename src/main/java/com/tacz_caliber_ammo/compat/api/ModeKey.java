package com.tacz_caliber_ammo.compat.api;

import com.tacz_caliber_ammo.TaczCaliberAmmo;

import net.minecraft.resources.ResourceLocation;

public record ModeKey(ResourceLocation provider, String mode, ResourceLocation variant) {

    public static final ModeKey MAIN = new ModeKey(TaczCaliberAmmo.prefix("base"), "main", null);

    public String storageKey() {
        return this.provider + "|" + this.mode + "|" + (this.variant == null ? "" : this.variant.toString());
    }
}
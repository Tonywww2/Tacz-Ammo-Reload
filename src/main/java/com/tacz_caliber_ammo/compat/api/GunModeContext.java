package com.tacz_caliber_ammo.compat.api;

import net.minecraft.resources.ResourceLocation;

public record GunModeContext(ModeKey key, ResourceLocation source, ModeKind kind,
        ResourceLocation activeGunId, ResourceLocation logicalAmmoId) {
}
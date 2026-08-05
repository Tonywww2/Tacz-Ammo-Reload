package com.tacz_caliber_ammo.compat.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record ShotContext(ItemStack gun, GunModeContext mode, ResourceLocation ammoId,
        int upstreamBulletAmount) {
}
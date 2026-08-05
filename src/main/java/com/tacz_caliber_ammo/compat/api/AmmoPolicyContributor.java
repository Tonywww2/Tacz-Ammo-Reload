package com.tacz_caliber_ammo.compat.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

@FunctionalInterface
public interface AmmoPolicyContributor {

    AmmoDecision test(GunModeContext context, ItemStack gun, ResourceLocation ammoId);
}
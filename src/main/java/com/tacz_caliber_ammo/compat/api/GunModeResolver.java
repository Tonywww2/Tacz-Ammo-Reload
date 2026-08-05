package com.tacz_caliber_ammo.compat.api;

import java.util.Optional;

import net.minecraft.world.item.ItemStack;

@FunctionalInterface
public interface GunModeResolver {

    Optional<GunModeContext> resolve(ItemStack gun);
}
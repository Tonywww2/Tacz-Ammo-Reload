package com.tacz_caliber_ammo.compat.api;

import net.minecraft.resources.ResourceLocation;

public interface CompatibilityModule {

    ResourceLocation id();

    void register(CompatibilityRegistrar registrar);
}
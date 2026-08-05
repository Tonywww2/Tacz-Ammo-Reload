package com.tacz_caliber_ammo.compat.runtime;

import java.util.List;

import net.minecraft.resources.ResourceLocation;

public record CompatibilityDescriptor(ResourceLocation id, List<ModRequirement> requirements,
        String factoryClassName) {

    public CompatibilityDescriptor {
        requirements = List.copyOf(requirements);
    }
}
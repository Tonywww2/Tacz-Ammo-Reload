package com.tacz_caliber_ammo.platform;

import com.tacz_caliber_ammo.platform.config.ModConfig;

//? if forge {
import net.minecraftforge.fml.ModLoadingContext;
//?} else {
/*import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig.Type;
*///?}

public final class PlatformConfig {

    //? if forge {
    @SuppressWarnings("removal")
    public static void register() {
        ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON, ModConfig.SPEC);
    }
    //?} else {
    /*public static void register(ModContainer container) {
        container.registerConfig(Type.COMMON, ModConfig.SPEC);
    }
    *///?}

    private PlatformConfig() {
    }
}
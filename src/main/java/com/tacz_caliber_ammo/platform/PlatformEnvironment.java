package com.tacz_caliber_ammo.platform;

//? if forge {
import net.minecraftforge.fml.loading.FMLEnvironment;
//?} else {
/*import net.neoforged.fml.loading.FMLEnvironment;
*///?}

public final class PlatformEnvironment {

    public static boolean isProduction() {
        return FMLEnvironment.production;
    }

    private PlatformEnvironment() {
    }
}
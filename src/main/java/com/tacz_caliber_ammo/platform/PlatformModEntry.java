package com.tacz_caliber_ammo.platform;

import com.tacz_caliber_ammo.TaczCaliberAmmo;

//? if forge {
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
//?} else {
/*import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
*///?}

@Mod(TaczCaliberAmmo.MODID)
public final class PlatformModEntry {

    //? if forge {
    @SuppressWarnings("removal")
    public PlatformModEntry() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        PlatformRegistries.register(modBus);
        PlatformConfig.register();
        TaczCaliberAmmo.LOGGER.info("[{}] TacZ Caliber Ammo loaded.", TaczCaliberAmmo.MODID);
    }
    //?} else {
    /*public PlatformModEntry(IEventBus modBus, ModContainer container) {
        PlatformRegistries.register(modBus);
        PlatformConfig.register(container);
        TaczCaliberAmmo.LOGGER.info("[{}] TacZ Caliber Ammo loaded.", TaczCaliberAmmo.MODID);
    }
    *///?}
}
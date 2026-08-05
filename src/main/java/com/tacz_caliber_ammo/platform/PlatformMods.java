package com.tacz_caliber_ammo.platform;

import java.util.Optional;

//? if forge {
import net.minecraftforge.fml.ModList;
//?} else {
/*import net.neoforged.fml.ModList;
*///?}

public final class PlatformMods {

    public static boolean isLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    public static Optional<String> getVersion(String modId) {
        return ModList.get().getModContainerById(modId)
                .map(container -> container.getModInfo().getVersion().toString());
    }

    private PlatformMods() {
    }
}
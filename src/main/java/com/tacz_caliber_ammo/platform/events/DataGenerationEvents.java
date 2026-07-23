package com.tacz_caliber_ammo.platform.events;

import com.tacz_caliber_ammo.TaczCaliberAmmo;
import com.tacz_caliber_ammo.datagen.ModDataGen;

//? if forge {
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?} else {
/*import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;
*///?}

//? if forge {
@Mod.EventBusSubscriber(modid = TaczCaliberAmmo.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
//?} else {
/*@EventBusSubscriber(modid = TaczCaliberAmmo.MODID, bus = EventBusSubscriber.Bus.MOD)
*///?}
public final class DataGenerationEvents {

    @SubscribeEvent
    public static void onGatherData(GatherDataEvent event) {
        ModDataGen.gather(event.getGenerator(), event.getGenerator().getPackOutput(), event.includeServer());
    }

    private DataGenerationEvents() {
    }
}
package com.tacz_caliber_ammo.platform.events;

import com.tacz.guns.init.ModCreativeTabs;
import com.tacz_caliber_ammo.TaczCaliberAmmo;
import com.tacz_caliber_ammo.registry.ModItems;

//? if forge {
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?} else {
/*import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
*///?}

//? if forge {
@Mod.EventBusSubscriber(modid = TaczCaliberAmmo.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
//?} else {
/*@EventBusSubscriber(modid = TaczCaliberAmmo.MODID, bus = EventBusSubscriber.Bus.MOD)
*///?}
public final class CreativeTabEvents {

    @SubscribeEvent
    public static void onBuildTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == ModCreativeTabs.OTHER_TAB.getKey()) {
            event.accept(ModItems.AMMO_POUCH.get());
        }
    }

    private CreativeTabEvents() {
    }
}
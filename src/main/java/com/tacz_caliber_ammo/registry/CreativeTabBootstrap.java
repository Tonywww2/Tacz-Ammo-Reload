package com.tacz_caliber_ammo.registry;

import com.tacz.guns.init.ModCreativeTabs;
import com.tacz_caliber_ammo.TaczCaliberAmmo;

import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Adds the ammo pouch to TacZ's ammo creative tab (ModCreativeTabs.AMMO_TAB). Self-registered on the
 * MOD event bus via @Mod.EventBusSubscriber; the main class is not modified.
 */
@Mod.EventBusSubscriber(modid = TaczCaliberAmmo.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CreativeTabBootstrap {

    private CreativeTabBootstrap() {
    }

    @SubscribeEvent
    public static void onBuildTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == ModCreativeTabs.OTHER_TAB.getKey()) {
            event.accept(ModItems.AMMO_POUCH.get());
        }
    }
}

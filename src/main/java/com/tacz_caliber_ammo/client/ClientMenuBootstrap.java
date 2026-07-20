package com.tacz_caliber_ammo.client;

import com.tacz_caliber_ammo.registry.ModMenus;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Registers the ammo pouch menu screen (MOD bus, client only). Self-registered via
 * @Mod.EventBusSubscriber; the main class is not modified.
 */
@Mod.EventBusSubscriber(modid = "tacz_caliber_ammo", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientMenuBootstrap {

    private ClientMenuBootstrap() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(ModMenus.AMMO_POUCH.get(), AmmoPouchScreen::new));
    }
}

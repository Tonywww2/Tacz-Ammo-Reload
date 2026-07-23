package com.tacz_caliber_ammo.platform.client;

import com.tacz_caliber_ammo.TaczCaliberAmmo;
import com.tacz_caliber_ammo.client.AmmoPouchScreen;
import com.tacz_caliber_ammo.client.ClientPouchTooltip;
import com.tacz_caliber_ammo.item.PouchTooltipData;
import com.tacz_caliber_ammo.registry.ModMenus;

import net.minecraft.client.gui.screens.MenuScreens;
//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.client.event.RegisterItemDecorationsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
//?} else {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
*///?}

//? if forge {
@Mod.EventBusSubscriber(modid = TaczCaliberAmmo.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
//?} else {
/*@EventBusSubscriber(modid = TaczCaliberAmmo.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
*///?}
public final class ClientModEvents {

    //? if forge {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(ModMenus.AMMO_POUCH.get(), AmmoPouchScreen::new));
    }
    //?} else {
    /*@SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.AMMO_POUCH.get(), AmmoPouchScreen::new);
    }
    *///?}

    @SubscribeEvent
    public static void onRegisterTooltipComponents(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(PouchTooltipData.class, ClientPouchTooltip::new);
    }

    @SubscribeEvent
    public static void onRegisterItemDecorations(RegisterItemDecorationsEvent event) {
        AmmoCodeDecorator.register(event);
    }

    private ClientModEvents() {
    }
}
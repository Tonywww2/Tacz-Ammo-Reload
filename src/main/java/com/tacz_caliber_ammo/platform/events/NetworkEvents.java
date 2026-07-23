package com.tacz_caliber_ammo.platform.events;

import com.tacz_caliber_ammo.TaczCaliberAmmo;
import com.tacz_caliber_ammo.platform.PlatformNetwork;

//? if forge {
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
//?} else {
/*import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
*///?}

//? if forge {
@Mod.EventBusSubscriber(modid = TaczCaliberAmmo.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
//?} else {
/*@EventBusSubscriber(modid = TaczCaliberAmmo.MODID, bus = EventBusSubscriber.Bus.MOD)
*///?}
public final class NetworkEvents {

    //? if forge {
    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(PlatformNetwork::register);
    }
    //?} else {
    /*@SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PlatformNetwork.register(event);
    }
    *///?}

    private NetworkEvents() {
    }
}
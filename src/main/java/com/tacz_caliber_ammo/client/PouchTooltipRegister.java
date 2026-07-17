package com.tacz_caliber_ammo.client;

import com.tacz_caliber_ammo.item.PouchTooltipData;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Registers the client renderer for the ammo pouch tooltip table (MOD bus, client only).
 * Self-registered via @Mod.EventBusSubscriber; the main class is not modified.
 */
@Mod.EventBusSubscriber(modid = "tacz_caliber_ammo", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class PouchTooltipRegister {

    private PouchTooltipRegister() {
    }

    @SubscribeEvent
    public static void onRegister(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(PouchTooltipData.class, ClientPouchTooltip::new);
    }
}

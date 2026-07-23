package com.tacz_caliber_ammo.platform.events;

import com.tacz.guns.api.event.common.EntityHurtByGunEvent;
import com.tacz.guns.api.event.common.EntityKillByGunEvent;
import com.tacz.guns.api.event.server.AmmoHitBlockEvent;
import com.tacz_caliber_ammo.TaczCaliberAmmo;
import com.tacz_caliber_ammo.caliber.CaliberDataBootstrap;
import com.tacz_caliber_ammo.caliber.GunCaliberModifyBootstrap;
import com.tacz_caliber_ammo.effect.AmmoEffectEvents;
import com.tacz_caliber_ammo.effect.EffectScriptManager;
import com.tacz_caliber_ammo.event.BulletCreatedEvent;

//? if forge {
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?} else {
/*import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
*///?}

//? if forge {
@Mod.EventBusSubscriber(modid = TaczCaliberAmmo.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
//?} else {
/*@EventBusSubscriber(modid = TaczCaliberAmmo.MODID, bus = EventBusSubscriber.Bus.GAME)
*///?}
public final class CommonGameEvents {

    @SubscribeEvent
    public static void onReload(AddReloadListenerEvent event) {
        event.addListener(CaliberDataBootstrap.listener());
        event.addListener(GunCaliberModifyBootstrap.listener());
        event.addListener(EffectScriptManager.listener());
    }

    @SubscribeEvent
    public static void onEntityHurt(EntityHurtByGunEvent.Post event) {
        AmmoEffectEvents.onEntityHurt(event);
    }

    @SubscribeEvent
    public static void onEntityKill(EntityKillByGunEvent event) {
        AmmoEffectEvents.onEntityKill(event);
    }

    @SubscribeEvent
    public static void onHitBlock(AmmoHitBlockEvent event) {
        AmmoEffectEvents.onHitBlock(event);
    }

    @SubscribeEvent
    public static void onBulletCreated(BulletCreatedEvent event) {
        AmmoEffectEvents.onBulletCreated(event.getBullet());
    }

    //? if forge {
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            AmmoEffectEvents.onServerTick();
        }
    }
    //?} else {
    /*@SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        AmmoEffectEvents.onServerTick();
    }
    *///?}

    private CommonGameEvents() {
    }
}
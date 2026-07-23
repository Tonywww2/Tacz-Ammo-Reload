package com.tacz_caliber_ammo.platform;

import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz_caliber_ammo.event.BulletCreatedEvent;

//? if forge {
import net.minecraftforge.common.MinecraftForge;
//?} else {
/*import net.neoforged.neoforge.common.NeoForge;
*///?}

public final class EventBridge {

    public static void postBulletCreated(EntityKineticBullet bullet) {
        //? if forge {
        MinecraftForge.EVENT_BUS.post(new BulletCreatedEvent(bullet));
        //?} else {
        /*NeoForge.EVENT_BUS.post(new BulletCreatedEvent(bullet));
        *///?}
    }

    private EventBridge() {
    }
}
package com.tacz_caliber_ammo.platform;

import net.minecraft.world.entity.Entity;

public final class PlatformEntity {

    public static void ignite(Entity entity, int seconds) {
        //? if forge {
        entity.setSecondsOnFire(seconds);
        //?} else {
        /*entity.igniteForSeconds(seconds);
        *///?}
    }

    private PlatformEntity() {
    }
}
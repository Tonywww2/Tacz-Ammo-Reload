package com.tacz_caliber_ammo.event;

import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz_caliber_ammo.platform.PlatformEvent;

import net.minecraft.resources.ResourceLocation;

/** Fired on the logical server after a TacZ kinetic bullet is fully constructed and before spawn. */
public final class BulletCreatedEvent extends PlatformEvent {

    private final EntityKineticBullet bullet;

    public BulletCreatedEvent(EntityKineticBullet bullet) {
        this.bullet = bullet;
    }

    public EntityKineticBullet getBullet() {
        return bullet;
    }

    public ResourceLocation getAmmoId() {
        return bullet.getAmmoId();
    }
}
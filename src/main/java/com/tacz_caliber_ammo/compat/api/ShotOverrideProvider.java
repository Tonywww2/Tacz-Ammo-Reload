package com.tacz_caliber_ammo.compat.api;

import java.util.OptionalInt;

@FunctionalInterface
public interface ShotOverrideProvider {

    OptionalInt resolveBulletAmount(ShotContext context);
}
package com.tacz_caliber_ammo.compat.api;

public interface CompatibilityRegistrar {

    void registerGunModeResolver(GunModeResolver resolver, int priority);

    void registerAmmoPolicy(AmmoPolicyContributor policy, int priority);

    void registerShotOverride(ShotOverrideProvider provider, int priority);
}
package com.tacz_caliber_ammo.compat.builtin;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz_caliber_ammo.TaczCaliberAmmo;
import com.tacz_caliber_ammo.caliber.AmmoProfile;
import com.tacz_caliber_ammo.caliber.CaliberManager;
import com.tacz_caliber_ammo.compat.api.AmmoDecision;
import com.tacz_caliber_ammo.compat.api.CompatibilityModule;
import com.tacz_caliber_ammo.compat.api.CompatibilityRegistrar;
import com.tacz_caliber_ammo.compat.api.GunModeContext;
import com.tacz_caliber_ammo.compat.api.ModeKey;
import com.tacz_caliber_ammo.compat.api.ModeKind;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class BaseTacZCompatibility implements CompatibilityModule {

    public static final ResourceLocation ID = TaczCaliberAmmo.prefix("base_tacz");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void register(CompatibilityRegistrar registrar) {
        registrar.registerGunModeResolver(this::resolveMode, Integer.MIN_VALUE);
        registrar.registerAmmoPolicy(this::testAmmo, Integer.MIN_VALUE);
        registrar.registerShotOverride(context -> {
            if (context.ammoId() == null) {
                return OptionalInt.empty();
            }
            AmmoProfile profile = CaliberManager.getAmmoProfile(context.ammoId());
            return profile != null && profile.pelletCount() > 0
                    ? OptionalInt.of(profile.pelletCount()) : OptionalInt.empty();
        }, Integer.MIN_VALUE);
    }

    private Optional<GunModeContext> resolveMode(ItemStack gun) {
        IGun item = IGun.getIGunOrNull(gun);
        if (item == null) {
            return Optional.empty();
        }
        ResourceLocation gunId = item.getGunId(gun);
        ResourceLocation ammoId = TimelessAPI.getCommonGunIndex(gunId)
                .map(index -> index.getGunData().getAmmoId()).orElse(null);
        return Optional.of(new GunModeContext(ModeKey.MAIN, ID, ModeKind.MAIN, gunId, ammoId));
    }

    private AmmoDecision testAmmo(GunModeContext context, ItemStack gun, ResourceLocation ammoId) {
        ResourceLocation ammoCaliber = CaliberManager.getAmmoCaliber(ammoId);
        if (CaliberManager.UNIVERSAL.equals(ammoCaliber)) {
            return AmmoDecision.ALLOW;
        }
        Set<ResourceLocation> gunCalibers = CaliberManager.getGunCalibers(context.activeGunId());
        if (!gunCalibers.isEmpty()) {
            return gunCalibers.contains(ammoCaliber) ? AmmoDecision.ALLOW : AmmoDecision.DENY;
        }
        boolean matchesDefault = TimelessAPI.getCommonGunIndex(context.activeGunId())
                .map(index -> index.getGunData().getAmmoId().equals(ammoId)).orElse(false);
        return matchesDefault ? AmmoDecision.ALLOW : AmmoDecision.DENY;
    }
}
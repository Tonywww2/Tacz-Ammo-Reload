package com.tacz_caliber_ammo.compat.builtin.arcana;

import java.util.Optional;
import java.util.OptionalInt;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz_caliber_ammo.caliber.CaliberManager;
import com.tacz_caliber_ammo.compat.api.AmmoDecision;
import com.tacz_caliber_ammo.compat.api.CompatibilityModule;
import com.tacz_caliber_ammo.compat.api.CompatibilityRegistrar;
import com.tacz_caliber_ammo.compat.api.GunModeContext;
import com.tacz_caliber_ammo.compat.api.ModeKey;
import com.tacz_caliber_ammo.compat.api.ModeKind;
import com.tacz_caliber_ammo.platform.PlatformItemData;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class ArcanaCompatibilityModule implements CompatibilityModule {

    public static final ResourceLocation ID = ResourceLocation.tryParse("taczexpands:arcana");
    private static final String GUN_ID = "GunId";

    private final ArcanaAccess access;

    public ArcanaCompatibilityModule() throws ReflectiveOperationException {
        this.access = new ArcanaAccess();
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void register(CompatibilityRegistrar registrar) {
        registrar.registerGunModeResolver(this::resolveMode, 1_000);
        registrar.registerAmmoPolicy(this::testAmmo, 1_000);
        registrar.registerShotOverride(context -> {
            try {
            GunData gunData = TimelessAPI.getCommonGunIndex(context.mode().activeGunId())
                .map(index -> index.getGunData()).orElse(null);
            return gunData != null && this.access.hasBulletAmountOverride(
                gunData, context.gun(), context.upstreamBulletAmount())
                    ? OptionalInt.of(context.upstreamBulletAmount()) : OptionalInt.empty();
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Unable to read Arcana bullet override", exception);
            }
        }, 1_000);
    }

    private Optional<GunModeContext> resolveMode(ItemStack gun) {
        IGun item = IGun.getIGunOrNull(gun);
        if (item == null) {
            return Optional.empty();
        }
        ResourceLocation activeGunId = item.getGunId(gun);
        GunData gunData = TimelessAPI.getCommonGunIndex(activeGunId)
                .map(index -> index.getGunData()).orElse(null);
        if (gunData == null) {
            return Optional.empty();
        }
        try {
            ResourceLocation logicalAmmoId = this.access.getCurrentAmmoId(gunData, gun);
            boolean underbarrel = isUsingUnderbarrel(gun, activeGunId);
            ModeKey key = underbarrel ? new ModeKey(ID, "underbarrel", activeGunId) : ModeKey.MAIN;
            ModeKind kind = underbarrel ? ModeKind.UNDERBARREL : ModeKind.MAIN;
            return Optional.of(new GunModeContext(key, ID, kind, activeGunId, logicalAmmoId));
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to resolve Arcana gun mode", exception);
        }
    }

    private AmmoDecision testAmmo(GunModeContext context, ItemStack gun, ResourceLocation ammoId) {
        if (!ID.equals(context.source())) {
            return AmmoDecision.PASS;
        }
        GunData gunData = TimelessAPI.getCommonGunIndex(context.activeGunId())
                .map(index -> index.getGunData()).orElse(null);
        if (gunData == null || context.logicalAmmoId() == null
                || context.logicalAmmoId().equals(gunData.getAmmoId())) {
            return AmmoDecision.PASS;
        }
        ResourceLocation candidateCaliber = CaliberManager.getAmmoCaliber(ammoId);
        if (CaliberManager.UNIVERSAL.equals(candidateCaliber)) {
            return AmmoDecision.ALLOW;
        }
        ResourceLocation logicalCaliber = CaliberManager.getAmmoCaliber(context.logicalAmmoId());
        if (CaliberManager.NONE.equals(logicalCaliber)) {
            return context.logicalAmmoId().equals(ammoId) ? AmmoDecision.ALLOW : AmmoDecision.DENY;
        }
        return logicalCaliber.equals(candidateCaliber) ? AmmoDecision.ALLOW : AmmoDecision.DENY;
    }

    private static boolean isUsingUnderbarrel(ItemStack gun, ResourceLocation activeGunId) {
        CompoundTag root = PlatformItemData.copy(gun);
        ResourceLocation storedGunId = ResourceLocation.tryParse(root.getString(GUN_ID));
        if (storedGunId == null) {
            return false;
        }
        return !storedGunId.equals(activeGunId);
    }
}
package com.tacz_caliber_ammo.mixin.client;

import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.gui.overlay.GunHudOverlay;
import com.tacz_caliber_ammo.caliber.CaliberManager;
import com.tacz_caliber_ammo.reload.PouchReloadSource;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * HUD inventory-ammo counter patch. TacZ GunHudOverlay.handleInventoryAmmo only sums loose inventory
 * ammo / ammo boxes, so ammo stored inside an Ammo Pouch is not counted and the bottom-right stock
 * reads 0. This adds the hotbar pouch's matching-caliber ammo count. TacZ-owned, remap=false.
 */
@Mixin(value = GunHudOverlay.class, remap = false)
public class GunHudOverlayMixin {

    @Shadow
    private static int cacheInventoryAmmoCount;

    @Inject(method = "handleInventoryAmmo", at = @At("TAIL"))
    private static void tacz_caliber_ammo$addPouchAmmo(ItemStack stack, Inventory inventory, CallbackInfo ci) {
        if (cacheInventoryAmmoCount >= 9999) {
            return; // creative box already maxed the counter
        }
        if (!(stack.getItem() instanceof IGun gun)) {
            return;
        }
        Set<ResourceLocation> calibers = CaliberManager.getGunCalibers(gun.getGunId(stack));
        cacheInventoryAmmoCount += PouchReloadSource.countUsableAmmo(inventory, calibers);
    }
}

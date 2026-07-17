package com.tacz_caliber_ammo.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.entity.shooter.ShooterDataHolder;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.util.AttachmentDataUtils;
import com.tacz_caliber_ammo.caliber.CaliberManager;
import com.tacz_caliber_ammo.duck.ISpeedDecayBullet;
import com.tacz_caliber_ammo.reload.PouchReloadSource;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;

/**
 * Patches on TacZ AbstractGunItem (TacZ-owned methods, remap=false):
 * <ul>
 * <li>dropAllAmmo: cancelled. It only drops the magazine, ignores the chambered round, and returns
 * the gun default ammo type (losing mixed loaded types); in creative it refills instead of emptying.
 * Replaced by reload.AttachmentAmmoHandler (network-packet hook) using our LoadedSeq real types.</li>
 * <li>canReload: patched so ammo held in an Ammo Pouch counts as available ammo.</li>
 * <li>doBulletSpread: sets flight friction (speed decay) for this mod's ammo before spawn.</li>
 * </ul>
 */
@Mixin(AbstractGunItem.class)
public class AbstractGunItemMixin {

    @Inject(method = "dropAllAmmo", at = @At("HEAD"), cancellable = true, remap = false)
    private void tacz_caliber_ammo$cancelVanillaDropAllAmmo(Player player, ItemStack gunItem, CallbackInfo ci) {
        ci.cancel();
    }

    /**
     * Ammo gate patch: TacZ canReload only counts loose inventory ammo / ammo boxes, so when all ammo
     * is inside an Ammo Pouch it reports "cannot reload" and LocalPlayerReload rejects the reload,
     * meaning consumeAmmoFromPlayer never runs. When canReload returned false AND the magazine is not
     * full AND the gun is not INVENTORY/dummy fed AND a hotbar pouch holds matching ammo, force true.
     */
    @Inject(method = "canReload", at = @At("RETURN"), cancellable = true, remap = false)
    private void tacz_caliber_ammo$pouchCanReload(LivingEntity shooter, ItemStack gunItem,
            CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            return;
        }
        if (!(shooter instanceof Player player) || !(gunItem.getItem() instanceof IGun gun)) {
            return;
        }
        AbstractGunItem self = (AbstractGunItem) (Object) this;
        if (self.useInventoryAmmo(gunItem) || self.useDummyAmmo(gunItem)) {
            return;
        }
        ResourceLocation gunId = gun.getGunId(gunItem);
        CommonGunIndex idx = TimelessAPI.getCommonGunIndex(gunId).orElse(null);
        if (idx == null) {
            return;
        }
        if (self.getCurrentAmmoCount(gunItem) >= AttachmentDataUtils.getAmmoCountWithAttachment(gunItem,
                idx.getGunData())) {
            return;
        }
        if (PouchReloadSource.hasUsableAmmo(player, CaliberManager.getGunCalibers(gunId))) {
            cir.setReturnValue(true);
        }
    }

    /**
     * Ammo gate patch for INVENTORY-fed guns (e.g. M134 minigun). hasInventoryAmmo only counts loose
     * inventory ammo / ammo boxes, so when all matching ammo sits inside an Ammo Pouch it reports
     * "no ammo" and LivingEntityShoot / reduceAmmoOnce refuse to fire (consumeAmmoFromPlayer, where the
     * pouch actually supplies, then never runs). Only INVENTORY-fed guns consult hasInventoryAmmo for
     * their shoot gate (magazine guns use ammoCount), so this is scoped to useInventoryAmmo &amp;&amp; !dummy;
     * a hotbar pouch holding matching-caliber ammo forces true. Runs on both sides (client predict + server).
     */
    @Inject(method = "hasInventoryAmmo", at = @At("RETURN"), cancellable = true, remap = false)
    private void tacz_caliber_ammo$pouchHasInventoryAmmo(LivingEntity shooter, ItemStack gun, boolean needCheckAmmo,
            CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            return; // already has loose inventory ammo
        }
        AbstractGunItem self = (AbstractGunItem) (Object) this;
        if (!self.useInventoryAmmo(gun) || self.useDummyAmmo(gun)) {
            return; // non-inventory guns' false is correct; dummy ammo handled by vanilla
        }
        if (!(shooter instanceof Player player) || !(gun.getItem() instanceof IGun iGun)) {
            return;
        }
        ResourceLocation gunId = iGun.getGunId(gun);
        if (PouchReloadSource.hasUsableAmmo(player, CaliberManager.getGunCalibers(gunId))) {
            cir.setReturnValue(true);
        }
    }

    /**
     * After bullet spread (muzzle velocity set) and before spawn, set flight friction for this mod's
     * ammo by ballistic coefficient (speed decay, see ISpeedDecayBullet). Placed here instead of
     * EntityKineticBullet.shoot(): TacZ fires via MC Projectile.shootFromRotation (m_37251_), not EKB's
     * own shoot overload, and that MC method is inherited so it cannot be injected on EKB directly.
     * Setting it before spawn lets friction sync to the client with spawn data.
     */
    @Inject(method = "doBulletSpread", at = @At("TAIL"), remap = false)
    private void tacz_caliber_ammo$initBulletSpeedDecay(ShooterDataHolder dataHolder, ItemStack gunItem,
            LivingEntity shooter, Projectile projectile, int bulletCnt, float processedSpeed, float inaccuracy,
            float pitch, float yaw, CallbackInfo ci) {
        if (projectile instanceof ISpeedDecayBullet bullet) {
            bullet.taczCaliberAmmo$initSpeedDecay();
        }
    }
}

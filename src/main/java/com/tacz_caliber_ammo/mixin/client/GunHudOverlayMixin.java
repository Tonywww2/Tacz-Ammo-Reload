package com.tacz_caliber_ammo.mixin.client;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.gui.overlay.GunHudOverlay;
import com.tacz_caliber_ammo.client.TooltipHandler;
import com.tacz_caliber_ammo.compat.api.GunModeContext;
import com.tacz_caliber_ammo.compat.runtime.CompatibilityRuntime;
import com.tacz_caliber_ammo.reload.PouchReloadSource;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
//? if forge {
import net.minecraftforge.client.gui.overlay.ForgeGui;
//?}

/**
 * HUD inventory-ammo counter patch. TacZ GunHudOverlay.handleInventoryAmmo only sums loose inventory
 * ammo / ammo boxes, so ammo stored inside an Ammo Pouch is not counted and the bottom-right stock
 * reads 0. This adds the hotbar pouch's matching-caliber ammo count. TacZ-owned, remap=false.
 */
@Mixin(value = GunHudOverlay.class, priority = 900, remap = false)
public class GunHudOverlayMixin {

    private static final ResourceLocation ARCANA_ID = ResourceLocation.tryParse("taczexpands:arcana");

    @Shadow
    private static int cacheInventoryAmmoCount;

    @Inject(method = "handleInventoryAmmo", at = @At("TAIL"))
    private static void tacz_caliber_ammo$addPouchAmmo(ItemStack stack, Inventory inventory, CallbackInfo ci) {
        if (cacheInventoryAmmoCount >= 9999) {
            return; // creative box already maxed the counter
        }
        if (!(stack.getItem() instanceof IGun)) {
            return;
        }
        cacheInventoryAmmoCount += PouchReloadSource.countUsableAmmo(inventory, stack);
    }

    //? if forge {
    @Inject(method = "render", at = @At("TAIL"), require = 0)
    private void tacz_caliber_ammo$replaceArcanaAmmoIcon(ForgeGui gui, GuiGraphics graphics,
            float partialTick, int screenWidth, int screenHeight, CallbackInfo ci) {
        List<String> caliberNames = tacz_caliber_ammo$arcanaCaliberNames();
        if (caliberNames.isEmpty()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        int iconX = screenWidth - 139;
        int iconY = screenHeight - 42;
        int rowRight = iconX + 16;
        int tagY = Math.max(2, iconY - (caliberNames.size() - 1) * 18);
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 200);
        for (String caliberName : caliberNames) {
            int textWidth = minecraft.font.width(caliberName);
            int left = Math.max(2, rowRight - textWidth - 4);
            graphics.fill(left, tagY, rowRight, tagY + 16, 0xFF101010);
            graphics.drawString(minecraft.font, caliberName, left + 2, tagY + 4, 0xFFFFFF, true);
            tagY += 18;
        }
        graphics.pose().popPose();
    }

    private static List<String> tacz_caliber_ammo$arcanaCaliberNames() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return List.of();
        }
        ItemStack gun = minecraft.player.getMainHandItem();
        if (!(gun.getItem() instanceof IGun)) {
            return List.of();
        }
        GunModeContext mode = CompatibilityRuntime.resolveGunMode(gun);
        if (!ARCANA_ID.equals(mode.source())) {
            return List.of();
        }
        return TooltipHandler.gunCaliberNames(mode.activeGunId());
    }
    //?}
}

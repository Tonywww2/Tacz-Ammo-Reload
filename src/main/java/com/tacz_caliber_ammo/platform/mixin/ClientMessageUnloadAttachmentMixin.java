package com.tacz_caliber_ammo.platform.mixin;

import com.tacz.guns.network.message.ClientMessageUnloadAttachment;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import com.tacz_caliber_ammo.reload.AttachmentAmmoHandler;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Runs only after an attachment was actually returned to the inventory and removed from the gun. */
@Mixin(value = ClientMessageUnloadAttachment.class, remap = false)
public abstract class ClientMessageUnloadAttachmentMixin {

    @Redirect(
            //? if forge {
            method = "lambda$handle$0",
            //?} else {
            /*method = "lambda$handle$2",
            *///?}
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/tacz/guns/resource/modifier/AttachmentPropertyManager;postChangeEvent(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;)V"))
    private static void tacz_caliber_ammo$afterSuccessfulUnload(LivingEntity entity, ItemStack gun) {
        AttachmentPropertyManager.postChangeEvent(entity, gun);
        if (entity instanceof ServerPlayer player) {
            AttachmentAmmoHandler.returnAllAndClear(player, gun);
        }
    }
}
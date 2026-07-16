package com.tacz_caliber_ammo.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.tacz.guns.api.item.gun.AbstractGunItem;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 取消 TacZ 自带的 {@code dropAllAmmo}（仅在配件变更 {@code ClientMessageRefitGun}/{@code ClientMessageUnloadAttachment}
 * 里被调用）。它有三个缺陷：<b>只退弹仓、无视膛内那一发、且退回的是枪的默认弹种（丢失玩家实际装填的混装弹种）</b>；
 * 创造模式还会把弹仓补满而非清空。改由 {@code reload.AttachmentAmmoHandler}（网络包 hook）用我方 LoadedSeq 的
 * 真实弹种，退还「弹仓 + 膛内」全部弹药并强制清空弹匣。CR-1: {@code dropAllAmmo} 为 TacZ 自有方法, {@code remap=false}。
 */
@Mixin(AbstractGunItem.class)
public class AbstractGunItemMixin {

    @Inject(method = "dropAllAmmo", at = @At("HEAD"), cancellable = true, remap = false)
    private void tacz_caliber_ammo$cancelVanillaDropAllAmmo(Player player, ItemStack gunItem, CallbackInfo ci) {
        ci.cancel();
    }
}

package com.tacz_caliber_ammo.network;

import com.tacz.guns.api.item.IGun;
import com.tacz_caliber_ammo.reload.UnloadHandler;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * 退弹请求包（client -&gt; server）。冻结契约 by PA-1：携带枪所在槽位（快捷栏 selected）。
 * 编解码与 loader 后端由 {@code PlatformNetwork} 注册。
 */
public record CMsgUnloadAmmo(int gunSlot) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(this.gunSlot);
    }

    public static CMsgUnloadAmmo decode(FriendlyByteBuf buf) {
        return new CMsgUnloadAmmo(buf.readVarInt());
    }

    public void handle(ServerPlayer player) {
        if (this.gunSlot < 0 || this.gunSlot >= player.getInventory().getContainerSize()) {
            return;
        }
        ItemStack gun = player.getInventory().getItem(this.gunSlot);
        if (IGun.getIGunOrNull(gun) != null) {
            UnloadHandler.unload(player, gun);
        }
    }
}

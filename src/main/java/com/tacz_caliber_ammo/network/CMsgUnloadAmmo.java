package com.tacz_caliber_ammo.network;

import java.util.function.Supplier;

import com.tacz.guns.api.item.IGun;
import com.tacz_caliber_ammo.reload.UnloadHandler;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

/**
 * 退弹请求包（client -&gt; server）。冻结契约 by PA-1：携带枪所在槽位（快捷栏 selected）。
 * PC-2 实现 encode/decode/handle；注册见 {@link ModNetwork#register()}。
 */
public record CMsgUnloadAmmo(int gunSlot) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(this.gunSlot);
    }

    public static CMsgUnloadAmmo decode(FriendlyByteBuf buf) {
        return new CMsgUnloadAmmo(buf.readVarInt());
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) {
                return;
            }
            if (this.gunSlot < 0 || this.gunSlot >= player.getInventory().getContainerSize()) {
                return;
            }
            ItemStack gun = player.getInventory().getItem(this.gunSlot);
            if (IGun.getIGunOrNull(gun) != null) {
                UnloadHandler.unload(player, gun);
            }
        });
        ctx.setPacketHandled(true);
    }
}

package com.tacz_caliber_ammo.mixin;

import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.tacz.guns.network.message.ClientMessageUnloadAttachment;
import com.tacz_caliber_ammo.reload.AttachmentAmmoHandler;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * 卸下配件后：返还弹匣内所有弹药给玩家并强制清空弹匣。做法同 {@link ClientMessageRefitGunMixin}：
 * 在 TacZ {@code handle} 的 {@code enqueueWork} 之后再排一个同队列任务，对手持枪执行
 * {@link AttachmentAmmoHandler#returnAllAndClear}。类级 {@code remap=false}。
 */
@Mixin(value = ClientMessageUnloadAttachment.class, remap = false)
public class ClientMessageUnloadAttachmentMixin {

    @Inject(method = "handle", at = @At("TAIL"))
    private static void tacz_caliber_ammo$afterUnloadAttachment(ClientMessageUnloadAttachment message,
            Supplier<NetworkEvent.Context> contextSupplier, CallbackInfo ci) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                AttachmentAmmoHandler.returnAllAndClear(player, player.getMainHandItem());
            }
        });
    }
}

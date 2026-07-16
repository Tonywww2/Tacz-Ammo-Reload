package com.tacz_caliber_ammo.mixin;

import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.tacz.guns.network.message.ClientMessageRefitGun;
import com.tacz_caliber_ammo.reload.AttachmentAmmoHandler;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * 安装/更换配件后：返还弹匣内所有弹药给玩家并强制清空弹匣。
 *
 * <p>在 TacZ {@code handle} 的 {@code enqueueWork} 之后再排一个同队列任务（服务端主线程按提交顺序执行，
 * 保证配件已装、状态已更新），对手持枪执行 {@link AttachmentAmmoHandler#returnAllAndClear}。类级 {@code remap=false}。
 */
@Mixin(value = ClientMessageRefitGun.class, remap = false)
public class ClientMessageRefitGunMixin {

    @Inject(method = "handle", at = @At("TAIL"))
    private static void tacz_caliber_ammo$afterRefitGun(ClientMessageRefitGun message,
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

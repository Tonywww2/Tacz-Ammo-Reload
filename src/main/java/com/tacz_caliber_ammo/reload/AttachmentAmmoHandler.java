package com.tacz_caliber_ammo.reload;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * 配件数据包钩子的稳定门面。实际退弹规则统一由 {@link UnloadHandler} 处理。
 */
public final class AttachmentAmmoHandler {

    private AttachmentAmmoHandler() {
    }

    /** 返还该枪弹匣内所有弹药（含膛内弹）给玩家，随后强制清空弹匣与我方序列。 */
    public static void returnAllAndClear(ServerPlayer player, ItemStack gun) {
        UnloadHandler.unload(player, gun);
    }
}

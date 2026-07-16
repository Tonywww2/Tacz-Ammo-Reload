package com.tacz_caliber_ammo.reload;

import java.util.Collections;
import java.util.List;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.builder.AmmoItemBuilder;
import com.tacz_caliber_ammo.caliber.Round;
import com.tacz_caliber_ammo.nbt.LoadedAmmoSequence;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * 配件（任意类型）装/卸之后：把弹匣里的所有弹药返还玩家，然后强制清空弹匣（{@code currentAmmoCount=0}）。
 *
 * <p>由 {@code ClientMessageRefitGunMixin} / {@code ClientMessageUnloadAttachmentMixin} 在服务端处理完
 * 配件变更后调用（事件驱动）。返还按玩家实际装填的（可能混装的）弹种，来自我方 {@link LoadedAmmoSequence}，
 * 而非 TacZ {@code dropAllAmmo} 的单一默认弹种；且不依赖 {@code dropAllAmmo}（它对 {@code useInventoryAmmo} 枪 early-return）。
 */
public final class AttachmentAmmoHandler {

    private AttachmentAmmoHandler() {
    }

    /** 返还该枪弹匣内所有弹药（含膛内弹）给玩家，随后强制清空弹匣与我方序列。 */
    public static void returnAllAndClear(ServerPlayer player, ItemStack gun) {
        IGun iGun = IGun.getIGunOrNull(gun);
        if (iGun == null) {
            return;
        }
        int count = iGun.getCurrentAmmoCount(gun);
        ResourceLocation def = TimelessAPI.getCommonGunIndex(iGun.getGunId(gun))
                .map(index -> index.getGunData().getAmmoId()).orElse(null);
        boolean hasBarrel = iGun.hasBulletInBarrel(gun);
        ResourceLocation barrelAmmo = LoadedAmmoSequence.peekBarrelAmmo(gun);

        // 先把序列对齐到弹匣数，以按玩家实际装填的（可能混装）弹种返还。
        LoadedAmmoSequence.reconcile(gun, count, def);
        List<Round> seq = LoadedAmmoSequence.getSequence(gun);

        // 返还弹药：创造 / dummy（虚拟弹药）/ useInventoryAmmo（弹药本就在背包、返还会翻倍）不返还，其余按弹种归还。
        if (!player.isCreative() && !iGun.useDummyAmmo(gun) && !iGun.useInventoryAmmo(gun)) {
            for (Round r : seq) {
                giveAmmo(player, r.ammoId(), r.count());
            }
            if (hasBarrel) {
                giveAmmo(player, barrelAmmo != null ? barrelAmmo : def, 1);
            }
        }

        // 强制清空：弹匣数归零 + 清膛 + 清我方弹匣序列与膛内弹记录。
        iGun.setCurrentAmmoCount(gun, 0);
        iGun.setBulletInBarrel(gun, false);
        LoadedAmmoSequence.setSequence(gun, Collections.emptyList());
        LoadedAmmoSequence.setBarrelAmmo(gun, null);
    }

    /** 把 amount 发 ammoId 弹药按堆叠上限分堆归还玩家（同 UnloadHandler 语义）。 */
    private static void giveAmmo(ServerPlayer player, ResourceLocation ammoId, int amount) {
        if (ammoId == null || amount <= 0) {
            return;
        }
        TimelessAPI.getCommonAmmoIndex(ammoId).ifPresent(ammoIndex -> {
            int stackSize = Math.max(1, ammoIndex.getStackSize());
            int remaining = amount;
            while (remaining > 0) {
                int give = Math.min(remaining, stackSize);
                ItemStack ammoItem = AmmoItemBuilder.create().setId(ammoId).setCount(give).build();
                ItemHandlerHelper.giveItemToPlayer(player, ammoItem);
                remaining -= give;
            }
        });
    }
}

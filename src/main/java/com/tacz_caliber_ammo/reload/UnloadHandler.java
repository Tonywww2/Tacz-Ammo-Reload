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
 * 退弹服务端逻辑门面（冻结契约 by PA-1）。PC-2 实现（读 LoadedSeq 逐类型归还 + 清空弹匣）。
 *
 * <p>与 TacZ 原 {@code AbstractGunItem.dropAllAmmo}（按单一 gunData.ammoId 归还）不同：本方法按
 * {@link LoadedAmmoSequence} 逐 (ammoId,count) 归还各自弹种；先 {@code reconcile} 到真实弹匣数以处理
 * 旧存档/外部装弹/desync。创造模式与无 item 弹药（dummy）只清空不归还，避免刷物品。
 */
public final class UnloadHandler {

    private UnloadHandler() {
    }

    /** 退出该枪弹匣内全部弹药，按 LoadedSeq 逐类型归还给玩家，并清空弹匣。 */
    public static void unload(ServerPlayer player, ItemStack gun) {
        IGun iGun = IGun.getIGunOrNull(gun);
        if (iGun == null) {
            return;
        }
        int count = iGun.getCurrentAmmoCount(gun);
        if (count <= 0) {
            LoadedAmmoSequence.setSequence(gun, Collections.emptyList());
            LoadedAmmoSequence.setBarrelAmmo(gun, null);
            return;
        }
        ResourceLocation defaultAmmo = TimelessAPI.getCommonGunIndex(iGun.getGunId(gun))
                .map(index -> index.getGunData().getAmmoId())
                .orElse(null);
        // 先按真实弹匣数对齐序列（旧存档/外部装弹/desync），再逐段读取。
        LoadedAmmoSequence.reconcile(gun, count, defaultAmmo);
        List<Round> sequence = LoadedAmmoSequence.getSequence(gun);
        if (sequence.isEmpty() && defaultAmmo != null) {
            sequence = List.of(new Round(defaultAmmo, count));
        }

        // 归还弹药：创造模式与 dummy 弹药（非 item）只清空不归还。
        ResourceLocation barrelAmmo = LoadedAmmoSequence.peekBarrelAmmo(gun);
        if (!player.isCreative() && !iGun.useDummyAmmo(gun)) {
            for (Round round : sequence) {
                giveAmmo(player, round.ammoId(), round.count());
            }
            // 膛内弹（TacZ 膛内那发，不在弹匣序列里）若有跟踪弹种也归还一发
            if (barrelAmmo != null && iGun.hasBulletInBarrel(gun)) {
                giveAmmo(player, barrelAmmo, 1);
            }
        }

        iGun.setCurrentAmmoCount(gun, 0);
        iGun.setBulletInBarrel(gun, false);
        LoadedAmmoSequence.setSequence(gun, Collections.emptyList());
        LoadedAmmoSequence.setBarrelAmmo(gun, null);
    }

    /** 把 amount 发 ammoId 弹药按其堆叠上限分堆归还给玩家（复用 TacZ 分堆语义）。 */
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

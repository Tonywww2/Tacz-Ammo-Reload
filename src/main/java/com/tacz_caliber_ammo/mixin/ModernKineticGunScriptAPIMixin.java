package com.tacz_caliber_ammo.mixin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.item.ModernKineticGunScriptAPI;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz_caliber_ammo.caliber.Round;
import com.tacz_caliber_ammo.nbt.LoadedAmmoSequence;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

/**
 * PB-2：换弹写序列 + 发射出队。
 * - 换弹：consumeAmmoFromPlayer 前后对背包弹药差分，得到实际消耗的 (ammoId,count)；
 *   putAmmoInMagazine 之后把它们追加进枪 LoadedSeq 并 reconcile 到新弹匣数。
 * - 发射：@Redirect 发射 lambda 里 gunData.getAmmoId()，改为出队 LoadedSeq 队首弹种（空则回退原弹种）。
 */
@Mixin(value = ModernKineticGunScriptAPI.class, remap = false)
public class ModernKineticGunScriptAPIMixin {

    @Shadow
    private LivingEntity shooter;
    @Shadow
    private ItemStack itemStack;
    @Shadow
    private AbstractGunItem abstractGunItem;
    @Shadow
    private CommonGunIndex gunIndex;

    @Unique
    private Map<Integer, Round> tacz_caliber_ammo$preSnap;
    @Unique
    private List<Round> tacz_caliber_ammo$pendingConsumed;

    /** 快照背包中每个含弹药/弹药盒槽位的 (ammoId, count)。 */
    @Unique
    private Map<Integer, Round> tacz_caliber_ammo$snapshot() {
        Map<Integer, Round> map = new LinkedHashMap<>();
        if (this.shooter == null) {
            return map;
        }
        this.shooter.getCapability(ForgeCapabilities.ITEM_HANDLER, null).ifPresent(cap -> {
            for (int i = 0; i < cap.getSlots(); i++) {
                ItemStack s = cap.getStackInSlot(i);
                Item it = s.getItem();
                if (it instanceof IAmmo ammo) {
                    map.put(i, new Round(ammo.getAmmoId(s), s.getCount()));
                } else if (it instanceof IAmmoBox box) {
                    map.put(i, new Round(box.getAmmoId(s), box.getAmmoCount(s)));
                }
            }
        });
        return map;
    }

    @Inject(method = "consumeAmmoFromPlayer", at = @At("HEAD"))
    private void tacz_caliber_ammo$preConsume(int neededAmount, CallbackInfoReturnable<Integer> cir) {
        this.tacz_caliber_ammo$preSnap = this.tacz_caliber_ammo$snapshot();
    }

    @Inject(method = "consumeAmmoFromPlayer", at = @At("RETURN"))
    private void tacz_caliber_ammo$postConsume(int neededAmount, CallbackInfoReturnable<Integer> cir) {
        Map<Integer, Round> pre = this.tacz_caliber_ammo$preSnap;
        this.tacz_caliber_ammo$preSnap = null;
        if (pre == null || pre.isEmpty()) {
            this.tacz_caliber_ammo$pendingConsumed = null;
            return;
        }
        Map<Integer, Round> post = this.tacz_caliber_ammo$snapshot();
        List<Round> consumed = new ArrayList<>();
        for (Map.Entry<Integer, Round> e : pre.entrySet()) {
            Round before = e.getValue();
            if (before.ammoId() == null) {
                continue;
            }
            Round after = post.get(e.getKey());
            int afterCount = (after != null && before.ammoId().equals(after.ammoId())) ? after.count() : 0;
            int delta = before.count() - afterCount;
            if (delta > 0) {
                consumed.add(new Round(before.ammoId(), delta));
            }
        }
        this.tacz_caliber_ammo$pendingConsumed = consumed;
    }

    @Inject(method = "putAmmoInMagazine", at = @At("RETURN"))
    private void tacz_caliber_ammo$afterPut(int amount, CallbackInfoReturnable<Integer> cir) {
        if (amount <= 0 || this.itemStack == null) {
            return;
        }
        List<Round> add = this.tacz_caliber_ammo$pendingConsumed;
        this.tacz_caliber_ammo$pendingConsumed = null;
        List<Round> seq = LoadedAmmoSequence.getSequence(this.itemStack);
        if (add != null && !add.isEmpty()) {
            seq.addAll(add);
        }
        LoadedAmmoSequence.setSequence(this.itemStack, seq);
        int count = this.abstractGunItem.getCurrentAmmoCount(this.itemStack);
        ResourceLocation def = null;
        if (add != null && !add.isEmpty()) {
            def = add.get(0).ammoId();
        } else {
            // 创造/未真正消耗背包（add 为空）：优先按背包内“该枪匹配口径”的弹药弹种装填/追踪，
            // 不扣除背包（保持创造无限）；背包无匹配弹药时才回退枪默认弹种（之前的换弹逻辑）。
            ResourceLocation invVariant = tacz_caliber_ammo$firstMatchingInventoryAmmo();
            if (invVariant != null) {
                def = invVariant;
            } else if (this.gunIndex != null) {
                def = this.gunIndex.getGunData().getAmmoId();
            }
        }
        LoadedAmmoSequence.reconcile(this.itemStack, count, def);
    }

    /** 背包内第一颗“该枪匹配口径”的弹药弹种（含弹药盒，跳过空盒/全类型创造盒）；无则 null。仅读取、不扣除。 */
    @Unique
    private ResourceLocation tacz_caliber_ammo$firstMatchingInventoryAmmo() {
        if (this.shooter == null || this.itemStack == null) {
            return null;
        }
        return this.shooter.getCapability(ForgeCapabilities.ITEM_HANDLER, null).map(cap -> {
            for (int i = 0; i < cap.getSlots(); i++) {
                ItemStack s = cap.getStackInSlot(i);
                Item it = s.getItem();
                if (it instanceof IAmmo ammo && ammo.isAmmoOfGun(this.itemStack, s)) {
                    return ammo.getAmmoId(s);
                }
                if (it instanceof IAmmoBox box && box.isAmmoBoxOfGun(this.itemStack, s)) {
                    ResourceLocation id = box.getAmmoId(s);
                    if (id != null && !DefaultAssets.EMPTY_AMMO_ID.equals(id)) {
                        return id;
                    }
                }
            }
            return null;
        }).orElse(null);
    }

    @Redirect(method = "lambda$shootOnce$2",
            at = @At(value = "INVOKE",
                    target = "Lcom/tacz/guns/resource/pojo/data/gun/GunData;getAmmoId()Lnet/minecraft/resources/ResourceLocation;"))
    private ResourceLocation tacz_caliber_ammo$dequeue(GunData gunData) {
        ResourceLocation popped = LoadedAmmoSequence.popNextRound(this.itemStack);
        return popped != null ? popped : gunData.getAmmoId();
    }
}

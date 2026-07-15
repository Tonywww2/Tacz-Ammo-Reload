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
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.item.ModernKineticGunScriptAPI;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.pojo.data.gun.BulletData;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz_caliber_ammo.caliber.AmmoProfile;
import com.tacz_caliber_ammo.caliber.CaliberManager;
import com.tacz_caliber_ammo.caliber.Round;
import com.tacz_caliber_ammo.config.ModConfig;
import com.tacz_caliber_ammo.nbt.LoadedAmmoSequence;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;

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
    /** 本次发射循环正出膛的弹种（dequeue 出队时记，供 doBulletSpread 每发覆写弹道用）。 */
    @Unique
    private ResourceLocation tacz_caliber_ammo$firingAmmoId;

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
        // 用 resolve() 取出 handler 后在能力回调外遍历：LazyOptional.map 的函数若返回 null，Forge 会
        // 包成 Optional.of(null) 抛 NPE（背包有该能力但无匹配弹药时必崩，见 reload 崩溃报告），故不在 map 里返回 null。
        IItemHandler cap = this.shooter.getCapability(ForgeCapabilities.ITEM_HANDLER, null).resolve().orElse(null);
        if (cap == null) {
            return null;
        }
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
    }

    @Redirect(method = "lambda$shootOnce$2",
            at = @At(value = "INVOKE",
                    target = "Lcom/tacz/guns/resource/pojo/data/gun/GunData;getAmmoId()Lnet/minecraft/resources/ResourceLocation;"))
    private ResourceLocation tacz_caliber_ammo$dequeue(GunData gunData) {
        ResourceLocation popped = LoadedAmmoSequence.popNextRound(this.itemStack);
        ResourceLocation id = popped != null ? popped : gunData.getAmmoId();
        this.tacz_caliber_ammo$firingAmmoId = id;
        return id;
    }

    // ==== 弹道：按“当前出膛弹种”每发覆写 初速 / 散布 / 弹丸数 ====

    /** 初速：弹药 speed(原始 m/s) × 配置 bulletSpeedScale / 20 = blocks/tick；speed<=0 或无档则保留 TacZ。 */
    @ModifyArg(method = "lambda$shootOnce$2",
            at = @At(value = "INVOKE",
                    target = "Lcom/tacz/guns/api/item/gun/AbstractGunItem;doBulletSpread(Lcom/tacz/guns/entity/shooter/ShooterDataHolder;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/projectile/Projectile;IFFFF)V"),
            index = 5)
    private float tacz_caliber_ammo$ammoSpeed(float processedSpeed) {
        ResourceLocation id = this.tacz_caliber_ammo$firingAmmoId;
        if (id == null) {
            return processedSpeed;
        }
        AmmoProfile p = CaliberManager.getAmmoProfile(id);
        if (p == null || p.speed() <= 0f) {
            return processedSpeed;
        }
        return (float) (p.speed() * ModConfig.bulletSpeedScale() / 20.0);
    }

    /** 精度：散布 ÷ (1 + 精度%/100)（正精度 = 更准 = 散布更小；精度% 可 >100；guard 分母 >0）。 */
    @ModifyArg(method = "lambda$shootOnce$2",
            at = @At(value = "INVOKE",
                    target = "Lcom/tacz/guns/api/item/gun/AbstractGunItem;doBulletSpread(Lcom/tacz/guns/entity/shooter/ShooterDataHolder;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/projectile/Projectile;IFFFF)V"),
            index = 6)
    private float tacz_caliber_ammo$ammoInaccuracy(float inaccuracy) {
        ResourceLocation id = this.tacz_caliber_ammo$firingAmmoId;
        if (id == null) {
            return inaccuracy;
        }
        AmmoProfile p = CaliberManager.getAmmoProfile(id);
        if (p == null) {
            return inaccuracy;
        }
        float denom = 1.0f + p.accuracyModifier() / 100.0f;
        if (denom < 0.01f) {
            denom = 0.01f;
        }
        return inaccuracy / denom;
    }

    /** 弹丸数：预读队首弹种；pelletCount>0 时覆写每发子弹数（每次扣扳机级，非逐发）。 */
    @Redirect(method = "shootOnce",
            at = @At(value = "INVOKE",
                    target = "Lcom/tacz/guns/resource/pojo/data/gun/BulletData;getBulletAmount()I"))
    private int tacz_caliber_ammo$ammoPelletCount(BulletData bulletData) {
        ResourceLocation next = LoadedAmmoSequence.peekHead(this.itemStack);
        if (next != null) {
            AmmoProfile p = CaliberManager.getAmmoProfile(next);
            if (p != null && p.pelletCount() > 0) {
                return p.pelletCount();
            }
        }
        return bulletData.getBulletAmount();
    }
}

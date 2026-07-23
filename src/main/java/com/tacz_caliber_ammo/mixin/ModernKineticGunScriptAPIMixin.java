package com.tacz_caliber_ammo.mixin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.item.ModernKineticGunScriptAPI;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import com.tacz.guns.resource.pojo.data.gun.BulletData;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz_caliber_ammo.caliber.AmmoProfile;
import com.tacz_caliber_ammo.caliber.CaliberManager;
import com.tacz_caliber_ammo.caliber.Round;
import com.tacz_caliber_ammo.platform.config.ModConfig;
import com.tacz_caliber_ammo.nbt.LoadedAmmoSequence;
import com.tacz_caliber_ammo.platform.PlatformInventory;
import com.tacz_caliber_ammo.reload.PouchReloadSource;
import com.tacz_caliber_ammo.util.DebugLog;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

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
    @Unique
    private ResourceLocation tacz_caliber_ammo$firingAmmoId;
    @Unique
    private boolean tacz_caliber_ammo$pouchSupplied;

    /** 快照背包中每个含弹药/弹药盒槽位的 (ammoId, count)。 */
    @Unique
    private Map<Integer, Round> tacz_caliber_ammo$snapshot() {
        Map<Integer, Round> map = new LinkedHashMap<>();
        if (this.shooter == null) {
            return map;
        }
        PlatformInventory.View inventory = PlatformInventory.find(this.shooter);
        if (inventory != null) {
            for (int i = 0; i < inventory.slots(); i++) {
                ItemStack s = inventory.stackInSlot(i);
                Item it = s.getItem();
                if (it instanceof IAmmo ammo) {
                    map.put(i, new Round(ammo.getAmmoId(s), s.getCount()));
                } else if (it instanceof IAmmoBox box) {
                    map.put(i, new Round(box.getAmmoId(s), box.getAmmoCount(s)));
                }
            }
        }
        return map;
    }

    @Inject(method = "consumeAmmoFromPlayer", at = @At("HEAD"), cancellable = true)
    private void tacz_caliber_ammo$preConsume(int neededAmount, CallbackInfoReturnable<Integer> cir) {
        this.tacz_caliber_ammo$pouchSupplied = false;
        // 弹药包优先：快捷栏(槽 0-8)有「图案(口径过滤后)非空」弹药包时，按图案从包内(不足回退背包同弹种)供弹，
        // 接管本次消耗(setReturnValue 取消原背包扣弹逻辑)。供不上(包+背包都无匹配弹种)则不接管，走原逻辑。
        if (this.shooter instanceof Player player && this.itemStack != null
                && this.itemStack.getItem() instanceof IGun gun) {
            Set<ResourceLocation> calibers = CaliberManager.getGunCalibers(gun.getGunId(this.itemStack));
            ItemStack pouch = PouchReloadSource.findUsablePouch(player, calibers);
            if (!pouch.isEmpty()) {
                // 图案位置取当前弹匣数(弹匣枪逐发换弹随之推进图案循环)。INVENTORY 供弹枪(如 M134)currentAmmoCount
                // 恒 0 -> startPos 恒 0 = 始终用「首位匹配弹」: 有 pattern 取 pattern 首项、无 pattern 取 store 首项, 不循环。
                int startPos = this.abstractGunItem.getCurrentAmmoCount(this.itemStack);
                List<Round> seq = PouchReloadSource.supply(player, pouch, startPos, neededAmount, calibers,
                        this.itemStack);
                int total = 0;
                for (Round r : seq) {
                    total += r.count();
                }
                if (total > 0) {
                    this.tacz_caliber_ammo$pendingConsumed = seq;
                    DebugLog.log("pouchReload takeover: need={} startPos={} suppliedSeq={} total={}",
                            neededAmount, startPos, seq, total);
                    this.tacz_caliber_ammo$pouchSupplied = true;
                    cir.setReturnValue(total);
                    return;
                }
            }
        }
        this.tacz_caliber_ammo$preSnap = this.tacz_caliber_ammo$snapshot();
    }

    @Inject(method = "consumeAmmoFromPlayer", at = @At("RETURN"))
    private void tacz_caliber_ammo$postConsume(int neededAmount, CallbackInfoReturnable<Integer> cir) {
        if (this.tacz_caliber_ammo$pouchSupplied) {
            return; // 弹药包已在 HEAD 设好 pendingConsumed，勿被背包差分覆盖
        }
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
        List<Round> consumed = this.tacz_caliber_ammo$pendingConsumed;
        this.tacz_caliber_ammo$pendingConsumed = null;
        List<Round> seq = LoadedAmmoSequence.getSequence(this.itemStack);
        int before = 0;
        for (Round r : seq) {
            before += r.count();
        }
        boolean pouch = this.tacz_caliber_ammo$pouchSupplied;
        this.tacz_caliber_ammo$pouchSupplied = false;
        int count = this.abstractGunItem.getCurrentAmmoCount(this.itemStack);
        int need = count - before; // 本次实际进弹匣的发数（consumed 若含进膛的一发会多出，不入匣）
        if (need > 0) {
            List<Round> incoming = tacz_caliber_ammo$resolveIncoming(consumed, need);
            if (pouch) {
                DebugLog.log("pouchAfterPut FIFO: need={} incoming={} newSeq={}", need, incoming, seq);
                // 弹药包换弹：按图案顺序(图案第 1 项先发)FIFO 尾插，使 LoadedSeq 队首=图案首项；
                // 逐发换弹(m870 每次 need=1)若顶插会 LIFO 反转成末项先发，故弹药包路径必须尾插。
                seq.addAll(incoming);
            } else {
                // 背包换弹：新装填弹药插入弹匣顶部(队首)，余弹时新装先于余弹射出(真实弹匣 LIFO)。
                // 发射从队首出队(popNextRound)，故 index 0 = 下一发。对生存(consumed 非空)/创造(空)统一头部插入。
                seq.addAll(0, incoming);
            }
        }
        LoadedAmmoSequence.setSequence(this.itemStack, seq);
        // 兜底：与真实弹匣数对齐(desync 时截断/补齐)；补齐弹种优先取队首(新装)，否则默认弹种。
        ResourceLocation def = !seq.isEmpty() ? seq.get(0).ammoId() : tacz_caliber_ammo$defaultVariant();
        LoadedAmmoSequence.reconcile(this.itemStack, count, def);
    }

    /**
     * 本次进弹匣 need 发的弹种序列：优先真实消耗的弹种（{@code consumed} 取前 need 发，多出的是进膛弹不入匣，
     * 从而不会误截断弹匣内原有余弹）；consumed 为空（创造/背包直取）时用背包内该枪匹配弹种、否则枪默认弹种凑齐。
     */
    @Unique
    private List<Round> tacz_caliber_ammo$resolveIncoming(List<Round> consumed, int need) {
        List<Round> incoming = new ArrayList<>();
        int left = need;
        if (consumed != null && !consumed.isEmpty()) {
            for (Round r : consumed) {
                if (left <= 0) {
                    break;
                }
                int take = Math.min(left, r.count());
                if (take > 0 && r.ammoId() != null) {
                    incoming.add(new Round(r.ammoId(), take));
                    left -= take;
                }
            }
        }
        if (left > 0) {
            ResourceLocation d = !incoming.isEmpty() ? incoming.get(0).ammoId() : tacz_caliber_ammo$defaultVariant();
            if (d != null) {
                incoming.add(new Round(d, left));
            }
        }
        return incoming;
    }

    /** 创造/背包直取时的弹种：背包内该枪匹配弹种优先，否则枪默认弹种；均无返回 null。 */
    @Unique
    private ResourceLocation tacz_caliber_ammo$defaultVariant() {
        ResourceLocation inv = tacz_caliber_ammo$firstMatchingInventoryAmmo();
        if (inv != null) {
            return inv;
        }
        return this.gunIndex != null ? this.gunIndex.getGunData().getAmmoId() : null;
    }

    /** 背包内第一颗“该枪匹配口径”的弹药弹种（含弹药盒，跳过空盒/全类型创造盒）；无则 null。仅读取、不扣除。 */
    @Unique
    private ResourceLocation tacz_caliber_ammo$firstMatchingInventoryAmmo() {
        if (this.shooter == null || this.itemStack == null) {
            return null;
        }
        // 用 resolve() 取出 handler 后在能力回调外遍历：LazyOptional.map 的函数若返回 null，Forge 会
        // 包成 Optional.of(null) 抛 NPE（背包有该能力但无匹配弹药时必崩，见 reload 崩溃报告），故不在 map 里返回 null。
        PlatformInventory.View inventory = PlatformInventory.find(this.shooter);
        if (inventory == null) {
            return null;
        }
        for (int i = 0; i < inventory.slots(); i++) {
            ItemStack s = inventory.stackInSlot(i);
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
        ResourceLocation id;
        if (tacz_caliber_ammo$isInventoryFed()) {
            // INVENTORY 供弹(如 M134 转管机枪, reload.type=inventory): 弹药实时从背包/弹药包扣
            // (reduceAmmoOnce->consumeAmmoFromPlayer(1)), 不入弹匣序列、无膛内弹。故用本次射击实际
            // 消耗的弹种(pendingConsumed, 由 consumeAmmoFromPlayer 的 pre/postConsume 或弹药包设置)。
            id = tacz_caliber_ammo$takeFiredFromPending();
        } else {
            Bolt bolt = tacz_caliber_ammo$bolt();
            if (bolt == Bolt.CLOSED_BOLT) {
                // 闭膛弹匣枪(稳态膛内恒有弹): 先击发膛内那发, 再从弹匣顶补一发进膛(轮换), 膛内始终有弹。
                // 镜像 TacZ 稳态 reduceAmmoOnce: 弹匣有弹则 reduceCurrentAmmoCount(弹匣-1)+膛内标记保持;
                // 弹匣空则 setBulletInBarrel(false)。故这里: 有膛内弹时 popNextRound(对应弹匣-1)补膛; 弹匣空则清膛。
                id = LoadedAmmoSequence.peekBarrelAmmo(this.itemStack);
                if (id != null) {
                    ResourceLocation refill = LoadedAmmoSequence.popNextRound(this.itemStack);
                    if (refill != null) {
                        LoadedAmmoSequence.setBarrelAmmo(this.itemStack, refill); // 弹匣顶补进膛
                    } else {
                        LoadedAmmoSequence.takeBarrelAmmo(this.itemStack); // 弹匣空: 打掉最后的膛内弹, 清膛
                    }
                } else {
                    // 膛内空(边界/desync): 退化打弹匣队首
                    id = LoadedAmmoSequence.popNextRound(this.itemStack);
                }
            } else if (bolt == Bolt.OPEN_BOLT) {
                // 开膛待击无常驻膛内弹: 弹匣队首优先, 兜底膛内
                id = LoadedAmmoSequence.popNextRound(this.itemStack);
                if (id == null) {
                    id = LoadedAmmoSequence.takeBarrelAmmo(this.itemStack);
                }
            } else {
                // MANUAL_ACTION(栓/泵动): 击发膛内那发, 不在此补膛(下次上膛 setAmmoInBarrel->onSetBarrel 从弹匣补)
                id = LoadedAmmoSequence.takeBarrelAmmo(this.itemStack);
                if (id == null) {
                    id = LoadedAmmoSequence.popNextRound(this.itemStack);
                }
            }
        }
        if (id == null) {
            id = gunData.getAmmoId();
        }
        this.tacz_caliber_ammo$firingAmmoId = id;
        return id;
    }

    /** 该枪的枪机类型（gunIndex 缺失时返回 null）。 */
    @Unique
    private Bolt tacz_caliber_ammo$bolt() {
        return this.gunIndex != null ? this.gunIndex.getGunData().getBolt() : null;
    }

    /** 该枪是否 INVENTORY 供弹（reload.type=inventory，如 M134 转管机枪；弹药实时从背包/弹药包取，不入弹匣、无膛内弹）。 */
    @Unique
    private boolean tacz_caliber_ammo$isInventoryFed() {
        return this.abstractGunItem != null && this.itemStack != null
                && this.abstractGunItem.useInventoryAmmo(this.itemStack);
    }

    /**
     * 取本次射击实际消耗的一发弹种并消费之：来自 {@link #tacz_caliber_ammo$pendingConsumed}
     * （由 consumeAmmoFromPlayer 的 pre/postConsume 差分或弹药包设置）。空则返回 null（调用方回退默认弹种）。
     */
    @Unique
    private ResourceLocation tacz_caliber_ammo$takeFiredFromPending() {
        List<Round> pc = this.tacz_caliber_ammo$pendingConsumed;
        if (pc == null || pc.isEmpty()) {
            return null;
        }
        Round first = pc.get(0);
        ResourceLocation id = first.ammoId();
        if (first.count() <= 1) {
            pc.remove(0);
        } else {
            pc.set(0, new Round(id, first.count() - 1));
        }
        if (pc.isEmpty()) {
            this.tacz_caliber_ammo$pendingConsumed = null;
        }
        return id;
    }

    /**
     * 记录膛内弹弹种。TacZ 的膛内弹经 {@code setAmmoInBarrel(true)} 进膛（换弹直接进膛的空仓第一发，
     * 或泵动/栓动上膛从弹匣移入），从不经过 putAmmoInMagazine，故弹匣序列不含它，须在此单独跟踪。
     * 发射打膛内弹(直接调 AbstractGunItem.setBulletInBarrel(false)，不经此方法)时不误触发。
     */
    @Inject(method = "setAmmoInBarrel", at = @At("HEAD"))
    private void tacz_caliber_ammo$onSetBarrel(boolean ammoInBarrel, CallbackInfo ci) {
        if (this.itemStack == null) {
            return;
        }
        if (!ammoInBarrel) {
            LoadedAmmoSequence.setBarrelAmmo(this.itemStack, null);
            return;
        }
        List<Round> pc = this.tacz_caliber_ammo$pendingConsumed;
        if (pc != null && !pc.isEmpty()) {
            // 换弹直接进膛(空仓第一发)：膛内弹 = 刚消耗的弹种（该发不进弹匣，故消费掉 pendingConsumed）
            LoadedAmmoSequence.setBarrelAmmo(this.itemStack, pc.get(0).ammoId());
            this.tacz_caliber_ammo$pendingConsumed = null;
        } else {
            // 上膛从弹匣移入膛：膛内弹 = 弹匣队首（popNextRound 同步 removeAmmoFromMagazine 已扣的弹匣数）
            ResourceLocation top = LoadedAmmoSequence.popNextRound(this.itemStack);
            if (top != null) {
                LoadedAmmoSequence.setBarrelAmmo(this.itemStack, top);
            }
        }
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
        ResourceLocation next = tacz_caliber_ammo$peekNext();
        if (next != null) {
            AmmoProfile p = CaliberManager.getAmmoProfile(next);
            if (p != null && p.pelletCount() > 0) {
                return p.pelletCount();
            }
        }
        return bulletData.getBulletAmount();
    }

    /**
     * 下一发要发射的弹种（不消费）：OPEN_BOLT 看弹匣队首优先，其余(闭膛/栓动)看膛内弹优先；
     * 与 {@link #tacz_caliber_ammo$dequeue} 的取用顺序保持一致，供弹丸数预读。
     */
    @Unique
    private ResourceLocation tacz_caliber_ammo$peekNext() {
        if (tacz_caliber_ammo$bolt() == Bolt.OPEN_BOLT) {
            ResourceLocation h = LoadedAmmoSequence.peekHead(this.itemStack);
            return h != null ? h : LoadedAmmoSequence.peekBarrelAmmo(this.itemStack);
        }
        ResourceLocation b = LoadedAmmoSequence.peekBarrelAmmo(this.itemStack);
        return b != null ? b : LoadedAmmoSequence.peekHead(this.itemStack);
    }
}

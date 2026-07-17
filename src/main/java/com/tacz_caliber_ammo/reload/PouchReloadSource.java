package com.tacz_caliber_ammo.reload;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.tacz.guns.api.item.IAmmo;
import com.tacz_caliber_ammo.caliber.CaliberManager;
import com.tacz_caliber_ammo.caliber.PatternEntry;
import com.tacz_caliber_ammo.caliber.Round;
import com.tacz_caliber_ammo.item.AmmoPouchItem;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;

/**
 * 弹药包换弹供弹（Stage 6 T6.6/T6.7）。策略：<b>弹药包优先、背包兜底</b>。
 *
 * <p>换弹时按弹药包压弹图案（口径过滤后）循环生成逐发弹种：每发先从弹药包 {@code withdraw}，
 * 包内该弹种耗尽则从背包扣<b>同一弹种</b>（{@link #extractSpecific}，TacZ 的 findAndExtractInventoryAmmo
 * 只按槽序扣任意匹配弹药、不认弹种，故此处自扫背包）；两者都没了即停（防死循环）。
 * 返回的逐发序列为<b>图案顺序</b>（图案第 1 项在前 = 先装先发），由调用方写入枪 {@code LoadedSeq}。
 * 图案位置由调用方传入（用换弹时的 {@code currentAmmoCount}，逐发/一次性换弹都自洽）。
 */
public final class PouchReloadSource {

    private PouchReloadSource() {
    }

    /** 快捷栏（槽 0-8）首个「能给该枪供弹」的弹药包（见 {@link #canSupply}）；无则 {@link ItemStack#EMPTY}。 */
    public static ItemStack findUsablePouch(Player player, Set<ResourceLocation> gunCalibers) {
        for (int i = 0; i <= 8; i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (canSupply(s, gunCalibers)) {
                return s;
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * 弹药包能否给该枪供弹：<b>有</b>压弹图案(口径过滤后非空)时——图案弹种在存储有货；
     * <b>无</b>图案时——存储有任一口径匹配弹(换弹将按库存首位序供弹)。只读。
     */
    public static boolean canSupply(ItemStack pouch, Set<ResourceLocation> gunCalibers) {
        if (!(pouch.getItem() instanceof AmmoPouchItem)) {
            return false;
        }
        Map<ResourceLocation, Integer> store = AmmoPouchItem.getStore(pouch);
        if (store.isEmpty()) {
            return false;
        }
        List<PatternEntry> pat = filterByCaliber(AmmoPouchItem.getPattern(pouch), gunCalibers);
        if (pat.isEmpty()) {
            for (ResourceLocation ammoId : store.keySet()) {
                if (gunCalibers.contains(CaliberManager.getAmmoCaliber(ammoId))) {
                    return true;
                }
            }
            return false;
        }
        for (PatternEntry pe : pat) {
            if (store.getOrDefault(pe.ammoId(), 0) > 0) {
                return true;
            }
        }
        return false;
    }

    /** 图案项按口径过滤：只保留 ammoId 所属口径在枪口径集内的项（T6.7）。 */
    public static List<PatternEntry> filterByCaliber(List<PatternEntry> pattern, Set<ResourceLocation> gunCalibers) {
        List<PatternEntry> out = new ArrayList<>();
        if (gunCalibers == null || gunCalibers.isEmpty()) {
            return out;
        }
        for (PatternEntry pe : pattern) {
            if (pe == null || pe.ammoId() == null || pe.perCycle() <= 0) {
                continue;
            }
            if (gunCalibers.contains(CaliberManager.getAmmoCaliber(pe.ammoId()))) {
                out.add(pe);
            }
        }
        return out;
    }

    /** 快捷栏(槽 0-8)是否有「能给该枪供弹」的弹药包(见 {@link #canSupply})；供 canReload 门控识别。只读。 */
    public static boolean hasUsableAmmo(Player player, Set<ResourceLocation> gunCalibers) {
        return !findUsablePouch(player, gunCalibers).isEmpty();
    }

    /** 快捷栏(槽 0-8)弹药包中「口径匹配该枪」的弹药总发数；供 HUD 库存显示叠加。只读。 */
    public static int countUsableAmmo(Inventory inventory, Set<ResourceLocation> gunCalibers) {
        int sum = 0;
        for (int i = 0; i <= 8; i++) {
            ItemStack s = inventory.getItem(i);
            if (!(s.getItem() instanceof AmmoPouchItem)) {
                continue;
            }
            for (Map.Entry<ResourceLocation, Integer> e : AmmoPouchItem.getStore(s).entrySet()) {
                if (gunCalibers.contains(CaliberManager.getAmmoCaliber(e.getKey()))) {
                    sum += e.getValue();
                }
            }
        }
        return sum;
    }

    /**
     * 从弹药包（优先）+ 背包（兜底）按图案供弹 {@code need} 发，图案位置从 {@code startPos} 起循环。
     * 返回实际扣到的逐发序列（RLE，图案顺序）；某发弹药包与背包都无该弹种即停（不卡死）。空图案返回空表。
     */
    public static List<Round> supply(Player player, ItemStack pouch, int startPos, int need,
            Set<ResourceLocation> gunCalibers, ItemStack gun) {
        List<Round> result = new ArrayList<>();
        if (need <= 0 || !(pouch.getItem() instanceof AmmoPouchItem)) {
            return result;
        }
        List<PatternEntry> pat = filterByCaliber(AmmoPouchItem.getPattern(pouch), gunCalibers);
        if (pat.isEmpty()) {
            // 无压弹图案 -> 按库存首位序供弹（库存首位弹种优先，纯库存不兜底背包）
            return supplyFromStore(pouch, need, gunCalibers);
        }
        // 展开图案到每发弹种循环：[HP,HP,HP,HP,HP,AP,AP,...]
        List<ResourceLocation> cycle = new ArrayList<>();
        for (PatternEntry pe : pat) {
            for (int i = 0; i < pe.perCycle(); i++) {
                cycle.add(pe.ammoId());
            }
        }
        if (cycle.isEmpty()) {
            return result;
        }
        IItemHandler inv = player.getCapability(ForgeCapabilities.ITEM_HANDLER, null).orElse(null);
        int base = ((startPos % cycle.size()) + cycle.size()) % cycle.size();
        for (int k = 0; k < need; k++) {
            ResourceLocation v = cycle.get((base + k) % cycle.size());
            int got = AmmoPouchItem.withdraw(pouch, v, 1);
            if (got == 0 && inv != null) {
                got = extractSpecific(inv, v, 1, gun);
            }
            if (got <= 0) {
                break; // 该弹种包内+背包都没了 -> 停（防死循环）
            }
            appendRle(result, v);
        }
        return result;
    }

    /** 从背包扣特定弹种 {@code ammoId} 共 {@code n} 发（且属于该枪 isAmmoOfGun）；返回实际扣数。 */
    private static int extractSpecific(IItemHandler inv, ResourceLocation ammoId, int n, ItemStack gun) {
        int left = n;
        for (int i = 0; i < inv.getSlots() && left > 0; i++) {
            ItemStack s = inv.getStackInSlot(i);
            if (s.getItem() instanceof IAmmo ammo && ammoId.equals(ammo.getAmmoId(s)) && ammo.isAmmoOfGun(gun, s)) {
                left -= inv.extractItem(i, left, false).getCount();
            }
        }
        return n - left;
    }

    /** 把一发 ammoId 追加进 RLE 序列（与末段同弹种则并段）。 */
    private static void appendRle(List<Round> seq, ResourceLocation ammoId) {
        if (!seq.isEmpty() && ammoId.equals(seq.get(seq.size() - 1).ammoId())) {
            Round last = seq.remove(seq.size() - 1);
            seq.add(new Round(ammoId, last.count() + 1));
        } else {
            seq.add(new Round(ammoId, 1));
        }
    }

    /**
     * 无压弹图案时的供弹：按存储顺序(首位弹种优先)取口径匹配弹药，直到凑满 {@code need} 或库存匹配弹耗尽。
     * 纯库存(不从背包兜底)——契合「无图案时直接使用库存补弹」。返回库存首位序的 RLE 序列。
     */
    private static List<Round> supplyFromStore(ItemStack pouch, int need, Set<ResourceLocation> gunCalibers) {
        List<Round> result = new ArrayList<>();
        int left = need;
        for (ResourceLocation ammoId : new ArrayList<>(AmmoPouchItem.getStore(pouch).keySet())) {
            if (left <= 0) {
                break;
            }
            if (!gunCalibers.contains(CaliberManager.getAmmoCaliber(ammoId))) {
                continue;
            }
            int got = AmmoPouchItem.withdraw(pouch, ammoId, left);
            if (got > 0) {
                appendRleCount(result, ammoId, got);
                left -= got;
            }
        }
        return result;
    }

    /** 把 {@code count} 发 ammoId 追加进 RLE 序列（与末段同弹种则并段）。 */
    private static void appendRleCount(List<Round> seq, ResourceLocation ammoId, int count) {
        if (count <= 0 || ammoId == null) {
            return;
        }
        if (!seq.isEmpty() && ammoId.equals(seq.get(seq.size() - 1).ammoId())) {
            Round last = seq.remove(seq.size() - 1);
            seq.add(new Round(ammoId, last.count() + count));
        } else {
            seq.add(new Round(ammoId, count));
        }
    }
}

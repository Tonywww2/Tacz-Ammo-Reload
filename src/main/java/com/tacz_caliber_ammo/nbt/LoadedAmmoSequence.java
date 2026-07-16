package com.tacz_caliber_ammo.nbt;

import java.util.ArrayList;
import java.util.List;

import com.tacz_caliber_ammo.caliber.Round;
import com.tacz_caliber_ammo.util.DebugLog;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * 枪的逐发弹匣序列（RLE）读写门面。实现 by PB-2。
 * NBT 形态：gun.tag[LOADED_SEQ] = ListTag of CompoundTag{ id:String, c:int }，每段 c 发同一 ammoId。
 * sum(c) 应与 IGun.currentAmmoCount 一致，不一致时由 {@link #reconcile} 用默认弹种修正。
 */
public final class LoadedAmmoSequence {

    private static final String ID = "id";
    private static final String COUNT = "c";

    private LoadedAmmoSequence() {
    }

    /** 读出逐发序列（RLE 段）。 */
    public static List<Round> getSequence(ItemStack gun) {
        List<Round> out = new ArrayList<>();
        CompoundTag tag = gun.getTag();
        if (tag == null || !tag.contains(NbtKeys.LOADED_SEQ, Tag.TAG_LIST)) {
            return out;
        }
        ListTag list = tag.getList(NbtKeys.LOADED_SEQ, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag e = list.getCompound(i);
            ResourceLocation id = ResourceLocation.tryParse(e.getString(ID));
            int c = e.getInt(COUNT);
            if (id != null && c > 0) {
                out.add(new Round(id, c));
            }
        }
        return out;
    }

    /** 写入逐发序列（丢弃 count<=0 的段）。 */
    public static void setSequence(ItemStack gun, List<Round> seq) {
        ListTag list = new ListTag();
        for (Round r : seq) {
            if (r == null || r.ammoId() == null || r.count() <= 0) {
                continue;
            }
            CompoundTag e = new CompoundTag();
            e.putString(ID, r.ammoId().toString());
            e.putInt(COUNT, r.count());
            list.add(e);
        }
        gun.getOrCreateTag().put(NbtKeys.LOADED_SEQ, list);
    }

    /** 查看队首（上膛/下一发）弹种，不出队；空返回 null。 */
    public static ResourceLocation peekHead(ItemStack gun) {
        CompoundTag tag = gun.getTag();
        if (tag == null || !tag.contains(NbtKeys.LOADED_SEQ, Tag.TAG_LIST)) {
            return null;
        }
        ListTag list = tag.getList(NbtKeys.LOADED_SEQ, Tag.TAG_COMPOUND);
        if (list.isEmpty()) {
            return null;
        }
        CompoundTag e = list.getCompound(0);
        return e.getInt(COUNT) > 0 ? ResourceLocation.tryParse(e.getString(ID)) : null;
    }

    /** 出队一发，返回该发弹种，队首段计数 -1（归零则移除）；空返回 null。 */
    public static ResourceLocation popNextRound(ItemStack gun) {
        CompoundTag tag = gun.getTag();
        if (tag == null || !tag.contains(NbtKeys.LOADED_SEQ, Tag.TAG_LIST)) {
            return null;
        }
        ListTag list = tag.getList(NbtKeys.LOADED_SEQ, Tag.TAG_COMPOUND);
        if (list.isEmpty()) {
            return null;
        }
        CompoundTag e = list.getCompound(0);
        ResourceLocation id = ResourceLocation.tryParse(e.getString(ID));
        int c = e.getInt(COUNT);
        if (c <= 1) {
            list.remove(0);
        } else {
            e.putInt(COUNT, c - 1);
        }
        tag.put(NbtKeys.LOADED_SEQ, list);
        DebugLog.log("popNextRound -> {} (remaining segs={})", id, list.size());
        return id;
    }

    /** 写入膛内弹弹种（null 则清除）。 */
    public static void setBarrelAmmo(ItemStack gun, ResourceLocation ammoId) {
        if (ammoId == null) {
            CompoundTag tag = gun.getTag();
            if (tag != null) {
                tag.remove(NbtKeys.BARREL_AMMO);
            }
            return;
        }
        gun.getOrCreateTag().putString(NbtKeys.BARREL_AMMO, ammoId.toString());
    }

    /** 查看膛内弹弹种，不清除；无则 null。 */
    public static ResourceLocation peekBarrelAmmo(ItemStack gun) {
        CompoundTag tag = gun.getTag();
        if (tag == null || !tag.contains(NbtKeys.BARREL_AMMO, Tag.TAG_STRING)) {
            return null;
        }
        return ResourceLocation.tryParse(tag.getString(NbtKeys.BARREL_AMMO));
    }

    /** 取出膛内弹弹种并清除；无则 null。 */
    public static ResourceLocation takeBarrelAmmo(ItemStack gun) {
        ResourceLocation id = peekBarrelAmmo(gun);
        if (id != null) {
            gun.getTag().remove(NbtKeys.BARREL_AMMO);
        }
        return id;
    }

    /**
     * 边界处理：当序列总和与 currentAmmoCount 不一致（旧存档/外部装弹/desync）时，
     * 以 currentAmmoCount 为准，用 defaultAmmoId 于队尾补齐 / 从队尾截断。
     */
    public static void reconcile(ItemStack gun, int currentAmmoCount, ResourceLocation defaultAmmoId) {
        List<Round> seq = getSequence(gun);
        int sum = 0;
        for (Round r : seq) {
            sum += r.count();
        }
        if (sum == currentAmmoCount) {
            return;
        }
        if (sum < currentAmmoCount) {
            int need = currentAmmoCount - sum;
            if (defaultAmmoId == null) {
                return;
            }
            if (!seq.isEmpty() && defaultAmmoId.equals(seq.get(seq.size() - 1).ammoId())) {
                Round last = seq.get(seq.size() - 1);
                seq.set(seq.size() - 1, new Round(defaultAmmoId, last.count() + need));
            } else {
                seq.add(new Round(defaultAmmoId, need));
            }
        } else {
            int excess = sum - currentAmmoCount;
            for (int i = seq.size() - 1; i >= 0 && excess > 0; i--) {
                Round r = seq.get(i);
                if (r.count() <= excess) {
                    excess -= r.count();
                    seq.remove(i);
                } else {
                    seq.set(i, new Round(r.ammoId(), r.count() - excess));
                    excess = 0;
                }
            }
        }
        setSequence(gun, seq);
    }
}

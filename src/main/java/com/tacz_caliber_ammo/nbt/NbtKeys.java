package com.tacz_caliber_ammo.nbt;

/** 本 mod 使用的 NBT 键常量（冻结契约 by PA-1）。 */
public final class NbtKeys {

    /** 枪：逐发弹匣序列（RLE）。 */
    public static final String LOADED_SEQ = "tacz_caliber_ammo:LoadedSeq";

    /** 枪：膛内弹弹种（TacZ 的膛内弹 bulletInBarrel 不在弹匣序列里，需单独记录其弹种）。 */
    public static final String BARREL_AMMO = "tacz_caliber_ammo:BarrelAmmo";

    private NbtKeys() {
    }
}

package com.tacz_caliber_ammo.nbt;

import java.util.function.Consumer;

import com.tacz_caliber_ammo.compat.api.ModeKey;
import com.tacz_caliber_ammo.compat.runtime.CompatibilityRuntime;
import com.tacz_caliber_ammo.platform.PlatformItemData;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

public final class AmmoStateStorage {

    private static final String STATES = "tacz_caliber_ammo:AmmoStates";

    public static CompoundTag copyActive(ItemStack gun) {
        CompoundTag root = PlatformItemData.copy(gun);
        String key = activeKey(gun);
        if (root.contains(STATES, Tag.TAG_COMPOUND)) {
            CompoundTag states = root.getCompound(STATES);
            if (states.contains(key, Tag.TAG_COMPOUND)) {
                return states.getCompound(key).copy();
            }
        }
        return ModeKey.MAIN.storageKey().equals(key) ? copyLegacy(root) : new CompoundTag();
    }

    public static void updateActive(ItemStack gun, Consumer<CompoundTag> updater) {
        String key = activeKey(gun);
        PlatformItemData.update(gun, root -> {
            CompoundTag states = root.contains(STATES, Tag.TAG_COMPOUND)
                    ? root.getCompound(STATES) : new CompoundTag();
            CompoundTag state;
            if (states.contains(key, Tag.TAG_COMPOUND)) {
                state = states.getCompound(key);
            } else if (ModeKey.MAIN.storageKey().equals(key)) {
                state = copyLegacy(root);
            } else {
                state = new CompoundTag();
            }
            updater.accept(state);
            states.put(key, state);
            root.put(STATES, states);
            if (ModeKey.MAIN.storageKey().equals(key)) {
                root.remove(NbtKeys.LOADED_SEQ);
                root.remove(NbtKeys.BARREL_AMMO);
            }
        });
    }

    private static CompoundTag copyLegacy(CompoundTag root) {
        CompoundTag state = new CompoundTag();
        if (root.contains(NbtKeys.LOADED_SEQ, Tag.TAG_LIST)) {
            state.put(NbtKeys.LOADED_SEQ, root.get(NbtKeys.LOADED_SEQ).copy());
        }
        if (root.contains(NbtKeys.BARREL_AMMO, Tag.TAG_STRING)) {
            state.putString(NbtKeys.BARREL_AMMO, root.getString(NbtKeys.BARREL_AMMO));
        }
        return state;
    }

    private static String activeKey(ItemStack gun) {
        return CompatibilityRuntime.resolveGunMode(gun).key().storageKey();
    }

    private AmmoStateStorage() {
    }
}
package com.tacz_caliber_ammo.network;

import com.tacz_caliber_ammo.TaczCaliberAmmo;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * 网络通道门面（冻结契约 by PA-1）。PC-2 负责注册具体消息（如 {@link CMsgUnloadAmmo}）。
 */
public final class ModNetwork {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(TaczCaliberAmmo.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private ModNetwork() {
    }

    /** 在 FMLCommonSetupEvent 中调用（见 {@code UnloadBootstrap}）。 */
    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, CMsgUnloadAmmo.class,
                CMsgUnloadAmmo::encode, CMsgUnloadAmmo::decode, CMsgUnloadAmmo::handle);
    }
}

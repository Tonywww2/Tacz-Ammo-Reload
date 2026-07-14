package com.tacz_ammo_reload;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
//? if forge {
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
//?} else {
/*import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
*///?}
import org.slf4j.Logger;

/**
 * TacZ Ammo Reload —— mod 主类（Forge 1.20.1 骨架）。
 * 在构造器里获取 mod 事件总线，后续把各 DeferredRegister 注册到此总线。
 */
@Mod(TaczAmmoReload.MODID)
public class TaczAmmoReload {

    public static final String MODID = "tacz_ammo_reload";
    public static final Logger LOGGER = LogUtils.getLogger();

    /** 构造本 mod 命名空间下的 ResourceLocation（Forge 构造器 / NeoForge 工厂）。 */
    public static ResourceLocation prefix(String path) {
        //? if forge {
        return new ResourceLocation(MODID, path);
        //?} else {
        /*return ResourceLocation.fromNamespaceAndPath(MODID, path);
        *///?}
    }

    //? if forge {
    public TaczAmmoReload() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
    //?} else {
    /*public TaczAmmoReload(IEventBus modBus) {
    *///?}

        // TODO: 在此把 DeferredRegister 注册到 modBus，例如：
        //   ModItems.REGISTER.register(modBus);
        modBus.hashCode();

        LOGGER.info("[{}] TacZ Ammo Reload loaded.", MODID);
    }
}

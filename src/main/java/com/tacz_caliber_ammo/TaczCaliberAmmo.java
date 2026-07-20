package com.tacz_caliber_ammo;

import com.mojang.logging.LogUtils;
import com.tacz_caliber_ammo.registry.ModItems;
import com.tacz_caliber_ammo.registry.ModMenus;
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
 * TacZ Caliber Ammo —— mod 主类（Forge 1.20.1 骨架）。
 * 在构造器里获取 mod 事件总线，后续把各 DeferredRegister 注册到此总线。
 */
@Mod(TaczCaliberAmmo.MODID)
public class TaczCaliberAmmo {

    public static final String MODID = "tacz_caliber_ammo";
    public static final Logger LOGGER = LogUtils.getLogger();

    /** 构造本 mod 命名空间下的 ResourceLocation（Forge 构造器 / NeoForge 工厂）。 */
    @SuppressWarnings("removal")
    public static ResourceLocation prefix(String path) {
        //? if forge {
        return new ResourceLocation(MODID, path);
        //?} else {
        /*return ResourceLocation.fromNamespaceAndPath(MODID, path);
        *///?}
    }

    //? if forge {
    public TaczCaliberAmmo() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
    //?} else {
    /*public TaczCaliberAmmo(IEventBus modBus) {
    *///?}

        // 物品注册（本 mod 首个 DeferredRegister；当前为弹药包）。
        ModItems.REGISTER.register(modBus);
        // 菜单类型注册（弹药包 GUI）。
        ModMenus.REGISTER.register(modBus);

        LOGGER.info("[{}] TacZ Caliber Ammo loaded.", MODID);
    }
}

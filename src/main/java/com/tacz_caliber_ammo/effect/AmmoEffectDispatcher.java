package com.tacz_caliber_ammo.effect;

import java.util.function.Supplier;

import com.mojang.logging.LogUtils;
import com.tacz_caliber_ammo.caliber.AmmoProfile;
import com.tacz_caliber_ammo.caliber.CaliberManager;
import com.tacz_caliber_ammo.platform.config.ModConfig;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.slf4j.Logger;

import net.minecraft.resources.ResourceLocation;

/**
 * 弹药效果脚本派发：按 ammoId 找到其绑定脚本，若脚本定义了对应钩子函数则以 api 调用。
 *
 * <p>关键点：
 * <ul>
 *   <li><b>仅当脚本定义了该钩子才派发</b>——{@code table.get(hook)} 非函数即跳过（on_bullet_tick 也据此不空跑）；</li>
 *   <li><b>api 惰性构建</b>——只有确定要调用时才 {@code apiFactory.get()}，避免逐 tick 无谓构建上下文；</li>
 *   <li><b>try/catch 兜底</b>——脚本任何错误只记 warn，绝不冒泡到游戏主循环（对照换弹崩溃教训）。</li>
 * </ul>
 */
public final class AmmoEffectDispatcher {

    static final Logger LOGGER = LogUtils.getLogger();

    private AmmoEffectDispatcher() {
    }

    /**
     * 派发一个钩子。{@code apiFactory} 惰性构建 api（仅当脚本存在且定义了该钩子时才调用）。
     */
    public static void dispatch(ResourceLocation ammoId, String hook, Supplier<AmmoEffectScriptAPI> apiFactory) {
        if (ammoId == null || !ModConfig.enableAmmoEffects()) {
            return;
        }
        AmmoProfile profile = CaliberManager.getAmmoProfile(ammoId);
        if (profile == null) {
            return;
        }
        ResourceLocation scriptId = profile.effects().script();
        if (scriptId == null) {
            return;
        }
        LuaTable script = EffectScriptManager.getScript(scriptId);
        if (script == null) {
            return;
        }
        LuaValue fn = script.get(hook);
        if (fn == null || !fn.isfunction()) {
            return; // 脚本未定义该钩子 -> 不派发
        }
        AmmoEffectScriptAPI api = apiFactory.get();
        try {
            fn.call(CoerceJavaToLua.coerce(api));
        } catch (Throwable e) {
            // 兜住脚本任何错误（含 LuaError/NPE，乃至无限递归的 StackOverflowError），只记 warn，绝不冒泡到主循环。
            LOGGER.warn("[tacz_caliber_ammo] effect script {} hook {} failed: {}", scriptId, hook, e.toString());
        }
    }
}

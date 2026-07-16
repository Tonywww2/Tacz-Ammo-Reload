package com.tacz_caliber_ammo.effect;

import com.tacz.guns.api.vmlib.LuaLibrary;

import org.luaj.vm2.LuaValue;

/**
 * 装入弹药效果脚本沙箱 globals 的库（扩展点）。
 *
 * <p>当前不注入额外全局——脚本所需的一切都由每次派发时传入的 api 对象（{@link AmmoEffectScriptAPI}）提供，
 * 脚本以 {@code function M.on_hit_entity(api) ... end} 形式接收。后续如需给脚本注入全局常量/工具，
 * 或复用 TacZ 的 {@code LuaEntityAccessor}/{@code LuaNbtAccessor}，在此 {@code globals.set(...)} 即可。
 */
public final class AmmoEffectLibrary implements LuaLibrary {

    @Override
    public void install(LuaValue globals) {
        // 预留扩展点：暂无额外全局注入。
    }
}

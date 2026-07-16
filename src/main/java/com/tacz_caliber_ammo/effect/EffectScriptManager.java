package com.tacz_caliber_ammo.effect;

import java.util.List;

import com.tacz.guns.resource.manager.ScriptManager;

import org.luaj.vm2.LuaTable;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;

/**
 * 弹药效果 Lua 脚本的加载/查询门面。
 *
 * <p>自建一个 TacZ {@link ScriptManager} 实例——复用其沙箱 luaj（无 io/os）、模块=返回表约定、
 * {@code package.preload} 互 require 机制——扫描 {@code data/<ns>/ammo_effect_scripts/*.lua}。
 * {@link ScriptManager} 本身是 {@code SimplePreparableReloadListener}，由 {@link EffectScriptBootstrap}
 * 经 {@code AddReloadListenerEvent} 注册，随服务端数据 reload 重新加载。
 */
public final class EffectScriptManager {

    /** 脚本目录：{@code data/<ns>/ammo_effect_scripts/*.lua}。 */
    public static final String DIRECTORY = "ammo_effect_scripts";

    private static final ScriptManager MANAGER = new ScriptManager(
            new FileToIdConverter(DIRECTORY, ".lua"),
            List.of(new AmmoEffectLibrary()));

    private EffectScriptManager() {
    }

    /** 供 {@link EffectScriptBootstrap} 注册的 reload 监听实例（即 ScriptManager 本身）。 */
    public static ScriptManager listener() {
        return MANAGER;
    }

    /**
     * 取脚本模块表（形如 {@code {on_hit_entity=fn, ...}}）；未找到返回 {@code null}。
     * {@code id} 例：{@code tacz_caliber_ammo:incendiary} 对应
     * {@code data/tacz_caliber_ammo/ammo_effect_scripts/incendiary.lua}。
     */
    public static LuaTable getScript(ResourceLocation id) {
        return MANAGER.getScript(id);
    }
}

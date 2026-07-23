package com.tacz_caliber_ammo.platform;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
//? if forge {
import net.minecraftforge.registries.ForgeRegistries;
//?}

public final class PlatformEffects {

    public static MobEffectInstance createInstance(ResourceLocation id, int duration, int amplifier) {
        //? if forge {
        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(id);
        return effect == null ? null : new MobEffectInstance(effect, duration, amplifier);
        //?} else {
        /*return BuiltInRegistries.MOB_EFFECT.getHolder(id)
                .map(holder -> new MobEffectInstance(holder, duration, amplifier))
                .orElse(null);
        *///?}
    }

    public static ParticleType<?> particle(ResourceLocation id) {
        //? if forge {
        return ForgeRegistries.PARTICLE_TYPES.getValue(id);
        //?} else {
        /*return BuiltInRegistries.PARTICLE_TYPE.get(id);
        *///?}
    }

    public static SoundEvent sound(ResourceLocation id) {
        //? if forge {
        return ForgeRegistries.SOUND_EVENTS.getValue(id);
        //?} else {
        /*return BuiltInRegistries.SOUND_EVENT.get(id);
        *///?}
    }

    private PlatformEffects() {
    }
}
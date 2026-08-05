package com.tacz_caliber_ammo.compat.runtime;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.CopyOnWriteArrayList;

import com.tacz_caliber_ammo.TaczCaliberAmmo;
import com.tacz_caliber_ammo.compat.api.AmmoDecision;
import com.tacz_caliber_ammo.compat.api.AmmoPolicyContributor;
import com.tacz_caliber_ammo.compat.api.CompatibilityModule;
import com.tacz_caliber_ammo.compat.api.CompatibilityRegistrar;
import com.tacz_caliber_ammo.compat.api.GunModeContext;
import com.tacz_caliber_ammo.compat.api.GunModeResolver;
import com.tacz_caliber_ammo.compat.api.ShotContext;
import com.tacz_caliber_ammo.compat.api.ShotOverrideProvider;
import com.tacz_caliber_ammo.compat.builtin.BaseTacZCompatibility;
import com.tacz_caliber_ammo.platform.PlatformMods;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class CompatibilityRuntime {

    private static final List<Capability<GunModeResolver>> MODE_RESOLVERS = new CopyOnWriteArrayList<>();
    private static final List<Capability<AmmoPolicyContributor>> AMMO_POLICIES = new CopyOnWriteArrayList<>();
    private static final List<Capability<ShotOverrideProvider>> SHOT_OVERRIDES = new CopyOnWriteArrayList<>();
    private static volatile boolean initialized;

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        registerModule(new BaseTacZCompatibility());
        for (CompatibilityDescriptor descriptor : builtInDescriptors()) {
            loadDescriptor(descriptor);
        }
        initialized = true;
    }

    public static synchronized void registerModule(CompatibilityModule module) {
        ResourceLocation owner = module.id();
        module.register(new CompatibilityRegistrar() {
            @Override
            public void registerGunModeResolver(GunModeResolver resolver, int priority) {
                add(MODE_RESOLVERS, new Capability<>(owner, priority, resolver));
            }

            @Override
            public void registerAmmoPolicy(AmmoPolicyContributor policy, int priority) {
                add(AMMO_POLICIES, new Capability<>(owner, priority, policy));
            }

            @Override
            public void registerShotOverride(ShotOverrideProvider provider, int priority) {
                add(SHOT_OVERRIDES, new Capability<>(owner, priority, provider));
            }
        });
        TaczCaliberAmmo.LOGGER.info("[{}] Registered compatibility module {}", TaczCaliberAmmo.MODID, owner);
    }

    public static GunModeContext resolveGunMode(ItemStack gun) {
        ensureInitialized();
        for (Capability<GunModeResolver> entry : MODE_RESOLVERS) {
            if (!entry.enabled) {
                continue;
            }
            try {
                Optional<GunModeContext> result = entry.value.resolve(gun);
                if (result.isPresent()) {
                    return result.get();
                }
            } catch (RuntimeException | LinkageError exception) {
                disable(entry, "gun mode resolver", exception);
            }
        }
        throw new IllegalStateException("No compatibility gun mode resolver accepted a TacZ gun");
    }

    public static boolean isAmmoAllowed(ItemStack gun, ResourceLocation ammoId) {
        if (ammoId == null) {
            return false;
        }
        GunModeContext mode = resolveGunMode(gun);
        for (Capability<AmmoPolicyContributor> entry : AMMO_POLICIES) {
            if (!entry.enabled) {
                continue;
            }
            try {
                AmmoDecision result = entry.value.test(mode, gun, ammoId);
                if (result != AmmoDecision.PASS) {
                    return result == AmmoDecision.ALLOW;
                }
            } catch (RuntimeException | LinkageError exception) {
                disable(entry, "ammo policy", exception);
            }
        }
        return false;
    }

    public static int resolveBulletAmount(ItemStack gun, ResourceLocation ammoId, int upstreamBulletAmount) {
        GunModeContext mode = resolveGunMode(gun);
        ShotContext context = new ShotContext(gun, mode, ammoId, upstreamBulletAmount);
        for (Capability<ShotOverrideProvider> entry : SHOT_OVERRIDES) {
            if (!entry.enabled) {
                continue;
            }
            try {
                OptionalInt result = entry.value.resolveBulletAmount(context);
                if (result.isPresent()) {
                    return Math.max(1, result.getAsInt());
                }
            } catch (RuntimeException | LinkageError exception) {
                disable(entry, "shot override", exception);
            }
        }
        return upstreamBulletAmount;
    }

    private static List<CompatibilityDescriptor> builtInDescriptors() {
        ModRequirement arcana = new ModRequirement("taczexpands", java.util.Set.of("1.1.4"), true);
        return List.of(new CompatibilityDescriptor(
            ResourceLocation.tryParse("taczexpands:arcana"), List.of(arcana),
                "com.tacz_caliber_ammo.compat.builtin.arcana.ArcanaCompatibilityModule"));
    }

    private static void loadDescriptor(CompatibilityDescriptor descriptor) {
        boolean degraded = false;
        for (ModRequirement requirement : descriptor.requirements()) {
            Optional<String> version = PlatformMods.getVersion(requirement.modId());
            if (version.isEmpty()) {
                return;
            }
            if (!requirement.testedVersions().isEmpty() && !requirement.testedVersions().contains(version.get())) {
                if (!requirement.allowUntestedVersions()) {
                    TaczCaliberAmmo.LOGGER.warn("[{}] Compatibility {} disabled: unsupported {} {}",
                            TaczCaliberAmmo.MODID, descriptor.id(), requirement.modId(), version.get());
                    return;
                }
                degraded = true;
            }
        }
        try {
            Class<?> type = Class.forName(descriptor.factoryClassName(), true,
                    CompatibilityRuntime.class.getClassLoader());
            registerModule((CompatibilityModule) type.getDeclaredConstructor().newInstance());
            if (degraded) {
                TaczCaliberAmmo.LOGGER.warn("[{}] Compatibility {} enabled for an untested dependency version",
                        TaczCaliberAmmo.MODID, descriptor.id());
            }
        } catch (ReflectiveOperationException | LinkageError exception) {
            Throwable cause = exception instanceof InvocationTargetException invocation
                    ? invocation.getTargetException() : exception;
            TaczCaliberAmmo.LOGGER.error("[{}] Failed to load compatibility module {}",
                    TaczCaliberAmmo.MODID, descriptor.id(), cause);
        }
    }

    private static void ensureInitialized() {
        if (!initialized) {
            initialize();
        }
    }

    private static <T> void add(List<Capability<T>> list, Capability<T> entry) {
        list.add(entry);
        List<Capability<T>> sorted = new ArrayList<>(list);
        sorted.sort(Comparator.comparingInt((Capability<T> value) -> value.priority).reversed()
                .thenComparing(value -> value.owner.toString()));
        list.clear();
        list.addAll(sorted);
    }

    private static void disable(Capability<?> entry, String capability, Throwable exception) {
        entry.enabled = false;
        TaczCaliberAmmo.LOGGER.error("[{}] Disabled {} from compatibility module {} after an error",
                TaczCaliberAmmo.MODID, capability, entry.owner, exception);
    }

    private static final class Capability<T> {
        private final ResourceLocation owner;
        private final int priority;
        private final T value;
        private volatile boolean enabled = true;

        private Capability(ResourceLocation owner, int priority, T value) {
            this.owner = owner;
            this.priority = priority;
            this.value = value;
        }
    }

    private CompatibilityRuntime() {
    }
}
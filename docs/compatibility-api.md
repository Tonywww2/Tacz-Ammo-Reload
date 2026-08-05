# Compatibility API

The compatibility layer keeps optional-mod knowledge outside caliber, reload, HUD, and shooting code.
Core code may depend on `compat.api` and `CompatibilityRuntime`; it must not reference another mod's
classes or mod ID directly.

## Module layout

- `compat/api`: stable capability contracts and domain values.
- `compat/runtime`: discovery, deterministic ordering, failure isolation, and fallback dispatch.
- `compat/builtin/<modid>`: optional integration implementations.
- `BaseTacZCompatibility`: lowest-priority fallback used in every installation.

An optional module must expose a public no-argument implementation of `CompatibilityModule`. Add its
descriptor to `CompatibilityRuntime.builtInDescriptors()`. The descriptor uses a factory class name so
the JVM does not resolve optional classes before all required mods are present.

## Capabilities

`CompatibilityRegistrar` currently accepts these independent capabilities:

- `GunModeResolver`: resolves the active gun/mode and its stable `ModeKey`.
- `AmmoPolicyContributor`: allows or denies an ammo ID for that resolved mode.
- `ShotOverrideProvider`: resolves dynamic shot values after upstream mods have modified them.

Register only capabilities owned by the module. A failure disables that capability while lower-priority
providers remain available.

## Ordering and decisions

Providers run by descending numeric priority, then ascending module ID. This makes equal-priority results
deterministic. Resolvers and shot overrides use the first handled value. Ammo policies use the first
`ALLOW` or `DENY`; `PASS` delegates to the next provider.

The base TaCZ module uses `Integer.MIN_VALUE`. Optional modules should normally use `1000`; reserve larger
values for narrowly scoped compatibility patches. Business decisions must not depend on Mixin priority.

## Mode state

All per-gun ammo state is stored under `tacz_caliber_ammo:AmmoStates`, keyed by `ModeKey.storageKey()`.
The base mode is `ModeKey.MAIN`. Legacy root-level `LoadedSeq` and `BarrelAmmo` data are lazily migrated to
the main mode on first write.

A mode key must be stable across restarts and must not include transient entity or inventory IDs. A module
must use a different key when two modes can retain independent magazines, such as a main gun and an
underbarrel launcher.

## Optional dependency rules

- Put optional class names behind reflection or in a separately gated compatibility class.
- Do not place optional types in shared method signatures, fields, annotations, or static initializers.
- Treat unknown dependency versions as either disabled or explicitly degraded in the descriptor.
- Query methods must be side-effect free. Inventory/NBT mutations remain in core transactional services.
- Prefer downstream value modification over exclusive `@Redirect` injections.

## Arcana policy

Arcana owns the active weapon mode, logical ammo selection, `BulletData`, and dynamic bullet-count override.
TacZ Caliber Ammo owns the actual loaded round sequence and per-ammo ballistics. Arcana alternate ammo modes
accept caliber variants of their logical ammo; an unmapped logical ammo ID accepts only itself. Main and
underbarrel magazines use different mode keys.
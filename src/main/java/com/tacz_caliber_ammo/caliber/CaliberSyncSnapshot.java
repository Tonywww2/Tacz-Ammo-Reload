package com.tacz_caliber_ammo.caliber;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** Compact server-authoritative caliber data sent to clients after datapack synchronization. */
public record CaliberSyncSnapshot(
        Map<ResourceLocation, AmmoProfile> ammo,
        Map<ResourceLocation, GunDamageModifier> guns,
        Map<ResourceLocation, Caliber> calibers,
        Map<ResourceLocation, Set<ResourceLocation>> modifiedGunCalibers,
        Map<ResourceLocation, GunDamageModifier> modifiedGunDamage) {

    private static final int MAX_ENTRIES = 65_536;
    private static final int MAX_CALIBERS_PER_GUN = 256;

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(this.ammo.size());
        for (Map.Entry<ResourceLocation, AmmoProfile> entry : this.ammo.entrySet()) {
            writeId(buffer, entry.getKey());
            writeAmmoProfile(buffer, entry.getValue());
        }
        writeGunModifiers(buffer, this.guns);

        buffer.writeVarInt(this.calibers.size());
        for (Map.Entry<ResourceLocation, Caliber> entry : this.calibers.entrySet()) {
            writeId(buffer, entry.getKey());
            buffer.writeUtf(entry.getValue().name());
        }

        buffer.writeVarInt(this.modifiedGunCalibers.size());
        for (Map.Entry<ResourceLocation, Set<ResourceLocation>> entry : this.modifiedGunCalibers.entrySet()) {
            writeId(buffer, entry.getKey());
            writeIds(buffer, entry.getValue());
        }
        writeGunModifiers(buffer, this.modifiedGunDamage);
    }

    public static CaliberSyncSnapshot decode(FriendlyByteBuf buffer) {
        Map<ResourceLocation, AmmoProfile> ammo = new HashMap<>();
        int ammoCount = readCount(buffer, MAX_ENTRIES, "ammo profiles");
        for (int index = 0; index < ammoCount; index++) {
            ammo.put(readId(buffer), readAmmoProfile(buffer));
        }

        Map<ResourceLocation, GunDamageModifier> guns = readGunModifiers(buffer);
        Map<ResourceLocation, Caliber> calibers = new HashMap<>();
        int caliberCount = readCount(buffer, MAX_ENTRIES, "calibers");
        for (int index = 0; index < caliberCount; index++) {
            ResourceLocation id = readId(buffer);
            calibers.put(id, new Caliber(id, buffer.readUtf()));
        }

        Map<ResourceLocation, Set<ResourceLocation>> modifiedGunCalibers = new HashMap<>();
        int modifiedCount = readCount(buffer, MAX_ENTRIES, "modified guns");
        for (int index = 0; index < modifiedCount; index++) {
            modifiedGunCalibers.put(readId(buffer), readIds(buffer));
        }
        return new CaliberSyncSnapshot(
                ammo, guns, calibers, modifiedGunCalibers, readGunModifiers(buffer));
    }

    private static void writeAmmoProfile(FriendlyByteBuf buffer, AmmoProfile profile) {
        writeId(buffer, profile.caliber());
        buffer.writeFloat(profile.baseDamage());
        buffer.writeFloat(profile.armorIgnore());
        buffer.writeFloat(profile.headShotMultiplier());
        buffer.writeVarInt(profile.pierce());
        buffer.writeFloat(profile.recoilModifier());
        buffer.writeFloat(profile.accuracyModifier());
        buffer.writeFloat(profile.speed());
        buffer.writeVarInt(profile.pelletCount());
        buffer.writeFloat(profile.ballisticCoefficient());
        writeEffects(buffer, profile.effects());
    }

    private static AmmoProfile readAmmoProfile(FriendlyByteBuf buffer) {
        return new AmmoProfile(
                readId(buffer), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readVarInt(),
                buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readVarInt(), buffer.readFloat(),
                readEffects(buffer));
    }

    private static void writeEffects(FriendlyByteBuf buffer, AmmoEffects effects) {
        AmmoEffects safeEffects = effects == null ? AmmoEffects.EMPTY : effects;
        AmmoEffects.ExplosionSpec explosion = safeEffects.explosion();
        buffer.writeBoolean(explosion != null);
        if (explosion != null) {
            buffer.writeBoolean(explosion.enabled());
            buffer.writeFloat(explosion.damage());
            buffer.writeFloat(explosion.radius());
            buffer.writeBoolean(explosion.knockback());
            buffer.writeBoolean(explosion.destroyBlock());
            buffer.writeFloat(explosion.delaySeconds());
        }
        AmmoEffects.IgniteSpec ignite = safeEffects.ignite();
        buffer.writeBoolean(ignite != null);
        if (ignite != null) {
            buffer.writeBoolean(ignite.igniteEntity());
            buffer.writeBoolean(ignite.igniteBlock());
            buffer.writeVarInt(ignite.entitySeconds());
        }
        buffer.writeBoolean(safeEffects.knockback() != null);
        if (safeEffects.knockback() != null) {
            buffer.writeFloat(safeEffects.knockback());
        }
        buffer.writeBoolean(safeEffects.script() != null);
        if (safeEffects.script() != null) {
            writeId(buffer, safeEffects.script());
        }
    }

    private static AmmoEffects readEffects(FriendlyByteBuf buffer) {
        AmmoEffects.ExplosionSpec explosion = null;
        if (buffer.readBoolean()) {
            explosion = new AmmoEffects.ExplosionSpec(
                    buffer.readBoolean(), buffer.readFloat(), buffer.readFloat(),
                    buffer.readBoolean(), buffer.readBoolean(), buffer.readFloat());
        }
        AmmoEffects.IgniteSpec ignite = null;
        if (buffer.readBoolean()) {
            ignite = new AmmoEffects.IgniteSpec(buffer.readBoolean(), buffer.readBoolean(), buffer.readVarInt());
        }
        Float knockback = buffer.readBoolean() ? buffer.readFloat() : null;
        ResourceLocation script = buffer.readBoolean() ? readId(buffer) : null;
        return new AmmoEffects(explosion, ignite, knockback, script);
    }

    private static void writeGunModifiers(
            FriendlyByteBuf buffer, Map<ResourceLocation, GunDamageModifier> modifiers) {
        buffer.writeVarInt(modifiers.size());
        for (Map.Entry<ResourceLocation, GunDamageModifier> entry : modifiers.entrySet()) {
            writeId(buffer, entry.getKey());
            GunDamageModifier modifier = entry.getValue();
            writeIds(buffer, modifier.calibers());
            buffer.writeFloat(modifier.flatDamage());
            buffer.writeFloat(modifier.percentDamage());
        }
    }

    private static Map<ResourceLocation, GunDamageModifier> readGunModifiers(FriendlyByteBuf buffer) {
        Map<ResourceLocation, GunDamageModifier> modifiers = new HashMap<>();
        int count = readCount(buffer, MAX_ENTRIES, "gun modifiers");
        for (int index = 0; index < count; index++) {
            modifiers.put(readId(buffer), new GunDamageModifier(
                    readIds(buffer), buffer.readFloat(), buffer.readFloat()));
        }
        return modifiers;
    }

    private static void writeIds(FriendlyByteBuf buffer, Set<ResourceLocation> ids) {
        buffer.writeVarInt(ids.size());
        for (ResourceLocation id : ids) {
            writeId(buffer, id);
        }
    }

    private static Set<ResourceLocation> readIds(FriendlyByteBuf buffer) {
        int count = readCount(buffer, MAX_CALIBERS_PER_GUN, "gun calibers");
        Set<ResourceLocation> ids = new HashSet<>();
        for (int index = 0; index < count; index++) {
            ids.add(readId(buffer));
        }
        return Set.copyOf(ids);
    }

    private static void writeId(FriendlyByteBuf buffer, ResourceLocation id) {
        buffer.writeUtf(id.toString());
    }

    private static ResourceLocation readId(FriendlyByteBuf buffer) {
        ResourceLocation id = ResourceLocation.tryParse(buffer.readUtf());
        if (id == null) {
            throw new IllegalArgumentException("Invalid resource location in caliber sync packet");
        }
        return id;
    }

    private static int readCount(FriendlyByteBuf buffer, int maximum, String name) {
        int count = buffer.readVarInt();
        if (count < 0 || count > maximum) {
            throw new IllegalArgumentException("Invalid " + name + " count: " + count);
        }
        return count;
    }
}
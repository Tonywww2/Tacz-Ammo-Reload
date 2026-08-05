package com.tacz_caliber_ammo.compat.builtin.arcana;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.resource.pojo.data.gun.BulletData;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

final class ArcanaAccess {

    private final Method getCurrentAmmoId;
    private final Method getCurrentBulletData;

    ArcanaAccess() throws ReflectiveOperationException {
        Class<?> accessor = Class.forName("group.taczexpands.common.accessor.IAccessorGunData");
        this.getCurrentAmmoId = findStatic(accessor, ResourceLocation.class, GunData.class, ItemStack.class);
        this.getCurrentBulletData = findStatic(accessor, BulletData.class, GunData.class, ItemStack.class);
    }

    ResourceLocation getCurrentAmmoId(GunData gunData, ItemStack gun) throws ReflectiveOperationException {
        return (ResourceLocation) this.getCurrentAmmoId.invoke(null, gunData, gun);
    }

    boolean hasBulletAmountOverride(GunData gunData, ItemStack gun, int upstreamAmount)
            throws ReflectiveOperationException {
        BulletData bulletData = (BulletData) this.getCurrentBulletData.invoke(null, gunData, gun);
        return bulletData != null && upstreamAmount != bulletData.getBulletAmount();
    }

    private static Method findStatic(Class<?> owner, Class<?> returnType, Class<?>... parameterTypes)
            throws NoSuchMethodException {
        return Arrays.stream(owner.getMethods())
                .filter(method -> Modifier.isStatic(method.getModifiers()))
                .filter(method -> method.getReturnType() == returnType)
                .filter(method -> Arrays.equals(method.getParameterTypes(), parameterTypes))
                .findFirst()
                .orElseThrow(() -> new NoSuchMethodException(owner.getName() + " " + returnType.getName()
                        + Arrays.toString(parameterTypes)));
    }
}
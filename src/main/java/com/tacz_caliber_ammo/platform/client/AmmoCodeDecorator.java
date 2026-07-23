package com.tacz_caliber_ammo.platform.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.api.item.IAmmo;
import com.tacz_caliber_ammo.platform.config.ModConfig;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
//? if forge {
import net.minecraftforge.client.IItemDecorator;
import net.minecraftforge.client.event.RegisterItemDecorationsEvent;
import net.minecraftforge.registries.ForgeRegistries;
//?} else {
/*import net.neoforged.neoforge.client.IItemDecorator;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;
*///?}

public final class AmmoCodeDecorator implements IItemDecorator {

    private static final AmmoCodeDecorator INSTANCE = new AmmoCodeDecorator();
    private static final int COLOR = 0xFFFFFF;
    private static final float ICON = 16.0f;
    private static final float FONT_SCALE = 0.5f;

    private AmmoCodeDecorator() {
    }

    @SuppressWarnings("deprecation")
    public static void register(RegisterItemDecorationsEvent event) {
        //? if forge {
        Iterable<Item> items = ForgeRegistries.ITEMS;
        //?} else {
        /*Iterable<Item> items = BuiltInRegistries.ITEM;
        *///?}
        for (Item item : items) {
            if (item instanceof IAmmo) {
                event.register(item, INSTANCE);
            }
        }
    }

    @Override
    public boolean render(GuiGraphics guiGraphics, Font font, ItemStack stack, int xOffset, int yOffset) {
        String code = codeOf(stack);
        if (code == null) {
            return false;
        }
        int width = font.width(code);
        if (width <= 0) {
            return false;
        }
        int localX = Math.round((ICON / FONT_SCALE - width) / 2.0f);
        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(xOffset, yOffset, 200.0);
        pose.scale(FONT_SCALE, FONT_SCALE, 1.0f);
        guiGraphics.drawString(font, code, localX, 0, COLOR, true);
        pose.popPose();
        return true;
    }

    private static String codeOf(ItemStack stack) {
        ModConfig.AmmoCodeDisplay mode = ModConfig.ammoCodeDisplay();
        if (mode == ModConfig.AmmoCodeDisplay.NEVER || !(stack.getItem() instanceof IAmmo ammo)) {
            return null;
        }
        ResourceLocation ammoId = ammo.getAmmoId(stack);
        if (ammoId == null) {
            return null;
        }
        String key = "ammo." + ammoId.getNamespace() + "." + ammoId.getPath().replace('/', '.') + ".abbr";
        boolean hasAbbr = I18n.exists(key);
        if (mode == ModConfig.AmmoCodeDisplay.ALWAYS) {
            String path = ammoId.getPath();
            String raw = hasAbbr ? I18n.get(key) : path.substring(path.lastIndexOf('/') + 1);
            String code = raw.replace('_', ' ').trim();
            return code.isEmpty() ? null : code;
        }
        if (!hasAbbr) {
            return null;
        }
        String offKey = key + ".off";
        if (I18n.exists(offKey) && isTrue(I18n.get(offKey))) {
            return null;
        }
        String abbr = I18n.get(key).replace('_', ' ').trim();
        return abbr.isEmpty() ? null : abbr;
    }

    private static boolean isTrue(String value) {
        String normalized = value.trim();
        return normalized.equalsIgnoreCase("true") || normalized.equals("1")
                || normalized.equalsIgnoreCase("yes") || normalized.equalsIgnoreCase("on");
    }
}
package com.tacz_caliber_ammo.platform;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

public final class PlatformClient {

    public static void renderBackground(Screen screen, GuiGraphics graphics,
            int mouseX, int mouseY, float partialTick) {
        //? if forge {
        screen.renderBackground(graphics);
        //?} else {
        /*screen.renderBackground(graphics, mouseX, mouseY, partialTick);
        *///?}
    }

    private PlatformClient() {
    }
}
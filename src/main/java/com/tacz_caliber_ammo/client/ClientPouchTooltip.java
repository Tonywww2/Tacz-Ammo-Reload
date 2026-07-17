package com.tacz_caliber_ammo.client;

import java.util.List;

import org.joml.Matrix4f;

import com.tacz_caliber_ammo.item.PouchTooltipData;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;

/**
 * Renders the ammo pouch tooltip as a two-column table so counts and names each line up:
 * the count column is right-aligned to a fixed width, the name column starts at a fixed x.
 * Header rows (capacity / "Stored:" / "Load pattern:") span the full width with no count column.
 */
public class ClientPouchTooltip implements ClientTooltipComponent {

    private static final int LINE = 10;
    private static final int COLUMN_GAP = 6;

    private final List<PouchTooltipData.Entry> entries;
    private final int countColWidth;

    public ClientPouchTooltip(PouchTooltipData data) {
        this.entries = data.entries();
        Font font = Minecraft.getInstance().font;
        int w = 0;
        for (PouchTooltipData.Entry e : entries) {
            if (!e.header()) {
                w = Math.max(w, font.width(e.count() + "x"));
            }
        }
        this.countColWidth = w;
    }

    @Override
    public int getHeight() {
        return entries.size() * LINE + 2;
    }

    @Override
    public int getWidth(Font font) {
        int max = 0;
        for (PouchTooltipData.Entry e : entries) {
            if (e.header()) {
                max = Math.max(max, font.width(e.text()));
            } else {
                max = Math.max(max, countColWidth + COLUMN_GAP + font.width(e.text()));
            }
        }
        return max;
    }

    @Override
    public void renderText(Font font, int x, int y, Matrix4f matrix, MultiBufferSource.BufferSource buffer) {
        int yy = y;
        for (PouchTooltipData.Entry e : entries) {
            if (e.header()) {
                font.drawInBatch(e.text(), (float) x, (float) yy, -1, false, matrix, buffer,
                        Font.DisplayMode.NORMAL, 0, 0xF000F0);
            } else {
                Component cnt = Component.literal(e.count() + "x").withStyle(ChatFormatting.WHITE);
                int cntX = x + Math.max(0, countColWidth - font.width(cnt));
                font.drawInBatch(cnt, (float) cntX, (float) yy, -1, false, matrix, buffer,
                        Font.DisplayMode.NORMAL, 0, 0xF000F0);
                font.drawInBatch(e.text(), (float) (x + countColWidth + COLUMN_GAP), (float) yy, -1, false,
                        matrix, buffer, Font.DisplayMode.NORMAL, 0, 0xF000F0);
            }
            yy += LINE;
        }
    }
}

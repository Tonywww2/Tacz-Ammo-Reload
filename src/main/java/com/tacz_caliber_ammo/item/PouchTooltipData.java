package com.tacz_caliber_ammo.item;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

/**
 * Ammo pouch tooltip table data. Built by {@code AmmoPouchItem.getTooltipImage} (client-side, during
 * tooltip gathering) and rendered by {@code ClientPouchTooltip} as a two-column table (count | name).
 */
public record PouchTooltipData(List<Entry> entries) implements TooltipComponent {

    /**
     * One tooltip row.
     * <ul>
     * <li>{@code header=true}: a full-width label row (only {@code text}, no count column) such as the
     * capacity line, "Stored:" or "Load pattern:".</li>
     * <li>{@code header=false}: a data row with the {@code count} column and the ammo-name {@code text} column.</li>
     * </ul>
     */
    public record Entry(boolean header, int count, Component text) {
    }
}

package com.tacz_caliber_ammo.network;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.tacz_caliber_ammo.caliber.PatternEntry;
import com.tacz_caliber_ammo.item.AmmoPouchItem;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

/**
 * Edit the pouch reload pattern (client -&gt; server): add an ammo type, remove an entry, change an
 * entry's per-cycle count, or reorder entries. Sent from the pouch GUI pattern area. The pattern is
 * stored as NBT on the pouch and syncs back to the client through the container inventory slot.
 * Registered in {@link ModNetwork#register()}.
 */
public record CMsgPouchPattern(int pouchSlot, int op, int index, String ammoId, int value) {

    public static final int OP_ADD = 0;          // append ammoId (perCycle=value), deduped, capped at MAX_PATTERN
    public static final int OP_REMOVE = 1;       // remove entry at index
    public static final int OP_SET_PERCYCLE = 2; // set entry[index].perCycle = value (clamped)
    public static final int OP_MOVE = 3;         // move entry from index to value

    public static final int MAX_PER_CYCLE = 240;

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(this.pouchSlot);
        buf.writeVarInt(this.op);
        buf.writeVarInt(this.index);
        buf.writeUtf(this.ammoId);
        buf.writeVarInt(this.value);
    }

    public static CMsgPouchPattern decode(FriendlyByteBuf buf) {
        return new CMsgPouchPattern(
                buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readUtf(), buf.readVarInt());
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) {
                return;
            }
            if (this.pouchSlot < 0 || this.pouchSlot >= player.getInventory().getContainerSize()) {
                return;
            }
            ItemStack pouch = player.getInventory().getItem(this.pouchSlot);
            if (!(pouch.getItem() instanceof AmmoPouchItem)) {
                return;
            }
            List<PatternEntry> pat = new ArrayList<>(AmmoPouchItem.getPattern(pouch));
            switch (this.op) {
                case OP_ADD -> {
                    ResourceLocation id = ResourceLocation.tryParse(this.ammoId);
                    // Duplicates are allowed: the same ammo type may appear multiple times.
                    if (id != null && pat.size() < AmmoPouchItem.MAX_PATTERN) {
                        pat.add(new PatternEntry(id, clampPer(this.value)));
                    }
                }
                case OP_REMOVE -> {
                    if (this.index >= 0 && this.index < pat.size()) {
                        pat.remove(this.index);
                    }
                }
                case OP_SET_PERCYCLE -> {
                    if (this.index >= 0 && this.index < pat.size()) {
                        PatternEntry e = pat.get(this.index);
                        pat.set(this.index, new PatternEntry(e.ammoId(), clampPer(this.value)));
                    }
                }
                case OP_MOVE -> {
                    if (this.index >= 0 && this.index < pat.size()
                            && this.value >= 0 && this.value < pat.size()) {
                        pat.add(this.value, pat.remove(this.index));
                    }
                }
                default -> {
                    return;
                }
            }
            AmmoPouchItem.setPattern(pouch, pat);
        });
        ctx.setPacketHandled(true);
    }

    private static int clampPer(int v) {
        if (v < 1) {
            return 1;
        }
        return Math.min(v, MAX_PER_CYCLE);
    }
}

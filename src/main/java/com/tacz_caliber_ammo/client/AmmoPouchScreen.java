package com.tacz_caliber_ammo.client;

import java.util.ArrayList;
import java.util.List;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.builder.AmmoItemBuilder;
import com.tacz_caliber_ammo.caliber.PatternEntry;
import com.tacz_caliber_ammo.caliber.Round;
import com.tacz_caliber_ammo.item.AmmoPouchItem;
import com.tacz_caliber_ammo.menu.AmmoPouchMenu;
import com.tacz_caliber_ammo.network.CMsgPouchDeposit;
import com.tacz_caliber_ammo.network.CMsgPouchPattern;
import com.tacz_caliber_ammo.network.CMsgPouchWithdraw;
import com.tacz_caliber_ammo.network.ModNetwork;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Ammo pouch GUI screen. Stage 3.2: draws the storage virtual-slot grid (ammo icon + count) and the
 * player inventory, supports Shift-click deposit (via the menu's quickMoveStack) and click-to-withdraw
 * one stack from a storage slot (network packet). The pattern area is added in stage 3.3.
 */
public class AmmoPouchScreen extends AbstractContainerScreen<AmmoPouchMenu> {

    // Staggered "ammo-can" storage layout: even rows hold ROW_A cells, odd rows hold ROW_B cells
    // shifted right by half a slot, with a compressed row pitch so the rows interlock like stacked rounds.
    private static final int STORAGE_X = 4;
    private static final int STORAGE_Y = 18;
    private static final int SLOT = 19;   // horizontal pitch (16px cell + ~1px gap between cells)
    private static final int ROW_H = 21;  // vertical pitch (16px cell + a few px gap between rows)
    private static final int ROW_A = 9;   // cells on even rows
    private static final int ROW_B = 8;   // cells on odd rows (shifted right by SLOT/2)
    private static final int STORAGE_ROWS = 4;
    private static final int PAIR = ROW_A + ROW_B;
    private static final int STORAGE_CAP = PAIR * (STORAGE_ROWS / 2);

    // Pattern area: a single row of MAX_PATTERN cells below the storage grid, each with an ammo icon,
    // the per-cycle count, and a [-]/[+] button row underneath to adjust it.
    private static final int PATTERN_X = 39;
    private static final int PATTERN_Y = 106;
    private static final int PATTERN_SLOT = 20;
    private static final int BTN_W = 8;
    private static final int BTN_H = 8;
    private static final int BTN_DY = 19;   // button row offset below the pattern cell top
    private static final int HELP_X = 162;  // help "?" marker top-left, relative to leftPos
    private static final int HELP_Y = 107;

    private static final ResourceLocation BG_TEXTURE =
            new ResourceLocation("tacz_caliber_ammo", "textures/gui/ammo_pouch.png");

    // Pattern drag-reorder state: the entry index being dragged, and whether the cursor has actually
    // moved since the press (so a plain click is not treated as a drag).
    private int patternDragFrom = -1;
    private boolean patternDragging = false;

    public AmmoPouchScreen(AmmoPouchMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = AmmoPouchMenu.IMAGE_WIDTH;
        this.imageHeight = AmmoPouchMenu.IMAGE_HEIGHT;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        // Panel background texture (placeholder art).
        g.blit(BG_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
        // All slot backgrounds (storage, pattern, player inventory) are baked into the texture.
        // Storage ammo icons + counts.
        List<Round> storage = this.menu.getStorageList();
        for (int i = 0; i < storage.size() && i < STORAGE_CAP; i++) {
            int[] p = slotPos(i);
            Round r = storage.get(i);
            ItemStack icon = AmmoItemBuilder.create().setId(r.ammoId()).setCount(1).build();
            g.renderItem(icon, p[0], p[1]);
            g.renderItemDecorations(this.font, icon, p[0], p[1], String.valueOf(r.count()));
        }
        // Pattern cells + per-cycle buttons.
        List<PatternEntry> pattern = this.menu.getPatternList();
        for (int i = 0; i < AmmoPouchItem.MAX_PATTERN; i++) {
            int px = x + PATTERN_X + i * PATTERN_SLOT;
            int py = y + PATTERN_Y;
            if (i < pattern.size()) {
                PatternEntry e = pattern.get(i);
                ItemStack icon = AmmoItemBuilder.create().setId(e.ammoId()).setCount(1).build();
                g.renderItem(icon, px, py);
                g.renderItemDecorations(this.font, icon, px, py, String.valueOf(e.perCycle()));
                drawButton(g, px, py + BTN_DY, "-");
                drawButton(g, px + BTN_W, py + BTN_DY, "+");
            }
        }
        // Help "?" marker (hover shows the controls tooltip).
        g.fill(x + HELP_X - 1, y + HELP_Y - 1, x + HELP_X + 8, y + HELP_Y + 9, 0xFF555555);
        g.fill(x + HELP_X, y + HELP_Y, x + HELP_X + 7, y + HELP_Y + 8, 0xFF8B8B8B);
        int qw = this.font.width("?");
        g.drawString(this.font, "?", x + HELP_X + (7 - qw) / 2 + 1, y + HELP_Y + 1, 0xFF303030, false);
    }

    /** Draw a small [-]/[+] button with a centered label; (bx,by) is its top-left, size BTN_W x BTN_H. */
    private void drawButton(GuiGraphics g, int bx, int by, String label) {
        g.fill(bx, by, bx + BTN_W, by + BTN_H, 0xFF555555);
        g.fill(bx + 1, by + 1, bx + BTN_W - 1, by + BTN_H - 1, 0xFF8B8B8B);
        int tw = this.font.width(label);
        g.drawString(this.font, label, bx + (BTN_W - tw) / 2 + 1, by + 1, 0xFF303030, false);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // Do not render the pouch title; keep only the inventory label.
        g.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        // While dragging a pattern entry, draw its icon following the cursor and skip tooltips.
        if (this.patternDragging && this.patternDragFrom >= 0) {
            List<PatternEntry> dpat = this.menu.getPatternList();
            if (this.patternDragFrom < dpat.size()) {
                ItemStack drag = AmmoItemBuilder.create().setId(dpat.get(this.patternDragFrom).ammoId()).setCount(1).build();
                g.renderItem(drag, mouseX - 8, mouseY - 8);
            }
            return;
        }
        // Help marker: hovering shows the controls tooltip.
        if (isOverHelp(mouseX, mouseY)) {
            List<Component> help = new ArrayList<>();
            help.add(Component.translatable("gui.tacz_caliber_ammo.pouch.help.title"));
            help.add(Component.translatable("gui.tacz_caliber_ammo.pouch.help.l1"));
            help.add(Component.translatable("gui.tacz_caliber_ammo.pouch.help.l2"));
            help.add(Component.translatable("gui.tacz_caliber_ammo.pouch.help.l3"));
            help.add(Component.translatable("gui.tacz_caliber_ammo.pouch.help.l4"));
            help.add(Component.translatable("gui.tacz_caliber_ammo.pouch.help.l5"));
            g.renderComponentTooltip(this.font, help, mouseX, mouseY);
            return;
        }
        // Tooltip for the hovered storage slot (ammo name).
        int idx = storageIndexAt(mouseX, mouseY);
        List<Round> storage = this.menu.getStorageList();
        int pIdx = patternSlotIndexAt(mouseX, mouseY);
        List<PatternEntry> pattern = this.menu.getPatternList();
        if (idx >= 0 && idx < storage.size()) {
            ItemStack icon = AmmoItemBuilder.create().setId(storage.get(idx).ammoId()).setCount(1).build();
            g.renderTooltip(this.font, icon, mouseX, mouseY);
        } else if (pIdx >= 0 && pIdx < pattern.size()) {
            ItemStack icon = AmmoItemBuilder.create().setId(pattern.get(pIdx).ammoId()).setCount(1).build();
            g.renderTooltip(this.font, icon, mouseX, mouseY);
        } else {
            this.renderTooltip(g, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int pouchSlot = this.menu.getPouchSlot();
        ItemStack carried = this.menu.getCarried();
        boolean carriedAmmo = !carried.isEmpty() && carried.getItem() instanceof IAmmo;
        // Storage slot: click with ammo on the cursor deposits it; otherwise left-click withdraws a
        // stack and right-click adds the ammo type to the pattern.
        int idx = storageIndexAt(mx, my);
        if (idx >= 0) {
            if (carriedAmmo) {
                ModNetwork.CHANNEL.sendToServer(new CMsgPouchDeposit(pouchSlot));
                return true;
            }
            List<Round> storage = this.menu.getStorageList();
            if (idx < storage.size()) {
                String id = storage.get(idx).ammoId().toString();
                if (button == 1) {
                    ModNetwork.CHANNEL.sendToServer(
                            new CMsgPouchPattern(pouchSlot, CMsgPouchPattern.OP_ADD, 0, id, 1));
                } else {
                    ModNetwork.CHANNEL.sendToServer(new CMsgPouchWithdraw(pouchSlot, id));
                }
                return true;
            }
            return true; // empty storage slot: swallow the click
        }
        // Pattern area: clicking with ammo on the cursor registers that ammo type into the pattern.
        if (carriedAmmo && patternSlotIndexAt(mx, my) >= 0) {
            ResourceLocation aid = ((IAmmo) carried.getItem()).getAmmoId(carried);
            if (aid != null && !DefaultAssets.EMPTY_AMMO_ID.equals(aid)) {
                ModNetwork.CHANNEL.sendToServer(
                        new CMsgPouchPattern(pouchSlot, CMsgPouchPattern.OP_ADD, 0, aid.toString(), 1));
            }
            return true;
        }
        // Pattern [-]/[+] button: adjust the per-cycle count.
        int[] btn = patternButtonAt(mx, my);
        if (btn != null) {
            List<PatternEntry> pattern = this.menu.getPatternList();
            if (btn[0] < pattern.size()) {
                int next = pattern.get(btn[0]).perCycle() + btn[1];
                ModNetwork.CHANNEL.sendToServer(
                        new CMsgPouchPattern(pouchSlot, CMsgPouchPattern.OP_SET_PERCYCLE, btn[0], "", next));
            }
            return true;
        }
        // Pattern cell: right-click removes it; left-click starts a drag (reorder / drag-out to remove).
        int pIdx = patternSlotIndexAt(mx, my);
        if (pIdx >= 0) {
            List<PatternEntry> pattern = this.menu.getPatternList();
            if (pIdx < pattern.size()) {
                if (button == 1) {
                    ModNetwork.CHANNEL.sendToServer(
                            new CMsgPouchPattern(pouchSlot, CMsgPouchPattern.OP_REMOVE, pIdx, "", 0));
                } else if (button == 0) {
                    this.patternDragFrom = pIdx;
                    this.patternDragging = false;
                }
            }
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (this.patternDragFrom >= 0 && button == 0) {
            this.patternDragging = true;
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (this.patternDragFrom >= 0 && button == 0) {
            int from = this.patternDragFrom;
            boolean wasDragging = this.patternDragging;
            this.patternDragFrom = -1;
            this.patternDragging = false;
            List<PatternEntry> pattern = this.menu.getPatternList();
            int pouchSlot = this.menu.getPouchSlot();
            int to = patternSlotIndexAt(mx, my);
            if (from < pattern.size()) {
                if (to >= 0 && to != from && to < pattern.size()) {
                    // Dropped onto another pattern cell -> reorder.
                    ModNetwork.CHANNEL.sendToServer(
                            new CMsgPouchPattern(pouchSlot, CMsgPouchPattern.OP_MOVE, from, "", to));
                } else if (wasDragging && to < 0) {
                    // Dragged out of the pattern row and released -> remove.
                    ModNetwork.CHANNEL.sendToServer(
                            new CMsgPouchPattern(pouchSlot, CMsgPouchPattern.OP_REMOVE, from, "", 0));
                }
            }
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    /** Whether (mx,my) is over the help "?" marker. */
    private boolean isOverHelp(double mx, double my) {
        int hx = this.leftPos + HELP_X;
        int hy = this.topPos + HELP_Y;
        return mx >= hx - 1 && mx < hx + 8 && my >= hy - 1 && my < hy + 9;
    }

    /** Storage virtual-slot index under (mx,my), or -1. */
    private int storageIndexAt(double mx, double my) {
        for (int i = 0; i < STORAGE_CAP; i++) {
            int[] p = slotPos(i);
            if (mx >= p[0] && mx < p[0] + 16 && my >= p[1] && my < p[1] + 16) {
                return i;
            }
        }
        return -1;
    }

    /** Absolute top-left (x,y) of the i-th staggered storage cell. */
    private int[] slotPos(int i) {
        int pair = i / PAIR;
        int r = i % PAIR;
        int row;
        int colInRow;
        int shift;
        if (r < ROW_A) {
            row = pair * 2;
            colInRow = r;
            shift = 0;
        } else {
            row = pair * 2 + 1;
            colInRow = r - ROW_A;
            shift = SLOT / 2;
        }
        int sx = this.leftPos + STORAGE_X + shift + colInRow * SLOT;
        int sy = this.topPos + STORAGE_Y + row * ROW_H;
        return new int[] { sx, sy };
    }

    /** Pattern cell index under (mx,my), or -1. */
    private int patternSlotIndexAt(double mx, double my) {
        int py = this.topPos + PATTERN_Y;
        if (my < py || my >= py + 16) {
            return -1;
        }
        for (int i = 0; i < AmmoPouchItem.MAX_PATTERN; i++) {
            int px = this.leftPos + PATTERN_X + i * PATTERN_SLOT;
            if (mx >= px && mx < px + 16) {
                return i;
            }
        }
        return -1;
    }

    /** Pattern per-cycle button under (mx,my) as {index, delta(-1 or +1)}, or null. */
    private int[] patternButtonAt(double mx, double my) {
        int by = this.topPos + PATTERN_Y + BTN_DY;
        if (my < by || my >= by + BTN_H) {
            return null;
        }
        for (int i = 0; i < AmmoPouchItem.MAX_PATTERN; i++) {
            int px = this.leftPos + PATTERN_X + i * PATTERN_SLOT;
            if (mx >= px && mx < px + BTN_W) {
                return new int[] { i, -1 };
            }
            if (mx >= px + BTN_W && mx < px + 2 * BTN_W) {
                return new int[] { i, 1 };
            }
        }
        return null;
    }
}

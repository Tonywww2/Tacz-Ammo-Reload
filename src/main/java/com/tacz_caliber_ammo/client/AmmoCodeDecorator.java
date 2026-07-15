package com.tacz_caliber_ammo.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.api.item.IAmmo;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.IItemDecorator;
import net.minecraftforge.client.event.RegisterItemDecorationsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 在子弹物品图标顶边用文字渲染其"代号缩写"——由弹药 id 映射到本地化缩写键
 * {@code ammo.<命名空间>.<路径(/换为.)>.abbr} 取得（下划线渲染为空格；无该键则不渲染）。
 * 例 {@code tacz_caliber_ammo:5_56x45/m855} -> 键 {@code ammo.tacz_caliber_ammo.5_56x45.m855.abbr} -> {@code M855}。
 *
 * <p>机制：Forge {@link IItemDecorator} + {@link RegisterItemDecorationsEvent}（MOD 总线，仅客户端）。
 * 遵循 §2 自注册约定（@Mod.EventBusSubscriber 自动发现，主类 {@code TaczCaliberAmmo} 不改）。
 * 仅作用于 TacZ 弹药物品（{@link IAmmo}；弹药盒 IAmmoBox 不含，符合"子弹"语义）。
 */
@Mod.EventBusSubscriber(modid = "tacz_caliber_ammo", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class AmmoCodeDecorator implements IItemDecorator {

    private static final AmmoCodeDecorator INSTANCE = new AmmoCodeDecorator();

    /** 顶边文字颜色（白色，配合阴影在各色图标上均可读）。 */
    private static final int COLOR = 0xFFFFFF;

    /** 物品图标边长（GUI 像素）。 */
    private static final float ICON = 16.0f;

    /** 固定字号缩放（相对原版字体；0.5 = 半字号）。所有代号统一此大小，不随长度自适应。 */
    private static final float FONT_SCALE = 0.5f;

    private AmmoCodeDecorator() {
    }

    /** 给所有 TacZ 弹药物品（IAmmo）挂上本装饰器。 */
    @SubscribeEvent
    public static void onRegisterItemDecorations(RegisterItemDecorationsEvent event) {
        for (Item item : ForgeRegistries.ITEMS) {
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
        // 固定字号（不随代号长度自适应）；水平居中，纵向贴顶边（y=0）。
        float scale = FONT_SCALE;
        int localX = Math.round((ICON / scale - width) / 2.0f);

        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        // z=200 使文字浮于物品模型之上（与原版数量文字一致）。
        pose.translate(xOffset, yOffset, 200.0);
        pose.scale(scale, scale, 1.0f);
        guiGraphics.drawString(font, code, localX, 0, COLOR, true);
        pose.popPose();
        return true;
    }

    /**
     * 取弹药代号缩写：读该弹药 id 对应的本地化缩写键
     * {@code ammo.<命名空间>.<路径(/换为.)>.abbr}，并把其中的下划线替换为空格。
     * 无该键（即未配置代号的弹药）则返回 null —— 不渲染。
     */
    private static String codeOf(ItemStack stack) {
        if (!(stack.getItem() instanceof IAmmo ammo)) {
            return null;
        }
        ResourceLocation ammoId = ammo.getAmmoId(stack);
        if (ammoId == null) {
            return null;
        }
        String key = "ammo." + ammoId.getNamespace() + "." + ammoId.getPath().replace('/', '.') + ".abbr";
        if (!I18n.exists(key)) {
            return null;
        }
        String abbr = I18n.get(key).replace('_', ' ').trim();
        return abbr.isEmpty() ? null : abbr;
    }
}

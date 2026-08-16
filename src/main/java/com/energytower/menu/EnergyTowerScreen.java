package com.energytower.menu;

import com.energytower.EnergyTower;
import com.energytower.network.C2SClearBindingsPacket;
import com.energytower.network.C2SSetModePacket;
import com.energytower.network.C2SSetRatePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * 能量塔控制面板屏幕（纯客户端渲染与交互）。
 * <p>
 * 展示：储能条、无线传输速率滑块、传输模式按钮、绑定设备数。
 * 交互：拖动滑块 / 点击按钮 → 发送 C2S 数据包 → 服务端校验后写入 → 数据槽同步回来刷新。
 */
public class EnergyTowerScreen extends AbstractContainerScreen<EnergyTowerMenu> {

    private static final ResourceLocation GUI =
            ResourceLocation.fromNamespaceAndPath(EnergyTower.MODID, "textures/gui/energy_tower.png");

    // ===== GUI 布局（相对 leftPos / topPos）=====
    private static final int ENERGY_BAR_X = 8;
    private static final int ENERGY_BAR_Y = 18;
    private static final int ENERGY_BAR_W = 16;
    private static final int ENERGY_BAR_H = 50;

    private static final int SLIDER_X = 42;
    private static final int SLIDER_Y = 56;
    private static final int SLIDER_W = 120;
    private static final int SLIDER_H = 8;

    /** 滑块可调范围上限（FE/t）。自定义输入不受此限制，最高到 {@link #CUSTOM_MAX_RATE} */
    private static final int SLIDER_MAX_RATE = 32_000;
    /** 自定义输入允许的最大速率（FE/t），超出自动设置为该值 */
    private static final int CUSTOM_MAX_RATE = 400_000;

    // 预设档位（点击快速选择）
    private static final int[] PRESET_VALUES = {100, 1_000, 5_000, 10_000, 32_000};
    private static final String[] PRESET_LABELS = {"100", "1k", "5k", "10k", "32k"};
    private static final int PRESET_Y = 72;
    private static final int PRESET_W = 22;
    private static final int PRESET_H = 10;
    private static final int PRESET_GAP = 2;

    // 自定义数值输入框
    private static final int RATE_BOX_X = 42;
    private static final int RATE_BOX_Y = 88;
    private static final int RATE_BOX_W = 90;
    private static final int RATE_BOX_H = 14;

    private static final int BTN_X = 42;
    private static final int BTN_Y = 108;
    private static final int BTN_W = 120;
    private static final int BTN_H = 16;

    // 绑定设备数文本行
    private static final int BINDINGS_Y = 128;

    // 清除全部绑定按钮（复用 BTN_X / BTN_W 的横坐标与宽度）
    private static final int CLEAR_BTN_Y = 140;
    private static final int CLEAR_BTN_H = 16;

    // 颜色
    private static final int COLOR_TRACK = 0xFF1E1E1E;
    private static final int COLOR_TRACK_BORDER = 0xFF6A6A6A;
    private static final int COLOR_KNOB = 0xFF33E0FF;
    private static final int COLOR_BTN = 0xFF2B2B2B;
    private static final int COLOR_BTN_BORDER = 0xFF7A7A7A;
    private static final int COLOR_TEXT = 0xFFE0E0E0;
    private static final int COLOR_TEXT_DIM = 0xFF9A9A9A;

    /** 自定义速率输入框 */
    private EditBox rateBox;
    private boolean draggingSlider;
    private int lastSentRate = -1;
    /** 最近一次已知速率（用于服务端同步回填输入框） */
    private int lastKnownRate = -1;
    /** 清除全部绑定按钮是否处于“再次点击确认”状态 */
    private boolean confirmingClear;

    public EnergyTowerScreen(EnergyTowerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        // 自定义速率输入框：只允许数字，回车生效
        this.rateBox = new EditBox(this.font, leftPos + RATE_BOX_X, topPos + RATE_BOX_Y, RATE_BOX_W, RATE_BOX_H,
                Component.translatable("gui.energy_tower.rate"));
        this.rateBox.setMaxLength(6);
        this.rateBox.setValue(String.valueOf(this.menu.getTransferRate()));
        this.rateBox.setFilter(s -> s.chars().allMatch(Character::isDigit));
        this.addRenderableWidget(this.rateBox);
        this.lastKnownRate = this.menu.getTransferRate();
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        // 服务端速率变化时同步回填输入框（玩家未在编辑输入框时）
        if (this.rateBox != null && !this.rateBox.isFocused()) {
            int current = menu.getTransferRate();
            if (current != lastKnownRate) {
                lastKnownRate = current;
                this.rateBox.setValue(String.valueOf(current));
            }
        }
        // 背景面板（8 参版本：显式指定纹理真实尺寸 176x166，保证 1:1 渲染，
        // 贴图内的能量槽位与下面代码绘制的能量条严格对齐；7 参版本会按 256 拉伸背景）
        gui.blit(GUI, leftPos, topPos, 0, 0, imageWidth, imageHeight, 176, 166);

        // ===== 储能条 =====
        int x = leftPos + ENERGY_BAR_X;
        int y = topPos + ENERGY_BAR_Y;
        // 背景槽
        gui.fill(x - 1, y - 1, x + ENERGY_BAR_W + 1, y + ENERGY_BAR_H + 1, COLOR_TRACK_BORDER);
        gui.fill(x, y, x + ENERGY_BAR_W, y + ENERGY_BAR_H, COLOR_TRACK);
        // 填充（自下而上）
        int max = Math.max(1, menu.getMaxEnergy());
        float pct = Mth.clamp(menu.getEnergyStored() / (float) max, 0F, 1F);
        int fillH = (int) (ENERGY_BAR_H * pct);
        int fillTop = y + ENERGY_BAR_H - fillH;
        int color = energyColor(pct);
        gui.fill(x, fillTop, x + ENERGY_BAR_W, y + ENERGY_BAR_H, color);
        // 储能数值：居中放在能量条正下方，保证与槽位对齐
        gui.drawCenteredString(this.font, Component.literal(fmt(menu.getEnergyStored())),
                x + ENERGY_BAR_W / 2, y + ENERGY_BAR_H + 4, COLOR_TEXT);

        // ===== 标题与数据 =====
        gui.drawString(this.font, Component.translatable("gui.energy_tower.title"), leftPos + 8, topPos + 6, 0xFFFFFFFF);
        gui.drawString(this.font, Component.translatable("gui.energy_tower.rate"),
                leftPos + SLIDER_X, topPos + SLIDER_Y - 10, COLOR_TEXT_DIM);

        // ===== 速率滑块 =====
        int sliderLeft = leftPos + SLIDER_X;
        int sliderTop = topPos + SLIDER_Y;
        gui.fill(sliderLeft, sliderTop, sliderLeft + SLIDER_W, sliderTop + SLIDER_H, COLOR_TRACK);
        gui.fill(sliderLeft, sliderTop, sliderLeft + SLIDER_W, sliderTop + 1, COLOR_TRACK_BORDER);
        gui.fill(sliderLeft, sliderTop + SLIDER_H - 1, sliderLeft + SLIDER_W, sliderTop + SLIDER_H, COLOR_TRACK_BORDER);
        int rate = menu.getTransferRate();
        // 滑块刻度按 SLIDER_MAX_RATE（32000）；自定义输入可超过滑块范围 → 夹到 1.0 使滑块停在最右端
        float t = Mth.clamp((rate - 1) / (float) Math.max(1, SLIDER_MAX_RATE - 1), 0F, 1F);
        int knobX = sliderLeft + (int) (t * SLIDER_W) - 2;
        gui.fill(knobX - 2, sliderTop - 3, knobX + 4, sliderTop + SLIDER_H + 3, COLOR_KNOB);

        // ===== 预设档位（点击快速选择，当前值匹配时高亮） =====
        int presetLeft = leftPos + SLIDER_X;
        int presetTop = topPos + PRESET_Y;
        for (int i = 0; i < PRESET_VALUES.length; i++) {
            int px = presetLeft + i * (PRESET_W + PRESET_GAP);
            boolean selected = rate == PRESET_VALUES[i];
            boolean hovered = isMouseIn(mouseX, mouseY, px, presetTop, PRESET_W, PRESET_H);
            gui.fill(px, presetTop, px + PRESET_W, presetTop + PRESET_H,
                    selected ? 0xFF1E6B8A : (hovered ? 0xFF3A3A3A : 0xFF2B2B2B));
            gui.fill(px, presetTop, px + PRESET_W, presetTop + 1, COLOR_BTN_BORDER);
            gui.fill(px, presetTop + PRESET_H - 1, px + PRESET_W, presetTop + PRESET_H, COLOR_BTN_BORDER);
            gui.drawCenteredString(this.font, Component.literal(PRESET_LABELS[i]),
                    px + PRESET_W / 2, presetTop + (PRESET_H - 8) / 2, 0xFFFFFFFF);
        }

        // ===== 自定义数值输入框（EditBox 由组件系统绘制）+ FE/t 单位 =====
        gui.drawString(this.font, Component.literal("FE/t"),
                leftPos + RATE_BOX_X + RATE_BOX_W + 6, topPos + RATE_BOX_Y + 3, COLOR_TEXT_DIM);

        // ===== 模式按钮 =====
        int btnLeft = leftPos + BTN_X;
        int btnTop = topPos + BTN_Y;
        boolean hovered = isMouseIn(mouseX, mouseY, btnLeft, btnTop, BTN_W, BTN_H);
        gui.fill(btnLeft, btnTop, btnLeft + BTN_W, btnTop + BTN_H, hovered ? 0xFF3A3A3A : COLOR_BTN);
        gui.fill(btnLeft, btnTop, btnLeft + BTN_W, btnTop + 1, COLOR_BTN_BORDER);
        gui.fill(btnLeft, btnTop + BTN_H - 1, btnLeft + BTN_W, btnTop + BTN_H, COLOR_BTN_BORDER);
        gui.fill(btnLeft, btnTop, btnLeft + 1, btnTop + BTN_H, COLOR_BTN_BORDER);
        gui.fill(btnLeft + BTN_W - 1, btnTop, btnLeft + BTN_W, btnTop + BTN_H, COLOR_BTN_BORDER);
        Component modeText = menu.isUnlimitedMode()
                ? Component.translatable("gui.energy_tower.mode.unlimited")
                : Component.translatable("gui.energy_tower.mode.limited");
        gui.drawCenteredString(this.font, modeText, btnLeft + BTN_W / 2, btnTop + (BTN_H - 8) / 2, 0xFFFFFFFF);

        // ===== 绑定设备数 =====
        gui.drawString(this.font,
                Component.translatable("gui.energy_tower.bindings", menu.getBindingCount()),
                leftPos + BTN_X, topPos + BINDINGS_Y, COLOR_TEXT_DIM);

        // ===== 清除全部绑定按钮（二次确认） =====
        int clearLeft = leftPos + BTN_X;
        int clearTop = topPos + CLEAR_BTN_Y;
        boolean clearHovered = isMouseIn(mouseX, mouseY, clearLeft, clearTop, BTN_W, CLEAR_BTN_H);
        int clearColor = confirmingClear ? 0xFF5A1E1E : (clearHovered ? 0xFF3A2B2B : 0xFF2B2B2B);
        gui.fill(clearLeft, clearTop, clearLeft + BTN_W, clearTop + CLEAR_BTN_H, clearColor);
        gui.fill(clearLeft, clearTop, clearLeft + BTN_W, clearTop + 1, COLOR_BTN_BORDER);
        gui.fill(clearLeft, clearTop + CLEAR_BTN_H - 1, clearLeft + BTN_W, clearTop + CLEAR_BTN_H, COLOR_BTN_BORDER);
        gui.fill(clearLeft, clearTop, clearLeft + 1, clearTop + CLEAR_BTN_H, COLOR_BTN_BORDER);
        gui.fill(clearLeft + BTN_W - 1, clearTop, clearLeft + BTN_W, clearTop + CLEAR_BTN_H, COLOR_BTN_BORDER);
        Component clearText = confirmingClear
                ? Component.translatable("gui.energy_tower.clear.confirm")
                : Component.translatable("gui.energy_tower.clear");
        gui.drawCenteredString(this.font, clearText, clearLeft + BTN_W / 2, clearTop + (CLEAR_BTN_H - 8) / 2, 0xFFFFFFFF);
    }

    // ============================== 交互 ==============================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int clearLeft = leftPos + BTN_X;
        int clearTop = topPos + CLEAR_BTN_Y;
        // 点击清除按钮以外的区域 → 取消“确认清除”状态
        if (button == 0 && !isMouseIn(mouseX, mouseY, clearLeft, clearTop, BTN_W, CLEAR_BTN_H)) {
            confirmingClear = false;
        }
        // 输入框处于编辑态且点击了别处 → 应用输入的数值
        if (button == 0 && this.rateBox.isFocused()
                && !isMouseIn(mouseX, mouseY, leftPos + RATE_BOX_X, topPos + RATE_BOX_Y, RATE_BOX_W, RATE_BOX_H)) {
            applyRateFromBox();
        }
        // 预设档位：点击即设为对应速率
        int presetLeft = leftPos + SLIDER_X;
        int presetTop = topPos + PRESET_Y;
        for (int i = 0; i < PRESET_VALUES.length; i++) {
            int px = presetLeft + i * (PRESET_W + PRESET_GAP);
            if (button == 0 && isMouseIn(mouseX, mouseY, px, presetTop, PRESET_W, PRESET_H)) {
                setRate(PRESET_VALUES[i]);
                return true;
            }
        }
        // 滑块：按下开始拖动并立即更新
        int sliderLeft = leftPos + SLIDER_X;
        int sliderTop = topPos + SLIDER_Y;
        if (button == 0 && isMouseIn(mouseX, mouseY, sliderLeft - 4, sliderTop - 4, SLIDER_W + 8, SLIDER_H + 8)) {
            draggingSlider = true;
            updateSlider(mouseX);
            return true;
        }
        // 模式按钮：点击切换
        int btnLeft = leftPos + BTN_X;
        int btnTop = topPos + BTN_Y;
        if (button == 0 && isMouseIn(mouseX, mouseY, btnLeft, btnTop, BTN_W, BTN_H)) {
            boolean next = !menu.isUnlimitedMode();
            PacketDistributor.sendToServer(new C2SSetModePacket(menu.getTowerPos(), next));
            return true;
        }
        // 清除全部绑定按钮：第一次进入确认态，第二次真正清除（防止误点）
        if (button == 0 && isMouseIn(mouseX, mouseY, clearLeft, clearTop, BTN_W, CLEAR_BTN_H)) {
            if (confirmingClear) {
                confirmingClear = false;
                PacketDistributor.sendToServer(new C2SClearBindingsPacket(menu.getTowerPos()));
            } else {
                confirmingClear = true;
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingSlider && button == 0) {
            updateSlider(mouseX);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingSlider = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 输入框聚焦时按回车 → 应用自定义数值
        if (this.rateBox.isFocused()
                && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            applyRateFromBox();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /** 根据鼠标 X 计算速率并发送 C2S 包（仅当数值变化时发送，减少网络流量） */
    private void updateSlider(double mouseX) {
        int sliderLeft = leftPos + SLIDER_X;
        float t = Mth.clamp((float) ((mouseX - sliderLeft) / (double) SLIDER_W), 0F, 1F);
        int rate = Math.max(1, Math.round(t * (SLIDER_MAX_RATE - 1)) + 1);
        if (rate != lastSentRate) {
            lastSentRate = rate;
            lastKnownRate = rate;
            PacketDistributor.sendToServer(new C2SSetRatePacket(menu.getTowerPos(), rate));
        }
        this.rateBox.setValue(String.valueOf(rate));
    }

    /** 直接设置速率（预设 / 自定义共用），并同步输入框。上限为自定义上限 {@link #CUSTOM_MAX_RATE}（400000） */
    private void setRate(int rate) {
        int clamped = Mth.clamp(rate, 1, CUSTOM_MAX_RATE);
        if (clamped != lastSentRate) {
            lastSentRate = clamped;
            lastKnownRate = clamped;
            PacketDistributor.sendToServer(new C2SSetRatePacket(menu.getTowerPos(), clamped));
        }
        this.rateBox.setValue(String.valueOf(clamped));
        this.rateBox.setFocused(false);
    }

    /** 读取输入框数值并应用；非法输入回退为当前值 */
    private void applyRateFromBox() {
        int value;
        try {
            value = Integer.parseInt(this.rateBox.getValue().trim());
        } catch (NumberFormatException ex) {
            value = this.menu.getTransferRate();
        }
        setRate(value);
    }

    private boolean isMouseIn(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    /** 电量比例 → 颜色（红→黄→绿） */
    private int energyColor(float pct) {
        int r, g;
        if (pct < 0.5F) {
            r = 255;
            g = (int) (pct * 2F * 255F);
        } else {
            r = (int) ((1F - pct) * 2F * 255F);
            g = 255;
        }
        return 0xFF000000 | (r << 16) | (g << 8) | 60;
    }

    /** 大数字缩写显示：12345 → "12.3k" */
    private String fmt(int value) {
        if (value >= 1_000_000) {
            return String.format("%.1fM", value / 1_000_000F);
        }
        if (value >= 1_000) {
            return String.format("%.1fk", value / 1_000F);
        }
        return String.valueOf(value);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

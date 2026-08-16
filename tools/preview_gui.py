# -*- coding: utf-8 -*-
"""生成能量塔 GUI 的预览图，用于核对布局对齐（模拟 EnergyTowerScreen 的精确绘制坐标）。"""
import os

from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEX = os.path.join(ROOT, "src", "main", "resources", "assets", "energy_tower", "textures", "gui", "energy_tower.png")

img = Image.open(TEX).convert("RGBA").copy()
d = ImageDraw.Draw(img)

# ===== 与 EnergyTowerScreen 完全一致的常量 =====
ENERGY_BAR_X, ENERGY_BAR_Y, ENERGY_BAR_W, ENERGY_BAR_H = 8, 18, 16, 50
SLIDER_X, SLIDER_Y, SLIDER_W, SLIDER_H = 42, 56, 120, 8
PRESET_VALUES = [100, 1000, 5000, 10000, 32000]
PRESET_LABELS = ["100", "1k", "5k", "10k", "32k"]
PRESET_Y, PRESET_W, PRESET_H, PRESET_GAP = 72, 22, 10, 2
RATE_BOX_X, RATE_BOX_Y, RATE_BOX_W, RATE_BOX_H = 42, 88, 90, 14
BTN_X, BTN_Y, BTN_W, BTN_H = 42, 108, 120, 16
BINDINGS_Y = 128
CLEAR_BTN_Y, CLEAR_BTN_H = 140, 16

COLOR_TRACK = (0x1E, 0x1E, 0x1E)
COLOR_TRACK_BORDER = (0x6A, 0x6A, 0x6A)
COLOR_KNOB = (0x33, 0xE0, 0xFF)
COLOR_BTN = (0x2B, 0x2B, 0x2B)
COLOR_BTN_BORDER = (0x7A, 0x7A, 0x7A)
COLOR_TEXT = (0xE0, 0xE0, 0xE0)
COLOR_TEXT_DIM = (0x9A, 0x9A, 0x9A)

pct = 0.6  # 模拟 60% 电量
rate = 5000


def energy_color(p):
    if p < 0.5:
        r, g = 255, int(p * 2 * 255)
    else:
        r, g = int((1 - p) * 2 * 255), 255
    return (r, g, 60)


# 1) 储能条
x, y = ENERGY_BAR_X, ENERGY_BAR_Y
d.rectangle([x - 1, y - 1, x + ENERGY_BAR_W + 1, y + ENERGY_BAR_H + 1], fill=COLOR_TRACK_BORDER)
d.rectangle([x, y, x + ENERGY_BAR_W, y + ENERGY_BAR_H], fill=COLOR_TRACK)
fill_h = int(ENERGY_BAR_H * pct)
fill_top = y + ENERGY_BAR_H - fill_h
d.rectangle([x, fill_top, x + ENERGY_BAR_W, y + ENERGY_BAR_H], fill=energy_color(pct))
# 储能数值：居中放在能量条正下方
tw = d.textlength("5k")
d.text((x + ENERGY_BAR_W // 2 - tw // 2, y + ENERGY_BAR_H + 4), "5k", fill=COLOR_TEXT)

# 2) 标题与速率标签
d.text((8, 6), "能量塔控制面板", fill=(255, 255, 255))
d.text((SLIDER_X, SLIDER_Y - 12), "无线传输速率", fill=COLOR_TEXT_DIM)

# 3) 滑块
sl, st = SLIDER_X, SLIDER_Y
d.rectangle([sl, st, sl + SLIDER_W, st + SLIDER_H], fill=COLOR_TRACK)
d.rectangle([sl, st, sl + SLIDER_W, st + 1], fill=COLOR_TRACK_BORDER)
d.rectangle([sl, st + SLIDER_H - 1, sl + SLIDER_W, st + SLIDER_H], fill=COLOR_TRACK_BORDER)
t = (rate - 1) / 31999.0
knob_x = sl + int(t * SLIDER_W) - 2
d.rectangle([knob_x - 2, st - 3, knob_x + 4, st + SLIDER_H + 3], fill=COLOR_KNOB)

# 4) 预设档位（当前值 5000 高亮）
preset_top = PRESET_Y
for i, label in enumerate(PRESET_LABELS):
    px = SLIDER_X + i * (PRESET_W + PRESET_GAP)
    selected = rate == PRESET_VALUES[i]
    d.rectangle([px, preset_top, px + PRESET_W, preset_top + PRESET_H],
                fill=(0x1E, 0x6B, 0x8A) if selected else COLOR_BTN)
    d.rectangle([px, preset_top, px + PRESET_W, preset_top + 1], fill=COLOR_BTN_BORDER)
    d.rectangle([px, preset_top + PRESET_H - 1, px + PRESET_W, preset_top + PRESET_H], fill=COLOR_BTN_BORDER)
    lw = d.textlength(label)
    d.text((px + PRESET_W // 2 - lw // 2, preset_top + 1), label, fill=(255, 255, 255))

# 5) 自定义数值输入框 + FE/t
d.rectangle([RATE_BOX_X, RATE_BOX_Y, RATE_BOX_X + RATE_BOX_W, RATE_BOX_Y + RATE_BOX_H],
            fill=(0x0E, 0x0F, 0x13), outline=COLOR_BTN_BORDER)
d.text((RATE_BOX_X + 3, RATE_BOX_Y + 3), "5000", fill=COLOR_TEXT)
d.text((RATE_BOX_X + RATE_BOX_W + 6, RATE_BOX_Y + 3), "FE/t", fill=COLOR_TEXT_DIM)

# 6) 模式按钮
bl, bt = BTN_X, BTN_Y
d.rectangle([bl, bt, bl + BTN_W, bt + BTN_H], fill=COLOR_BTN)
d.rectangle([bl, bt, bl + BTN_W, bt + 1], fill=COLOR_BTN_BORDER)
d.rectangle([bl, bt + BTN_H - 1, bl + BTN_W, bt + BTN_H], fill=COLOR_BTN_BORDER)
d.rectangle([bl, bt, bl + 1, bt + BTN_H], fill=COLOR_BTN_BORDER)
d.rectangle([bl + BTN_W - 1, bt, bl + BTN_W, bt + BTN_H], fill=COLOR_BTN_BORDER)
mt = d.textlength("速率限制模式")
d.text((bl + BTN_W // 2 - mt // 2, bt + 4), "速率限制模式", fill=(255, 255, 255))

# 7) 绑定设备数
d.text((BTN_X, BINDINGS_Y), "绑定设备数：2", fill=COLOR_TEXT_DIM)

# 8) 清除全部绑定按钮
cl, ct = BTN_X, CLEAR_BTN_Y
d.rectangle([cl, ct, cl + BTN_W, ct + CLEAR_BTN_H], fill=(0x2B, 0x2B, 0x2B))
d.rectangle([cl, ct, cl + BTN_W, ct + 1], fill=COLOR_BTN_BORDER)
d.rectangle([cl, ct + CLEAR_BTN_H - 1, cl + BTN_W, ct + CLEAR_BTN_H], fill=COLOR_BTN_BORDER)
d.rectangle([cl, ct, cl + 1, ct + CLEAR_BTN_H], fill=COLOR_BTN_BORDER)
d.rectangle([cl + BTN_W - 1, ct, cl + BTN_W, ct + CLEAR_BTN_H], fill=COLOR_BTN_BORDER)
ctw = d.textlength("清除全部绑定")
d.text((cl + BTN_W // 2 - ctw // 2, ct + 4), "清除全部绑定", fill=(255, 255, 255))

# 放大 3 倍便于查看
out = img.resize((img.width * 3, img.height * 3), Image.NEAREST)
out.save(os.path.join(ROOT, "tools", "gui_preview.png"))
print("已生成预览:", os.path.join(ROOT, "tools", "gui_preview.png"))

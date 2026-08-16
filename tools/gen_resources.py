# -*- coding: utf-8 -*-
"""生成 EnergyTower 模组的全部贴图：
- 能量塔方块：顶部（金属圆盘 + 蓝色能量核心）、侧面（金属面板 + 能量纹路）、底部
- 能量链路扳手物品（16x16）
- 能量塔控制面板 GUI 纹理（176x166 深色科技面板）
"""
import os

from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))  # EnergyTower/
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "energy_tower")
TEX_BLOCK = os.path.join(ASSETS, "textures", "block")
TEX_ITEM = os.path.join(ASSETS, "textures", "item")
TEX_GUI = os.path.join(ASSETS, "textures", "gui")


def new_canvas(size=16):
    return Image.new("RGBA", (size, size), (0, 0, 0, 0))


def save(img, folder, name):
    os.makedirs(folder, exist_ok=True)
    img.save(os.path.join(folder, f"{name}.png"))


# =====================================================================
# 1) 能量塔方块贴图
# =====================================================================
def gen_tower():
    # ---- 顶面：深灰金属圆盘 + 中央蓝色能量核心 + 外圈铆钉 ----
    img = new_canvas()
    d = ImageDraw.Draw(img)
    # 基座底色
    d.rectangle([0, 0, 15, 15], fill=(64, 68, 76, 255))
    # 外圈金属环
    d.ellipse([1, 1, 14, 14], fill=(88, 94, 104, 255))
    d.ellipse([1, 1, 14, 14], outline=(120, 128, 140, 255))
    # 内圈凹槽
    d.ellipse([3, 3, 12, 12], fill=(52, 56, 64, 255))
    d.ellipse([3, 3, 12, 12], outline=(96, 102, 114, 255))
    # 中央能量核心（发光蓝）
    d.ellipse([5, 5, 10, 10], fill=(56, 130, 255, 255))
    d.ellipse([6, 6, 9, 9], fill=(120, 190, 255, 255))
    d.ellipse([7, 7, 8, 8], fill=(220, 240, 255, 255))
    # 四角铆钉
    for (x, y) in [(2, 2), (13, 2), (2, 13), (13, 13)]:
        d.ellipse([x - 1, y - 1, x + 1, y + 1], fill=(150, 158, 168, 255))
    save(img, TEX_BLOCK, "energy_tower_top")

    # ---- 侧面：金属面板 + 竖向蓝色能量灯条 ----
    img = new_canvas()
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 15, 15], fill=(62, 66, 74, 255))
    # 面板外框
    d.rectangle([0, 0, 15, 15], outline=(105, 112, 122, 255))
    d.rectangle([1, 1, 14, 14], outline=(80, 86, 94, 255))
    # 中央发光能量条（竖条）
    d.rectangle([6, 3, 9, 12], fill=(40, 110, 230, 255))
    d.rectangle([7, 3, 8, 12], fill=(120, 190, 255, 255))
    # 两侧装饰横线
    for y in (4, 8, 12):
        d.line([2, y, 5, y], fill=(100, 108, 118, 255))
        d.line([10, y, 13, y], fill=(100, 108, 118, 255))
    save(img, TEX_BLOCK, "energy_tower_side")

    # ---- 底面：普通深色金属 ----
    img = new_canvas()
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 15, 15], fill=(48, 51, 58, 255))
    d.rectangle([0, 0, 15, 15], outline=(70, 75, 84, 255))
    d.line([0, 8, 15, 8], fill=(58, 62, 70, 255))
    d.line([8, 0, 8, 15], fill=(58, 62, 70, 255))
    for (x, y) in [(3, 3), (12, 3), (3, 12), (12, 12)]:
        d.rectangle([x - 1, y - 1, x + 1, y + 1], fill=(70, 75, 84, 255))
    save(img, TEX_BLOCK, "energy_tower_bottom")


# =====================================================================
# 2) 能量链路扳手物品贴图
# =====================================================================
def gen_wrench():
    img = new_canvas()
    d = ImageDraw.Draw(img)
    gray = (150, 158, 168, 255)
    dark = (110, 116, 126, 255)
    light = (205, 212, 222, 255)
    blue = (70, 150, 255, 255)

    # 手柄（斜向粗杆，沿对角线）
    d.line([1, 15, 14, 2], fill=dark, width=3)
    d.line([1, 14, 14, 1], fill=gray, width=2)
    d.line([2, 14, 14, 2], fill=light, width=1)
    # 扳手头部（左下角 U 形开口）
    d.rectangle([1, 10, 6, 15], fill=gray)
    d.rectangle([4, 8, 7, 13], fill=gray)
    d.rectangle([1, 12, 4, 15], fill=gray)
    # 头部开口（U 形内缺口）
    d.rectangle([3, 9, 5, 11], fill=(0, 0, 0, 0))
    # 头部高光
    d.line([1, 10, 3, 10], fill=light)
    d.line([1, 11, 1, 14], fill=light)
    # 能量节点：手柄末端蓝色亮点
    d.ellipse([12, 3, 14, 5], fill=blue)
    d.ellipse([13, 4, 14, 5], fill=(180, 220, 255, 255))
    save(img, TEX_ITEM, "energy_link_wrench")


# =====================================================================
# 3) 能量塔控制面板 GUI 纹理（176x166）
# =====================================================================
def gen_gui():
    W, H = 176, 166
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # 主背景：深蓝灰渐变（模拟科技面板）
    top = (38, 42, 52, 255)
    bottom = (24, 26, 33, 255)
    for y in range(H):
        t = y / (H - 1)
        r = int(top[0] + (bottom[0] - top[0]) * t)
        g = int(top[1] + (bottom[1] - top[1]) * t)
        b = int(top[2] + (bottom[2] - top[2]) * t)
        d.line([0, y, W - 1, y], fill=(r, g, b, 255))

    # 外边框
    d.rectangle([0, 0, W - 1, H - 1], outline=(20, 21, 27, 255))
    d.rectangle([1, 1, W - 2, H - 2], outline=(70, 78, 92, 255))
    # 顶部标题条
    d.rectangle([4, 4, W - 5, 13], fill=(30, 34, 43, 255))
    d.line([4, 13, W - 5, 13], fill=(70, 78, 92, 255))
    # 左侧能量槽背景（与屏幕中 ENERGY_BAR 位置对应：x=8, y=18, 16x50）
    d.rectangle([7, 17, 25, 69], fill=(16, 17, 22, 255))
    d.rectangle([7, 17, 25, 69], outline=(70, 78, 92, 255))
    d.rectangle([8, 18, 24, 68], fill=(14, 15, 19, 255))
    # 右下角装饰：小的能量标
    d.ellipse([W - 20, H - 20, W - 8, H - 8], fill=(40, 110, 230, 120))
    d.ellipse([W - 18, H - 18, W - 10, H - 10], fill=(120, 190, 255, 140))
    save(img, TEX_GUI, "energy_tower")


if __name__ == "__main__":
    gen_tower()
    gen_wrench()
    gen_gui()
    print("贴图生成完成：")
    for root, _, files in os.walk(ASSETS):
        for f in files:
            if f.endswith(".png"):
                print(" -", os.path.relpath(os.path.join(root, f), ROOT))

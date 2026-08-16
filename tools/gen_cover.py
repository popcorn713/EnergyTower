# -*- coding: utf-8 -*-
"""生成 mc百科 封面横幅与模组图标（风格与游戏内贴图一致）：
- 深色科技面板背景 + 蓝图网格
- 金属能量塔（蓝色发光核心）+ 无线能量弧光 → 绑定的机器方块
- 能量链路扳手（装饰）
输出到发布文件夹 d:\\桌面\\deepseek\\mods\\：
  mc百科封面.png           1600x900（16:9 高清）
  mc百科封面_512x288.png   512x288（mc百科推荐尺寸）
  模组图标.png              512x512（方形，适合图标/头像）
"""
import os

from PIL import Image, ImageDraw, ImageFilter, ImageFont

OUT = r"d:\桌面\deepseek\mods"

# ===== 调色板（与 tools/gen_resources.py 完全一致）=====
METAL_DARK = (64, 68, 76)
METAL_MID = (88, 94, 104)
METAL_LIGHT = (120, 128, 140)
METAL_HI = (205, 212, 222)
METAL_BOTTOM = (48, 51, 58)
PANEL_TOP = (38, 42, 52)
PANEL_BOTTOM = (24, 26, 33)
CORE = (56, 130, 255)
CORE_MID = (120, 190, 255)
CORE_HI = (220, 240, 255)
ACCENT = (51, 224, 255)
GREEN = (80, 220, 140)


def font(size, bold=True):
    for p in ((r"C:\Windows\Fonts\msyhbd.ttc" if bold else r"C:\Windows\Fonts\msyh.ttc"),
              r"C:\Windows\Fonts\simhei.ttf", r"C:\Windows\Fonts\simkai.ttf"):
        if os.path.exists(p):
            try:
                return ImageFont.truetype(p, size)
            except Exception:
                continue
    return ImageFont.load_default()


def gradient(size, top, bottom):
    w, h = size
    img = Image.new("RGB", (w, h))
    d = ImageDraw.Draw(img)
    for y in range(h):
        t = y / max(1, h - 1)
        c = tuple(int(top[i] + (bottom[i] - top[i]) * t) for i in range(3))
        d.line([(0, y), (w, y)], fill=c)
    return img


def bezier(p0, p1, p2, n=48):
    pts = []
    for i in range(n + 1):
        t = i / n
        x = (1 - t) ** 2 * p0[0] + 2 * (1 - t) * t * p1[0] + t * t * p2[0]
        y = (1 - t) ** 2 * p0[1] + 2 * (1 - t) * t * p1[1] + t * t * p2[1]
        pts.append((x, y))
    return pts


def glow_arc(pts, color, width, alpha=120):
    """返回一条带辉光的弧线所需的点列表（配合主图层绘制）"""
    return pts


def draw_iso_cube(d, cx, cy, w, h, top, left, right, outline=None):
    """等距立方体：cx,cy=顶面中心；w=半宽；h=高度"""
    d.polygon([(cx, cy), (cx + w, cy - w // 2), (cx, cy - w), (cx - w, cy - w // 2)], fill=top)
    d.polygon([(cx, cy), (cx - w, cy - w // 2), (cx - w, cy - w // 2 + h), (cx, cy + h)], fill=left)
    d.polygon([(cx, cy), (cx + w, cy - w // 2), (cx + w, cy - w // 2 + h), (cx, cy + h)], fill=right)
    if outline:
        d.line([(cx - w, cy - w // 2), (cx, cy - w), (cx + w, cy - w // 2)], fill=outline)
        d.line([(cx - w, cy - w // 2), (cx - w, cy - w // 2 + h)], fill=outline)
        d.line([(cx + w, cy - w // 2), (cx + w, cy - w // 2 + h)], fill=outline)
        d.line([(cx, cy + h), (cx - w, cy - w // 2 + h)], fill=outline)
        d.line([(cx, cy + h), (cx + w, cy - w // 2 + h)], fill=outline)


def draw_tower(d, cx, cy, s):
    """能量塔（正面圆柱 + 顶部能量核心），s=缩放。cx,cy=塔中心"""
    bw, bh = int(120 * s), int(120 * s)          # 塔身半宽 / 半高
    body_h = int(330 * s)                          # 塔身高度
    top_y = cy - body_h // 2
    bot_y = cy + body_h // 2
    # 底座椭圆
    d.ellipse([cx - int(110 * s), bot_y - int(26 * s), cx + int(110 * s), bot_y + int(26 * s)],
              fill=METAL_DARK, outline=METAL_LIGHT)
    # 塔身（圆角柱）
    d.rounded_rectangle([cx - bw, top_y, cx + bw, bot_y], radius=int(30 * s), fill=METAL_MID,
                        outline=METAL_LIGHT, width=max(2, int(3 * s)))
    d.rounded_rectangle([cx - int(92 * s), top_y + int(10 * s), cx + int(92 * s), bot_y - int(8 * s)],
                        radius=int(22 * s), fill=METAL_DARK)
    # 侧面竖向能量灯条
    bar_x = [int(-66 * s), int(-30 * s), int(30 * s), int(66 * s)]
    for bx in bar_x:
        x0 = cx + bx - int(6 * s)
        d.rounded_rectangle([x0, top_y + int(28 * s), x0 + int(12 * s), bot_y - int(20 * s)],
                            radius=int(6 * s), fill=CORE, outline=CORE_MID)
        d.rounded_rectangle([x0 + int(3 * s), top_y + int(30 * s), x0 + int(9 * s), bot_y - int(22 * s)],
                            radius=int(3 * s), fill=CORE_HI)
    # 顶部圆盘
    d.ellipse([cx - int(120 * s), top_y - int(26 * s), cx + int(120 * s), top_y + int(26 * s)],
              fill=METAL_MID, outline=METAL_LIGHT, width=max(2, int(3 * s)))
    d.ellipse([cx - int(96 * s), top_y - int(20 * s), cx + int(96 * s), top_y + int(20 * s)],
              fill=METAL_DARK, outline=METAL_LIGHT)
    # 能量核心（发光蓝，多层同心圆）
    d.ellipse([cx - int(52 * s), top_y - int(12 * s), cx + int(52 * s), top_y + int(12 * s)], fill=CORE)
    d.ellipse([cx - int(34 * s), top_y - int(8 * s), cx + int(34 * s), top_y + int(8 * s)], fill=CORE_MID)
    d.ellipse([cx - int(14 * s), top_y - int(4 * s), cx + int(14 * s), top_y + int(4 * s)], fill=CORE_HI)
    # 顶部天线 + 火花节点
    d.rectangle([cx - int(3 * s), top_y - int(46 * s), cx + int(3 * s), top_y - int(24 * s)], fill=METAL_LIGHT)
    d.ellipse([cx - int(8 * s), top_y - int(58 * s), cx + int(8 * s), top_y - int(44 * s)], fill=ACCENT)
    d.ellipse([cx - int(4 * s), top_y - int(54 * s), cx + int(4 * s), top_y - int(48 * s)], fill=(230, 250, 255, 255))
    return (cx, top_y - int(51 * s))  # 火花源点


def draw_wrench(img, x, y, s, angle=0):
    """能量链路扳手（斜向），画在独立图层旋转后合成到 img，返回新 img"""
    w, h = int(90 * s), int(30 * s)
    layer = Image.new("RGBA", (int(w * 2), int(h * 2)), (0, 0, 0, 0))
    dl = ImageDraw.Draw(layer)
    gray, dark, light, blue = METAL_LIGHT, METAL_DARK, METAL_HI, CORE
    # 手柄
    dl.line([int(w * 0.15), int(h * 1.6), int(w * 1.6), int(h * 0.4)], fill=dark, width=max(4, int(8 * s)))
    dl.line([int(w * 0.15), int(h * 1.55), int(w * 1.6), int(h * 0.35)], fill=gray, width=max(3, int(6 * s)))
    dl.line([int(w * 0.2), int(h * 1.5), int(w * 1.5), int(h * 0.45)], fill=light, width=max(1, int(2 * s)))
    # U 形开口头
    dl.rectangle([int(w * 0.1), int(h * 1.1), int(w * 0.5), int(h * 1.7)], fill=gray)
    dl.rectangle([int(w * 0.32), int(h * 0.9), int(w * 0.58), int(h * 1.5)], fill=gray)
    # 能量节点
    dl.ellipse([int(w * 1.35), int(h * 0.28), int(w * 1.55), int(h * 0.5)], fill=blue)
    layer = layer.rotate(angle, resample=Image.BICUBIC, expand=True)
    overlay = Image.new("RGBA", img.size, (0, 0, 0, 0))
    overlay.paste(layer, (int(x - layer.width / 2), int(y - layer.height / 2)), layer)
    img = Image.alpha_composite(img, overlay)
    return img


def build_cover(W, H):
    # 背景渐变 + 蓝图网格
    img = gradient((W, H), PANEL_TOP, PANEL_BOTTOM).convert("RGBA")
    d = ImageDraw.Draw(img, "RGBA")
    grid = 60
    for gx in range(0, W, grid):
        d.line([(gx, 0), (gx, H)], fill=(90, 100, 120, 26))
    for gy in range(0, H, grid):
        d.line([(0, gy), (W, gy)], fill=(90, 100, 120, 26))
    # 暗角（两侧压暗，突出主体）
    shade = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ds = ImageDraw.Draw(shade)
    ds.rectangle([0, 0, W, H], fill=(0, 0, 0, 0))
    vign = Image.new("L", (W, H), 0)
    dv = ImageDraw.Draw(vign)
    dv.rectangle([0, 0, W, H], fill=90)
    vign = vign.filter(ImageFilter.GaussianBlur(max(W, H) // 3))
    dark = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    dark.putalpha(vign.point(lambda a: int(a * 0.7)))
    img = Image.alpha_composite(img, dark)

    d = ImageDraw.Draw(img, "RGBA")

    # ===== 主体：能量塔（左中）=====
    tcx, tcy = int(W * 0.34), int(H * 0.56)
    s = H / 900.0 * 1.05
    spark_src = draw_tower(d, tcx, tcy, s)

    # ===== 塔核心辉光 =====
    glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    dg = ImageDraw.Draw(glow)
    dg.ellipse([tcx - int(130 * s), tcy - int(90 * s), tcx + int(130 * s), tcy + int(90 * s)], fill=(56, 130, 255, 60))
    glow = glow.filter(ImageFilter.GaussianBlur(int(40 * s)))
    img = Image.alpha_composite(img, glow)
    d = ImageDraw.Draw(img, "RGBA")

    # ===== 无线能量弧光（塔 → 机器）=====
    beam_layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    db = ImageDraw.Draw(beam_layer)
    machines = [
        (int(W * 0.72), int(H * 0.34), int(70 * s), int(90 * s), (96, 108, 122), (70, 80, 94), (58, 66, 78)),
        (int(W * 0.82), int(H * 0.66), int(60 * s), int(80 * s), (70, 100, 120), (52, 74, 90), (44, 60, 74)),
    ]
    for (mx, my, mw, mh, tp, lf, rt) in machines:
        pts = bezier(spark_src, (tcx + (mx - tcx) * 0.35, spark_src[1] + (my - spark_src[1]) * 0.5), (mx, my - int(0.35 * mw)))
        for i in range(len(pts) - 1):
            db.line([pts[i], pts[i + 1]], fill=(120, 190, 255, 90), width=max(2, int(7 * s)))
        for i in range(len(pts) - 1):
            db.line([pts[i], pts[i + 1]], fill=(230, 248, 255, 200), width=1)
        # 弧光上的能量粒子
        import random
        random.seed(7)
        for _ in range(14):
            t = random.random()
            px = pts[int(t * (len(pts) - 1))][0]
            py = pts[int(t * (len(pts) - 1))][1]
            r = random.randint(2, 4)
            db.ellipse([px - r, py - r, px + r, py + r], fill=(180, 225, 255, 220))
    beam_layer = beam_layer.filter(ImageFilter.GaussianBlur(1))
    img = Image.alpha_composite(img, beam_layer)
    d = ImageDraw.Draw(img, "RGBA")

    # ===== 机器方块（等距立方体）=====
    for (mx, my, mw, mh, tp, lf, rt) in machines:
        draw_iso_cube(d, mx, my, mw, mh, tp, lf, rt, outline=METAL_LIGHT)
        # 机器上的蓝色能量条装饰
        d.rounded_rectangle([mx - int(0.2 * mw), my - int(0.5 * mw), mx + int(0.2 * mw), my - int(0.1 * mw)],
                            radius=3, fill=CORE_MID)

    # ===== 扳手（右下装饰）=====
    img = draw_wrench(img, int(W * 0.90), int(H * 0.88), s * 1.1, angle=-20)
    d = ImageDraw.Draw(img, "RGBA")

    # ===== 标题文字（左上）=====
    title = "能量塔科技"
    sub = "Energy Tower"
    tag = "无线供电  ·  无距离限制  ·  标准 FE 兼容"
    f_title = font(int(150 * s) if W > 900 else int(120 * s))
    f_sub = font(int(78 * s), bold=False)
    f_tag = font(int(52 * s), bold=False)
    tx, ty = int(W * 0.09), int(H * 0.12)
    # 标题辉光阴影
    sh = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    dsh = ImageDraw.Draw(sh)
    dsh.text((tx, ty), title, font=f_title, fill=(0, 0, 0, 255))
    sh = sh.filter(ImageFilter.GaussianBlur(18))
    img = Image.alpha_composite(img, sh)
    d = ImageDraw.Draw(img, "RGBA")
    # 主标题（白色微青）
    d.text((tx, ty), title, font=f_title, fill=(236, 246, 255, 255))
    # 副标题（青色）
    d.text((tx + 6, ty + int(175 * s)), sub, font=f_sub, fill=(ACCENT[0], ACCENT[1], ACCENT[2], 255))
    # 标语（灰色）
    d.text((tx + 6, ty + int(268 * s)), tag, font=f_tag, fill=(176, 188, 204, 255))
    # 标题下装饰线
    d.rectangle([tx, ty + int(348 * s), tx + int(430 * s), ty + int(352 * s)], fill=(56, 130, 255, 200))
    d.rectangle([tx, ty + int(352 * s), tx + int(240 * s), ty + int(354 * s)], fill=ACCENT)
    return img


def build_icon(S=512):
    img = gradient((S, S), PANEL_TOP, PANEL_BOTTOM).convert("RGBA")
    d = ImageDraw.Draw(img, "RGBA")
    grid = 64
    for gx in range(0, S, grid):
        d.line([(gx, 0), (gx, S)], fill=(90, 100, 120, 24))
    for gy in range(0, S, grid):
        d.line([(0, gy), (S, gy)], fill=(90, 100, 120, 24))
    # 圆形底衬
    d.ellipse([30, 30, S - 30, S - 30], fill=(30, 34, 42, 255), outline=(80, 90, 106, 255), width=6)
    s = S / 900.0 * 0.72
    tcx, tcy = S // 2, int(S * 0.52)
    spark_src = draw_tower(d, tcx, tcy, s)
    # 核心辉光
    glow = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    dg = ImageDraw.Draw(glow)
    dg.ellipse([tcx - int(130 * s), tcy - int(90 * s), tcx + int(130 * s), tcy + int(90 * s)], fill=(56, 130, 255, 70))
    glow = glow.filter(ImageFilter.GaussianBlur(int(36 * s)))
    img = Image.alpha_composite(img, glow)
    return img.convert("RGB")


def main():
    os.makedirs(OUT, exist_ok=True)
    cover = build_cover(1600, 900)
    cover.save(os.path.join(OUT, "mc百科封面.png"))
    cover.resize((512, 288), Image.LANCZOS).save(os.path.join(OUT, "mc百科封面_512x288.png"))
    build_icon(512).save(os.path.join(OUT, "模组图标.png"))
    print("已生成:")
    print(" ", os.path.join(OUT, "mc百科封面.png"))
    print(" ", os.path.join(OUT, "mc百科封面_512x288.png"))
    print(" ", os.path.join(OUT, "模组图标.png"))


if __name__ == "__main__":
    main()

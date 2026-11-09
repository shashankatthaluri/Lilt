import math
from PIL import Image, ImageDraw, ImageFilter

def create_flow_icon(size=512):
    scale = 2
    W = size * scale
    H = size * scale

    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))

    tile_margin = int(36 * scale)
    tile_radius = int(108 * scale)
    tile_box = [tile_margin, tile_margin, W - tile_margin, H - tile_margin]

    # App Tile Background
    bg_layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    bg_draw = ImageDraw.Draw(bg_layer)
    bg_draw.rounded_rectangle(tile_box, radius=tile_radius, fill=(8, 10, 18, 255), outline=(255, 255, 255, 32), width=int(2.5 * scale))

    # Ambient Background Halo
    glow_layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    glow_draw = ImageDraw.Draw(glow_layer)
    cx, cy = W // 2, H // 2
    glow_draw.ellipse([cx - int(210 * scale), cy - int(120 * scale), cx + int(210 * scale), cy + int(120 * scale)], fill=(139, 92, 246, 95))
    glow_draw.ellipse([cx - int(120 * scale), cy - int(80 * scale), cx + int(120 * scale), cy + int(80 * scale)], fill=(236, 72, 153, 75))
    glow_layer = glow_layer.filter(ImageFilter.GaussianBlur(int(50 * scale)))

    img = Image.alpha_composite(img, bg_layer)
    img = Image.alpha_composite(img, glow_layer)

    # Pill Container (More compact, heroic button size)
    pill_w = int(390 * scale)
    pill_h = int(140 * scale)
    pill_x0 = (W - pill_w) // 2
    pill_y0 = (H - pill_h) // 2
    pill_box = [pill_x0, pill_y0, pill_x0 + pill_w, pill_y0 + pill_h]
    pill_radius = pill_h // 2

    # Draw pill interior
    pill_interior = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    pi_draw = ImageDraw.Draw(pill_interior)
    pi_draw.rounded_rectangle(pill_box, radius=pill_radius, fill=(11, 14, 24, 240))

    # Pill Gradient Border (Warm Amber -> Magenta -> Violet -> Cyan)
    border_layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    b_draw = ImageDraw.Draw(border_layer)
    
    # Draw segmented gradient outline around pill
    border_w = int(5 * scale)
    b_draw.rounded_rectangle(pill_box, radius=pill_radius, outline=(168, 85, 247, 240), width=border_w)
    
    # Soft glowing border halo
    border_glow = border_layer.filter(ImageFilter.GaussianBlur(int(10 * scale)))
    border_glow_outer = border_layer.filter(ImageFilter.GaussianBlur(int(22 * scale)))

    img = Image.alpha_composite(img, pill_interior)
    img = Image.alpha_composite(img, border_glow_outer)
    img = Image.alpha_composite(img, border_glow)
    img = Image.alpha_composite(img, border_layer)

    # Multi-Harmonic Wave Ribbon
    wave_w = int(330 * scale)
    wave_x0 = (W - wave_w) // 2
    mid_y = H // 2

    wave_layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    wave_draw = ImageDraw.Draw(wave_layer)

    STRANDS = 11

    warm_colors = [
        (255, 69, 0),    # Crimson orange
        (255, 115, 0),   # Bright amber
        (255, 175, 20),  # Luminous gold
        (255, 45, 85),   # Hot rose
    ]

    cool_colors = [
        (56, 189, 248),  # Electric cyan
        (99, 102, 241),  # Indigo
        (168, 85, 247),  # Electric violet
        (236, 72, 153),  # Hot magenta
    ]

    def interpolate_color(colors, factor):
        n = len(colors) - 1
        pos = factor * n
        idx = min(int(pos), n - 1)
        t = pos - idx
        c1, c2 = colors[idx], colors[idx + 1]
        r = int(c1[0] + (c2[0] - c1[0]) * t)
        g = int(c1[1] + (c2[1] - c1[1]) * t)
        b = int(c1[2] + (c2[2] - c1[2]) * t)
        return r, g, b

    t_icon = 1.35
    amp_icon = 38.0 * scale

    for s in range(STRANDS):
        norm_idx = (s - (STRANDS - 1) / 2) / ((STRANDS - 1) / 2)
        phase_offset = s * 0.28
        is_warm = (s % 2 == 1)
        is_core = (s == STRANDS // 2)

        line_color_palette = warm_colors if is_warm else cool_colors

        points = []
        for xi in range(0, wave_w + 1, 2):
            p = xi / wave_w
            x = wave_x0 + xi

            # Envelope tapering
            env = math.pow(math.sin(math.pi * p), 1.85)

            h1 = math.sin(p * math.pi * 4.2 - t_icon * 1.4 + phase_offset)
            h2 = math.sin(p * math.pi * 7.6 + t_icon * 1.9 - phase_offset * 1.3)
            h3 = math.cos(p * math.pi * 11.2 - t_icon * 2.6 + norm_idx * 1.8)

            strand_spread = norm_idx * 13.0 * scale
            y = mid_y + env * ((h1 * 0.64 + h2 * 0.26 + h3 * 0.10) * amp_icon + strand_spread)
            points.append((x, y))

        line_width = int((3.2 if is_core else 2.2) * scale)

        for i in range(len(points) - 1):
            p1 = points[i]
            p2 = points[i + 1]
            seg_p = (p1[0] - wave_x0) / wave_w
            r, g, b = interpolate_color(line_color_palette, seg_p)

            alpha = int(255 if is_core else 225)
            if is_core:
                r = min(255, int(r * 0.45 + 255 * 0.55))
                g = min(255, int(g * 0.45 + 245 * 0.55))
                b = min(255, int(b * 0.45 + 255 * 0.55))

            wave_draw.line([p1, p2], fill=(r, g, b, alpha), width=line_width)

    wave_bloom = wave_layer.filter(ImageFilter.GaussianBlur(int(6 * scale)))
    wave_bloom_wide = wave_layer.filter(ImageFilter.GaussianBlur(int(15 * scale)))

    img = Image.alpha_composite(img, wave_bloom_wide)
    img = Image.alpha_composite(img, wave_bloom)
    img = Image.alpha_composite(img, wave_layer)

    final_icon = img.resize((size, size), Image.Resampling.LANCZOS)
    return final_icon

if __name__ == "__main__":
    icon512 = create_flow_icon(512)
    icon256 = icon512.resize((256, 256), Image.Resampling.LANCZOS)

    icon512.save("c:/Users/Shiva/applications/vocawin/src-tauri/icons/icon.png", "PNG")
    icon256.save("c:/Users/Shiva/applications/vocawin/src-tauri/icons/installer/setup-icon-256.png", "PNG")
    icon512.save("c:/Users/Shiva/applications/vocawin/web/assets/brand/voca-logo-512.png", "PNG")

    icon512.save(
        "c:/Users/Shiva/applications/vocawin/src-tauri/icons/icon.ico",
        format="ICO",
        sizes=[(256, 256), (128, 128), (64, 64), (48, 48), (32, 32), (16, 16)]
    )
    icon256.save(
        "c:/Users/Shiva/applications/vocawin/src-tauri/icons/installer/setup-icon.ico",
        format="ICO",
        sizes=[(256, 256), (128, 128), (64, 64), (48, 48), (32, 32), (16, 16)]
    )

    icon512.save("C:/Users/Shiva/.gemini/antigravity/brain/b7de546f-f271-4cac-a9d5-d7d75000fe70/flow_icon_512.png", "PNG")
    print("Enhanced Flow icons generated successfully!")

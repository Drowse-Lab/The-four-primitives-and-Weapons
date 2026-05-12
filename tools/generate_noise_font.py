#!/usr/bin/env python3
"""
Minecraft bitmap noise font generator.
Generates a 256x96 PNG sprite sheet with ASCII printable characters
rendered with static noise / glitch effects.

Output: src/main/resources/assets/the_four_primitives_and_weapons/textures/font/static_noise.png
"""

import random
import os
from PIL import Image, ImageDraw, ImageFont

# ---- settings -------------------------------------------------------
CELL_W     = 16    # pixels per character cell (width)
CELL_H     = 16    # pixels per character cell (height)
COLS       = 16    # columns in the sprite sheet
FONT_SIZE  = 12    # rendered font size in pixels

# Noise parameters
NOISE_DENSITY   = 0.04   # fraction of cell pixels that become noise dots
GLITCH_CHANCE   = 0.08   # probability a scanline gets a horizontal glitch shift
GLITCH_MAX_PX   = 2      # max pixels to shift on a glitch line
SCANLINE_DARK   = 0.20   # fraction to darken every-other scanline (CRT effect)
EDGE_BLEED      = True   # let noise bleed slightly outside character bounds

# Characters to include (printable ASCII U+0020 to U+007E = 95 chars)
CHARS = [chr(c) for c in range(0x20, 0x7F)]  # space .. ~

# Pad to a multiple of COLS
while len(CHARS) % COLS != 0:
    CHARS.append('\x00')   # null = empty cell

ROWS = len(CHARS) // COLS
IMG_W = COLS * CELL_W
IMG_H = ROWS * CELL_H

# The chars array for the Minecraft font JSON (rows of 16 chars each)
CHARS_JSON_ROWS = [
    ''.join(CHARS[r * COLS:(r + 1) * COLS])
    for r in range(ROWS)
]

# ---- find a system monospace font -----------------------------------
def find_font(size):
    candidates = [
        "/usr/share/fonts/truetype/dejavu/DejaVuSansMono-Bold.ttf",
        "/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf",
        "/usr/share/fonts/truetype/liberation/LiberationMono-Regular.ttf",
        "/usr/share/fonts/truetype/ubuntu/UbuntuMono-R.ttf",
        "/usr/share/fonts/truetype/freefont/FreeMono.ttf",
        "/System/Library/Fonts/Menlo.ttc",
        "C:/Windows/Fonts/cour.ttf",
    ]
    for path in candidates:
        if os.path.exists(path):
            return ImageFont.truetype(path, size)
    return ImageFont.load_default()

# ---- helpers --------------------------------------------------------
rng = random.Random(42)   # fixed seed for reproducibility

def add_noise(img_array, cx, cy, density):
    """Scatter salt noise in/around a character cell."""
    bleed = 2 if EDGE_BLEED else 0
    x0 = max(0, cx - bleed)
    y0 = max(0, cy - bleed)
    x1 = min(IMG_W, cx + CELL_W + bleed)
    y1 = min(IMG_H, cy + CELL_H + bleed)
    area = (x1 - x0) * (y1 - y0)
    n_dots = int(area * density)
    for _ in range(n_dots):
        px = rng.randint(x0, x1 - 1)
        py = rng.randint(y0, y1 - 1)
        # randomize brightness for organic look
        bright = rng.randint(140, 255)
        alpha  = rng.randint(100, 200)
        img_array[py][px] = (bright, bright, bright, alpha)

def apply_glitch(pixels, cy):
    """Randomly shift some scanlines within a character row left/right."""
    for row in range(cy, cy + CELL_H):
        if rng.random() < GLITCH_CHANCE:
            shift = rng.randint(-GLITCH_MAX_PX, GLITCH_MAX_PX)
            if shift == 0:
                continue
            line = [pixels[row][x] for x in range(IMG_W)]
            if shift > 0:
                line = [(0, 0, 0, 0)] * shift + line[:-shift]
            else:
                line = line[-shift:] + [(0, 0, 0, 0)] * (-shift)
            for x in range(IMG_W):
                pixels[row][x] = line[x]

def apply_scanlines(pixels, cy):
    """Darken every other scanline inside a character row (CRT effect)."""
    for row in range(cy, cy + CELL_H):
        if (row % 2) == 1:
            for x in range(IMG_W):
                r, g, b, a = pixels[row][x]
                if a > 0:
                    factor = 1.0 - SCANLINE_DARK
                    pixels[row][x] = (int(r * factor), int(g * factor), int(b * factor), a)

# ---- main -----------------------------------------------------------
def main():
    out_path = os.path.join(
        os.path.dirname(__file__),
        "../src/main/resources/assets/the_four_primitives_and_weapons/textures/font/static_noise.png"
    )
    out_path = os.path.normpath(out_path)

    font = find_font(FONT_SIZE)

    # Work on a 2D list of RGBA tuples for easy per-pixel manipulation
    img = Image.new("RGBA", (IMG_W, IMG_H), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # Step 1: render all characters cleanly (white text)
    for idx, ch in enumerate(CHARS):
        if ch == '\x00' or ch == ' ':
            continue
        col = idx % COLS
        row = idx // COLS
        cx = col * CELL_W
        cy = row * CELL_H
        # center the glyph in the cell
        bbox = font.getbbox(ch)
        gw = bbox[2] - bbox[0]
        gh = bbox[3] - bbox[1]
        ox = cx + (CELL_W - gw) // 2 - bbox[0]
        oy = cy + (CELL_H - gh) // 2 - bbox[1]
        draw.text((ox, oy), ch, font=font, fill=(255, 255, 255, 255))

    # Convert to 2D pixel array for manipulation
    pixels = [[list(img.getpixel((x, y))) for x in range(IMG_W)] for y in range(IMG_H)]

    # Step 2: apply per-cell effects
    for idx, ch in enumerate(CHARS):
        col = idx % COLS
        row = idx // COLS
        cx = col * CELL_W
        cy = row * CELL_H
        add_noise(pixels, cx, cy, NOISE_DENSITY)

    # Step 3: glitch pass (whole image scanlines)
    for row in range(ROWS):
        apply_glitch(pixels, row * CELL_H)

    # Step 4: CRT scanline darkening
    for row in range(ROWS):
        apply_scanlines(pixels, row * CELL_H)

    # Write back
    for y in range(IMG_H):
        for x in range(IMG_W):
            img.putpixel((x, y), tuple(pixels[y][x]))

    img.save(out_path)
    print(f"Saved: {out_path}  ({IMG_W}x{IMG_H})")
    print()
    print("=== Font JSON chars field ===")
    for line in CHARS_JSON_ROWS:
        print(repr(line))

if __name__ == "__main__":
    main()

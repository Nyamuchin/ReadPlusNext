from PIL import Image, ImageDraw
import numpy as np
import os

SRC = r'F:\Android\ReadPlusNext\.tmp\icon-src'
RES = r'F:\Android\ReadPlusNext\app\src\main\res'
BLUE = (19, 117, 193)

playstore = Image.open(os.path.join(SRC, 'playstore.png')).convert('RGB')
square = np.asarray(playstore).astype(np.float32)
blue_luma = 0.299 * BLUE[0] + 0.587 * BLUE[1] + 0.114 * BLUE[2]
luma = 0.299 * square[:, :, 0] + 0.587 * square[:, :, 1] + 0.114 * square[:, :, 2]
alpha = np.clip((luma - blue_luma) / (255.0 - blue_luma), 0.0, 1.0)

# Tight glyph bounds (white book).
mask = alpha > 0.06
ys, xs = np.where(mask)
x0, x1, y0, y1 = xs.min(), xs.max() + 1, ys.min(), ys.max() + 1
glyph_alpha = (alpha[y0:y1, x0:x1] * 255.0).round().astype(np.uint8)
glyph_h, glyph_w = glyph_alpha.shape
print('glyph', glyph_w, glyph_h)

# White glyph RGBA image.
glyph = Image.new('RGBA', (glyph_w, glyph_h), (255, 255, 255, 0))
glyph.putalpha(Image.fromarray(glyph_alpha, 'L'))


def glyph_on_canvas(canvas_px: int, glyph_height_ratio: float) -> Image.Image:
    """Place the glyph centered on a square canvas; height = ratio * canvas."""
    target_h = max(1, round(canvas_px * glyph_height_ratio))
    target_w = max(1, round(target_h * glyph_w / glyph_h))
    resized = glyph.resize((target_w, target_h), Image.LANCZOS)
    canvas = Image.new('RGBA', (canvas_px, canvas_px), (255, 255, 255, 0))
    canvas.alpha_composite(resized, ((canvas_px - target_w) // 2, (canvas_px - target_h) // 2))
    return canvas


DENSITIES = {'mdpi': 1.0, 'hdpi': 1.5, 'xhdpi': 2.0, 'xxhdpi': 3.0, 'xxxhdpi': 4.0}
# Adaptive icon canvas is 108dp; keep the book comfortably inside the 72dp mask.
GLYPH_RATIO = 56.0 / 108.0

for name, scale in DENSITIES.items():
    folder = os.path.join(RES, f'mipmap-{name}')
    os.makedirs(folder, exist_ok=True)

    # Adaptive foreground + monochrome on a 108dp transparent canvas.
    canvas_px = int(round(108 * scale))
    fg = glyph_on_canvas(canvas_px, GLYPH_RATIO)
    fg.save(os.path.join(folder, 'ic_launcher_foreground.png'))
    fg.save(os.path.join(folder, 'ic_launcher_monochrome.png'))

    # Legacy square icon: reuse the provided rounded-square artwork.
    legacy_px = int(round(48 * scale))
    rounded = Image.open(os.path.join(SRC, 'icon.png')).convert('RGBA').resize(
        (legacy_px, legacy_px), Image.LANCZOS
    )
    rounded.save(os.path.join(folder, 'ic_launcher.png'))

    # Legacy round icon: full-bleed blue circle with the white book.
    circle = Image.new('RGBA', (legacy_px, legacy_px), (0, 0, 0, 0))
    draw = ImageDraw.Draw(circle)
    draw.ellipse((0, 0, legacy_px - 1, legacy_px - 1), fill=BLUE + (255,))
    round_glyph_h = round(legacy_px * 0.52)
    round_glyph_w = round(round_glyph_h * glyph_w / glyph_h)
    round_glyph = glyph.resize((round_glyph_w, round_glyph_h), Image.LANCZOS)
    circle.alpha_composite(round_glyph, ((legacy_px - round_glyph_w) // 2, (legacy_px - round_glyph_h) // 2))
    circle.save(os.path.join(folder, 'ic_launcher_round.png'))

# Play Store listing icon (512x512, no alpha).
docs = r'F:\Android\ReadPlusNext\docs'
os.makedirs(docs, exist_ok=True)
playstore.save(os.path.join(docs, 'ic_launcher-playstore.png'))
Image.open(os.path.join(SRC, 'icon.png')).convert('RGBA').save(os.path.join(docs, 'icon.png'))

# Preview: adaptive icon under a circular mask (108dp canvas -> 512px preview).
preview = Image.new('RGBA', (512, 512), (0, 0, 0, 0))
d = ImageDraw.Draw(preview)
d.ellipse((0, 0, 511, 511), fill=BLUE + (255,))
big = glyph_on_canvas(512, GLYPH_RATIO)
preview.alpha_composite(big)
preview.save(os.path.join(SRC, 'preview-adaptive-circle.png'))
print('done')

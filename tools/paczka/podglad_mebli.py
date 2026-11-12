#!/usr/bin/env python3
"""Podgląd nowych mebli z tools/paczka/meble.py: izometria każdego modelu (przód od strony widza).

    python3 tools/paczka/podglad_mebli.py meble-podglad.png
"""
import os
import sys

from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, os.path.dirname(__file__))
import meble  # noqa: E402

S = 5.5  # pikseli na 1/16 kratki


def avg(name):
    img = meble.TEXTURES[name].convert('RGB')
    px = [img.getpixel((x, y)) for y in range(16) for x in range(16)]
    return tuple(sum(c[i] for c in px) // len(px) for i in range(3))


def shade(c, k):
    return tuple(int(v * k) for v in c)


def render(elements, size=320):
    img = Image.new('RGB', (size, size), (242, 240, 234))
    d = ImageDraw.Draw(img)
    ox, oy = size // 2, size - 40

    def p(x, y, z):  # widok od południowego wschodu: +x w prawo-dół... przód (+z) do widza
        return ox + (x - z) * S * 0.87, oy + (x + z) * S * 0.5 - y * S - 8 * S

    els = sorted(elements, key=lambda e: (e['from'][0] + e['to'][0]) + (e['from'][2] + e['to'][2]) + (e['from'][1] + e['to'][1]) * 0.5)
    for e in els:
        (x0, y0, z0), (x1, y1, z1) = e['from'], e['to']
        x0 -= 8; x1 -= 8; z0 -= 8; z1 -= 8
        f = e['faces']
        top = avg(f['up']['texture'][1:])
        south = avg(f['south']['texture'][1:])
        east = avg(f['east']['texture'][1:])
        d.polygon([p(x0, y1, z0), p(x1, y1, z0), p(x1, y1, z1), p(x0, y1, z1)], fill=top, outline=shade(top, .7))
        d.polygon([p(x0, y0, z1), p(x1, y0, z1), p(x1, y1, z1), p(x0, y1, z1)], fill=shade(south, .85), outline=shade(south, .6))
        d.polygon([p(x1, y0, z0), p(x1, y0, z1), p(x1, y1, z1), p(x1, y1, z0)], fill=shade(east, .7), outline=shade(east, .5))
    return img


def main(out):
    names = list(meble.MODELS)
    cols = 4
    cell = 320
    sheet = Image.new('RGB', (cols * cell, ((len(names) + cols - 1) // cols) * (cell + 24)), (242, 240, 234))
    d = ImageDraw.Draw(sheet)
    try:
        font = ImageFont.truetype('DejaVuSans.ttf', 18)
    except OSError:
        font = ImageFont.load_default()
    for i, n in enumerate(names):
        x, y = (i % cols) * cell, (i // cols) * (cell + 24)
        sheet.paste(render(meble.MODELS[n]()), (x, y))
        d.text((x + 10, y + cell), 'axon:' + n, fill=(30, 30, 30), font=font)
    sheet.save(out)
    print('zapisano', out)


if __name__ == '__main__':
    main(sys.argv[1] if len(sys.argv) > 1 else 'meble-podglad.png')

#!/usr/bin/env python3
"""Generuje dodatek do paczki zasobów z nowymi meblami AXON (modele, tekstury, definicje przedmiotów).

    python3 tools/paczka/meble.py paczka/axon-meble

Wynik to gotowa paczka (pack.mcmeta + assets/axon/...), którą wkleja się do paczki serwera
(QualityArmory-texturepack) albo wgrywa obok niej. Każdy mebel to przedmiot axon:<nazwa>, który plugin stawia
na bloku-nośniku z meble.bloki w config.yml. Przód modelu jest od strony +z (południe, z = 16), tył przy z = 0,
tak jak w pozostałych meblach paczki; plugin obraca model w stronę facing bloku. Model 1 × 1 kratka mieści się
w 0..16, szafa i automat do gier sięgają do y = 32 (nad nimi w strukturze stoi bariera).
Wymaga Pythona 3 i Pillow.
"""
import json
import os
import random
import sys

from PIL import Image

PACK_FORMAT = 46  # 1.21.4


# ------------------------------------------------------------------ tekstury 16 × 16

def tex(base, noise=12, pattern=None, seed=0):
    rnd = random.Random(seed)
    img = Image.new('RGBA', (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            c = base(x, y) if callable(base) else base
            if pattern:
                c = pattern(x, y, c)
            d = rnd.randint(-noise, noise)
            px[x, y] = tuple(max(0, min(255, v + d)) for v in c[:3]) + (255,)
    return img


def planks(c):
    return lambda x, y, col: tuple(v - 25 for v in col) if y % 4 == 0 or (x + (y // 4) * 5) % 16 == 0 else col


def stripes(c, step=3):
    return lambda x, y, col: tuple(v - 30 for v in col) if x % step == 0 else col


def border(x, y, col, w=1, d=35):
    return tuple(v - d for v in col) if x < w or y < w or x > 15 - w or y > 15 - w else col


TEXTURES = {
    'drewno': tex((132, 94, 58), 8, planks(None), 1),
    'drewno_ciemne': tex((92, 64, 40), 8, planks(None), 2),
    'metal': tex((150, 152, 156), 10, None, 3),
    'metal_rdza': tex(lambda x, y: (150, 105, 70) if (x * 7 + y * 3) % 11 < 3 else (138, 140, 144), 12, None, 4),
    'metal_ciemny': tex((70, 72, 76), 8, None, 5),
    'karton': tex((182, 140, 92), 10, lambda x, y, c: (150, 120, 85) if y in (7, 8) else c, 6),
    'karton_bok': tex((176, 134, 86), 10, border, 7),
    'worek': tex((196, 180, 140), 14, lambda x, y, c: tuple(v - 18 for v in c) if (x + y) % 3 == 0 else c, 8),
    'tkanina_czerwona': tex((142, 46, 40), 12, lambda x, y, c: tuple(v - 15 for v in c) if (x // 2 + y // 2) % 2 else c, 9),
    'tkanina_zielona': tex((70, 96, 62), 12, None, 10),
    'guma': tex((32, 32, 35), 6, lambda x, y, c: (48, 48, 52) if x % 4 == 0 else c, 11),
    'emalia': tex((226, 226, 222), 6, None, 12),
    'emalia_brudna': tex(lambda x, y: (200, 196, 180) if (x * 5 + y * 9) % 13 < 2 else (228, 228, 224), 6, None, 13),
    'szyba': tex((40, 52, 64), 8, lambda x, y, c: (90, 110, 130) if x == y or x == y + 1 else c, 14),
    'ekran': tex(lambda x, y: (30, 160, 120) if 2 < x < 13 and 2 < y < 12 else (20, 20, 26), 10, None, 15),
    'automat': tex((70, 48, 112), 10, lambda x, y, c: (220, 180, 40) if y in (2, 3) else c, 16),
    'warzywa': tex(lambda x, y: [(200, 70, 40), (230, 140, 30), (110, 160, 60), (150, 90, 50)][((x // 3) + (y // 3) * 2) % 4], 18, None, 17),
    'zabawki': tex(lambda x, y: [(220, 60, 60), (60, 120, 220), (240, 200, 50), (90, 190, 90), (230, 120, 180)][((x // 4) + (y // 4) * 3) % 5], 14, None, 18),
    'gruz': tex(lambda x, y: [(128, 124, 118), (100, 98, 94), (150, 120, 100), (90, 86, 80)][(x * 3 + y * 5 + x * y) % 4], 12, None, 19),
    'beton': tex((140, 140, 136), 14, None, 20),
    'maszyna': tex((84, 104, 86), 10, lambda x, y, c: (40, 44, 40) if x in (0, 15) or y in (0, 15) else c, 21),
    'zolty': tex((214, 170, 40), 10, lambda x, y, c: (30, 30, 30) if (x + y) % 8 < 2 else c, 22),
    'garnek': tex((96, 96, 102), 8, lambda x, y, c: (60, 60, 64) if y < 2 else c, 23),
}


# ------------------------------------------------------------------ modele

def box(f, t, texture, faces=None):
    """Prostopadłościan z teksturą na wszystkich ścianach (lub mapą ściana -> tekstura)."""
    names = ('north', 'south', 'east', 'west', 'up', 'down')
    out = {}
    for n in names:
        tx = (faces or {}).get(n, texture)
        if f[0] == t[0] or f[1] == t[1] or f[2] == t[2]:
            pass
        out[n] = {'texture': '#' + tx}
    return {'from': list(f), 'to': list(t), 'faces': out}


def legs(texture, y=0, h=7, inset=1, w=2, x0=0, x1=16, z0=0, z1=16):
    return [box((x, y, z), (x + w, y + h, z + w), texture)
            for x in (x0 + inset, x1 - inset - w) for z in (z0 + inset, z1 - inset - w)]


MODELS = {
    # krzesło: siedzisko, oparcie z tyłu (z = 0), przód na +z
    'krzeslo': lambda: legs('drewno', h=8, inset=3) + [
        box((3, 8, 3), (13, 10, 13), 'drewno'),
        box((3, 10, 3), (13, 22, 5), 'drewno'),
        box((4, 14, 5), (12, 20, 5.5), 'tkanina_zielona'),
    ],
    'stol': lambda: legs('drewno_ciemne', h=14, inset=1) + [
        box((0, 14, 0), (16, 16, 16), 'drewno'),
    ],
    'szafa': lambda: [
        box((1, 0, 3), (15, 31, 16), 'drewno_ciemne'),
        box((1.5, 2, 16), (7.8, 29, 16.4), 'drewno'),
        box((8.2, 2, 16), (14.5, 29, 16.4), 'drewno'),
        box((7, 14, 16.4), (7.6, 18, 17), 'metal'),
        box((8.4, 14, 16.4), (9, 18, 17), 'metal'),
        box((0.5, 31, 2.5), (15.5, 32, 16.5), 'drewno_ciemne'),
    ],
    'lawka': lambda: [
        box((0, 0, 4), (2, 7, 14), 'metal_ciemny'), box((14, 0, 4), (16, 7, 14), 'metal_ciemny'),
        box((0, 7, 3), (16, 8.5, 14), 'drewno'),
        box((0, 9, 1), (16, 11, 2.5), 'drewno'), box((0, 13, 1), (16, 15, 2.5), 'drewno'),
        box((1, 7, 0.5), (2.5, 16, 2), 'metal_ciemny'), box((13.5, 7, 0.5), (15, 16, 2), 'metal_ciemny'),
    ],
    'kartony': lambda: [
        box((1, 0, 2), (10, 8, 12), 'karton', {'north': 'karton_bok', 'south': 'karton_bok', 'east': 'karton_bok', 'west': 'karton_bok'}),
        box((9, 0, 6), (15, 6, 15), 'karton', {'north': 'karton_bok', 'south': 'karton_bok', 'east': 'karton_bok', 'west': 'karton_bok'}),
        box((2, 8, 3), (9, 14, 10), 'karton', {'north': 'karton_bok', 'south': 'karton_bok', 'east': 'karton_bok', 'west': 'karton_bok'}),
    ],
    'worki': lambda: [
        box((1, 0, 1), (9, 6, 8), 'worek'), box((2, 6, 2), (8, 7, 7), 'worek'),
        box((8, 0, 7), (15, 6, 15), 'worek'), box((9, 6, 8), (14, 7, 14), 'worek'),
        box((3, 6, 7), (12, 11, 13), 'worek'), box((4, 11, 8), (11, 12, 12), 'worek'),
    ],
    'skrzynka_warzywa': lambda: [
        box((1, 0, 1), (15, 2, 15), 'drewno'),
        box((1, 2, 1), (15, 8, 2), 'drewno'), box((1, 2, 14), (15, 8, 15), 'drewno'),
        box((1, 2, 2), (2, 8, 14), 'drewno'), box((14, 2, 2), (15, 8, 14), 'drewno'),
        box((2, 2, 2), (14, 9, 14), 'warzywa'),
    ],
    'zabawki': lambda: [
        box((0, 0, 0), (16, 16, 2), 'metal'),
        box((0, 0, 2), (1, 16, 14), 'metal'), box((15, 0, 2), (16, 16, 14), 'metal'),
        box((1, 0, 2), (15, 1, 14), 'metal'), box((1, 7.5, 2), (15, 8.5, 14), 'metal'), box((1, 15, 2), (15, 16, 14), 'metal'),
        box((2, 1, 4), (6, 6, 10), 'zabawki'), box((7, 1, 3), (10, 4, 12), 'zabawki'), box((11, 1, 5), (14, 7, 11), 'zabawki'),
        box((2, 8.5, 5), (5, 12, 9), 'zabawki'), box((6, 8.5, 3), (11, 11, 12), 'zabawki'), box((12, 8.5, 6), (14, 14, 10), 'zabawki'),
    ],
    'automat_gier': lambda: [
        box((1, 0, 1), (15, 30, 13), 'automat'),
        box((2, 14, 11), (14, 26, 13.5), 'ekran', {'north': 'automat', 'east': 'automat', 'west': 'automat', 'up': 'automat', 'down': 'automat'}),
        box((1, 10, 13), (15, 13, 16), 'metal_ciemny'),
        box((4, 13, 14), (5, 15, 15), 'zolty'), box((8, 13, 14), (9, 14, 15), 'tkanina_czerwona'), box((10, 13, 14), (11, 14, 15), 'zabawki'),
        box((1, 28, 1), (15, 32, 14), 'zolty'),
    ],
    'opony': lambda: [b for y in (0, 4.5, 9) for b in (
        box((1, y, 1), (15, y + 4, 4), 'guma'), box((1, y, 12), (15, y + 4, 15), 'guma'),
        box((1, y, 4), (4, y + 4, 12), 'guma'), box((12, y, 4), (15, y + 4, 12), 'guma'),
    )],
    'kaloryfer': lambda: [box((x, 2, 1), (x + 1.5, 13, 4), 'emalia_brudna') for x in range(1, 15, 2)] + [
        box((1, 3, 1.5), (15, 4, 3.5), 'emalia_brudna'), box((1, 11, 1.5), (15, 12, 3.5), 'emalia_brudna'),
        box((14, 0, 2), (15, 2, 3), 'metal'),
    ],
    'pralka': lambda: [
        box((1, 0, 1), (15, 15, 15), 'emalia'),
        box((4, 3, 15), (12, 11, 15.5), 'szyba'),
        box((1, 12, 15), (15, 15, 15.3), 'metal'),
        box((11, 12.5, 15.3), (13, 14.5, 15.8), 'metal_ciemny'),
    ],
    'kanapa': lambda: [
        box((0, 1, 1), (16, 7, 15), 'tkanina_czerwona'),
        box((0, 7, 0), (16, 16, 4), 'tkanina_czerwona'),
        box((0, 7, 4), (16, 9, 15), 'tkanina_czerwona'),
        box((1, 0, 2), (3, 1, 4), 'drewno_ciemne'), box((13, 0, 2), (15, 1, 4), 'drewno_ciemne'),
        box((1, 0, 12), (3, 1, 14), 'drewno_ciemne'), box((13, 0, 12), (15, 1, 14), 'drewno_ciemne'),
    ],
    'garnek': lambda: [
        box((3, 0, 3), (4, 3, 4), 'metal_ciemny'), box((12, 0, 3), (13, 3, 4), 'metal_ciemny'),
        box((3, 0, 12), (4, 3, 13), 'metal_ciemny'), box((12, 0, 12), (13, 3, 13), 'metal_ciemny'),
        box((2, 3, 2), (14, 4, 14), 'metal_ciemny'),
        box((4, 4, 4), (12, 11, 12), 'garnek'),
        box((2, 9, 7), (4, 10, 9), 'metal_ciemny'), box((12, 9, 7), (14, 10, 9), 'metal_ciemny'),
    ],
    'maszyna': lambda: [
        box((0, 0, 0), (16, 2, 16), 'metal_ciemny'),
        box((1, 2, 2), (11, 12, 14), 'maszyna'),
        box((11, 3, 4), (15, 9, 12), 'metal_rdza'),
        box((3, 12, 5), (5, 16, 7), 'metal_rdza'), box((7, 12, 9), (9, 15, 11), 'metal_rdza'),
        box((4, 6, 14), (8, 10, 14.5), 'zolty'),
    ],
    'gruz': lambda: [
        box((0, 0, 0), (16, 2, 16), 'gruz'),
        box((1, 2, 2), (9, 6, 10), 'gruz'), box((7, 2, 6), (14, 5, 14), 'beton'),
        box((3, 6, 4), (8, 9, 8), 'beton'), box((10, 5, 9), (13, 7, 12), 'gruz'),
        box((12, 2, 1), (15, 3, 4), 'drewno_ciemne'), box((2, 2, 12), (10, 3, 13.5), 'metal_rdza'),
    ],
}


def main(out):
    tdir = os.path.join(out, 'assets/axon/textures/item/meble')
    mdir = os.path.join(out, 'assets/axon/models/item/meble')
    idir = os.path.join(out, 'assets/axon/items')
    for d in (tdir, mdir, idir):
        os.makedirs(d, exist_ok=True)
    with open(os.path.join(out, 'pack.mcmeta'), 'w', encoding='utf-8') as fh:
        json.dump({'pack': {'pack_format': PACK_FORMAT, 'description': 'AXON-Structure: nowe meble'}}, fh, indent=2,
                  ensure_ascii=False)
    for name, img in TEXTURES.items():
        img.save(os.path.join(tdir, name + '.png'))
    for name, build in MODELS.items():
        elements = build()
        used = sorted({f['texture'][1:] for e in elements for f in e['faces'].values()})
        model = {
            'textures': {'particle': 'axon:item/meble/' + used[0], **{t: 'axon:item/meble/' + t for t in used}},
            'elements': elements,
        }
        with open(os.path.join(mdir, name + '.json'), 'w', encoding='utf-8') as fh:
            json.dump(model, fh, indent=1)
        with open(os.path.join(idir, name + '.json'), 'w', encoding='utf-8') as fh:
            json.dump({'model': {'type': 'minecraft:model', 'model': 'axon:item/meble/' + name}}, fh, indent=2)
    print('meble:', ', '.join(MODELS))


if __name__ == '__main__':
    main(sys.argv[1] if len(sys.argv) > 1 else 'paczka/axon-meble')

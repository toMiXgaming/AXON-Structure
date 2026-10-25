"""Wspólne narzędzia: siatka bloków, zapis .axs i render izometryczny PNG."""
import random
from PIL import Image, ImageDraw, ImageFont


class S:
    def __init__(self, w, h, d, seed=1):
        self.w, self.h, self.d = w, h, d
        self.b = {}
        self.crates = []   # (x, y, z, facing, pool)
        self.signs = []    # (x, y, z, [lines])
        self.rng = random.Random(seed)
        self.meta = {}

    def set(self, x, y, z, s):
        if not (0 <= x < self.w and 0 <= y < self.h and 0 <= z < self.d):
            raise ValueError("poza: %s %s %s %s" % (x, y, z, s))
        if s is None or s == 'air':
            self.b.pop((x, y, z), None)
        else:
            self.b[(x, y, z)] = s if ':' in s else 'minecraft:' + s

    def get(self, x, y, z):
        return self.b.get((x, y, z), 'minecraft:air')

    def fill(self, x1, y1, z1, x2, y2, z2, s):
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for y in range(min(y1, y2), max(y1, y2) + 1):
                for z in range(min(z1, z2), max(z1, z2) + 1):
                    self.set(x, y, z, s(x, y, z) if callable(s) else s)

    def mix(self, *opts):
        """Losowy wybór z wagami: mix(('stone', 3), ('gravel', 1))."""
        tot = sum(w for _, w in opts)
        def pick(*_):
            r = self.rng.random() * tot
            for s, w in opts:
                r -= w
                if r <= 0:
                    return s
            return opts[-1][0]
        return pick

    def crate(self, x, y, z, facing, pool):
        self.set(x, y, z, 'air')
        self.crates.append((x, y, z, facing, pool))

    def point(self, x, y, z, facing, pool):
        """Punkt w widocznym bloku (stanowisko GUI): blok zostaje, wpis idzie do skrzynie:."""
        assert self.get(x, y, z) != 'minecraft:air'
        self.crates.append((x, y, z, facing, pool))

    def sign(self, x, y, z, state, *lines):
        assert len(lines) <= 4
        self.set(x, y, z, state)
        self.signs.append((x, y, z, list(lines)))

    def save(self, path):
        pal = ['minecraft:air']
        idx = {'minecraft:air': 0}
        rows = []
        for y in range(self.h):
            for z in range(self.d):
                cells = [self.get(x, y, z) for x in range(self.w)]
                if all(c == 'minecraft:air' for c in cells):
                    continue
                ids = []
                for c in cells:
                    if c not in idx:
                        idx[c] = len(pal)
                        pal.append(c)
                    ids.append(idx[c])
                toks, i = [], 0
                while i < len(ids):
                    j = i
                    while j < len(ids) and ids[j] == ids[i]:
                        j += 1
                    toks.append(str(ids[i]) + ('*%d' % (j - i) if j - i > 1 else ''))
                    i = j
                rows.append('%d %d: %s' % (y, z, ' '.join(toks)))
        out = ['# AXON-Structure v1']
        for k, v in self.meta.items():
            out.append('%s: %s' % (k, v))
        out.append('paleta:')
        out += ['%d %s' % (i, p) for i, p in enumerate(pal)]
        out.append('warstwy:')
        out += rows
        out.append('skrzynie:')
        out += ['%d %d %d %s %s' % c for c in self.crates]
        out.append('ramki:')
        out.append('napisy:')
        for x, y, z, lines in self.signs:
            lines = (lines + [''] * 4)[:4]
            out.append('%d %d %d|%s' % (x, y, z, '|'.join(lines)))
        open(path, 'w', encoding='utf-8').write('\n'.join(out) + '\n')
        return len(pal)


# ---------------------------------------------------------------- render

COL = {
    'grass_block': (95, 140, 55), 'dirt': (120, 85, 58), 'coarse_dirt': (110, 80, 55), 'rooted_dirt': (125, 90, 65),
    'podzol': (90, 65, 35), 'gravel': (130, 125, 122), 'stone': (125, 125, 125), 'cobblestone': (115, 115, 115),
    'mossy_cobblestone': (100, 115, 90), 'andesite': (135, 135, 135), 'polished_andesite': (140, 142, 142),
    'tuff': (108, 109, 102), 'smooth_stone': (160, 160, 160), 'gray_concrete': (55, 58, 62),
    'black_concrete': (20, 22, 26), 'light_gray_concrete': (125, 125, 115), 'white_concrete': (207, 213, 214),
    'red_concrete': (142, 33, 33), 'orange_concrete': (224, 97, 1), 'yellow_concrete': (240, 175, 21),
    'cyan_concrete': (21, 119, 136), 'blue_concrete': (45, 47, 143), 'green_concrete': (73, 91, 36),
    'brown_concrete': (96, 60, 32), 'gray_concrete_powder': (76, 81, 84), 'light_gray_concrete_powder': (154, 154, 148),
    'white_terracotta': (210, 178, 161), 'orange_terracotta': (162, 84, 38), 'brown_terracotta': (77, 51, 36),
    'cyan_terracotta': (87, 91, 91), 'light_gray_terracotta': (135, 107, 98), 'terracotta': (152, 94, 68),
    'red_terracotta': (143, 61, 47), 'black_terracotta': (37, 23, 16), 'yellow_terracotta': (186, 133, 35),
    'smooth_sandstone': (223, 214, 170), 'sandstone': (216, 203, 155), 'cut_red_sandstone': (189, 101, 31),
    'smooth_quartz': (235, 229, 222), 'quartz_block': (235, 229, 222), 'bricks': (150, 97, 83),
    'iron_block': (220, 220, 220), 'iron_bars': (140, 140, 140), 'chain': (60, 65, 75),
    'glass': (200, 225, 230), 'glass_pane': (200, 225, 230), 'gray_stained_glass_pane': (70, 75, 80),
    'black_stained_glass_pane': (30, 30, 35), 'light_blue_stained_glass_pane': (110, 160, 200),
    'oak_planks': (162, 131, 79), 'spruce_planks': (115, 85, 50), 'dark_oak_planks': (66, 43, 20),
    'oak_log': (110, 85, 50), 'spruce_log': (60, 45, 28), 'stripped_spruce_log': (115, 90, 52),
    'oak_fence': (162, 131, 79), 'spruce_fence': (115, 85, 50), 'dark_oak_fence': (66, 43, 20),
    'barrel': (140, 105, 60), 'chest': (160, 115, 45), 'cauldron': (70, 70, 75), 'smoker': (85, 80, 75),
    'blast_furnace': (95, 95, 100), 'furnace': (110, 110, 110), 'hopper': (70, 70, 75), 'dropper': (110, 110, 110),
    'anvil': (68, 68, 68), 'lever': (110, 100, 90), 'tripwire_hook': (110, 100, 90),
    'cobweb': (225, 225, 225), 'dead_bush': (130, 95, 50), 'short_grass': (95, 145, 60), 'tall_grass': (95, 145, 60),
    'fern': (80, 130, 55), 'moss_carpet': (90, 110, 45), 'moss_block': (90, 110, 45), 'vine': (70, 110, 40),
    'glow_lichen': (110, 130, 115), 'red_bed': (160, 40, 40), 'brown_carpet': (114, 72, 41),
    'white_carpet': (230, 230, 230), 'yellow_carpet': (240, 190, 40), 'gray_carpet': (62, 68, 71),
    'black_carpet': (25, 25, 30), 'orange_carpet': (240, 118, 19), 'lantern': (90, 80, 60),
    'iron_trapdoor': (200, 200, 200), 'spruce_trapdoor': (105, 78, 45), 'oak_trapdoor': (150, 120, 70),
    'iron_door': (200, 200, 200), 'oak_door': (150, 120, 70), 'spruce_door': (105, 78, 45),
    'lightning_rod': (180, 110, 80), 'stone_button': (125, 125, 125), 'polished_blackstone_button': (50, 45, 50),
    'decorated_pot': (150, 90, 65), 'flower_pot': (120, 65, 50), 'bookshelf': (115, 85, 50), 'loom': (140, 115, 85),
    'lectern': (160, 125, 75), 'composter': (110, 75, 40), 'note_block': (100, 65, 45), 'jukebox': (100, 65, 45),
    'red_wool': (160, 40, 40), 'white_wool': (230, 230, 230), 'black_wool': (25, 25, 30), 'orange_wool': (240, 118, 19),
    'hay_block': (180, 150, 30), 'coal_block': (16, 16, 16), 'sea_lantern': (175, 200, 190), 'redstone_lamp': (95, 55, 30),
    'snow': (240, 245, 245), 'water': (60, 90, 200), 'scaffolding': (170, 135, 80), 'ladder': (130, 100, 55),
    'rail': (120, 110, 95), 'tinted_glass': (45, 40, 50), 'weathered_copper': (100, 150, 120),
    'exposed_copper': (160, 125, 105), 'oxidized_copper': (80, 160, 135), 'copper_block': (190, 105, 75),
    'cracked_stone_bricks': (115, 113, 113), 'stone_bricks': (122, 122, 122), 'mud_bricks': (137, 104, 79),
    'packed_mud': (142, 106, 79), 'polished_deepslate': (72, 72, 73), 'deepslate_tiles': (54, 54, 55),
    'dried_kelp_block': (45, 50, 35), 'polished_blackstone': (53, 48, 56), 'heavy_weighted_pressure_plate': (220, 220, 220),
    'stone_pressure_plate': (125, 125, 125), 'light_weighted_pressure_plate': (240, 200, 60),
    'item_frame': (130, 100, 60), 'bell': (220, 180, 50), 'candle': (220, 210, 180), 'red_candle': (160, 40, 40),
    'brewing_stand': (110, 100, 90), 'end_rod': (230, 225, 215), 'brown_mushroom': (150, 110, 80),
    'oak_leaves': (60, 100, 40), 'cracked_deepslate_tiles': (52, 52, 53), 'pale_oak_planks': (225, 215, 210),
    'mangrove_planks': (117, 54, 48), 'crimson_planks': (101, 48, 70), 'warped_planks': (43, 104, 99),
    'crafting_table': (130, 95, 55), 'grindstone': (140, 140, 140), 'stonecutter': (125, 125, 125),
    'smithing_table': (60, 50, 60), 'cartography_table': (100, 80, 60), 'fletching_table': (190, 170, 120),
    'black_stained_glass': (30, 30, 35), 'gray_stained_glass': (70, 75, 80), 'cyan_stained_glass_pane': (70, 130, 150),
    'orange_stained_glass_pane': (220, 120, 40), 'white_stained_glass_pane': (225, 225, 225),
    'mud_brick_wall': (137, 104, 79), 'red_banner': (160, 40, 40),
}
COL.update({
    'ender_chest': (55, 58, 62), 'jack_o_lantern': (150, 110, 70), 'end_portal_frame': (236, 236, 232),
    'bee_nest': (140, 100, 60), 'calibrated_sculk_sensor': (170, 210, 225), 'damaged_anvil': (190, 40, 35),
    'sticky_piston': (110, 118, 125), 'vault': (190, 190, 196), 'respawn_anchor': (185, 45, 30),
    'lodestone': (165, 168, 175),
    'blue_terracotta': (74, 60, 91), 'light_blue_terracotta': (113, 108, 137), 'green_terracotta': (76, 83, 42),
    'lime_terracotta': (103, 117, 53), 'purple_terracotta': (118, 70, 86), 'pink_terracotta': (161, 78, 78),
    'gray_terracotta': (57, 42, 35), 'magenta_terracotta': (149, 88, 108), 'warped_door': (43, 104, 99),
    'acacia_planks': (168, 90, 50), 'smooth_red_sandstone': (181, 98, 31), 'red_sandstone': (186, 99, 29),
    'nether_bricks': (44, 21, 26), 'red_nether_bricks': (69, 7, 9), 'quartz_bricks': (234, 229, 221),
    'white_glazed_terracotta': (188, 212, 202), 'cyan_wool': (21, 137, 145), 'polished_diorite': (192, 193, 194),
    'diorite': (188, 182, 183), 'calcite': (223, 224, 220), 'chiseled_bookshelf': (130, 95, 55),
    'oak_sign': (162, 131, 79), 'iron_ore': (135, 130, 125), 'pale_oak_log': (90, 80, 75),
})
# modele z paczki zajmujące 2 kratki: bariera nad nimi rysowana kolorem modelu
# bloki z większych struktur (kombinat, jednostka, szpital, oaza, zajezdnia, elektrownia, osiedle)
COL.update({
    'birch_planks': (192, 175, 121), 'birch_log': (216, 215, 210), 'birch_leaves': (110, 150, 70),
    'azalea_leaves': (95, 125, 45), 'dark_oak_log': (60, 46, 26), 'stripped_oak_log': (177, 144, 86),
    'stripped_spruce_wood': (115, 89, 52), 'stripped_dark_oak_log': (96, 76, 49),
    'blackstone': (42, 36, 41), 'polished_blackstone_bricks': (48, 42, 49),
    'cracked_polished_blackstone_bricks': (44, 38, 44), 'cobbled_deepslate': (77, 77, 80),
    'mossy_stone_bricks': (115, 121, 105), 'mud': (60, 57, 60), 'granite': (149, 103, 85),
    'sand': (219, 207, 163), 'packed_ice': (141, 180, 250), 'quartz_pillar': (235, 230, 222),
    'lime_concrete': (94, 168, 24), 'black_concrete_powder': (25, 26, 31), 'magma_block': (142, 63, 31),
    'blue_stained_glass': (60, 80, 170), 'light_blue_stained_glass': (110, 150, 210),
    'white_stained_glass': (235, 235, 235), 'light_blue_carpet': (58, 175, 217), 'lime_carpet': (112, 185, 25),
    'pink_carpet': (237, 141, 172), 'dirt_path': (148, 122, 65), 'farmland': (110, 75, 45),
    'campfire': (110, 80, 50), 'water_cauldron': (70, 70, 75), 'chipped_anvil': (68, 68, 68),
    'observer': (98, 98, 98), 'target': (226, 170, 157), 'dispenser': (110, 110, 110),
    'daylight_detector': (130, 116, 94), 'beehive': (180, 146, 90), 'carved_pumpkin': (198, 118, 24),
    'pumpkin': (198, 118, 24), 'melon': (111, 145, 31), 'copper_bulb': (156, 87, 60),
    'waxed_weathered_copper_grate': (108, 153, 110), 'skeleton_skull': (200, 200, 200),
    'redstone_wire': (170, 20, 10), 'tripwire': (200, 200, 200), 'lily_pad': (32, 128, 48),
    'large_fern': (90, 130, 60), 'sweet_berry_bush': (60, 100, 50),
    'wheat': (170, 160, 60), 'carrots': (80, 150, 40), 'potatoes': (80, 150, 40), 'beetroots': (80, 130, 40),
    'poppy': (190, 30, 30), 'dandelion': (240, 220, 40), 'allium': (180, 110, 210), 'azure_bluet': (220, 230, 230),
    'cornflower': (70, 100, 200), 'oxeye_daisy': (230, 230, 210), 'lily_of_the_valley': (235, 235, 235),
    'red_tulip': (200, 40, 30), 'white_tulip': (230, 230, 230), 'spruce_leaves': (50, 85, 50),
})
for _k in list(COL):
    COL.setdefault('potted_' + _k, (120, 70, 50))
COL['potted_dead_bush'] = COL['potted_fern'] = COL['potted_oak_sapling'] = (120, 70, 50)

TALL = ('calibrated_sculk_sensor', 'damaged_anvil')

THIN = ('fence', 'wall', 'iron_bars', 'pane', 'chain', 'lightning_rod', 'end_rod', 'rod')
FLAT = ('carpet', 'pressure_plate', 'rail', 'snow', 'trapdoor', 'lily')
SMALL = ('button', 'lever', 'tripwire_hook', 'sign', 'banner', 'candle', 'lantern', 'grass', 'fern', 'bush',
         'mushroom', 'flower_pot', 'cobweb', 'vine', 'lichen', 'torch', 'pot', 'sapling', 'frame', 'ladder')


def base(s):
    return s.split('[')[0].split(':')[-1]


def prop(s, key):
    if '[' not in s:
        return None
    for kv in s.split('[')[1].rstrip(']').split(','):
        k, _, v = kv.partition('=')
        if k == key:
            return v
    return None


def color(s):
    b = base(s)
    if b in COL:
        return COL[b]
    for suf in ('_stairs', '_slab', '_wall', '_fence_gate', '_fence', '_wall_sign', '_sign', '_hanging_sign',
                '_wall_banner', '_banner', '_carpet', '_button', '_pressure_plate', '_door', '_trapdoor', '_pane'):
        if b.endswith(suf):
            k = b[:-len(suf)]
            for cand in (k, k + '_planks', k + 's', k + '_block', k.replace('_brick', '_bricks'),
                         k.replace('waxed_', ''), k.replace('waxed_', '') + '_block', k + '_wool',
                         k + '_concrete'):
                if cand in COL:
                    return COL[cand]
            if 'cut_copper' in k:
                k2 = k.replace('waxed_', '').replace('cut_', '')
                return COL.get(k2.replace('_copper', '_copper') if k2 != 'copper' else 'copper_block',
                               COL.get(k2, (170, 110, 80)))
            if k.endswith('_stained_glass'):
                return COL.get(k + '_pane', (150, 150, 150))
    b2 = b.replace('waxed_', '')
    if b2 in COL:
        return COL[b2]
    if 'cut_copper' in b2:
        return COL.get(b2.replace('cut_', ''), COL['copper_block'] if b2 == 'cut_copper' else (160, 125, 105))
    if b.endswith('_bed'):
        return COL.get(b.replace('_bed', '_wool'), (150, 40, 40))
    if b.endswith('_wool'):
        return (150, 150, 150)
    return (200, 0, 200)


def shape(s):
    """(x0, x1, y0, y1, z0, z1) w jednostkach bloku."""
    b = base(s)
    if b.endswith('_slab'):
        t = prop(s, 'type')
        return (0, 1, .5, 1, 0, 1) if t == 'top' else (0, 1, 0, 1, 0, 1) if t == 'double' else (0, 1, 0, .5, 0, 1)
    if b.endswith('_trapdoor'):
        if prop(s, 'open') == 'true':
            f = prop(s, 'facing')
            return {'north': (0, 1, 0, 1, .8, 1), 'south': (0, 1, 0, 1, 0, .2),
                    'west': (.8, 1, 0, 1, 0, 1), 'east': (0, .2, 0, 1, 0, 1)}.get(f, (0, 1, 0, .2, 0, 1))
        return (0, 1, .8, 1, 0, 1) if prop(s, 'half') == 'top' else (0, 1, 0, .2, 0, 1)
    if b.endswith('_door'):
        f = prop(s, 'facing')
        if prop(s, 'open') == 'true':
            f = {'north': 'east', 'east': 'south', 'south': 'west', 'west': 'north'}[f]
        return {'north': (0, 1, 0, 1, .8, 1), 'south': (0, 1, 0, 1, 0, .2),
                'west': (.8, 1, 0, 1, 0, 1), 'east': (0, .2, 0, 1, 0, 1)}.get(f, (0, 1, 0, 1, 0, 1))
    if b.endswith('_wall_sign') or b.endswith('_wall_banner'):
        f = prop(s, 'facing')
        y0 = 0 if b.endswith('banner') else .3
        return {'north': (.05, .95, y0, .8, .88, 1), 'south': (.05, .95, y0, .8, 0, .12),
                'west': (.88, 1, y0, .8, .05, .95), 'east': (0, .12, y0, .8, .05, .95)}.get(f)
    if b == 'ladder':
        f = prop(s, 'facing')
        return {'north': (0, 1, 0, 1, .88, 1), 'south': (0, 1, 0, 1, 0, .12),
                'west': (.88, 1, 0, 1, 0, 1), 'east': (0, .12, 0, 1, 0, 1)}.get(f)
    if any(k in b for k in FLAT):
        return (0, 1, 0, .1, 0, 1)
    if b.endswith('_pane') or b == 'iron_bars':
        return (.4, .6, 0, 1, .4, .6) if True else None
    if any(k in b for k in THIN):
        return (.35, .65, 0, 1, .35, .65)
    if b in ('ender_chest', 'end_portal_frame', 'vault'):
        return (.15, .85, 0, .7, .15, .85)
    if b in ('cauldron', 'hopper', 'composter'):
        return (0, 1, 0, .9, 0, 1)
    if b.endswith('_bed'):
        return (0, 1, 0, .55, 0, 1)
    if b in ('jack_o_lantern', 'sea_lantern'):
        return (0, 1, 0, 1, 0, 1)
    if any(b == k or b.endswith('_' + k) for k in SMALL):
        return (.3, .7, 0, .5, .3, .7)
    if b.endswith('_stairs'):
        return (0, 1, 0, .75, 0, 1)
    if b in ('lectern', 'grindstone', 'stonecutter', 'brewing_stand', 'bell', 'decorated_pot', 'anvil'):
        return (.15, .85, 0, .8, .15, .85)
    return (0, 1, 0, 1, 0, 1)


def render(st, path, title=None, legend=None, cut=None, markers=None, scale=16, hide=None):
    """Izometria od strony południowo-wschodniej (+x, +z bliżej widza).
    cut: maks. y do pokazania; markers: {(x,y,z): (rgb, kształt)}; hide: funkcja (x,y,z,s)->bool."""
    a = scale
    hh = scale  # wysokość bloku na ekranie
    items = []
    for (x, y, z), s in st.b.items():
        if cut is not None and y > cut:
            continue
        if hide and hide(x, y, z, s):
            continue
        if base(s) == 'barrier':
            below = st.get(x, y - 1, z)
            if base(below) not in TALL:
                continue
            s = below
        items.append((x, y, z, s, color(s), shape(s)))
    for (x, y, z), (rgb, shp) in (markers or {}).items():
        items.append((x, y, z, 'marker', rgb, shp))
    items.sort(key=lambda t: (t[0] + t[2], t[1], t[0]))

    W = (st.w + st.d) * a + 40
    top_pad = 120 if legend else 30
    H = int((st.w + st.d) * a / 2 + (st.h + 1) * hh) + top_pad + 30
    img = Image.new('RGB', (W, H), (242, 241, 236))
    dr = ImageDraw.Draw(img)
    ox = st.d * a + 20
    oy = (st.h + 1) * hh + top_pad

    def P(x, y, z):
        return (ox + (x - z) * a, oy + (x + z) * a / 2 - y * hh)

    rng = random.Random(7)
    for x, y, z, s, rgb, shp in items:
        if shp is None:
            continue
        x0, x1, y0, y1, z0, z1 = shp
        X0, X1, Y0, Y1, Z0, Z1 = x + x0, x + x1, y + y0, y + y1, z + z0, z + z1
        n = rng.randint(-9, 9) if s != 'marker' else 0
        c = tuple(max(0, min(255, v + n)) for v in rgb)
        top = c
        left = tuple(int(v * .72) for v in c)
        right = tuple(int(v * .86) for v in c)
        # górna ściana
        dr.polygon([P(X0, Y1, Z0), P(X1, Y1, Z0), P(X1, Y1, Z1), P(X0, Y1, Z1)], fill=top)
        # ściana +z (lewa na ekranie)
        dr.polygon([P(X0, Y1, Z1), P(X1, Y1, Z1), P(X1, Y0, Z1), P(X0, Y0, Z1)], fill=left)
        # ściana +x (prawa)
        dr.polygon([P(X1, Y1, Z0), P(X1, Y1, Z1), P(X1, Y0, Z1), P(X1, Y0, Z0)], fill=right)
        if base(s) in ('glass', 'glass_pane') or 'stained_glass' in s:
            pass
    try:
        font = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf', 17)
    except OSError:
        font = ImageFont.load_default()
    ty = 14
    if title:
        dr.text((20, ty), title, fill=(30, 30, 30), font=font)
        ty += 26
    for rgb, label in (legend or []):
        dr.rectangle([20, ty + 3, 34, ty + 17], fill=rgb)
        dr.text((42, ty), label, fill=(30, 30, 30), font=font)
        ty += 24
    img.save(path)

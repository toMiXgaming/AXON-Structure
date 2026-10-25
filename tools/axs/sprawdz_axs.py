"""Sprawdza plik .axs tak jak StructureFile.read i wypisuje podsumowanie. Użycie: python3 sprawdz_axs.py plik.axs [--png katalog [przekrój-y]]"""
import sys, collections

def wczytaj(path):
    meta, pal, rows, pts, signs, props = {}, [], {}, [], [], []
    sec = ''
    for n, line in enumerate(open(path, encoding='utf-8'), 1):
        line = line.rstrip('\n')
        if not line.strip() or line.startswith('#'):
            continue
        try:
            if line.endswith(':') and ' ' not in line:
                sec = line[:-1]
                assert sec in ('paleta', 'warstwy', 'skrzynie', 'ramki', 'napisy', 'modele'), 'nieznana sekcja ' + sec
                if sec == 'paleta':
                    w, h, d = map(int, meta['rozmiar'].split())
                    assert 'id' in meta, "brak 'id'"
                continue
            if sec == '':
                k, v = line.split(':', 1)
                meta[k.strip()] = v.strip()
            elif sec == 'paleta':
                i, s = line.split(' ', 1)
                assert int(i) == len(pal), 'indeksy palety muszą iść po kolei od 0'
                pal.append(s.strip())
            elif sec == 'warstwy':
                yz, cells = line.split(':', 1)
                y, z = map(int, yz.split())
                assert 0 <= y < h and 0 <= z < d, 'y/z poza rozmiarem'
                row = []
                for t in cells.split():
                    i, c = (t.split('*') + ['1'])[:2]
                    assert 0 <= int(i) < len(pal), 'indeks %s spoza palety' % i
                    row += [int(i)] * int(c)
                assert len(row) == w, 'wiersz ma %d bloków zamiast %d' % (len(row), w)
                rows[(y, z)] = row
            elif sec in ('skrzynie', 'ramki'):
                p = line.split()
                assert len(p) == 5, "oczekiwano 'x y z kierunek pula'"
                x, y, z = map(int, p[:3])
                assert 0 <= x < w and 0 <= y < h and 0 <= z < d, 'punkt poza rozmiarem'
                assert p[3] in ('north', 'south', 'east', 'west', 'up', 'down'), 'zły kierunek'
                pts.append((sec, x, y, z, p[3], p[4]))
            elif sec == 'napisy':
                p = line.split('|')
                x, y, z = map(int, p[0].split())
                blok = pal[rows.get((y, z), [0] * w)[x]]
                if 'sign' not in blok:
                    print('uwaga: linia %d: w %d %d %d nie ma tabliczki (%s), napis przepadnie' % (n, x, y, z, blok))
                signs.append((x, y, z, p[1:5]))
            elif sec == 'modele':
                p = line.split()
                assert len(p) == 6, "oczekiwano 'x y z model skala obrot'"
                list(map(float, p[:3] + p[4:]))
                props.append(p)
        except Exception as e:
            sys.exit('%s: linia %d: %s' % (path, n, e))
    naglowek(path, meta)
    return meta, pal, rows, pts, signs, props


def naglowek(path, meta):
    """Pola z Traits.parse: błędna wartość przerywa wczytywanie pliku w pluginie."""
    w, h, d = map(int, meta['rozmiar'].split())
    def zle(pole, v):
        sys.exit('%s: zły %s: %r' % (path, pole, v))
    if 'grunt' in meta and not 0 <= int(meta['grunt']) < h:
        zle('grunt', meta['grunt'])
    for part in meta.get('wjazdy', '').split(';'):
        if part.strip():
            p = part.split()
            if len(p) != 3 or not (0 <= int(p[0]) < w and 0 <= int(p[1]) < d) \
                    or p[2].lower() not in ('north', 'south', 'east', 'west'):
                zle('wjazd', part.strip())
    for pole, ok in (('mapa', ('tak', 'nie')), ('bezpieczna', ('tak', 'nie')),
                     ('rozmiar-klasa', ('duza', 'srednia', 'mala'))):
        if pole in meta and meta[pole].lower() not in ok:
            zle(pole, meta[pole])
    for pole, ok in (('strefa', ('rownina', 'przy-gorach', 'las', 'ustronnie', 'srodek', 'plaza', 'dowolna',
                                 'bezpieczna')), ('klimat', ('snieg', 'umiarkowany', 'suchy'))):
        for v in meta.get(pole, '').split(','):
            if v.strip() and v.strip().lower() not in ok:
                zle(pole, v.strip())

if __name__ == '__main__':
    meta, pal, rows, pts, signs, props = wczytaj(sys.argv[1])
    print({k: meta[k] for k in meta if k != 'opis'})
    print('paleta', len(pal), 'wiersze', len(rows), 'napisy', len(signs), 'modele', len(props))
    print('punkty:', dict(collections.Counter(p[5] for p in pts)))
    if len(sys.argv) > 2:
        # --png <katalog> [przekrój-y]: podgląd i przekrój jak w folderze projektu
        import os
        sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
        from lib import S, color, base
        bez = sorted({base(b) for b in pal[1:] if color(b) == (200, 0, 200)} - {'barrier'})
        if bez:
            print('uwaga: bez koloru w lib.py (na PNG fioletowe):', ', '.join(bez))
        from preview import previews
        w, h, d = map(int, meta['rozmiar'].split())
        s = S(w, h, d)
        s.meta = meta
        for (y, z), row in rows.items():
            for x, i in enumerate(row):
                if i:
                    s.b[(x, y, z)] = pal[i]
        s.crates = [(x, y, z, f, pool) for sec, x, y, z, f, pool in pts if sec == 'skrzynie']
        cut = int(sys.argv[4]) if len(sys.argv) > 4 else int(meta.get('grunt', 0)) + 4
        previews(s, sys.argv[3], meta['id'], cut=cut)
        print('zapisano', sys.argv[3] + '/' + meta['id'] + '-podglad.png i -przekroj.png')

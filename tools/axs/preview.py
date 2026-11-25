from lib import render

LEGEND = [((150, 95, 40), 'skrzynie i beczki'), ((50, 90, 220), 'lepsze skrzynie'),
          ((220, 30, 30), 'NPC'), ((240, 200, 0), 'wjazd z drogi')]
BETTER = ('wrak', 'magazyn', 'lodowki', 'zrzut', 'sejf', 'skarbiec')


def previews(s, out, name, cut, hide=None):
    marks = {}
    for x, y, z, f, pool in s.crates:
        if pool.startswith('punkt-'):
            continue
        if pool.startswith('npc-'):
            marks[(x, y, z)] = ((220, 30, 30), (.3, .7, 0, 1.8, .3, .7))
        else:
            rgb = (50, 90, 220) if pool in BETTER else (150, 95, 40)
            marks[(x, y, z)] = (rgb, (.1, .9, 0, .85, .1, .9))
    ent = {}
    for part in s.meta.get('wjazdy', '').split(';'):
        if part.strip():
            x, z, _ = part.split()
            ent[(int(x), 0, int(z))] = ((240, 200, 0), (0, 1, 0, 1.08, 0, 1))
    render(s, '%s/%s-podglad.png' % (out, name), markers={k: v for k, v in marks.items()})
    m2 = dict(marks)
    m2.update(ent)
    render(s, '%s/%s-przekroj.png' % (out, name), title='Przekrój: bez dachów, kolory = rodzaj punktu',
           legend=LEGEND, cut=cut, markers=m2, hide=hide)

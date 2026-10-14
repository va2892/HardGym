"""Generate solid upholstery and colored bench resources with the existing geometry.

Generated JSON/PNG files are included; Gradle builds need no Python.
Black benches retain their original registry ID and custom upholstery texture.
"""
from pathlib import Path
import copy
import json
import struct
import zlib

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/hardgym'
DATA = ROOT / 'src/main/resources/data/hardgym'
COLORS = {
    'white': 'Белая',
    'orange': 'Оранжевая',
    'magenta': 'Пурпурная',
    'light_blue': 'Голубая',
    'yellow': 'Жёлтая',
    'lime': 'Лаймовая',
    'pink': 'Розовая',
    'gray': 'Серая',
    'light_gray': 'Светло-серая',
    'cyan': 'Бирюзовая',
    'purple': 'Фиолетовая',
    'blue': 'Синяя',
    'brown': 'Коричневая',
    'green': 'Зелёная',
    'red': 'Красная',
    'black': 'Чёрная',
}
PAD_COLORS = {
    'white': 'F9FFFE',
    'orange': 'F9801D',
    'magenta': 'C74EBD',
    'light_blue': '3AB3DA',
    'yellow': 'FED83D',
    'lime': '80C71F',
    'pink': 'F38BAA',
    'gray': '474F52',
    'light_gray': '9D9D97',
    'cyan': '169C9C',
    'purple': '8932B8',
    'blue': '3C44AA',
    'brown': '835432',
    'green': '5E7C16',
    'red': 'B02E26',
}


def read(path):
    return json.loads(path.read_text(encoding='utf-8'))


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


def write_solid_pad(path, rgb_hex):
    """Write a uniform, opaque 16x16 RGBA texture using only the standard library."""
    pixel = bytes.fromhex(rgb_hex) + b'\xff'
    rows = (b'\x00' + pixel * 16) * 16

    def chunk(kind, data):
        return (struct.pack('>I', len(data)) + kind + data
                + struct.pack('>I', zlib.crc32(kind + data) & 0xffffffff))

    header = struct.pack('>IIBBBBB', 16, 16, 8, 6, 0, 0, 0)
    png = (b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', header)
           + chunk(b'IDAT', zlib.compress(rows, 9)) + chunk(b'IEND', b''))
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(png)


def colored_model(model, color):
    prefix = 'hardgym:block/bench_press_station_'
    if not model.startswith(prefix):
        raise ValueError('Unexpected bench model: ' + model)
    return prefix + color + '_' + model[len(prefix):]


def main():
    base_states = read(ASSETS / 'blockstates/bench_press_station.json')
    base_item = read(ASSETS / 'models/item/bench_press_station.json')
    base_loot = read(DATA / 'loot_tables/blocks/bench_press_station.json')
    base_recipe = read(DATA / 'recipes/bench_press_station.json')
    base_unlock = read(DATA / 'advancements/recipes/bench_press_station.json')
    models = sorted({variant['model'] for variant in base_states['variants'].values()}
                    | {base_item['parent']})
    languages = {language: read(ASSETS / ('lang/' + language + '.json'))
                 for language in ('ru_ru', 'en_us')}

    for color, russian in COLORS.items():
        black = color == 'black'
        name = 'bench_press_station' if black else color + '_bench_press_station'
        identifier = 'hardgym:' + name
        if not black:
            write_solid_pad(ASSETS / ('textures/block/bench_pad_' + color + '.png'), PAD_COLORS[color])
            states = copy.deepcopy(base_states)
            for variant in states['variants'].values():
                variant['model'] = colored_model(variant['model'], color)
            write(ASSETS / ('blockstates/' + name + '.json'), states)

            for parent in models:
                # Parent elements and #pad references preserve the rack/bar grip geometry.
                child = {'parent': parent, 'textures': {'pad': 'hardgym:block/bench_pad_' + color}}
                model_path = colored_model(parent, color).split(':', 1)[1]
                write(ASSETS / ('models/' + model_path + '.json'), child)

            item = copy.deepcopy(base_item)
            item['parent'] = colored_model(item['parent'], color)
            write(ASSETS / ('models/item/' + name + '.json'), item)

            loot = copy.deepcopy(base_loot)
            loot['pools'][0]['entries'][0]['name'] = identifier
            write(DATA / ('loot_tables/blocks/' + name + '.json'), loot)

            unlock = copy.deepcopy(base_unlock)
            unlock['criteria']['has_the_recipe']['conditions']['recipe'] = identifier
            unlock['rewards']['recipes'] = [identifier]
            write(DATA / ('advancements/recipes/' + name + '.json'), unlock)

        recipe = copy.deepcopy(base_recipe)
        recipe['group'] = 'hardgym:bench_press_stations'
        recipe['key']['W']['item'] = 'minecraft:' + color + '_wool'
        recipe['result']['item'] = identifier
        write(DATA / ('recipes/' + name + '.json'), recipe)

        key = 'block.hardgym.' + name
        languages['ru_ru'][key] = russian + ' скамья для жима'
        languages['en_us'][key] = color.replace('_', ' ').title() + ' Bench Press Station'

    for language, entries in languages.items():
        write(ASSETS / ('lang/' + language + '.json'), entries)
    print('Generated 16 bench colors with solid upholstery, matching wool recipes and inherited models.')


if __name__ == '__main__':
    main()

#!/usr/bin/env python3
"""Generate the 12 paired-dumbbell models, crafting recipes and translations."""
import copy
import json
import math
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RESOURCES = ROOT / 'src/main/resources'
WEIGHTS = range(10, 121, 10)
DISC_THICKNESS = 1.65
DISC_GAP = 0.12
GRIP_EDGE = 2.05
DISPLAY = {
    'thirdperson_righthand': {'rotation': [0, 0, 90], 'translation': [0, -2, 1], 'scale': [0.65] * 3},
    'thirdperson_lefthand': {'rotation': [0, 0, -90], 'translation': [0, -2, 1], 'scale': [0.65] * 3},
    'firstperson_righthand': {'rotation': [0, 0, 75], 'translation': [1, 3, 1], 'scale': [0.75] * 3},
    'firstperson_lefthand': {'rotation': [0, 0, -75], 'translation': [1, 3, 1], 'scale': [0.75] * 3},
    'gui': {'rotation': [25, 225, 0], 'translation': [0, 0, 0], 'scale': [0.9] * 3},
    'ground': {'translation': [0, 2, 0], 'scale': [0.6] * 3},
    'fixed': {'rotation': [0, 90, 0], 'scale': [0.8] * 3}
}
PIXEL_FONT = {
    '0': ['111', '101', '101', '101', '111'],
    '1': ['010', '110', '010', '010', '111'],
    '2': ['111', '001', '111', '100', '111'],
    '3': ['111', '001', '111', '001', '111'],
    '4': ['101', '101', '111', '001', '001'],
    '5': ['111', '100', '111', '001', '111'],
    '6': ['111', '100', '111', '101', '111'],
    '7': ['111', '001', '010', '010', '010'],
    '8': ['111', '101', '111', '101', '111'],
    '9': ['111', '101', '111', '001', '111'],
    'K': ['101', '110', '100', '110', '101'],
    'G': ['111', '100', '101', '101', '111']
}


def draw_pixels(draw, text, x, y, scale, fill):
    for index, char in enumerate(text):
        for row, bits in enumerate(PIXEL_FONT[char]):
            for column, bit in enumerate(bits):
                if bit == '1':
                    left, top = x + (index * 4 + column) * scale, y + row * scale
                    draw.rectangle((left, top, left + scale - 1, top + scale - 1), fill=fill)


def plate_face():
    from PIL import Image, ImageDraw
    image = Image.new('RGB', (32, 32), (24, 26, 28))
    draw = ImageDraw.Draw(image)
    # Raised black rims, as on a stack of metal plates. Keep every sprite
    # opaque so the rack can reuse its single solid atlas buffer.
    draw.ellipse((1, 1, 30, 30), outline=(61, 64, 67), width=1)
    draw.ellipse((2, 2, 29, 29), outline=(35, 38, 41), width=1)
    draw.ellipse((4, 4, 27, 27), outline=(12, 14, 16), width=1)
    draw.ellipse((11, 11, 20, 20), outline=(48, 51, 54), width=1)
    return image


def make_label(weight):
    # Exact weight lettering is generated pixel by pixel, without a font or
    # external texture dependency. The label is a native Minecraft atlas sprite.
    from PIL import Image, ImageDraw
    image = plate_face()
    draw = ImageDraw.Draw(image)
    text = str(weight)
    scale = 1
    width = (len(text) * 4 - 1) * scale
    draw_pixels(draw, text, (32 - width) // 2, 5, scale, (223, 225, 227))
    draw_pixels(draw, 'KG', 12, 25, 1, (169, 173, 178))
    path = RESOURCES / 'assets/hardgym/textures/item' / ('dumbbell_label_' + str(weight) + 'kg.png')
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path)


def make_shared_textures():
    from PIL import Image, ImageDraw
    folder = RESOURCES / 'assets/hardgym/textures/item'
    folder.mkdir(parents=True, exist_ok=True)
    plate_face().save(folder / 'dumbbell_plate_face.png')
    edge = Image.new('RGB', (32, 16), (27, 29, 31))
    draw = ImageDraw.Draw(edge)
    for x, shade in ((0, 12), (1, 75), (2, 49), (29, 49), (30, 75), (31, 12)):
        draw.line((x, 0, x, 15), fill=(shade, shade + 2, shade + 4))
    edge.save(folder / 'dumbbell_plate_edge.png')
    grip = Image.new('RGB', (16, 16), (159, 163, 168))
    for y in range(16):
        for x in range(16):
            if (x + y) % 4 == 0 or (x - y) % 4 == 0:
                grip.putpixel((x, y), (137, 141, 146))
    grip.save(folder / 'dumbbell_grip.png')


def cube(start, end, texture, omit=()):
    return {'from': start, 'to': end,
            'faces': {face: {'texture': '#' + texture, 'uv': [0, 0, 16, 16]}
                      for face in ('north', 'south', 'east', 'west', 'up', 'down') if face not in omit}}


def plate_disc(x0, x1, radius, label_side=None):
    # Three joined boxes make a coarse rounded profile in the mod's block
    # style. Each disc remains actual geometry, rather than a painted band.
    # Avoid tiny cubes, rotated parts and a separate render pass per plate.
    cut = round(radius * 0.72, 4)
    parts = [
        cube([x0, 8 - cut, 8 - radius], [x1, 8 + cut, 8 + radius], 'edge'),
        cube([x0, 8 + cut, 8 - cut], [x1, 8 + radius, 8 + cut], 'edge', ('down',)),
        cube([x0, 8 - radius, 8 - cut], [x1, 8 - cut, 8 + cut], 'edge', ('up',))
    ]
    for part in parts:
        part['from'] = [round(value, 4) for value in part['from']]
        part['to'] = [round(value, 4) for value in part['to']]
        for face in part['faces'].values():
            face['uv'] = [0, 0, 16, 16]
        low, high = part['from'], part['to']
        v0 = (8 + radius - high[1]) * 8 / radius
        v1 = (8 + radius - low[1]) * 8 / radius
        # Both end faces use one shared image across the three boxes, with
        # native east/west UV directions so the weight is readable either way.
        for side in ('west', 'east'):
            u0 = ((low[2] - 8 + radius) if side == 'west' else (8 + radius - high[2])) * 8 / radius
            u1 = ((high[2] - 8 + radius) if side == 'west' else (8 + radius - low[2])) * 8 / radius
            part['faces'][side] = {
                'texture': '#label' if side == label_side else '#face',
                'uv': [round(value, 4) for value in (u0, v0, u1, v1)]
            }
    return parts


def make_model(weight, index, display):
    make_label(weight)
    half_height = round(3.0 if weight == 10 else 3.5 + max(0, weight - 20) * 0.009, 4)
    count = 1 + index // 2
    depth = round(count * DISC_THICKNESS + (count - 1) * DISC_GAP, 4)
    # Heavy stacks use a shorter exposed handle, while leaving enough clear
    # grip for the hand. This also lets both heads rest on the rack's rails.
    inner = round(min(4.0, GRIP_EDGE + (count - 1) * 0.65), 4)
    outer = round(inner - depth, 4)
    # Preserve the central grip and extend the shaft OUTWARD. Every plate
    # keeps the same thickness, regardless of how many are on the dumbbell.
    elements = [
        cube([inner, 7, 7], [16 - inner, 9, 9], 'grip', ('west', 'east')),
        cube([outer, 7, 7], [inner, 9, 9], 'metal', ('west', 'east')),
        cube([16 - inner, 7, 7], [round(16 - outer, 4), 9, 9], 'metal', ('west', 'east'))
    ]
    for plate in range(count):
        x0 = round(outer + plate * (DISC_THICKNESS + DISC_GAP), 4)
        x1 = round(x0 + DISC_THICKNESS, 4)
        # A slightly smaller outside disc reveals the rim of the next plate,
        # while at least one full-size disc still rests on the rack's shelf.
        radius = round(half_height * (0.92 if count > 1 and plate == 0 else 1.0), 4)
        elements.extend(plate_disc(x0, x1, radius, 'west' if plate == 0 else None))
        elements.extend(plate_disc(round(16 - x1, 4), round(16 - x0, 4), radius,
                                   'east' if plate == 0 else None))
    elements.extend([
        cube([inner, 6.8, 6.8], [round(inner + 0.18, 4), 9.2, 9.2], 'metal', ('west',)),
        cube([round(16 - inner - 0.18, 4), 6.8, 6.8], [round(16 - inner, 4), 9.2, 9.2], 'metal', ('east',)),
        cube([round(outer - 0.2, 4), 7.25, 7.25], [round(outer + 0.001, 4), 8.75, 8.75], 'metal', ('east',)),
        cube([round(16 - outer - 0.001, 4), 7.25, 7.25], [round(16 - outer + 0.2, 4), 8.75, 8.75], 'metal', ('west',))
    ])
    transforms = copy.deepcopy(display)
    # The inventory has a fixed 16-pixel cell. Fit the pair icon there only;
    # hand, ground and rack models retain their physical weight-dependent size.
    length = 16 - 2 * (outer - 0.2)
    diameter = half_height * 2
    angle = math.radians(25)
    span_x = (length + diameter * (math.sin(angle) + math.cos(angle))) / math.sqrt(2)
    span_y = diameter * math.cos(angle) + (length + diameter) * math.sin(angle) / math.sqrt(2)
    gui_scale = round(min(display['gui']['scale'][0], 10.24 / (0.65 * span_x),
                          8.96 / (0.65 * span_y)), 4)
    transforms['gui']['scale'] = [gui_scale] * 3
    return {
        'ambientocclusion': False,
        'textures': {'metal': 'minecraft:block/iron_block',
                     'grip': 'hardgym:item/dumbbell_grip',
                     'edge': 'hardgym:item/dumbbell_plate_edge',
                     'face': 'hardgym:item/dumbbell_plate_face',
                     'label': 'hardgym:item/dumbbell_label_' + str(weight) + 'kg',
                     'particle': 'minecraft:block/black_concrete'},
        'elements': elements,
        'display': transforms
    }


def write_model_dimensions(models):
    # Derive rack measurements from the actual delivered model bounds. This
    # keeps future geometry edits and the rack's placement in sync.
    heights, lengths = [], []
    for model in models:
        corners = [point for element in model['elements'] for point in (element['from'], element['to'])]
        heights.append(max(abs(point[1] - 8) for point in corners))
        lengths.append(max(abs(point[0] - 8) for point in corners))
    template = '''package com.hardgym.item;

/** Generated from the paired-dumbbell models by tools/generate_dumbbell_pairs.py. */
public final class DumbbellModelDimensions {
    private static final float[] HALF_HEIGHTS = {@HEIGHTS@};
    private static final float[] HALF_LENGTHS = {@LENGTHS@};

    private DumbbellModelDimensions() { }

    private static int index(int weight) {
        return Math.max(0, Math.min(HALF_HEIGHTS.length - 1, weight / 10 - 1));
    }

    public static float halfHeight(int weight) {
        return HALF_HEIGHTS[index(weight)];
    }

    public static float halfLength(int weight) {
        return HALF_LENGTHS[index(weight)];
    }
}
'''
    template = template.replace('@HEIGHTS@', ', '.join(f'{value:g}F' for value in heights))
    template = template.replace('@LENGTHS@', ', '.join(f'{value:g}F' for value in lengths))
    path = ROOT / 'src/main/java/com/hardgym/item/DumbbellModelDimensions.java'
    path.write_text(template, encoding='utf-8')


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


def recipe(name, data, unlock):
    write_json(RESOURCES / 'data/hardgym/recipes' / (name + '.json'), data)
    recipe_id = 'hardgym:' + name
    write_json(RESOURCES / 'data/hardgym/advancements/recipes' / (name + '.json'), {
        'parent': 'minecraft:recipes/root',
        'criteria': {
            'has_ingredient': {'trigger': 'minecraft:inventory_changed',
                               'conditions': {'items': [{'item': unlock} if isinstance(unlock, str) else unlock]}},
            'has_the_recipe': {'trigger': 'minecraft:recipe_unlocked',
                               'conditions': {'recipe': recipe_id}}
        },
        'requirements': [['has_ingredient', 'has_the_recipe']],
        'rewards': {'recipes': [recipe_id]}
    })


def main():
    make_shared_textures()
    models = []
    for index, weight in enumerate(WEIGHTS):
        name = 'dumbbell_pair_' + str(weight) + 'kg'
        # One physical model per hand/position. ItemRendererMixin draws two in GUI.
        model = make_model(weight, index, DISPLAY)
        models.append(model)
        write_json(RESOURCES / 'assets/hardgym/models/item' / (name + '.json'), model)
        previous = 'hardgym:dumbbell_pair_' + str(weight - 10) + 'kg'
        key = {'I': {'item': 'minecraft:iron_ingot'}}
        if weight == 10:
            pattern = ['III', 'S S', 'III']
            key['S'] = {'item': 'minecraft:stick'}
        else:
            pattern = ['I I', ' D ', 'I I']
            key['D'] = {'item': previous}
        recipe(name, {'type': 'minecraft:crafting_shaped', 'pattern': pattern, 'key': key,
                      'result': {'item': 'hardgym:' + name, 'count': 1}},
               'minecraft:iron_ingot' if weight == 10 else previous)

    write_model_dimensions(models)
    recipe('green_dye_from_leaves', {
        'type': 'minecraft:crafting_shapeless',
        'ingredients': [{'tag': 'minecraft:leaves'} for _ in range(4)],
        'result': {'item': 'minecraft:green_dye', 'count': 1}
    }, {'tag': 'minecraft:leaves'})

    translations = {
        'ru_ru': {
            'message.hardgym.rack_dumbbells_only': 'Соседние стойки одного направления объединяются. ПКМ пустой рукой по любой гантели — забрать пару; Shift + ПКМ — забрать всё из секции.',
            'message.hardgym.rack_empty_place': 'Это место пустое. ПКМ парой гантелей или блинов — положить на подходящую стойку.'
        },
        'en_us': {
            'message.hardgym.rack_dumbbells_only': 'Adjacent racks facing the same way connect. Empty-hand right-click on either dumbbell takes its pair; sneak-right-click empties the connected row.',
            'message.hardgym.rack_empty_place': 'This position is empty. Right-click with a dumbbell pair or plate pair to store it on its rack.'
        }
    }
    for locale, additions in translations.items():
        path = RESOURCES / 'assets/hardgym/lang' / (locale + '.json')
        values = json.loads(path.read_text(encoding='utf-8'))
        values.update(additions)
        for weight in WEIGHTS:
            values['item.hardgym.dumbbell_pair_' + str(weight) + 'kg'] = (
                'Пара гантелей по ' + str(weight) + ' кг' if locale == 'ru_ru'
                else 'Dumbbell Pair (' + str(weight) + ' kg each)')
        write_json(path, values)


if __name__ == '__main__':
    main()

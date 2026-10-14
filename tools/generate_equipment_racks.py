"""Generate matching rack models and Java layout. Resources are committed for normal builds."""
from pathlib import Path
import json
import math
from copy import deepcopy
from itertools import product

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/hardgym'
DATA = ROOT / 'src/main/resources/data/hardgym'
JAVA = ROOT / 'src/main/java/com/hardgym/block'
FACES = ('north', 'south', 'east', 'west', 'up', 'down')
COLORS = ('green', 'yellow', 'blue', 'red', 'black')

# Model pixels; the seventh value selects metal (0) or rubber (1).
DUMBBELL_FRAME = [
    [0.5, 0, 1.5, 2.5, 1.25, 15.5, 0], [13.5, 0, 1.5, 15.5, 1.25, 15.5, 0],
    [0.25, 0, 1, 2.75, 1.5, 2.5, 1], [0.25, 0, 14, 2.75, 1.5, 16, 1],
    [13.25, 0, 1, 15.75, 1.5, 2.5, 1], [13.25, 0, 14, 15.75, 1.5, 16, 1],
    [0.65, 3.2, 1.7, 2.1, 4, 9.3, 0], [13.9, 3.2, 1.7, 15.35, 4, 9.3, 0],
    [0.65, 10.2, 4.7, 2.1, 11, 13.6, 0], [13.9, 10.2, 4.7, 15.35, 11, 13.6, 0],
    [0.65, 4, 1.7, 15.35, 4.7, 3.3, 0], [0.65, 4.7, 1.7, 15.35, 5, 3.3, 1],
    [0.65, 4, 7.7, 15.35, 4.7, 9.3, 0], [0.65, 4.7, 7.7, 15.35, 5, 9.3, 1],
    [0.65, 11, 4.7, 15.35, 11.7, 6.3, 0], [0.65, 11.7, 4.7, 15.35, 12, 6.3, 1],
    [0.65, 11, 10.7, 15.35, 11.7, 12.3, 0], [0.65, 11.7, 10.7, 15.35, 12, 12.3, 1],
]
DUMBBELL_ROTATED = [
    {'bounds': [0.3, 1.25, 8.05, 0.95, 15.65, 9.45, 0],
     'rotation': {'origin': [0.625, 1.25, 8.75], 'axis': 'x', 'angle': 22.5}},
    {'bounds': [15.05, 1.25, 8.05, 15.7, 15.65, 9.45, 0],
     'rotation': {'origin': [15.375, 1.25, 8.75], 'axis': 'x', 'angle': 22.5}},
]
PLATE_FRAME = [
    [3, 0, 4, 5, 1.25, 15, 0], [11, 0, 4, 13, 1.25, 15, 0],
    [2.75, 0, 3.5, 5.25, 1.5, 5, 1], [2.75, 0, 14, 5.25, 1.5, 15.5, 1],
    [10.75, 0, 3.5, 13.25, 1.5, 5, 1], [10.75, 0, 14, 13.25, 1.5, 15.5, 1],
    [5, 6, 9.8, 11, 7, 11.2, 0],
    [6.75, 11.9, 9.25, 9.25, 12.55, 11.75, 1],
    [1.8, 5.05, 10.05, 5.8, 5.95, 10.95, 0],
    [10.2, 5.05, 10.05, 14.2, 5.95, 10.95, 0],
    [7.55, 6.05, 5.1, 8.45, 6.95, 10.5, 0],
    [7.55, 6.05, 10.5, 8.45, 6.95, 16, 0],
    [7.55, 12.4, 10.05, 8.45, 16, 10.95, 0],
    [5.7, 4.75, 9.75, 6, 6.25, 11.25, 1],
    [10, 4.75, 9.75, 10.3, 6.25, 11.25, 1],
    [7.25, 5.75, 9.2, 8.75, 7.25, 9.5, 1],
    [7.25, 5.75, 11.4, 8.75, 7.25, 11.7, 1],
]
PLATE_ROTATED = [
    {'bounds': [3.3, 1.25, 9.8, 4.7, 12.65, 11.2, 0],
     'rotation': {'origin': [4, 1.25, 10.5], 'axis': 'z', 'angle': -22.5}},
    {'bounds': [11.3, 1.25, 9.8, 12.7, 12.65, 11.2, 0],
     'rotation': {'origin': [12, 1.25, 10.5], 'axis': 'z', 'angle': 22.5}},
]
DUMBBELL_SLOTS = [(3, 6.5, 5.5), (8, 6.5, 5.5), (13, 6.5, 5.5),
                  (3, 13.5, 8.5), (8, 13.5, 8.5), (13, 13.5, 8.5)]
# First disk origin; axis and sign point away from the frame. Each item renders two disks.
PLATE_ANCHORS = [(5.5, 5.5, 10.5), (10.5, 5.5, 10.5), (8, 6.5, 8.9),
                 (8, 6.5, 13), (8, 12.7, 10.5)]
PLATE_AXES = [(0, -1), (0, 1), (2, -1), (2, 1), (1, 1)]
PLATE_SPACING = .19
PLATE_THICKNESS = .13
PLATE_SLOTS = []
for anchor, (axis, sign) in zip(PLATE_ANCHORS, PLATE_AXES):
    center = list(anchor)
    center[axis] += sign * (PLATE_SPACING * 15 + PLATE_THICKNESS) / 2
    PLATE_SLOTS.append(center)


def rotated_points(bounds, rotation):
    origin = rotation['origin']
    angle = math.radians(rotation['angle'])
    axis = 'xyz'.index(rotation['axis'])
    u, v = (axis + 1) % 3, (axis + 2) % 3
    points = []
    for values in product(*zip(bounds[:3], bounds[3:6])):
        point = list(values)
        a, b = point[u] - origin[u], point[v] - origin[v]
        point[u] = origin[u] + a * math.cos(angle) - b * math.sin(angle)
        point[v] = origin[v] + a * math.sin(angle) + b * math.cos(angle)
        points.append(point)
    return points


def collision_frame(frame, tilted):
    result = list(frame)
    for piece in tilted:
        bounds = piece['bounds']
        # Thin horizontal slices follow the sloping uprights instead of filling their whole bounding box.
        count = math.ceil((bounds[4] - bounds[1]) * 2)
        for segment in range(count):
            part = list(bounds)
            part[1] = bounds[1] + (bounds[4] - bounds[1]) * segment / count
            part[4] = bounds[1] + (bounds[4] - bounds[1]) * (segment + 1) / count
            points = rotated_points(part, piece['rotation'])
            result.append([round(min(p[i] for p in points), 5) for i in range(3)]
                          + [round(max(p[i] for p in points), 5) for i in range(3)] + [bounds[6]])
    return result


def dumbbell_section(connections):
    """Continuous rails; two half supports form one post at each shared boundary."""
    frame, tilted = deepcopy(DUMBBELL_FRAME), deepcopy(DUMBBELL_ROTATED)
    for side, flag in enumerate((1, 2)):
        if not connections & flag:
            continue
        shift = -.625 if side == 0 else .625
        for index in ([0, 2, 3, 6, 8] if side == 0 else [1, 4, 5, 7, 9]):
            frame[index][0] = max(0, frame[index][0] + shift)
            frame[index][3] = min(16, frame[index][3] + shift)
        piece = tilted[side]
        piece['bounds'][0] = max(0, piece['bounds'][0] + shift)
        piece['bounds'][3] = min(16, piece['bounds'][3] + shift)
        piece['rotation']['origin'][0] += shift
        for beam in frame[10:]:
            beam[0 if side == 0 else 3] = 0 if side == 0 else 16
    return frame, tilted


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


def element(bounds, texture):
    return {'from': bounds[:3], 'to': bounds[3:6],
            'faces': {side: {'texture': texture, 'uv': [0, 0, 16, 16]} for side in FACES}}


def java_array(values):
    return '{\n' + ',\n'.join('        {' + ', '.join(f'{n:g}F' for n in row) + '}'
                              for row in values) + '\n    }'


def generate_layout():
    template = '''package com.hardgym.block;

import com.hardgym.item.DumbbellModelDimensions;

/** Generated by tools/generate_equipment_racks.py; coordinates use model pixels. */
public final class EquipmentRackLayout {
    public static final int PAIRS_PER_PEG = 8;
    public static final float DUMBBELL_SCALE = 0.45F;
    private static final int[] WEIGHTS = {10, 15, 20, 25, 50};
    private static final float[][] DUMBBELL_FRAME = @DUMBBELL_FRAME@;
    private static final float[][][] DUMBBELL_FRAMES = @DUMBBELL_FRAMES@;
    private static final float[][] PLATE_FRAME = @PLATE_FRAME@;
    private static final float[][] DUMBBELL_SLOTS = @DUMBBELL_SLOTS@;
    private static final float[][] PLATE_SLOTS = @PLATE_SLOTS@;
    private static final float[][] PLATE_ANCHORS = @PLATE_ANCHORS@;
    private static final float[][] PLATE_AXES = @PLATE_AXES@;

    private EquipmentRackLayout() { }

    public static float[][] frameBounds(boolean plates) {
        return plates ? PLATE_FRAME : DUMBBELL_FRAME;
    }

    public static float[][] frameBounds(boolean plates, int connections) {
        return plates ? PLATE_FRAME : DUMBBELL_FRAMES[connections & 3];
    }

    public static float slotX(boolean plates, int slot) {
        return (plates ? PLATE_SLOTS : DUMBBELL_SLOTS)[slot][0];
    }

    public static float slotY(boolean plates, int slot) {
        return (plates ? PLATE_SLOTS : DUMBBELL_SLOTS)[slot][1];
    }

    public static float slotZ(boolean plates, int slot) {
        return (plates ? PLATE_SLOTS : DUMBBELL_SLOTS)[slot][2];
    }

    public static int nearestSlot(boolean plates, double x, double y, double z) {
        float[][] slots = plates ? PLATE_SLOTS : DUMBBELL_SLOTS;
        int nearest = 0;
        double distance = Double.POSITIVE_INFINITY;
        for (int slot = 0; slot < slots.length; slot++) {
            double dx = x - slots[slot][0], dy = y - slots[slot][1];
            double dz = z - slots[slot][2];
            double next = dx * dx + dy * dy + dz * dz;
            if (next < distance) { nearest = slot; distance = next; }
        }
        return nearest;
    }

    public static int plateWeight(int slot) {
        return WEIGHTS[slot];
    }

    public static int plateSlot(int weight) {
        for (int slot = 0; slot < WEIGHTS.length; slot++) if (WEIGHTS[slot] == weight) return slot;
        return -1;
    }

    public static float dumbbellCenterY(int slot, int weight) {
        float halfHeight = DumbbellModelDimensions.halfHeight(weight) * DUMBBELL_SCALE;
        return (slot < 3 ? 5.0F : 12.0F) + halfHeight;
    }

    public static float dumbbellCenterZ(int slot, int weight) {
        float halfLength = DumbbellModelDimensions.halfLength(weight) * DUMBBELL_SCALE;
        float minimum = halfLength + 0.15F;
        float maximum = 16.0F - minimum;
        return Math.max(minimum, Math.min(maximum, slotZ(false, slot)));
    }

    /** Two visible plates per pair, with a hole for the storage peg. */
    public static float[][] plateBounds(int slot, int plateIndex) {
        if (slot < 0 || slot >= 5 || plateIndex < 0 || plateIndex >= PAIRS_PER_PEG * 2)
            throw new IllegalArgumentException("Invalid rack plate");
        float radius = 2.0F + slot * 0.25F;
        float hole = 0.65F;
        float depth = plateIndex * @PLATE_SPACING@;
        float[][] ring = {
                {-radius, -radius, depth, radius, -hole, depth + @PLATE_THICKNESS@},
                {-radius, hole, depth, radius, radius, depth + @PLATE_THICKNESS@},
                {-radius, -hole, depth, -hole, hole, depth + @PLATE_THICKNESS@},
                {hole, -hole, depth, radius, hole, depth + @PLATE_THICKNESS@}
        };
        for (int i = 0; i < ring.length; i++) ring[i] = plateTransform(slot, ring[i]);
        return ring;
    }

    private static float[] plateTransform(int slot, float[] ring) {
        int axis = (int) PLATE_AXES[slot][0];
        int u = (axis + 1) % 3, v = (axis + 2) % 3;
        float sign = PLATE_AXES[slot][1];
        float[] anchor = PLATE_ANCHORS[slot];
        float[] result = new float[6];
        result[u] = anchor[u] + ring[0]; result[u + 3] = anchor[u] + ring[3];
        result[v] = anchor[v] + ring[1]; result[v + 3] = anchor[v] + ring[4];
        float first = anchor[axis] + sign * ring[2], last = anchor[axis] + sign * ring[5];
        result[axis] = Math.min(first, last); result[axis + 3] = Math.max(first, last);
        return result;
    }

    /** Separate click regions keep front/back and left/right holders selectable. */
    public static float[][] selectionBounds(boolean plates) {
        float[][] result = new float[plates ? 5 : 6][];
        for (int slot = 0; slot < result.length; slot++) {
            if (plates) {
                float radius = 2.0F + slot * 0.25F;
                result[slot] = plateTransform(slot, new float[]{-radius, -radius, 0,
                        radius, radius, (PAIRS_PER_PEG * 2 - 1) * @PLATE_SPACING@ + @PLATE_THICKNESS@});
            } else {
                float x = slotX(false, slot), z = slotZ(false, slot), y = slot < 3 ? 5 : 12;
                result[slot] = new float[]{x - 1.6F, y, z - 3.6F, x + 1.6F, y + 3.2F, z + 3.6F};
            }
        }
        return result;
    }
}
'''
    for key, values in [('DUMBBELL_FRAME', collision_frame(DUMBBELL_FRAME, DUMBBELL_ROTATED)),
                        ('PLATE_FRAME', collision_frame(PLATE_FRAME, PLATE_ROTATED)),
                        ('DUMBBELL_SLOTS', DUMBBELL_SLOTS), ('PLATE_SLOTS', PLATE_SLOTS),
                        ('PLATE_ANCHORS', PLATE_ANCHORS), ('PLATE_AXES', PLATE_AXES)]:
        template = template.replace('@' + key + '@', java_array(values))
    template = template.replace('@PLATE_SPACING@', f'{PLATE_SPACING:g}F')
    template = template.replace('@PLATE_THICKNESS@', f'{PLATE_THICKNESS:g}F')
    template = template.replace('@DUMBBELL_FRAMES@', '{\n        '
                                + ',\n        '.join(java_array(collision_frame(*dumbbell_section(mask)))
                                                     for mask in range(4)) + '\n    }')
    (JAVA / 'EquipmentRackLayout.java').write_text(template, encoding='utf-8')


def main():
    generate_layout()
    for name, frame in [('dumbbell_rack', DUMBBELL_FRAME), ('plate_rack', PLATE_FRAME)]:
        plates = name == 'plate_rack'
        elements = [element(bounds, '#rubber' if bounds[6] else '#metal') for bounds in frame]
        for piece in PLATE_ROTATED if plates else DUMBBELL_ROTATED:
            tilted = element(piece['bounds'], '#metal')
            tilted['rotation'] = piece['rotation']
            elements.append(tilted)
        textures = {'metal': 'hardgym:block/bench_metal', 'rubber': 'hardgym:block/bench_pad',
                    'particle': 'hardgym:block/bench_metal'}
        if plates:
            for slot, anchor in enumerate(PLATE_ANCHORS):
                textures[COLORS[slot]] = 'hardgym:block/plate_' + COLORS[slot]
                axis, sign = PLATE_AXES[slot]
                low, high = [c - .85 for c in anchor], [c + .85 for c in anchor]
                a, b = anchor[axis] - sign * .25, anchor[axis] - sign * .05
                low[axis], high[axis] = min(a, b), max(a, b)
                elements.append(element(low + high, '#' + COLORS[slot]))
        write(ASSETS / ('models/block/' + name + '.json'), {
            'ambientocclusion': False, 'textures': textures, 'elements': elements,
            'display': {
                'gui': {'rotation': [25, 225, 0], 'translation': [0, 0, 0],
                        'scale': [.72 if plates else .8] * 3},
                'ground': {'translation': [0, 3, 0], 'scale': [.4] * 3},
                'fixed': {'rotation': [0, 180, 0], 'scale': [.5] * 3},
                'thirdperson_righthand': {'rotation': [75, 45, 0], 'scale': [.3] * 3},
                'thirdperson_lefthand': {'rotation': [75, -45, 0], 'scale': [.3] * 3},
                'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, 0, 0], 'scale': [.35] * 3},
                'firstperson_lefthand': {'rotation': [0, -45, 0], 'translation': [0, 0, 0], 'scale': [.35] * 3},
            }})
        write(ASSETS / ('models/item/' + name + '.json'), {'parent': 'hardgym:block/' + name})
        variants = {}
        for connections in range(1 if plates else 4):
            model = name if connections == 0 else name + '_connected_' + str(connections)
            if connections:
                connected_frame, connected_tilted = dumbbell_section(connections)
                parts = [element(bounds, '#rubber' if bounds[6] else '#metal') for bounds in connected_frame]
                for piece in connected_tilted:
                    part = element(piece['bounds'], '#metal')
                    part['rotation'] = piece['rotation']
                    parts.append(part)
                write(ASSETS / ('models/block/' + model + '.json'), {
                    'ambientocclusion': False, 'textures': textures, 'elements': parts})
            for facing, rotation in [('north', 0), ('east', 90), ('south', 180), ('west', 270)]:
                key = 'facing=' + facing
                if not plates:
                    key += ',left=' + str(bool(connections & 1)).lower()
                    key += ',right=' + str(bool(connections & 2)).lower()
                variants[key] = {'model': 'hardgym:block/' + model, 'y': rotation}
        write(ASSETS / ('blockstates/' + name + '.json'), {'variants': variants})
        write(DATA / ('loot_tables/blocks/' + name + '.json'), {
            'type': 'minecraft:block', 'pools': [{'rolls': 1,
                'entries': [{'type': 'minecraft:item', 'name': 'hardgym:' + name}],
                'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
        pattern = [' I ', 'BIB', 'III'] if plates else ['I I', 'BBB', 'III']
        write(DATA / ('recipes/' + name + '.json'), {
            'type': 'minecraft:crafting_shaped', 'group': 'hardgym:equipment_racks', 'pattern': pattern,
            'key': {'I': {'item': 'minecraft:iron_ingot'}, 'B': {'item': 'minecraft:iron_bars'}},
            'result': {'item': 'hardgym:' + name, 'count': 1}})
        write(DATA / ('advancements/recipes/' + name + '.json'), {
            'parent': 'minecraft:recipes/root', 'criteria': {
                'has_ingredient': {'trigger': 'minecraft:inventory_changed',
                    'conditions': {'items': [{'item': 'minecraft:iron_ingot'}]}},
                'has_the_recipe': {'trigger': 'minecraft:recipe_unlocked',
                    'conditions': {'recipe': 'hardgym:' + name}}},
            'requirements': [['has_ingredient', 'has_the_recipe']],
            'rewards': {'recipes': ['hardgym:' + name]}})

    translations = {
        'ru_ru': {
            'block.hardgym.dumbbell_rack': 'Стойка для гантелей',
            'block.hardgym.plate_rack': 'Стойка для блинов',
            'message.hardgym.rack_plates_only': 'Эта стойка хранит пары блинов. ПКМ пустой рукой — забрать, Shift + ПКМ — забрать всё.',
            'message.hardgym.rack_dumbbells_only': 'Соседние стойки одного направления объединяются. ПКМ пустой рукой по любой гантели — забрать пару; Shift + ПКМ — забрать всё из секции.',
            'message.hardgym.rack_full': 'Для этого предмета на стойке нет свободного места.',
            'message.hardgym.rack_stored': 'Положено: %s. Занято мест: %s/%s.',
            'message.hardgym.rack_taken_all': 'Со стойки снято предметов: %s.',
            'message.hardgym.rack_empty_place': 'Это место пустое. ПКМ парой гантелей или блинов — положить на подходящую стойку.',
            'message.hardgym.rack_taken': 'Взято со стойки: %s.',
        },
        'en_us': {
            'block.hardgym.dumbbell_rack': 'Dumbbell Rack',
            'block.hardgym.plate_rack': 'Plate Storage Rack',
            'message.hardgym.rack_plates_only': 'This rack stores plate pairs. Empty-hand right-click takes one; sneak-right-click takes all.',
            'message.hardgym.rack_dumbbells_only': 'Adjacent racks facing the same way connect. Empty-hand right-click on either dumbbell takes its pair; sneak-right-click empties the connected row.',
            'message.hardgym.rack_full': 'There is no free space for this item on the rack.',
            'message.hardgym.rack_stored': 'Stored: %s. Occupied positions: %s/%s.',
            'message.hardgym.rack_taken_all': 'Items removed from rack: %s.',
            'message.hardgym.rack_empty_place': 'This position is empty. Right-click with a dumbbell pair or plate pair to store it on its rack.',
            'message.hardgym.rack_taken': 'Taken from rack: %s.',
        },
    }
    for language, additions in translations.items():
        path = ASSETS / ('lang/' + language + '.json')
        data = json.loads(path.read_text(encoding='utf-8'))
        data.update(additions)
        write(path, data)
    print('Generated two rack models, matching geometry, crafts, loot, recipe unlocks and translations.')


if __name__ == '__main__':
    main()

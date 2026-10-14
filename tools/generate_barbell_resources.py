"""Generate plate-pair crafts and station frames. Committed resources need no Python at build time."""
from pathlib import Path
import copy
import json
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/hardgym'
DATA = ROOT / 'src/main/resources/data/hardgym'
JAVA = ROOT / 'src/main/java/com/hardgym'
COLORS = {10: 'green', 15: 'yellow', 20: 'blue', 25: 'red', 50: 'black'}
RUSSIAN = {10: 'зелёные', 15: 'жёлтые', 20: 'синие', 25: 'красные', 50: 'чёрные'}


def read(path):
    return json.loads(path.read_text(encoding='utf-8'))


def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


def cuboid(bounds, texture):
    return {'from': bounds[:3], 'to': bounds[3:],
            'faces': {face: {'texture': texture, 'uv': [0, 0, 16, 16]}
                      for face in ('north', 'south', 'east', 'west', 'up', 'down')}}


def empty_bar_bounds():
    # Compile the actual Java geometry rather than maintaining duplicate anchors here.
    with tempfile.TemporaryDirectory(prefix='hardgym-bars-') as folder:
        temp = Path(folder)
        program = temp / 'EmptyBars.java'
        program.write_text('''
import com.hardgym.client.BarbellGeometry;
import java.util.Locale;
public class EmptyBars {
    public static void main(String[] args) {
        for (int capacity : new int[]{5, 7, 9}) {
            System.out.print(capacity);
            for (float bound : BarbellGeometry.barBounds(capacity, 0, false, 0))
                System.out.printf(Locale.ROOT, " %.8f", bound);
            System.out.println();
        }
    }
}
''')
        compiler = [shutil.which('javac')] if shutil.which('javac') else [
            'java', '--module', 'jdk.compiler/com.sun.tools.javac.Main']
        subprocess.run(compiler + ['-source', '8', '-target', '8', '-Xlint:-options', '-d', folder,
                                   str(JAVA / 'animation/ExerciseAnimation.java'),
                                   str(JAVA / 'client/BenchPressArmPose.java'),
                                   str(JAVA / 'client/StandingBarbellPose.java'),
                                   str(JAVA / 'client/BarbellGeometry.java'), str(program)], check=True)
        output = subprocess.check_output(['java', '-cp', folder, 'EmptyBars'], text=True)
    return {int(line.split()[0]): [float(n) for n in line.split()[1:]]
            for line in output.splitlines()}


def main():
    bounds = empty_bar_bounds()
    for name, capacity, frame_count in [('bench_press_station', 5, 9),
                                         ('squat_rack', 7, 9), ('deadlift_bar', 9, 0)]:
        template = read(ASSETS / ('models/block/' + name + '_40_a0.json'))
        frame = copy.deepcopy(template)
        frame['elements'] = frame['elements'][:frame_count]
        write(ASSETS / ('models/block/' + name + '_frame.json'), frame)
        empty = copy.deepcopy(frame)
        empty['elements'].append(cuboid(bounds[capacity], '#bar'))
        write(ASSETS / ('models/block/' + name + '_20.json'), empty)
        write(ASSETS / ('models/item/' + name + '.json'),
              {'parent': 'hardgym:block/' + name + '_20'})
        # ANIM/ACTIVE are read by the block entity renderer; weight is a legacy property.
        write(ASSETS / ('blockstates/' + name + '.json'),
              {'variants': {'facing=' + facing: {'model': 'hardgym:block/' + name + '_frame', 'y': rotation}
                            for facing, rotation in [('north', 0), ('east', 90), ('south', 180), ('west', 270)]}})

    languages = {lang: read(ASSETS / ('lang/' + lang + '.json')) for lang in ('ru_ru', 'en_us')}
    patterns = {10: [' I ', 'IDI', ' I '], 15: ['III', 'IDI', ' I '],
                20: ['III', 'IDI', 'III'], 25: [' I ', 'BDB', ' I '],
                50: ['BIB', 'IDI', 'BIB']}
    for weight, color in COLORS.items():
        name = 'plate_pair_' + str(weight) + 'kg'
        identifier = 'hardgym:' + name
        display = {
            'gui': {'rotation': [30, 225, 0], 'translation': [0, 0, 0], 'scale': [.85, .85, .85]},
            'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [.5, .5, .5]},
            'fixed': {'rotation': [0, 90, 0], 'translation': [0, 0, 0], 'scale': [.75, .75, .75]},
            'thirdperson_righthand': {'rotation': [0, 90, 0], 'translation': [0, 2, 0], 'scale': [.55, .55, .55]},
            'thirdperson_lefthand': {'rotation': [0, 90, 0], 'translation': [0, 2, 0], 'scale': [.55, .55, .55]},
            'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, 1, 0], 'scale': [.65, .65, .65]},
            'firstperson_lefthand': {'rotation': [0, 225, 0], 'translation': [0, 1, 0], 'scale': [.65, .65, .65]},
        }
        write(ASSETS / ('models/item/' + name + '.json'),
              {'textures': {'particle': 'hardgym:block/plate_' + color, 'plate': 'hardgym:block/plate_' + color},
               'elements': [cuboid([3, 4, 4, 4.1, 12, 12], '#plate'),
                            cuboid([11.9, 4, 4, 13, 12, 12], '#plate')], 'display': display})
        keys = {'I': {'item': 'minecraft:iron_ingot'}, 'D': {'item': 'minecraft:' + color + '_dye'}}
        if weight >= 25:
            keys['B'] = {'item': 'minecraft:iron_block'}
        write(DATA / ('recipes/' + name + '.json'),
              {'type': 'minecraft:crafting_shaped', 'group': 'hardgym:plate_pairs',
               'pattern': patterns[weight], 'key': keys, 'result': {'item': identifier, 'count': 1}})
        write(DATA / ('advancements/recipes/' + name + '.json'),
              {'parent': 'minecraft:recipes/root',
               'criteria': {'has_ingredient': {'trigger': 'minecraft:inventory_changed',
                                               'conditions': {'items': [{'item': 'minecraft:iron_ingot'}]}},
                            'has_the_recipe': {'trigger': 'minecraft:recipe_unlocked',
                                               'conditions': {'recipe': identifier}}},
               'requirements': [['has_ingredient', 'has_the_recipe']], 'rewards': {'recipes': [identifier]}})
        languages['ru_ru']['item.hardgym.' + name] = 'Пара блинов по ' + str(weight) + ' кг (' + RUSSIAN[weight] + ')'
        languages['en_us']['item.hardgym.' + name] = color.title() + ' Plate Pair (' + str(weight) + ' kg each)'
    languages['ru_ru']['tooltip.hardgym.plate_pair'] = '2 × %s кг: добавляет %s кг к штанге.'
    languages['ru_ru']['tooltip.hardgym.plate_pair_use'] = 'ПКМ по тренажёру — установить пару.'
    languages['en_us']['tooltip.hardgym.plate_pair'] = '2 × %s kg: adds %s kg to the bar.'
    languages['en_us']['tooltip.hardgym.plate_pair_use'] = 'Right-click a station to add this pair.'
    for language, entries in languages.items():
        write(ASSETS / ('lang/' + language + '.json'), entries)
    subprocess.run(['python3', str(ROOT / 'tools/generate_colored_benches.py')], check=True)
    print('Generated 5 plate pairs, 18 empty stations and synchronized bar frames.')


if __name__ == '__main__':
    main()

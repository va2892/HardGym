"""Regenerate active bench bar models from BenchPressArmPose; needs Python and a JDK.
The generated JSON files are committed, so normal Gradle builds need no Python.
"""
from pathlib import Path
import copy
import json
import re
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/hardgym'
POSE = ROOT / 'src/main/java/com/hardgym/client/BenchPressArmPose.java'
MODELS = ASSETS / 'models/block'
STATES = ASSETS / 'blockstates/bench_press_station.json'


def grip_centres():
    with tempfile.TemporaryDirectory(prefix='hardgym-grip-') as directory:
        temp = Path(directory)
        java_source = temp / 'GripCentres.java'
        java_source.write_text('''
import com.hardgym.client.BenchPressArmPose;
import java.util.Locale;
public final class GripCentres {
 public static void main(String[] args) {
  for (int phase=0; phase<3; phase++) {
   float[] point=BenchPressArmPose.barCenterBlockModel(phase);
   System.out.printf(Locale.ROOT, "%d %.8f %.8f%n", phase, point[1], point[2]);
  }
 }
}
''')
        compiler = shutil.which('javac')
        compile_command = [compiler] if compiler else ['java', '--module', 'jdk.compiler/com.sun.tools.javac.Main']
        subprocess.run(compile_command + ['-source', '8', '-target', '8', '-Xlint:-options', '-d', str(temp),
                       str(ROOT / 'src/main/java/com/hardgym/animation/ExerciseAnimation.java'),
                       str(POSE), str(java_source)], check=True)
        output = subprocess.check_output(['java', '-cp', str(temp), 'GripCentres'], text=True)
        return {int(phase): (float(y), float(z)) for phase, y, z in (line.split() for line in output.splitlines())}


def main():
    centres = grip_centres()
    count = 0
    for source in sorted(MODELS.glob('bench_press_station_*_a*.json')):
        match = re.fullmatch(r'bench_press_station_(\d+)_a([012])\.json', source.name)
        if not match:
            continue
        weight, phase = match.group(1), int(match.group(2))
        original = json.loads(source.read_text())
        held = copy.deepcopy(original)
        bar = original['elements'][9]
        assert all(face['texture'] == '#bar' for face in bar['faces'].values()), source
        bar_y = (bar['from'][1] + bar['to'][1]) * 0.5
        bar_z = (bar['from'][2] + bar['to'][2]) * 0.5
        dy = centres[phase][0] - bar_y
        dz = centres[phase][1] - bar_z
        # First nine elements are the rack/pad; the rest are the bar and plates.
        for element in held['elements'][9:]:
            for edge in ('from', 'to'):
                element[edge][1] = round(element[edge][1] + dy, 6)
                element[edge][2] = round(element[edge][2] + dz, 6)
        target = MODELS / f'bench_press_station_{weight}_held_a{phase}.json'
        target.write_text(json.dumps(held, indent=2) + '\n')
        count += 1

    states = json.loads(STATES.read_text())
    for key, variant in states['variants'].items():
        if 'active=true' not in key:
            continue
        variant['model'] = re.sub(r'_(?:held_)?a([012])$', r'_held_a\1', variant['model'])
    STATES.write_text(json.dumps(states, indent=2) + '\n')
    print(f'Generated {count} active bar models.')
    for phase, (y, z) in sorted(centres.items()):
        print(f'phase {phase}: bar centre Y={y:.6f}, Z={z:.6f}')


if __name__ == '__main__':
    main()

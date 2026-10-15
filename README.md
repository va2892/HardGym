# HardGym

HardGym добавляет в Minecraft силовые тренировки, развитие мышц и систему
восстановления. Создай свой зал, подбирай рабочий вес, выполняй подходы и
постепенно улучшай физические возможности персонажа.

## Механики мода

### Упражнения и рабочий вес

В моде есть жим лёжа, присед со штангой, становая тяга и подъём пары гантелей
молотковым хватом. Упражнения развивают соответствующие мышцы: жим - грудь и
трицепс, присед и становая - ноги, гантели - бицепс. Доступный вес и возможности
в подходе зависят от развития мышц, участвующих в конкретном упражнении.

Штанги устанавливаются с пустым грифом. Вес собирается вручную из пар блинов:
применение предмета добавляет одинаковые блины с обеих сторон. Можно смешивать
разные веса; сложность упражнения определяется общим весом штанги. Во время
подхода установленный вес сохраняется.

Чтобы начать подход со штангой, нажми ПКМ по тренажёру, затем удерживай ПКМ
для движения. В жиме и приседе отпускание кнопки позволяет удерживать текущую
позицию, продолжая тратить стамину. В становой штанга при отпускании опускается
к полу. Shift завершает подход. С парой гантелей удерживай ПКМ; для тренировки
понадобятся обе руки, поэтому освободи другую руку от предметов.

### Прокачка и физические бонусы

Завершённые повторения дают опыт мышц и упражнений. Развитие жима повышает
урон, присед улучшает бег, становая увеличивает прыжок, бицепс ускоряет добычу
блоков, а общая сила увеличивает запас здоровья. Бонусы растут постепенно по
мере тренировок.

Силовые возможности жима определяются средним уровнем груди и трицепса.
Присед и становая используют общую прокачку мышц ног, сохраняя отдельный
прогресс упражнений для бега и прыжка. Гантели зависят от уровня бицепса.
Долгое пребывание в игре без успешных силовых повторений постепенно снижает
общую силу, поэтому регулярные тренировки помогают поддерживать форму.

### Стамина, усталость и восстановление

Стамина расходуется во время работы со снарядом и восстанавливается при отдыхе.
Тяжёлый вес, сложная фаза движения и удержание штанги требуют больше сил.
Тренировки также расходуют голод и накапливают FAT - общую усталость,
которая сохраняется между подходами и ограничивает дальнейшую нагрузку.

Чтобы быстрее восстановить стамину, можно выпить обычную бутылочку воды.
Еда немного уменьшает FAT, а длительный отдых постепенно снимает усталость.
Полноценный ночной сон восстанавливает стамину полностью и заметно снижает
FAT. Для такого восстановления нужно действительно проспать до утра.
Пропущенные ночи повышают усталость, а несколько ночей подряд без сна
усиливают этот эффект.

При истощении стамины в жиме или приседе персонажа может придавить штангой,
после чего он начинает получать урон. Shift позволяет выйти из этого состояния.
В становой при неудачном подъёме штанга падает и подход заканчивается.

### Снаряды, крафты и хранение

Тренажёры, пары гантелей и пары блинов создаются через обычные крафты.
Гантели улучшаются последовательно из предыдущей пары; тяжёлые варианты
отличаются размером и количеством дисков. Цвет обивки скамьи зависит от
шерсти в рецепте, а сама обивка остаётся гладкой и однотонной.

Есть отдельные стойки для гантелей и блинов. На них видны хранящиеся снаряды,
которые можно положить и забрать. Соседние одинаково повёрнутые стойки для
гантелей соединяются в общий ряд: пара может занять места на соседних секциях.
Содержимое сохраняется при перезаходе в мир и возвращается при разрушении
стойки.

Shift + ПКМ пустой рукой по штанге снимает последнюю установленную пару блинов.
На стойке этот же жест забирает всё содержимое, а обычный ПКМ пустой рукой
позволяет взять выбранный снаряд. 

Зелёный краситель для крафтов также можно получить из листвы.

### Анимации, камера и статистика

Упражнения сопровождаются плавными движениями персонажа и штанги. При
установленных PlayerAnimator и BendyLib сгибаются локти и колени. Камера от
первого лица следует за упражнением, а от третьего лица можно свободно
осматриваться. В мультиплеере другие игроки видят подход и положение снаряда.

Тренировочный HUD показывает упражнение, рабочий вес, повторы, фазу движения,
стамину и FAT. Команда `/gymstats` выводит уровни, физические характеристики,
счётчики повторений и личные рекорды. Команда `/gymreset` сбрасывает собственный
прогресс и состояние восстановления. Прокачка сохраняется при выходе из мира
и переносится на персонажа после возрождения.

Dumbbell artwork by Digital Art Studio via Vecteezy, adapted for the HardGym icon.

# История версий

## v0.10.71 - Empty Hands and Workout Inventory Lock

- Required both hands to be empty before starting bench press, squats or deadlifts; rejected starts show a message without spending stamina.
- Kept paired dumbbell training available with the pair in one hand and the other hand empty.
- Blocked hotbar changes through number keys and scrolling, offhand swaps, item drops, inventory moves, creative item changes and pick-block during a workout.
- Temporarily blocked opening the inventory and collecting dropped items so equipment cannot appear in the hands during a set.
- Enforced the restrictions on the server and restored the authoritative inventory after rejected item-move packets.
- Preserved right-click repetitions, release-to-stop for dumbbells, Shift exit, camera controls and normal inventory use after the set.


## v0.10.70 - Workout Interaction Lock

- Blocked opening containers, using blocks, placing blocks, using other items, interacting with entities and attacking while performing any exercise.
- Applied the restriction on both client and server, including paused and pinned barbell phases and block breaking started before a set.
- Preserved exercise controls: holding right-click performs repetitions, Shift exits barbell exercises, and releasing right-click ends dumbbell training.
- Kept nearby players unaffected and restored normal interactions after the workout ends.
- Matched the mod license metadata to the existing HardGym Non-Commercial License and included LICENSE in the built JAR.


## v0.10.69 - Forge 1.20.1 Port

- Ported HardGym from Fabric 1.16.5 to Minecraft 1.20.1 Forge.
- Updated the project to Java 17, ForgeGradle and the official Mojang mappings.
- Reworked registration, networking, block-entity synchronization and client rendering for Forge.
- Preserved exercises, muscle progression, physical bonuses, recovery, equipment, recipes and saved player data.
- Retained optional elbow and knee bending through Forge PlayerAnimator + BendyLib.
- Added Forge GameTests covering bar loads, racks, player progression, paired repetitions and crafting.


## v0.10.68 - Unified Bench Level

- Made displayed bench level equal the average Chest and Triceps level, rounded down.
- Used the same level for working weights, repetitions, tempo, damage, /gymstats and client synchronization.
- Preserved legacy Bench XP in saves for compatibility while deriving the current level from trained muscles.
- Kept Chest and Triceps XP rewards unchanged.


## v0.10.67 - Recipe Conflict Fix and Legacy Dumbbell Removal

- Changed the 10 kg dumbbell-pair recipe so it no longer conflicts with vanilla rails.
- Removed legacy single 5 kg, 10 kg and 20 kg dumbbells and their associated resources.
- Added a shapeless recipe turning four blocks of any leaves into one green dye.
- Allowed mixed leaf types and crafting the dye in the inventory grid.
- Kept the existing heavier dumbbell-pair upgrade recipes.


## v0.10.66 - Limb Bending After Respawn

- Fixed lost elbow and knee bending after death and respawn.
- Rebound PlayerAnimator layers to the actual new player object instead of reusing a stale entity-ID entry.
- Handled replaced local players and remote players returning to view.
- Disabled stale layers and ended dumbbell workout state immediately after death.


## v0.10.65 - Connected Dumbbell Racks

- Joined adjacent, equally oriented dumbbell racks into continuous shelves.
- Distributed both dumbbells of a pair across the connected row before moving to another shelf.
- Moved a previously split pair onto the same shelf when a neighboring rack was added.
- Returned the whole pair when either visible dumbbell was selected.
- Preserved cross-block links, original item NBT and correct drops without duplication.


## v0.10.64 - Full-Size Heavy Dumbbells

- Stopped compressing disc stacks as dumbbell weight increases.
- Kept consistent disc thickness and increased stack length and diameter for heavier weights.
- Made heavy dumbbells visibly larger in the hands and on storage racks.
- Adjusted shelf contact and support placement to fit the larger models.


## v0.10.63 - Black Metal Dumbbell Style

- Restored the black metal dumbbell look with stacked discs, rims and metal end fittings.
- Made weight variants differ through disc stacks and subtle end-face markings.
- Kept the hammer grip and aligned handles in the hands.


## v0.10.62 - Dumbbell Start Validation and Immediate HUD

- Started dumbbell animation only after the server confirmed the workout.
- Prevented unavailable weights, insufficient stamina, excessive fatigue or an occupied second hand from starting the pose.
- Displayed the workout HUD immediately instead of waiting for the first completed repetition.
- Gave each dumbbell weight a distinct stripe, weight marking and head thickness.
- Cleared workout confirmation after releasing use, changing items or interrupting the set.


## v0.10.61 - Paired Dumbbells

- Replaced one-handed dumbbell training with a paired-dumbbell item and simultaneous curls.
- Added pairs with each dumbbell weighing 10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110 or 120 kg.
- Required the other hand to be empty for the paired exercise and displayed both dumbbells in first person.
- Counted one shared repetition and applied XP, stamina and FAT costs once per cycle.
- Made a stored pair occupy two rack positions while remaining one recoverable item.
- Added progressive recipes, translations and models for the new weights.


## v0.10.60 - Persistent Rack Contents Fix

- Fixed dumbbells and plate pairs disappearing after world reloads or chunk loading.
- Stopped validating saved items against block state before that state was available.
- Restored saved item counts, positions, names and other NBT data.
- Kept insertion validation and the existing rack save format.


## v0.10.59 - Fast Rack Outlines and Fewer Particles

- Replaced detailed rack selection outlines with a simple cached bounding outline.
- Preserved precise item selection and collision shapes.
- Limited rack-breaking effects to 32 particles instead of multiplying them by shape complexity.
- Preserved rack models, contents and dropped items.


## v0.10.58 - Rack Rendering Optimization

- Batched rack contents into shared texture-atlas buffers.
- Cached plate geometry and reusable dumbbell model faces.
- Removed unnecessary internal plate faces and redundant texture switches.
- Skipped rendering empty or off-screen rack contents while preserving special item rendering.


## v0.10.57 - Per-Player Multiplayer Workout State

- Bound squat and deadlift animations to synchronized state on the exercising player.
- Removed nearby-station radius checks that could animate passing players.
- Prevented other players’ sets from changing the local camera, hiding hands or capturing input.
- Handled late observers, death, disconnects and dimension changes correctly.


## v0.10.56 - Squat and Deadlift Bar Grip Alignment

- Made the squat bar follow shoulder movement, torso lean and leg-height compensation.
- Made the deadlift bar follow the player’s actual hand positions throughout the movement.
- Kept every installed plate attached to the moving bar.
- Adjusted the squat first-person camera together with the upper-body movement.


## v0.10.55 - Smooth Exercise Animations

- Replaced abrupt barbell phase changes with continuous movement throughout each repetition.
- Smoothed the player’s torso, arms, elbows, knees, bar and installed plates together.
- Smoothed dumbbell lifting, lowering and transitions between repetitions.
- Synchronized bar progress and interpolated first-person camera movement.
- Preserved workout timing, XP, fatigue and weight requirements.


## v0.10.54 - Full-Sleep Stamina Recovery

- Made a completed night of sleep fully restore stamina on waking.
- Preserved fatigue recovery and resetting the missed-night streak after full sleep.
- Kept entering and leaving a bed early from granting full sleep recovery.


## v0.10.53 - Gym-Style Rack Models

- Rebuilt dumbbell racks with open shelves, sloping supports and dark padded rails.
- Rebuilt plate racks as metal A-frames with dedicated side, front, rear and top holders.
- Added colored holder markers and precise selection by the aimed point.
- Kept crafting, storage capacity and item transfer behavior.


## v0.10.52 - Remove the Last Plate Pair

- Changed Shift + RMB with an empty hand on a bar to remove only its last installed plate pair.
- Applied the same last-in, first-out removal to bench, squat and deadlift.
- Returned the removed pair to the player and preserved the remaining bar load.


## v0.10.51 - Dumbbell and Plate Storage Racks

- Added craftable dumbbell racks and plate racks with visible stored equipment.
- Added shelves for six original single dumbbells and separate holders for each plate weight.
- Supported inserting equipment, selecting individual slots and taking items back.
- Preserved item NBT and synchronized rack inventories in multiplayer.
- Returned the rack contents when the block is broken.


## v0.10.50 - Hidden First-Person Workout Hands

- Hid both first-person hands and sleeves during bench press, squat and deadlift.
- Applied the change during movement, pauses and pinned-bar states.
- Restored normal hand rendering after the set while preserving third-person exercise arms.


## v0.10.49 - Yarn Vector Compile Fix

- Replaced the unavailable Vector3f import with Vec3f in the barbell renderer.
- Restored compilation with the project’s Minecraft 1.16.5 Yarn mappings.


## v0.10.48 - Strength Caps Matched to Level 40

- Mapped maximum bench, squat and deadlift working weights to the relevant muscle level 40.
- Kept the old lower-weight requirements and extended progression above them.
- Updated /gymstats to account for actual plate combinations and bar capacity.
- Preserved the existing XP formulas and rewards.


## v0.10.47 - Manual Plate Loading and Heavy Bars

- Made every exercise station start with an empty 20 kg bar.
- Added plate-pair items: 10 kg green, 15 kg yellow, 20 kg blue, 25 kg red and 50 kg black.
- Made one plate-pair item add one matching plate to each side of the bar.
- Calculated workout difficulty from total weight, regardless of the plate combination.
- Added 5 plate slots per side for bench, 7 for squat and 9 for deadlift; extended the deadlift bar.
- Raised equipment limits to 520 kg, 720 kg and 920 kg respectively.
- Added plate crafting, persistent bar inventories and returning plates when removing or breaking equipment.


## v0.10.46 - Higher Physical Progression Caps

- Extended jump progression to 280% at Deadlift Lv.40 and running speed to 270% at Squat Lv.40.
- Raised maximum base damage to 10 at Bench Lv.40.
- Raised maximum health to 30 hearts at Strength Lv.35.
- Extended mining speed to 220% at Biceps Lv.25.
- Preserved XP formulas, rewards and leveling pace while raising the upper limits.


## v0.10.45 - Extended Deadlift Jump Progression

- Moved the previous maximum jump bonus from Deadlift Lv.19 to Lv.30.
- Extended jump progression while preserving its previous maximum and the existing XP pace.


## v0.10.44 - Third-Person Free Look

- Allowed free third-person camera movement during squat and deadlift, like bench press.
- Kept the player’s exercise position fixed without resetting look direction every server tick.
- Preserved the equipment-facing body pose and the exercise-specific first-person camera.


## v0.10.43 - Multiplayer Limb Bending Recovery

- Checked PlayerAnimator layers for every visible player and restored missing or replaced layers.
- Retried temporary registration failures instead of permanently disabling bending.
- Gave exercise bending priority while passing unrelated animation transforms through.
- Cleared old layers when leaving a server or switching worlds.
- Preserved the normal rigid-limb fallback without the optional libraries.


## v0.10.42 - Muscle-Based Working Weights

- Changed bench strength to the average Chest and Triceps level.
- Made squat and deadlift working weights depend on Legs, and dumbbells depend on Biceps.
- Calculated available weights, repetition capacity and tempo from the trained muscles.
- Kept the shared leg-muscle progression between squat and deadlift.
- Retained separate exercise XP for physical bonuses, records and statistics.


## v0.10.41 - Independent Exercise Progression

- Made working weights, repetition calculations and training tempo depend on the relevant exercise skill.
- Separated bench, squat, deadlift and dumbbell strength calculations from overall Strength.
- Normalized unsupported legacy estimated 1RMs when loading player data.
- Kept overall Strength progression and shared stamina/FAT.


## v0.10.40 - Progress Reset Command

- Added /gymreset to reset the player’s own HardGym progression.
- Reset XP, levels, records, estimated 1RMs and repetition counters.
- Restored stamina and cleared fatigue and the missed-sleep streak.
- Ended active workouts before resetting and refreshed attributes and HUD immediately.


## v0.10.39 - Solid Bench Upholstery

- Replaced wool-pattern upholstery with smooth, solid-color bench pads.
- Matched all colored pads to the visual style of the original black bench.
- Preserved colored recipes, drops and exercise behavior.


## v0.10.38 - Colored Bench Press Stations

- Added bench press stations in every wool color.
- Made the wool used in crafting determine the bench pad color.
- Preserved color during weight changes, all animation phases and block drops.
- Kept existing benches compatible with the original black bench ID.


## v0.10.37 - Hammer Curls and Biceps Mining Bonus

- Aligned the dumbbell handle with the palm and changed the exercise to a neutral hammer grip.
- Moved each curl toward its own shoulder and reduced elbow range to avoid shoulder clipping.
- Kept the upper arm close to the body and supported left-handed players.
- Made Biceps progression increase block-breaking speed, starting below normal speed.
- Synchronized the mining multiplier and added it to /gymstats.


## v0.10.36 - Dumbbell Grip Rotation

- Rotated dumbbells by 90 degrees in both hands.
- Updated first-person and third-person transforms so the handle sits across the palm.


## v0.10.35 - Dumbbell Curl Animation and Fatigue

- Replaced the vanilla bow-aiming pose with a custom dumbbell curl.
- Added a smooth lift, short top hold and lowering phase with optional elbow bending.
- Synchronized the repetition timing when a set begins.
- Spread FAT over the expected set instead of applying the old large penalty to every repetition.
- Kept XP, stamina and hunger costs tied to completed repetitions.


## v0.10.34 - Craftable Gym Equipment

- Added crafting recipes for all existing dumbbells and exercise stations.
- Replaced the /gymkit equipment command with normal crafting progression.
- Made heavier dumbbells consume the previous dumbbell during crafting.
- Added recipe-book unlocks for equipment and upgrades.


## v0.10.33 - Directional Bench and Squat Hitboxes

- Rotated selection and collision shapes together with the bench and squat rack.
- Corrected front/back placement for all horizontal directions.
- Accounted for bench parts extending outside the block.
- Cached the rotated shapes instead of recalculating them during rendering.


## v0.10.32 - Stable Bench Player Lock

- Stopped normal client/server physics from shifting the player during bench press.
- Replaced constant teleport corrections with position updates only when needed.
- Bound the bench pose and camera to the player’s synchronized workout state.
- Released the lock correctly after stopping, death, respawn, disconnect or station removal.
- Prevented old bench coordinates from being reused after a dimension change.


## v0.10.31 - Bench Bar in the Hands

- Aligned the active bench bar and plates with the centres of the player’s hands in every press phase.
- Accounted for elbow bending, shoulder rotation and the lying player transform.
- Kept the original on-rack bar position outside an active set.
- Added generated grip-aligned bar models for every supported bench weight.


## v0.10.30 - Bent-Elbow Bench Press

- Added real elbow bending for bench press through optional PlayerAnimator + BendyLib.
- Coordinated shoulder rotation and forearm bending with the top, middle and bottom bar phases.
- Kept the current arm pose when holding the bar; used the bottom pose when pinned.
- Preserved the accepted bench leg placement and multiplayer rendering.
- Made fabric.mod.json use the project version during resource processing.


## v0.10.29 - Bench Leg Contact Fine Tune

- Added a small final outward adjustment to the bench-press legs.
- Preserved the accepted thigh angles and knee bend while improving bench-edge clearance.


## v0.10.28 - Bench Leg Spacing Tune

- Moved each bench-press leg farther from the bench edge.
- Preserved the accepted thigh rotation and knee bending from v0.10.27.


## v0.10.27 - Additional Bench Edge Clearance

- Added another small outward adjustment to both leg pivots.
- Slightly increased thigh spread while preserving hip pitch and knee bend.


## v0.10.26 - Bench Leg Clearance

- Moved both bench-press legs slightly farther outward.
- Increased outward thigh roll to reduce overlap with the bench pad.
- Kept the previous hip pitch and knee bend.


## v0.10.25 - Bench Hip and Thigh Pose Rework

- Moved the hip pivots back toward the torso to keep the legs connected.
- Replaced the strong outward thigh yaw with controlled roll-based leg spread.
- Retuned optional BendyLib knee bending for the new bench leg pose.


## v0.10.24 - bench thigh roll direction corrected

- Kept the same wide bench leg placement and hip spread.
- Reversed the thigh roll direction from v0.10.23 so the thighs rotate in place instead of visually collapsing inward.
- Roll magnitude is moderate (0.18) to stay closer to the desired sketch than the older, more extreme 0.30 setup.


## v0.10.23 - bench thigh rotation retuned

- Kept the same wide leg placement as v0.10.22, but rotated the thighs around their axis so they read flatter and more level.
- This version targets the user sketch more closely by changing thigh twist only, not additional leg spread.


## v0.10.22 - bench thigh twist corrected

- Kept the wide bench-press leg spread from v0.10.21, but removed most of the thigh twist/roll.
- Goal: the thigh block should read flatter, with its lower face closer to parallel with the ground instead of looking twisted outward.


## v0.10.21 - bench legs opened farther outward

- Rotated the bench-press legs farther outward and made the thighs leave the torso straighter.
- Increased lateral spread and outward rotation so the side and front view are closer to the sketched target pose.


## v0.10.20 - bench thighs tuned further toward the target pose

- Reduced the bench-press upper-leg/hip bend again so the thighs sit closer to the side and front-view target sketch.
- Keeps the wide stance and bent-knee setup, but makes the upper legs extend straighter from the torso.


## v0.10.19 - bench thighs made even straighter

- Reduced the bench-press upper-leg/hip bend a bit more so the legs sit even closer to the sketched target pose.
- Keeps the wider stance, but makes the thigh line straighter and less tucked upward.


## v0.10.18 - bench thighs made straighter

- Reduced the bench-press upper-leg/hip bend so the legs connect to the torso more naturally.
- Keeps the wider stance from v0.10.17, but makes the thighs less folded up onto the body.


## v0.10.17 - bench legs spread wider

- Spread the bench-press legs farther out to the sides so they no longer climb onto the bench.
- Increased lateral leg pivot, yaw and roll while keeping the lifted torso and bent-knee setup.


## v0.10.16 - bench legs raised to match torso

- Raised both bench-press legs by the same local vertical offset as the torso.
- This keeps the hips/legs aligned with the already-raised upper body from v0.10.15.
- Leg spread and knee bend are unchanged.


## v0.10.15 - bench body lift corrected

- Reverted the previous bench-body lift approach and restored the v0.10.12 player placement.
- Lifted only the upper body away from the bench surface in the correct direction.
- Goal: chest/torso should sit above the bench instead of sinking into it.


## v0.10.14 - Intermediate Version — Release Notes Unavailable

- No separate detailed release note was retained for this version.


## v0.10.13 - Bench Player Height Adjustment

- Raised the whole bench-press player pose above the bench pad.
- Adjusted placement to reduce torso clipping into the bench.


## v0.10.12 - bench press legs spread and knees bent

- Bench press legs are now posed wider apart instead of lying straight together on the bench.
- Thighs are raised more, and when PlayerAnimator + BendyLib are installed the knees now bend so the shins can drop down.
- Goal: match a more realistic bench setup with the feet/legs planted wider.


## v0.10.11 - bench press moved a bit farther forward

- Shifted the player a little farther toward the foot-end of the bench during bench press.
- Target: with the arms around 90 degrees to the torso, the hands/bar path should clear the uprights better.
- This is a small follow-up adjustment on top of v0.10.10.


## v0.10.10 - bench press player moved in the opposite direction

- Reversed the previous bench shift direction.
- The player is now moved farther toward the foot-end of the bench.
- This should bring the legs closer to the edge and pull the shoulders farther out of the bench/rack area.


## v0.10.9 - bench press player moved forward on the bench

- Moved the player farther toward the foot-end of the bench during bench press.
- This puts the legs closer to the bench edge and pulls the shoulders out of the bench/rack area.
- The goal is to make the bar look less like it is still resting on the uprights.


## v0.10.8 - squat first-person bar hidden

- Kept the v0.10.7 third-person squat bar position unchanged.
- Moved only the custom first-person squat camera forward, past the mounted bar.
- The bar and plates should no longer cut across the screen in first person.


## v0.10.7 - squat mounted bar moved in the opposite direction

- Reversed the previous mounted-bar Z adjustment for squat.
- Mid and bottom squat phases now shift the mounted bar/plates toward the body in the opposite direction from v0.10.6.
- Intended to match phase 1 better and keep the bar less visible from first person.


## v0.10.6 - Squat Mounted Bar Position

- Moved the active squat bar and plates closer to the exercising player.
- Applied a larger position adjustment during the middle and bottom phases.
- Retuned the mounted bar placement to reduce its visibility in first person.


## v0.10.5 - Squat Upper-Body and Bar Alignment

- Kept the accepted leg positions from v0.10.4.
- Raised the torso, head and arms to reconnect the hips with the lifted legs.
- Moved the mounted bar and plates upward together with the upper body.


## v0.10.4 - Squat Leg-Only Ground Contact

- Reverted the whole-player vertical correction from v0.10.3.
- Raised only the leg pivots during the middle and bottom squat phases.
- Restored the previous height of the torso, head, arms and mounted bar.


## v0.10.3 - Squat Ground Contact Fix

- Retuned the squat player Y-offset for the real BendyLib knee geometry.
- Mid squat now lowers the player by about 1 model pixel instead of 4.
- Bottom squat now lowers the player by about 3.4 model pixels instead of 8.
- Raised the active squat barbell and plates by the matching amount, so the bar stays attached to the shoulders while the feet no longer sink into the ground.


## v0.10.2 - Squat Hip Connection Fix

- Fixed the visible gap between the torso and legs during the squat.
- Added hip-pivot compensation to the squat, using the same method that fixed the deadlift torso/hip connection.
- The waist now stays anchored to the leg pivots while the torso leans forward.
- Existing knee bend, thigh direction, arm pose and bar position are preserved.


## v0.10.1 - Squat Knee Direction Fix

- Fixed the squat legs bending backwards after enabling real knee joints.
- The BendyLib knee bend remains positive, matching the working deadlift.
- Squat upper-leg pitch is now negative so the knees travel forward naturally.
- Bottom and middle squat phases preserve the existing torso, arm and bar geometry.


## v0.10.0 - Squat Knee Bend

- Added real BendyLib knee bending to the squat animation.
- Standing/top: knees are almost straight.
- Mid squat: knees bend progressively.
- Bottom squat: knees are deeply bent instead of the entire leg staying rigid.
- Multiplayer observers use the same PlayerAnimator layer, so the bend is visible remotely too.
- Requires Player Animation Lib + BendyLib for true joint bending; fallback pose still works without them.


## v0.9.9 - Weight-scaled set fatigue

- Barbell FAT now depends on absolute weight as well as how much of the set is completed.
- Equally hard 10-rep sets: 60 kg ~8 FAT, 100 kg ~10 FAT, 140 kg ~12 FAT, 200 kg ~15 FAT.
- This means an advanced lifter doing 100x10 gets more FAT than a novice doing 60x10.
- Very heavy sets are capped at 20 FAT from normal reps so ordinary training does not instantly max the bar.
- Pinned/crushed FAT, sleep deprivation, food recovery, sleep recovery and passive recovery are unchanged.


## v0.9.8 - Lower food FAT recovery

- Food now restores much less fatigue.
- Recovery is roughly half the food's hunger value, rounded up.
- Example: steak restores 4 FAT instead of 16; bread 3 instead of 10; apple 2 instead of 8.
- Sleep remains the primary fast way to recover fatigue.


## v0.9.7 - Sleep deprivation fatigue

- Missing one full night adds 30 FAT.
- Consecutive missed nights add 45, 60, 75, then 90 FAT.
- From the fifth consecutive missed night onward, the nightly penalty stays capped at 90 FAT.
- A completed night of sleep resets the consecutive missed-night streak to zero and still restores up to 60 FAT.
- Simply entering and leaving a bed does not reset the streak.
- Missed-night state is persisted in player NBT and copied through respawn.


## v0.9.6 - Full-sleep FAT recovery

- Getting into bed and leaving early no longer restores fatigue.
- Sleep FAT recovery is applied only after the world actually skips to the next Minecraft day.
- Full sleep still restores up to 60 FAT.
- Food and passive FAT recovery are unchanged.


## v0.9.5 - Lighter set fatigue

- A full normal set now adds about 10 FAT instead of about 30.
- FAT is distributed across the reps, so it still rises during the set.
- Very high-rep sets add FAT periodically and still total 10 at the full target.
- Pinned/failure FAT penalties remain intentionally much stronger and are unchanged.
- Passive recovery, food recovery, and sleep recovery are unchanged.


## v0.9.4 - Pinned fatigue

- Failing a bench press or squat and getting pinned now immediately adds a weight-scaled FAT penalty.
- Bench example: 200 kg gives about +13 FAT instantly, then about +4 FAT per second while pinned.
- Squat example: 300 kg gives about +15 FAT instantly, then about +5 FAT per second while pinned.
- FAT is clamped to 100 by the existing player-data system.
- Deadlift failure is unchanged: the bar is dropped rather than pinning/damaging the player.


## v0.9.3 - Persistent fatigue system

- FAT now acts as between-set fatigue: it rises during a set and reduces the rep target of the NEXT set.
- 0 FAT = full %1RM calculator reps; ~30 FAT = about 75% of normal reps; ~60 FAT = about half; ~90 FAT = about a quarter.
- A hard full set builds roughly 30 FAT regardless of whether it is a heavy low-rep or lighter high-rep set.
- Passive FAT recovery slowed from 2%/second to 1% every 10 seconds. Stamina still recovers quickly.
- Food reduces FAT by roughly 2% per hunger point restored (e.g. steak ~16 FAT).
- Sleeping for at least ~2 seconds and then waking restores up to 60 FAT.
- FAT 95+ still blocks starting a new barbell set.


## v0.9.2 - %1RM rep system

- Reworked barbell stamina from absolute kilograms to relative % of estimated 1RM.
- Uses the strength column table: 100%=1; 95%=1-2; 90%=2-3; 85%=4-5; 80%=6-7; 75%=8-9; 70%=10-11; 65%=12-17; 60%=18-25; 55%=26-29; 50%=30-37.
- Example: 100 kg x5 estimates roughly 118 kg 1RM; 70 kg is then ~60%, giving about 22 reps when fully rested instead of ~8.
- Bench/squat static pauses still drain stamina by bar position; moving reps no longer get double-charged by the hold drain.
- Deadlift rep stamina is also %1RM-based; pulling still costs hunger.
- Estimated 1RM is stored separately for bench, squat and deadlift and shown in /gymstats. Old PRs migrate as approximate 5RM values.


## v0.9.1 - Default starting jump

- Starting jump is now exactly vanilla Minecraft jump power (100%).
- Deadlift progression still increases jump height above vanilla.
- Other starting stats remain below vanilla as before.


## v0.9.0 - Exercise-specific physical stats

- Bench press progression now controls attack damage. New players start at 0.5 base damage vs vanilla 1.0.
- Squat progression now controls movement speed. New players start at 85% of vanilla movement speed.
- Deadlift progression now controls jump height. New players start at roughly 66% of vanilla jump height.
- Overall Strength now controls maximum health. New players start at 8 hearts vs vanilla 10; higher Strength adds health up to 20 hearts.
- Added separate Bench/Squat/Deadlift XP with migration from existing save data.
- /gymstats now displays all four physical effects.


## v0.8.19 - Heavy weights

- Bench press extended to 220 kg: 220 = 5 blue plates per side.
- Squat and deadlift extended to 300 kg.
- 220 = 5 blue; 240 = 5 blue + 1 green; 260 = 6 blue; 280 = 6 blue + 1 green; 300 = 7 blue per side.
- Plate gaps from v0.8.18 are preserved on all newly generated squat/deadlift models.
- Progression requirements were extended for the new weights.


## v0.8.18 - Gaps between weight

- Added small gaps between weight plates on squat rack and deadlift bar models to match bench press spacing.


## v0.8.17 - Deadlift top bar fine tune

- Body pose and arm pose from v0.8.16 are unchanged.
- Only the top deadlift phase was adjusted.
- The bar and plates were moved 1 extra model unit closer to Steve so the bar sits better in the hands and sticks out less.
- Updated model files: 12; adjusted elements: 72.


## v0.8.16 - Deadlift top bar closer

- Body and arm pose from v0.8.15 are unchanged.
- First and second deadlift phases are unchanged.
- Only the top-phase bar and plates were moved 2 additional model units closer to Steve.
- Updated model files: 12; adjusted elements: 72.


## v0.8.15 - Deadlift top arm angle

- Keeps the shoulder pivot in the same place at lockout.
- Removes the v0.8.14 shoulder translation.
- Rotates the straight arms themselves forward by about 0.28 rad (~16°) toward the bar.
- Top bar position from v0.8.14 is unchanged.
- First and second deadlift phases are unchanged.


## v0.8.14 - Deadlift top grip tune

- First and second phases are unchanged.
- At lockout the straight arms are shifted slightly forward toward the bar.
- The top-phase bar keeps its top height, but uses the mid-phase horizontal position and is moved 1 model unit closer to Steve.
- Updated top bar models: 12; moved bar/plate elements: 72.


## v0.8.13 - Deadlift mid-phase bar grip alignment

- First phase is unchanged.
- Second/mid phase body animation from v0.8.12 is unchanged.
- Only the a1 deadlift bar + plates are shifted toward the player by +3 model units on local Z so the bar sits back in the hands.
- Updated 12 model files / 72 bar-or-plate elements.


## v0.8.12 - Deadlift mid-phase tuning

- First/bottom phase from v0.8.11 is unchanged.
- Mid phase torso leans farther forward while hips stay back.
- Mid phase arms are vertical (arm pitch 0).
- Mid phase thigh pitch and knee bend are matched (-0.55 / +0.55) so the shin renders close to vertical.
- Lockout phase unchanged.


## v0.8.11 - deadlift torso + vertical arms fix

- Keeps the working bottom leg setup and knee bend 1.3F.
- Bottom torso pitch increased from 0.78F to 0.95F for a stronger forward hinge.
- Bottom arm pitch set to 0.0F so the arms hang vertically toward the bar.
- Feet-grounding renderer offset from v0.8.10 is unchanged.
- Body/arm rotation is applied in BipedEntityModelMixin, not the BendyLib BEND transform.


## v0.8.10 - Deadlift start feet grounded

- Uses the user-tested bottom knee bend of 1.3F.
- Keeps the v0.8.6 thigh position.
- Lowers the whole rendered player only in the bottom deadlift phase from -0.12D to -0.28D so the feet sit on the ground.
- Mid and lockout phases are unchanged.


## v0.8.9 - Deadlift bottom shin vertical tune

- Keeps the v0.8.6 thigh angle unchanged.
- Uses the shin bend direction confirmed by the user's manual sign flip.
- Reduces bottom knee bend magnitude to 0.62 rad so the shin should sit closer to perpendicular to the ground.
- The more vertical shin also lowers the foot toward the ground without moving the thigh.
- Mid and lockout phases remain unchanged.


## v0.8.8 - compile fix

- Fixed an accidental literal \n sequence in OptionalPlayerAnimator comments that commented out the kneeBend declaration.
- Deadlift shin tuning from v0.8.7 is unchanged.


## v0.8.7 - Deadlift start shin fix

- Kept the v0.8.6 thigh position for the lowest deadlift phase.
- Flipped and retuned the knee bend only at the floor position so the shin moves forward, aiming for a near-vertical lower leg.
- Intended result: shin closer to perpendicular to the ground and the foot staying on the ground.
- Mid and top deadlift phases unchanged.


## v0.8.6 - Deadlift bottom-leg direction fix

- Corrected the sign of the upper-leg rotation in the lowest deadlift phase.
- Thighs now rotate toward the bar/front instead of kicking backward.
- Knee bend remains unchanged from v0.8.5 (~60° target).
- Other deadlift phases and multiplayer sync are unchanged.


## v0.8.5 - Deadlift start-leg tuning

- Focuses only on the bottom deadlift position.
- Keeps the hip-pivot torso fix from v0.8.4.
- Moves the upper legs much farther forward at ANIM 0 so the thigh is visually closer to parallel with the ground.
- Knee bend values remain the same (~60 degrees at the bottom with PlayerAnimator/BendyLib).


## v0.8.4 - Deadlift hip-pivot fix

- Reverted the incorrect v0.8.3 thigh-direction experiment.
- Keeps the v0.8.2 knee bend and leg trajectory.
- Fixes torso/leg separation by translating the upper-body pivots so the torso rotates around the hips instead of around the shoulders/top of the torso.
- Head and arm pivots follow the same hip compensation, keeping the upper body together.
- Multiplayer deadlift sync remains enabled.


## v0.8.2 - Deadlift multiplayer animation sync

- Server now broadcasts which player owns each active deadlift bar.
- Remote clients bind the deadlift pose and BendyLib knee bend by player UUID instead of relying only on interpolated position.
- Added a slightly wider positional fallback for missed/late packets.
- No deadlift gameplay/balance values were changed.


## v0.8.1 - Deadlift knee bend trajectory

- Added real mid-leg bending through the existing optional PlayerAnimator + BendyLib bridge.
- Floor position has the strongest knee bend, knee-height position is partially extended, lockout is nearly straight.
- Retuned upper-leg and torso angles to follow a more natural floor → knees → lockout deadlift trajectory.
- Squat elbow animation and multiplayer registration remain intact.


## v0.8.0 - Deadlift

- New deadlift bar exercise with 40–200 kg plate presets.
- Shift+RMB cycles weight; RMB starts a set; hold RMB to pull.
- Releasing RMB makes the bar lower back toward the floor instead of freezing in mid-air.
- Three visual phases: floor, knees, lockout.
- Rep counts at full lockout, then the bar lowers for the next rep.
- Deadlift trains Strength + Legs, has separate reps and PR in /gymstats.
- Stamina/hunger load is highest from the floor, medium near the knees, and lowest near lockout.
- Failure behavior: if stamina runs out during the pull, the bar drops to the floor and the set ends. **No HP damage.**
- HUD, sounds, multiplayer state detection, respawn persistence, and /gymkit support added.


## v0.7.18 - squat bar moved much farther from the rack

- On-rack position stays the same.
- Once the player takes the bar, the active squat bar/plates move much farther onto the back.
- Applied offset to mounted squat models only: **-6.0 on Z** from the original v0.7.16 position.


## v0.7.16 - squat bar stays on rack when idle, shifts onto the back when active

- Restored the idle squat-rack bar position on the stands.
- Added separate active model variants: when the rack becomes ACTIVE during a squat, the bar/plates shift onto the player's back.
- Mounted variants generated: 36.


## v0.7.15 - squat bar shifted away from the uprights

- Shifted the squat bar and all plates farther away from the rack uprights.
- Keeps the player offset + 180° orientation from v0.7.14.
- Updated model variants: 36.


## v0.7.14 - squat moved off the uprights and rotated

- Player anchor is shifted farther away from the squat uprights.
- Squat pose is rotated 180° during use, including multiplayer render sync.
- First-person squat camera follows the new position/orientation.


## v0.7.13 - shoulders much farther back

- Elbow bend remains identical to v0.7.9.
- Shoulder pivots moved substantially farther backward and further inward.
- Intended to create a much stronger 'shoulder blades squeezed together' squat position.


## v0.7.12 - shoulders farther back

- Keeps the v0.7.9 elbow bend unchanged.
- Retracts shoulder pivots much farther backward.
- Moves shoulders a little more inward to imitate stronger scapular retraction.


## v0.7.11 - Squat shoulders back

- Based directly on v0.7.9 multiplayer elbow sync.
- Keeps the exact v0.7.9 elbow bend.
- Retracts both shoulder pivots backward and slightly inward to imitate squeezed shoulder blades under the bar.


## v0.7.9 - Multiplayer elbow animation

- Registers the optional PlayerAnimator/BendyLib squat elbow layer for every visible client-side player, not only the local player.
- Remote players doing squats now render the same bent-elbow pose for observers.
- Each observing client still needs PlayerAnimator + BendyLib installed for the mid-arm bend to render.


## v0.7.8 - tighter squat elbow bend

- Increased optional PlayerAnimator elbow bend for the squat pose.
- Adjusted upper-arm pose so hands sit closer to the bar and less on the plates.


## v0.7.7 - stale PlayerAnimator source fix

- Added a dependency-free compatibility stub at the old SquatElbowAnimation path.
- This overwrites the stale v0.7.5 source when updating the same project folder.
- Real optional elbow bending remains handled by OptionalPlayerAnimator via reflection.


## v0.7.6 - Bent-elbow compatibility fix

- Removed compile-time/nested PlayerAnimator and BendyLib dependencies so Fabric Loom 0.7 can build HardGym again.
- Real elbow bending is now an optional runtime integration loaded through reflection.
- For bent elbows install PlayerAnimator Fabric 0.3.5+1.16.5 and BendyLib Fabric 1.2.1 as separate jars in `.minecraft/mods`.
- Without those libraries HardGym still starts; the squat simply uses the normal rigid-arm pose.


## v0.7.5 - Bent-elbow squat grip

- Added PlayerAnimator 0.4.0+1.16.5 and BendyLib 1.2.1 as nested Fabric dependencies.
- No separate PlayerAnimator/BendyLib install is required when using the built HardGym jar.
- Squat upper arms are positioned outward/back and the lower half bends at the elbow about 85 degrees toward the bar.
- Elbow bending is active only while the player is on the squat rack.


## v0.7.3 - Tuned squat pose

- Squat arms are now raised higher while keeping the wide side grip pose.


## v0.7.2 - Tuned squat pose

- Squat pose arms moved outward to the sides to simulate holding the bar near its edges.


## v0.7.1 - Tuned squat pose

- Arms are now pulled back instead of pointing forward.


## v0.7.0 - Barbell Squat

- New squat rack block and barbell squat exercise.
- Shift + RMB on the rack cycles 40–200 kg using the same plate color system as bench press.
- RMB starts a set; hold RMB to move through the squat, release to freeze at the current depth, Shift ends/bails out.
- Three visual positions: standing/top, mid squat, bottom squat. The bar and player/camera move with the depth.
- New Legs stat, squat reps and squat PR in `/gymstats`.
- Squats train Strength + Legs, consume stamina/fatigue/hunger, and reset Strength detraining.
- Heavier/deeper squats drain more stamina and hunger. If stamina reaches zero at the bar, the player gets pinned in the bottom position and takes damage until Shift is pressed.
- Added unrack/lowering/pressing/rack/drop sounds and relative-difficulty HUD colors for squat.


## v0.6.7 - Higher Workout Hunger

- Increased hunger/exhaustion cost from dumbbell reps.
- Increased bench press hunger cost while holding the bar.
- Increased extra exhaustion for completed bench reps.
- Heavier weights and lower bar positions remain more demanding.


## v0.6.6 - Workout Hunger

- Training now consumes hunger due to vanilla-style exhaustion mechanics.
- Dumbbells: consumption depends on the weight of each successful rep.
- Bench press: hunger is consumed every second while holding the bar during a rep, as well as for the full rep itself.
- Water consumption is higher at the bottom position than at the middle or top positions.


## v0.6.5 - Water stamina

- Drinking a vanilla water bottle restores 25 stamina (up to 100).
- Recovery HUD updates immediately after drinking.


## v0.6.4 - Strength Decay

- Strength inactivity is tracked while the player is online.
- 3 Minecraft days (60 real minutes) without a successful strength-training rep are a grace period.
- After the grace period, every Minecraft day (20 real minutes) Strength XP decays by 2% (minimum 10 XP).
- Any successful dumbbell or bench-press rep resets the inactivity timer.
- /gymstats shows time until detraining / next decay tick.
- If a Strength level drops, melee damage is updated immediately.


## v0.6.3 - Strength Damage

- Strength level now directly changes melee attack damage.
- Lv.1 starts weaker than a vanilla player: 0.5 base fist damage instead of 1.0.
- Each Strength level adds +0.2 base damage.
- The bonus/penalty also affects melee weapons because it is applied through GENERIC_ATTACK_DAMAGE.
- `/gymstats` now shows current bare-hand damage.


## v0.6.2 - HUD fix

- HUD stays visible after training while stamina recovers, and hides exactly at 100%.
- Recovery HUD displays live ST/FAT updates.
- Bench difficulty accent is relative to the player's currently unlocked maximum weight.


## v0.6.1 - HUD polish

- Workout state text moved into the top HUD row.
- HUD moved upward so vanilla set-finished actionbar text sits below it.
- Light bench weights (40-80 kg) use a green left accent; normal weights remain yellow; pinned remains red.


## v0.6.0 - Gym HUD

- New custom workout HUD rendered above the vanilla hotbar.
- Shows exercise + weight and rep counter.
- Separate smooth bars for rep progress, stamina and fatigue.
- Stamina changes color as it becomes low.
- Fatigue changes color as it becomes dangerous.
- Waiting state shows `ДЕРЖИ ПКМ`.
- Pinned-under-bar state switches to a red emergency panel with HP and `SHIFT — СБРОСИТЬ ШТАНГУ`.
- Removed the constantly-updating vanilla action-bar text from workout progress.


## v0.5.4 - Bench Sounds

Added bench-press sounds using Minecraft metal/chain sounds:

- unrack
- lowering phase
- pressing phase
- rerack
- dropped/crushed bar clang
Heavier weights use a slightly lower pitch.


## v0.5.3 - Square Plates

- Made all bench press plates square, instead of rectangular.


## v0.5.2 - Plate System

- Added a more detailed bench press weight progression:
  40 / 50 / 60 / 70 / 80 / 90 / 100 / 120 / 140 / 160 / 180 / 200 kg.
- For example: 40 kg - bar + 1 green plate each site; 90 kg - bar + 1 blue + 1 yellow plates; 120 kg - 2 blue + 1 green plates.
- Added new plate combinations for each weight.
- Added green plates.
- Reworked bench models to visually represent the selected weight.
- Standardized plate thickness.


## v0.5.1 - Position-Based Stamina

- Bench holding stamina drain now depends on bar position.
- Holding the bar at lockout consumes the least stamina.
- Holding it around the middle consumes more stamina.
- Holding the bar near the chest consumes the most stamina.
- Kept the manual bench and failure mechanics introduced in v0.5.0.


## v0.5.0 - Manual Bench & Failure

- Completely reworked the bench press from automatic reps to manual control.
- Hold RMB to move the bar and perform the press.
- Releasing RMB freezes the bar at its current position.
- Only fully completed reps are counted.
- Stamina continues draining while holding the bar stationary.
- Heavier weights consume more stamina.
- Completed reps consume additional stamina and generate fatigue.
- Added bench press failure.
- At 0 stamina, the player becomes pinned under the bar.
- A pinned bar drops to the chest and periodically damages the player.
- Shift can be used as an emergency exit from the set.
- Preserved first-person bench camera, player animation and progression.
- Supported bench weights up to 200 kg.


## v0.4.4 - Bench 200 kg

- Expanded the bench press from 120 kg to 200 kg.
- Added 140 / 160 / 180 / 200 kg weights.
- Added separate plate models for the new heavy weights.
- Added higher Strength requirements:
  - 140 kg -> Strength 12
  - 160 kg -> Strength 14
  - 180 kg -> Strength 16
  - 200 kg -> Strength 18


## v0.4.3 - Shift Stop

- Added Shift as an immediate way to end a bench press set.
- The player is released from the bench immediately.
- The bar returns to the top position after stopping.


## v0.4.2 - First-Person Look At Bar

- Reworked the first-person bench camera.
- Camera direction is now calculated toward the bar instead of using a fixed pitch.
- Improved camera placement relative to the rack.
- The player now naturally looks toward the bar during the bench press.


## v0.4.1 - First-Person Bar Visible

- Adjusted the bench first-person camera so the bar is visible.
- Lowered and moved the camera.
- Reduced the extreme downward camera angle.


## v0.4.0 - First-Person Bench Camera

- Added a custom first-person camera while bench pressing.
- Camera is positioned around the player's head while lying on the bench.
- Added automatic camera orientation during the exercise.
- Third-person camera remains unaffected.


## v0.3.13 - Arm Pose Tune

- Reworked bench press arm poses.
- Added different arm angles for the top, middle and bottom phases.
- Improved elbow positioning near the chest.
- Improved transition between press phases.
- Made the lockout pose wider and more natural.


## v0.3.12 - Chest Position Corrected

- Corrected the direction of the player's longitudinal bench offset.
- Moved the player further toward the rack.
- Improved shoulder/chest alignment under the bar.
- Reduced excessive leg overhang from the bench.


## v0.3.11 - Chest Contact Fix

- Fine-tuned the player's longitudinal position on the bench.
- Adjusted body placement to improve the relationship between the chest and bar.


## v0.3.10 - Player Longitudinal Fix

- Moved the player farther along the bench toward the rack.
- Increased the bench anchor offset from 0.32 to 0.68 blocks.
- Positioned the shoulders closer underneath the bar.
- Reduced how far the head extended beyond the rack area.


## v0.3.9 - Player Height Fix

- Lowered the player's server-side bench position.
- Lowered the rendered player closer to the bench pad.
- Reduced the visible gap between the player's back and the bench.


## v0.3.8 - Player Alignment Fix

- Aligned the player exactly with the bench axis.
- Removed diagonal body alignment caused by interpolated body rotation.
- Lowered the player's anchor point.
- Improved contact between the player's back and the bench pad.


## v0.3.7 - Bench Orientation Fix

- Rotated the player 180° into the correct bench orientation.
- The player's head is now positioned toward the rack/bar.
- Feet now point toward the free end of the bench.
- Moved the player anchor toward the rack.
- Synced client-side active-bench detection with the new position.


## v0.3.6 - Bench Lock & Animation Sync

- Improved detection of which active bench belongs to the exercising player.
- Increased the active bench search area.
- Added position checks so nearby active benches do not animate the wrong player.
- Improved server/client position synchronization while lying on the bench.
- Added explicit teleport synchronization during the set.
- Improved restoration of the player's original position after the set.


## v0.3.5 - Bench UV Fix

- Fixed broken/stretched textures on the enlarged bench model.
- Added explicit UV coordinates to bench, rack, bar and plate faces.
- Prevented oversized model elements from sampling neighboring areas of the Minecraft texture atlas.


## v0.3.4 - Clean Bench Textures

- Added dedicated textures for the bench frame.
- Added a separate bench pad texture.
- Added dedicated barbell and plate textures.
- Removed stretched vanilla block textures from the gym equipment.
- Preserved the enlarged bench geometry.


## v0.3.3 - Larger Bench

- Enlarged the bench press station to roughly 2.1 blocks in length.
- Extended the bench pad so the full player model can fit on it.
- Moved the rack posts farther apart.
- Widened the bar beyond the bench itself.
- Expanded and rotated the collision shape with the bench.
- Preserved the three bar animation positions.


## v0.3.2b - Body Field Fix

- Fixed compatibility with Yarn 1.16.5+build.10.
- Replaced the incorrect `torso` model field with `body`.
- Fixed compilation of the custom bench player pose.


## v0.3.2a - No-Bed Animation Fix

- Fixed issues in the new render-based bench animation.
- Improved player rendering while lying on the bench without using Minecraft's sleeping state.


## v0.3.2 - No-Bed Bench Animation

- Removed the vanilla sleeping pose from bench pressing.
- Fixed the unwanted sleeping screen and "Leave Bed" button.
- The player now remains a normal player entity during the exercise.
- Added a synchronized ACTIVE state to the bench.
- Added client-side rendering that visually rotates the player into a lying position.
- Arms continue to animate based on bar position.
- Player position is restored after the set.


## v0.3.1 - Player Bench Animation

- Added the first player animation for the bench press.
- Player lies down on the bench during a set.
- Player position and direction are locked to the station.
- Arm angles change together with bar position.
- Player pose and position are restored after finishing the set.


## v0.3.0 - Bench Bar Animation

- Added three bench press animation states: top / middle / bottom.
- The bar now visually moves down and up during repetitions.
- Added separate animated bench models for each weight.
- Bar position returns to the top after a set ends.
- Connected bar animation state to the bench press logic.


## v0.2.1 - Bench Update Fix

- Fixed Fabric/Yarn 1.16.5 compatibility for the bench block.
- Corrected the `ShapeContext` import so the project compiles correctly.


## v0.2.0 - Bench Update

- Added the placeable 3D Bench Press Station with visible and selectable bench weights:
  40 / 60 / 80 / 100 / 120 kg.
- To select weight - Shift + RMB.
- To start/stop for bench sets - RMB.
- Added automatic timed bench press reps.
- Bench rep speed depends on Strength relative to the selected weight.
- Added Chest XP, Triceps XP.
- Added bench press PR tracking, bench rep counter.
- Added correct gym-data copying after death/respawn.
- Improved the action-bar training HUD.
- Dumbbell rep speed now increases with Strength.
- Added new 3D dumbbell models.


## v0.1.1 - Compile Fix

- Fixed Minecraft 1.16.5 Yarn mapping compatibility.
- Corrected `UseAction` package.
- Replaced old `CompoundTag` usage with `NbtCompound`.
- Updated player NBT save/load hooks to:
  `writeCustomDataToNbt` and `readCustomDataFromNbt`.


## v0.1.0 - Initial HardGym prototype for Minecraft 1.16.5 Fabric.

- Added 5 kg, 10 kg and 20 kg dumbbells.
- Hold RMB to perform dumbbell curls.
- Added Strength XP and levels; Biceps XP and levels.
- Added stamina, fatigue, recovery while not training.
- Added `/gymstats`, `/gymkit` commands.
- Added persistent player progression through NBT.
- Added Strength requirements for heavier dumbbells.

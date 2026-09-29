# HardGym roadmap

## v0.1 - Dumbbell prototype [DONE]
- Core progression
- Dumbbell curls
- Strength / biceps / stamina / fatigue
- Save data

## v0.2 - Bench Update [CURRENT]
- 3D dumbbells
- Improved action-bar HUD
- Strength-dependent rep speed
- Placeable bench press station
- Selectable 40 / 60 / 80 / 100 / 120 kg bench weights
- Chest / triceps progression
- Bench PR tracking
- Respawn/death data copy

## v0.2.x - Animation pass
- Player lies on bench
- Bar moves with the rep
- Proper arm press animation
- Set summary and rest timer
- Separate barbell + plate loading system

## v0.3 - Big three
- Squat rack
- Squat
- Deadlift
- 1RM / PR system
- Form / failure mechanic

## v0.4 - Full gym
- Cable machine
- Lat pulldown
- Leg press
- Leg extension / curl
- Shoulder press
- Pull-up station
- Treadmill / bike

## v0.5 - Simulator layer
- G-key stats screen
- Muscle-group overview
- Bodyweight / muscle mass
- Pump
- Recovery / nutrition
- Unlock progression
- Custom sounds and particles

## v0.6 - Visual body progression
- Custom player rendering
- Larger arms / shoulders / torso depending on muscle stats
- Proper exercise animations

- [x] Barbell squat + squat rack + Legs stat (v0.7.0)


0.7.1
- Squat arm pose moved back for cleaner silhouette.


## v0.7.5
- Real elbow bending for the squat grip using PlayerAnimator + BendyLib.

- v0.8.18: Plate gap alignment for squat and deadlift (matched to bench press).

- v0.9.2: Relative %1RM repetition/stamina model based on the strength-oriented rep calculator table; persistent estimated 1RMs per barbell lift.

- v0.9.3: Persistent between-set fatigue, slow passive recovery, food + sleep recovery.

- v0.9.4: Immediate + continuous FAT accumulation when pinned under bench/squat.

- v0.9.7: escalating FAT for consecutive nights without sleep (30/45/60/75/90, then 90 max).

- v0.9.8: reduced FAT recovery from food; sleep remains the main recovery method.

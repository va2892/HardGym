## v0.1.0 - Initial HardGym prototype for Minecraft 1.16.5 Fabric.
- Added 5 kg, 10 kg and 20 kg dumbbells.
- Hold RMB to perform dumbbell curls.
- Added Strength XP and levels; Biceps XP and levels.
- Added stamina, fatigue, recovery while not training.
- Added `/gymstats`, `/gymkit` commands.
- Added persistent player progression through NBT.
- Added Strength requirements for heavier dumbbells.


## v0.1.1 - Compile Fix
- Fixed Minecraft 1.16.5 Yarn mapping compatibility.
- Corrected `UseAction` package.
- Replaced old `CompoundTag` usage with `NbtCompound`.
- Updated player NBT save/load hooks to:
  `writeCustomDataToNbt` and `readCustomDataFromNbt`.


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


## v0.2.1 - Bench Update Fix
- Fixed Fabric/Yarn 1.16.5 compatibility for the bench block.
- Corrected the `ShapeContext` import so the project compiles correctly.


## v0.3.0 - Bench Bar Animation
- Added three bench press animation states: top / middle / bottom.
- The bar now visually moves down and up during repetitions.
- Added separate animated bench models for each weight.
- Bar position returns to the top after a set ends.
- Connected bar animation state to the bench press logic.


## v0.3.1 - Player Bench Animation
- Added the first player animation for the bench press.
- Player lies down on the bench during a set.
- Player position and direction are locked to the station.
- Arm angles change together with bar position.
- Player pose and position are restored after finishing the set.


## v0.3.2 - No-Bed Bench Animation
- Removed the vanilla sleeping pose from bench pressing.
- Fixed the unwanted sleeping screen and "Leave Bed" button.
- The player now remains a normal player entity during the exercise.
- Added a synchronized ACTIVE state to the bench.
- Added client-side rendering that visually rotates the player into a lying position.
- Arms continue to animate based on bar position.
- Player position is restored after the set.


## v0.3.2a - No-Bed Animation Fix
- Fixed issues in the new render-based bench animation.
- Improved player rendering while lying on the bench without using Minecraft's sleeping state.


## v0.3.2b - Body Field Fix
- Fixed compatibility with Yarn 1.16.5+build.10.
- Replaced the incorrect `torso` model field with `body`.
- Fixed compilation of the custom bench player pose.


## v0.3.3 - Larger Bench
- Enlarged the bench press station to roughly 2.1 blocks in length.
- Extended the bench pad so the full player model can fit on it.
- Moved the rack posts farther apart.
- Widened the bar beyond the bench itself.
- Expanded and rotated the collision shape with the bench.
- Preserved the three bar animation positions.


## v0.3.4 - Clean Bench Textures
- Added dedicated textures for the bench frame.
- Added a separate bench pad texture.
- Added dedicated barbell and plate textures.
- Removed stretched vanilla block textures from the gym equipment.
- Preserved the enlarged bench geometry.


## v0.3.5 - Bench UV Fix
- Fixed broken/stretched textures on the enlarged bench model.
- Added explicit UV coordinates to bench, rack, bar and plate faces.
- Prevented oversized model elements from sampling neighboring areas of the Minecraft texture atlas.


## v0.3.6 - Bench Lock & Animation Sync
- Improved detection of which active bench belongs to the exercising player.
- Increased the active bench search area.
- Added position checks so nearby active benches do not animate the wrong player.
- Improved server/client position synchronization while lying on the bench.
- Added explicit teleport synchronization during the set.
- Improved restoration of the player's original position after the set.


## v0.3.7 - Bench Orientation Fix
- Rotated the player 180° into the correct bench orientation.
- The player's head is now positioned toward the rack/bar.
- Feet now point toward the free end of the bench.
- Moved the player anchor toward the rack.
- Synced client-side active-bench detection with the new position.


## v0.3.8 - Player Alignment Fix
- Aligned the player exactly with the bench axis.
- Removed diagonal body alignment caused by interpolated body rotation.
- Lowered the player's anchor point.
- Improved contact between the player's back and the bench pad.


## v0.3.9 - Player Height Fix
- Lowered the player's server-side bench position.
- Lowered the rendered player closer to the bench pad.
- Reduced the visible gap between the player's back and the bench.


## v0.3.10 - Player Longitudinal Fix
- Moved the player farther along the bench toward the rack.
- Increased the bench anchor offset from 0.32 to 0.68 blocks.
- Positioned the shoulders closer underneath the bar.
- Reduced how far the head extended beyond the rack area.


## v0.3.11 - Chest Contact Fix
- Fine-tuned the player's longitudinal position on the bench.
- Adjusted body placement to improve the relationship between the chest and bar.


## v0.3.12 - Chest Position Corrected
- Corrected the direction of the player's longitudinal bench offset.
- Moved the player further toward the rack.
- Improved shoulder/chest alignment under the bar.
- Reduced excessive leg overhang from the bench.


## v0.3.13 - Arm Pose Tune
- Reworked bench press arm poses.
- Added different arm angles for the top, middle and bottom phases.
- Improved elbow positioning near the chest.
- Improved transition between press phases.
- Made the lockout pose wider and more natural.


## v0.4.0 - First-Person Bench Camera
- Added a custom first-person camera while bench pressing.
- Camera is positioned around the player's head while lying on the bench.
- Added automatic camera orientation during the exercise.
- Third-person camera remains unaffected.


## v0.4.1 - First-Person Bar Visible
- Adjusted the bench first-person camera so the bar is visible.
- Lowered and moved the camera.
- Reduced the extreme downward camera angle.


## v0.4.2 - First-Person Look At Bar
- Reworked the first-person bench camera.
- Camera direction is now calculated toward the bar instead of using a fixed pitch.
- Improved camera placement relative to the rack.
- The player now naturally looks toward the bar during the bench press.


## v0.4.3 - Shift Stop
- Added Shift as an immediate way to end a bench press set.
- The player is released from the bench immediately.
- The bar returns to the top position after stopping.


## v0.4.4 - Bench 200 kg
- Expanded the bench press from 120 kg to 200 kg.
- Added 140 / 160 / 180 / 200 kg weights.
- Added separate plate models for the new heavy weights.
- Added higher Strength requirements:
  - 140 kg -> Strength 12
  - 160 kg -> Strength 14
  - 180 kg -> Strength 16
  - 200 kg -> Strength 18


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


## v0.5.1 - Position-Based Stamina
- Bench holding stamina drain now depends on bar position.
- Holding the bar at lockout consumes the least stamina.
- Holding it around the middle consumes more stamina.
- Holding the bar near the chest consumes the most stamina.
- Kept the manual bench and failure mechanics introduced in v0.5.0.


## v0.5.2 - Plate System
- Added a more detailed bench press weight progression:
  40 / 50 / 60 / 70 / 80 / 90 / 100 / 120 / 140 / 160 / 180 / 200 kg.
- For example: 40 kg - bar + 1 green plate each site; 90 kg - bar + 1 blue + 1 yellow plates; 120 kg - 2 blue + 1 green plates.
- Added new plate combinations for each weight.
- Added green plates.
- Reworked bench models to visually represent the selected weight.
- Standardized plate thickness.


## v0.5.3 - Square Plates
- Made all bench press plates square, instead of rectangular.


## v0.5.4 - Bench Sounds
Added bench-press sounds using Minecraft metal/chain sounds:
- unrack
- lowering phase
- pressing phase
- rerack
- dropped/crushed bar clang
Heavier weights use a slightly lower pitch.


## v0.6.0 - Gym HUD
- New custom workout HUD rendered above the vanilla hotbar.
- Shows exercise + weight and rep counter.
- Separate smooth bars for rep progress, stamina and fatigue.
- Stamina changes color as it becomes low.
- Fatigue changes color as it becomes dangerous.
- Waiting state shows `ДЕРЖИ ПКМ`.
- Pinned-under-bar state switches to a red emergency panel with HP and `SHIFT — СБРОСИТЬ ШТАНГУ`.
- Removed the constantly-updating vanilla action-bar text from workout progress.


## v0.6.1 - HUD polish
- Workout state text moved into the top HUD row.
- HUD moved upward so vanilla set-finished actionbar text sits below it.
- Light bench weights (40-80 kg) use a green left accent; normal weights remain yellow; pinned remains red.


## v0.6.2 - HUD fix
- HUD stays visible after training while stamina recovers, and hides exactly at 100%.
- Recovery HUD displays live ST/FAT updates.
- Bench difficulty accent is relative to the player's currently unlocked maximum weight.


## v0.6.3 - Strength Damage
- Strength level now directly changes melee attack damage.
- Lv.1 starts weaker than a vanilla player: 0.5 base fist damage instead of 1.0.
- Each Strength level adds +0.2 base damage.
- The bonus/penalty also affects melee weapons because it is applied through GENERIC_ATTACK_DAMAGE.
- `/gymstats` now shows current bare-hand damage.

## v0.6.4 - Strength Decay
- Strength inactivity is tracked while the player is online.
- 3 Minecraft days (60 real minutes) without a successful strength-training rep are a grace period.
- After the grace period, every Minecraft day (20 real minutes) Strength XP decays by 2% (minimum 10 XP).
- Any successful dumbbell or bench-press rep resets the inactivity timer.
- /gymstats shows time until detraining / next decay tick.
- If a Strength level drops, melee damage is updated immediately.


## v0.6.5 - Water stamina
- Drinking a vanilla water bottle restores 25 stamina (up to 100).
- Recovery HUD updates immediately after drinking.


## v0.6.6 - Workout Hunger
- Training now consumes hunger due to vanilla-style exhaustion mechanics.
- Dumbbells: consumption depends on the weight of each successful rep.
- Bench press: hunger is consumed every second while holding the bar during a rep, as well as for the full rep itself.
- Water consumption is higher at the bottom position than at the middle or top positions.


## v0.6.7 - Higher Workout Hunger
- Increased hunger/exhaustion cost from dumbbell reps.
- Increased bench press hunger cost while holding the bar.
- Increased extra exhaustion for completed bench reps.
- Heavier weights and lower bar positions remain more demanding.


## v0.7.0 - Barbell Squat
- New squat rack block and barbell squat exercise.
- Shift + RMB on the rack cycles 40–200 kg using the same plate color system as bench press.
- RMB starts a set; hold RMB to move through the squat, release to freeze at the current depth, Shift ends/bails out.
- Three visual positions: standing/top, mid squat, bottom squat. The bar and player/camera move with the depth.
- New Legs stat, squat reps and squat PR in `/gymstats`.
- Squats train Strength + Legs, consume stamina/fatigue/hunger, and reset Strength detraining.
- Heavier/deeper squats drain more stamina and hunger. If stamina reaches zero at the bar, the player gets pinned in the bottom position and takes damage until Shift is pressed.
- Added unrack/lowering/pressing/rack/drop sounds and relative-difficulty HUD colors for squat.


## v0.7.1 - Tuned squat pose
- Arms are now pulled back instead of pointing forward.


## v0.7.2 - Tuned squat pose
- Squat pose arms moved outward to the sides to simulate holding the bar near its edges.


## v0.7.3 - Tuned squat pose
- Squat arms are now raised higher while keeping the wide side grip pose.


## v0.7.5 - Bent-elbow squat grip
- Added PlayerAnimator 0.4.0+1.16.5 and BendyLib 1.2.1 as nested Fabric dependencies.
- No separate PlayerAnimator/BendyLib install is required when using the built HardGym jar.
- Squat upper arms are positioned outward/back and the lower half bends at the elbow about 85 degrees toward the bar.
- Elbow bending is active only while the player is on the squat rack.


## v0.7.6 - Bent-elbow compatibility fix
- Removed compile-time/nested PlayerAnimator and BendyLib dependencies so Fabric Loom 0.7 can build HardGym again.
- Real elbow bending is now an optional runtime integration loaded through reflection.
- For bent elbows install PlayerAnimator Fabric 0.3.5+1.16.5 and BendyLib Fabric 1.2.1 as separate jars in `.minecraft/mods`.
- Without those libraries HardGym still starts; the squat simply uses the normal rigid-arm pose.


## v0.7.7 - stale PlayerAnimator source fix
- Added a dependency-free compatibility stub at the old SquatElbowAnimation path.
- This overwrites the stale v0.7.5 source when updating the same project folder.
- Real optional elbow bending remains handled by OptionalPlayerAnimator via reflection.


## v0.7.8 - tighter squat elbow bend
- Increased optional PlayerAnimator elbow bend for the squat pose.
- Adjusted upper-arm pose so hands sit closer to the bar and less on the plates.


## v0.7.9 - Multiplayer elbow animation
- Registers the optional PlayerAnimator/BendyLib squat elbow layer for every visible client-side player, not only the local player.
- Remote players doing squats now render the same bent-elbow pose for observers.
- Each observing client still needs PlayerAnimator + BendyLib installed for the mid-arm bend to render.


## v0.7.11 - Squat shoulders back
- Based directly on v0.7.9 multiplayer elbow sync.
- Keeps the exact v0.7.9 elbow bend.
- Retracts both shoulder pivots backward and slightly inward to imitate squeezed shoulder blades under the bar.


## v0.7.12 - shoulders farther back
- Keeps the v0.7.9 elbow bend unchanged.
- Retracts shoulder pivots much farther backward.
- Moves shoulders a little more inward to imitate stronger scapular retraction.


## v0.7.13 - shoulders much farther back
- Elbow bend remains identical to v0.7.9.
- Shoulder pivots moved substantially farther backward and further inward.
- Intended to create a much stronger 'shoulder blades squeezed together' squat position.


## v0.7.14 - squat moved off the uprights and rotated
- Player anchor is shifted farther away from the squat uprights.
- Squat pose is rotated 180° during use, including multiplayer render sync.
- First-person squat camera follows the new position/orientation.


## v0.7.15 - squat bar shifted away from the uprights
- Shifted the squat bar and all plates farther away from the rack uprights.
- Keeps the player offset + 180° orientation from v0.7.14.
- Updated model variants: 36.


## v0.7.16 - squat bar stays on rack when idle, shifts onto the back when active
- Restored the idle squat-rack bar position on the stands.
- Added separate active model variants: when the rack becomes ACTIVE during a squat, the bar/plates shift onto the player's back.
- Mounted variants generated: 36.


## v0.7.18 - squat bar moved much farther from the rack
- On-rack position stays the same.
- Once the player takes the bar, the active squat bar/plates move much farther onto the back.
- Applied offset to mounted squat models only: **-6.0 on Z** from the original v0.7.16 position.


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


## v0.8.1 - Deadlift knee bend trajectory
- Added real mid-leg bending through the existing optional PlayerAnimator + BendyLib bridge.
- Floor position has the strongest knee bend, knee-height position is partially extended, lockout is nearly straight.
- Retuned upper-leg and torso angles to follow a more natural floor → knees → lockout deadlift trajectory.
- Squat elbow animation and multiplayer registration remain intact.


## v0.8.2 - Deadlift multiplayer animation sync
- Server now broadcasts which player owns each active deadlift bar.
- Remote clients bind the deadlift pose and BendyLib knee bend by player UUID instead of relying only on interpolated position.
- Added a slightly wider positional fallback for missed/late packets.
- No deadlift gameplay/balance values were changed.


## v0.8.4 - Deadlift hip-pivot fix
- Reverted the incorrect v0.8.3 thigh-direction experiment.
- Keeps the v0.8.2 knee bend and leg trajectory.
- Fixes torso/leg separation by translating the upper-body pivots so the torso rotates around the hips instead of around the shoulders/top of the torso.
- Head and arm pivots follow the same hip compensation, keeping the upper body together.
- Multiplayer deadlift sync remains enabled.


## v0.8.5 - Deadlift start-leg tuning
- Focuses only on the bottom deadlift position.
- Keeps the hip-pivot torso fix from v0.8.4.
- Moves the upper legs much farther forward at ANIM 0 so the thigh is visually closer to parallel with the ground.
- Knee bend values remain the same (~60 degrees at the bottom with PlayerAnimator/BendyLib).


## v0.8.6 - Deadlift bottom-leg direction fix
- Corrected the sign of the upper-leg rotation in the lowest deadlift phase.
- Thighs now rotate toward the bar/front instead of kicking backward.
- Knee bend remains unchanged from v0.8.5 (~60° target).
- Other deadlift phases and multiplayer sync are unchanged.


## v0.8.7 - Deadlift start shin fix
- Kept the v0.8.6 thigh position for the lowest deadlift phase.
- Flipped and retuned the knee bend only at the floor position so the shin moves forward, aiming for a near-vertical lower leg.
- Intended result: shin closer to perpendicular to the ground and the foot staying on the ground.
- Mid and top deadlift phases unchanged.


## v0.8.8 - compile fix
- Fixed an accidental literal \n sequence in OptionalPlayerAnimator comments that commented out the kneeBend declaration.
- Deadlift shin tuning from v0.8.7 is unchanged.


## v0.8.9 - Deadlift bottom shin vertical tune
- Keeps the v0.8.6 thigh angle unchanged.
- Uses the shin bend direction confirmed by the user's manual sign flip.
- Reduces bottom knee bend magnitude to 0.62 rad so the shin should sit closer to perpendicular to the ground.
- The more vertical shin also lowers the foot toward the ground without moving the thigh.
- Mid and lockout phases remain unchanged.


## v0.8.10 - Deadlift start feet grounded
- Uses the user-tested bottom knee bend of 1.3F.
- Keeps the v0.8.6 thigh position.
- Lowers the whole rendered player only in the bottom deadlift phase from -0.12D to -0.28D so the feet sit on the ground.
- Mid and lockout phases are unchanged.


## v0.8.11 - deadlift torso + vertical arms fix
- Keeps the working bottom leg setup and knee bend 1.3F.
- Bottom torso pitch increased from 0.78F to 0.95F for a stronger forward hinge.
- Bottom arm pitch set to 0.0F so the arms hang vertically toward the bar.
- Feet-grounding renderer offset from v0.8.10 is unchanged.
- Body/arm rotation is applied in BipedEntityModelMixin, not the BendyLib BEND transform.


## v0.8.12 - Deadlift mid-phase tuning
- First/bottom phase from v0.8.11 is unchanged.
- Mid phase torso leans farther forward while hips stay back.
- Mid phase arms are vertical (arm pitch 0).
- Mid phase thigh pitch and knee bend are matched (-0.55 / +0.55) so the shin renders close to vertical.
- Lockout phase unchanged.


## v0.8.13 - Deadlift mid-phase bar grip alignment
- First phase is unchanged.
- Second/mid phase body animation from v0.8.12 is unchanged.
- Only the a1 deadlift bar + plates are shifted toward the player by +3 model units on local Z so the bar sits back in the hands.
- Updated 12 model files / 72 bar-or-plate elements.


## v0.8.14 - Deadlift top grip tune
- First and second phases are unchanged.
- At lockout the straight arms are shifted slightly forward toward the bar.
- The top-phase bar keeps its top height, but uses the mid-phase horizontal position and is moved 1 model unit closer to Steve.
- Updated top bar models: 12; moved bar/plate elements: 72.


## v0.8.15 - Deadlift top arm angle
- Keeps the shoulder pivot in the same place at lockout.
- Removes the v0.8.14 shoulder translation.
- Rotates the straight arms themselves forward by about 0.28 rad (~16°) toward the bar.
- Top bar position from v0.8.14 is unchanged.
- First and second deadlift phases are unchanged.


## v0.8.16 - Deadlift top bar closer
- Body and arm pose from v0.8.15 are unchanged.
- First and second deadlift phases are unchanged.
- Only the top-phase bar and plates were moved 2 additional model units closer to Steve.
- Updated model files: 12; adjusted elements: 72.


## v0.8.17 - Deadlift top bar fine tune
- Body pose and arm pose from v0.8.16 are unchanged.
- Only the top deadlift phase was adjusted.
- The bar and plates were moved 1 extra model unit closer to Steve so the bar sits better in the hands and sticks out less.
- Updated model files: 12; adjusted elements: 72.


## v0.8.18 - Gaps between weight
- Added small gaps between weight plates on squat rack and deadlift bar models to match bench press spacing.


## v0.8.19 - Heavy weights
- Bench press extended to 220 kg: 220 = 5 blue plates per side.
- Squat and deadlift extended to 300 kg.
- 220 = 5 blue; 240 = 5 blue + 1 green; 260 = 6 blue; 280 = 6 blue + 1 green; 300 = 7 blue per side.
- Plate gaps from v0.8.18 are preserved on all newly generated squat/deadlift models.
- Progression requirements were extended for the new weights.


## v0.9.0 - Exercise-specific physical stats
- Bench press progression now controls attack damage. New players start at 0.5 base damage vs vanilla 1.0.
- Squat progression now controls movement speed. New players start at 85% of vanilla movement speed.
- Deadlift progression now controls jump height. New players start at roughly 66% of vanilla jump height.
- Overall Strength now controls maximum health. New players start at 8 hearts vs vanilla 10; higher Strength adds health up to 20 hearts.
- Added separate Bench/Squat/Deadlift XP with migration from existing save data.
- /gymstats now displays all four physical effects.


## v0.9.1 - Default starting jump
- Starting jump is now exactly vanilla Minecraft jump power (100%).
- Deadlift progression still increases jump height above vanilla.
- Other starting stats remain below vanilla as before.


## v0.9.2 - %1RM rep system
- Reworked barbell stamina from absolute kilograms to relative % of estimated 1RM.
- Uses the strength column table: 100%=1; 95%=1-2; 90%=2-3; 85%=4-5; 80%=6-7; 75%=8-9; 70%=10-11; 65%=12-17; 60%=18-25; 55%=26-29; 50%=30-37.
- Example: 100 kg x5 estimates roughly 118 kg 1RM; 70 kg is then ~60%, giving about 22 reps when fully rested instead of ~8.
- Bench/squat static pauses still drain stamina by bar position; moving reps no longer get double-charged by the hold drain.
- Deadlift rep stamina is also %1RM-based; pulling still costs hunger.
- Estimated 1RM is stored separately for bench, squat and deadlift and shown in /gymstats. Old PRs migrate as approximate 5RM values.


## v0.9.3 - Persistent fatigue system
- FAT now acts as between-set fatigue: it rises during a set and reduces the rep target of the NEXT set.
- 0 FAT = full %1RM calculator reps; ~30 FAT = about 75% of normal reps; ~60 FAT = about half; ~90 FAT = about a quarter.
- A hard full set builds roughly 30 FAT regardless of whether it is a heavy low-rep or lighter high-rep set.
- Passive FAT recovery slowed from 2%/second to 1% every 10 seconds. Stamina still recovers quickly.
- Food reduces FAT by roughly 2% per hunger point restored (e.g. steak ~16 FAT).
- Sleeping for at least ~2 seconds and then waking restores up to 60 FAT.
- FAT 95+ still blocks starting a new barbell set.


## v0.9.4 - Pinned fatigue
- Failing a bench press or squat and getting pinned now immediately adds a weight-scaled FAT penalty.
- Bench example: 200 kg gives about +13 FAT instantly, then about +4 FAT per second while pinned.
- Squat example: 300 kg gives about +15 FAT instantly, then about +5 FAT per second while pinned.
- FAT is clamped to 100 by the existing player-data system.
- Deadlift failure is unchanged: the bar is dropped rather than pinning/damaging the player.


## v0.9.5 - Lighter set fatigue
- A full normal set now adds about 10 FAT instead of about 30.
- FAT is distributed across the reps, so it still rises during the set.
- Very high-rep sets add FAT periodically and still total 10 at the full target.
- Pinned/failure FAT penalties remain intentionally much stronger and are unchanged.
- Passive recovery, food recovery, and sleep recovery are unchanged.


## v0.9.6 - Full-sleep FAT recovery
- Getting into bed and leaving early no longer restores fatigue.
- Sleep FAT recovery is applied only after the world actually skips to the next Minecraft day.
- Full sleep still restores up to 60 FAT.
- Food and passive FAT recovery are unchanged.


## v0.9.7 - Sleep deprivation fatigue
- Missing one full night adds 30 FAT.
- Consecutive missed nights add 45, 60, 75, then 90 FAT.
- From the fifth consecutive missed night onward, the nightly penalty stays capped at 90 FAT.
- A completed night of sleep resets the consecutive missed-night streak to zero and still restores up to 60 FAT.
- Simply entering and leaving a bed does not reset the streak.
- Missed-night state is persisted in player NBT and copied through respawn.


## v0.9.8 - Lower food FAT recovery
- Food now restores much less fatigue.
- Recovery is roughly half the food's hunger value, rounded up.
- Example: steak restores 4 FAT instead of 16; bread 3 instead of 10; apple 2 instead of 8.
- Sleep remains the primary fast way to recover fatigue.

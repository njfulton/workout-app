# Routine Design Handoff

This document gives a new Claude session everything it needs to design a training routine for this app. It covers the import format the app expects, the data model behind it, and the current state of the user's setup.

## Who this is for

Nick uses this Android workout tracker daily. He's on a PPL (Push/Pull/Legs) hypertrophy program. The app tracks workouts on both phone and Pixel Watch, with scheduling, PR detection, and progression tracking.

## How to get a routine into the app

The app has a text import feature (Import screen under Utilities). You paste structured text and it parses it into templates + a weekly schedule. **This is the easiest path** -- just produce text in the right format and Nick can paste it in.

### Text format specification

```
Routine: Push Day
---
Bench Press: 4x8 rest 2 min
Incline Dumbbell Press: 3x10 rest 90s
A1 Lateral Raise: 3x12 rest 0s
A2 Face Pull: 3x12 rest 60s
Tricep Pushdown: 3x12 rest 60s

Routine: Pull Day
---
Deadlift: 4x5 rest 3 min
Barbell Row: 4x8 rest 2 min
Lat Pulldown: 3x10 rest 90s
B1 Hammer Curl: 3x12 rest 0s
B2 Reverse Curl: 3x12 rest 60s

Routine: Leg Day
---
Barbell Squat: 4x6 rest 3 min
Romanian Deadlift: 3x10 rest 2 min
Leg Press: 3x12 rest 90s
Leg Curl: 3x10 rest 60s
Calf Raise: 4x15 rest 60s
```

### Format rules

**Routine header:** Each routine block starts with `Routine: Name` (or `Name: Name`) followed by `---` on the next line or inline (`Routine: Push Day---`).

**Exercise line:** `[SupersetTag] Exercise Name: SetsxReps rest RestTime`

| Component | Format | Examples | Notes |
|---|---|---|---|
| Superset tag | Optional `A1`, `A2`, `B1`, `B2` | `A1 Lateral Raise` | Letter groups exercises; A=superset 1, B=superset 2 |
| Exercise name | Free text before the colon | `Bench Press`, `Incline DB Press` | Matched against 80+ built-in exercises (see list below) |
| Sets x Reps | `NxM`, `NXM`, `N*M` | `4x8`, `3x6-8` | Rep ranges use the high end (6-8 becomes 8) |
| Rest time | `rest Ns`, `rest N sec`, `rest N min` | `rest 90s`, `rest 2 min` | Default 90s if omitted |

**Lines that are skipped:** Lines starting with `Phase`, `Week`, `Progression`, `---`, `===`, `#`, `//`, `Description:`, or lines containing RPE/RIR/deload keywords without a sets-x-reps pattern.

**Numbering/bullets are stripped:** `1. Bench Press: 4x8` and `- Bench Press: 4x8` both work.

### Exercise name aliases

The parser resolves common abbreviations:
- `DB` -> `Dumbbell`, `BB` -> `Barbell`
- `RDL` -> `Romanian Deadlift`
- `OHP` -> `Overhead Press`
- Hyphens are replaced with spaces

If an exercise name doesn't match anything in the database, a new custom exercise is created automatically.

### Phase/progression support (optional)

You can append progression phases after the routines:

```
10-Week Progression Framework
Phase 1, Weeks 1-3 -- Accumulation
Phase 2, Weeks 4-6 -- Intensification
  anchor sets to 5
  accessories cut to 2 sets
Phase 3, Weeks 7-9 -- Peaking
  anchor sets to 3
Phase 4, Week 10 -- Deload
  anchor sets to 2
  accessories cut to 2 sets
```

Phase modifications create variant templates (e.g., "Push Day (Intensification)") with adjusted set counts. The schedule maps each week to the correct variant.

**Modification syntax:**
- `anchor sets to N` -- changes the first exercise's set count
- `accessories cut to N sets` -- changes all non-anchor exercises
- `on Day 1 and Day 2` -- limits changes to specific routines (0-indexed)
- `use routine <Name>` -- swaps the entire rotation to a named routine for those weeks (mutually exclusive with set modifiers)

### Routine swap by phase

You can define a secondary routine (e.g., travel/bodyweight) and swap to it for specific weeks. This is useful for planned breaks, travel, or deload blocks with different exercises:

```
Routine: Push Day
---
Bench Press: 4x8 rest 2 min
Incline Dumbbell Press: 3x10 rest 90s

Routine: Pull Day
---
Barbell Row: 4x8 rest 2 min
Lat Pulldown: 3x10 rest 90s

Routine: Leg Day
---
Barbell Squat: 4x6 rest 3 min
Romanian Deadlift: 3x10 rest 2 min

Routine: Travel Maintenance
---
Push-Up: 3x15 rest 60s
Bulgarian Split Squat: 3x12 rest 60s
Dumbbell Row: 3x12 rest 60s

10-Week Program
Phase 1, Weeks 1-4 -- Accumulation
Phase 2, Weeks 5-6 -- Travel
  use routine Travel Maintenance
Phase 3, Weeks 7-10 -- Intensification
```

During weeks 5-6, the "Travel Maintenance" template fills all day slots (cycling if fewer templates than slots — so a single travel routine repeats across Mon/Wed/Fri). Weeks 1-4 and 7-10 run the normal PPL rotation.

### Week count detection

Include something like `10-Week Program` or `8 week` in the text and the parser auto-detects it. Default is 10 weeks.

## Built-in exercises (80+)

These are already in the database and will match by name:

**Chest:** Bench Press, Incline Bench Press, Decline Bench Press, Dumbbell Bench Press, Incline Dumbbell Press, Dumbbell Fly, Cable Crossover, Push-Up, Chest Dip, Machine Chest Press

**Back:** Barbell Row, Dumbbell Row, Lat Pulldown, Seated Cable Row, Pull-Up, Chin-Up, T-Bar Row, Face Pull, Straight Arm Pulldown

**Shoulders:** Overhead Press, Dumbbell Shoulder Press, Lateral Raise, Front Raise, Rear Delt Fly, Arnold Press, Upright Row

**Biceps:** Barbell Curl, Dumbbell Curl, Hammer Curl, Preacher Curl, Concentration Curl, Cable Curl, Incline Dumbbell Curl

**Triceps:** Tricep Pushdown, Skull Crusher, Overhead Tricep Extension, Dip, Close Grip Bench Press, Tricep Kickback

**Quads:** Barbell Squat, Front Squat, Leg Press, Leg Extension, Goblet Squat, Bulgarian Split Squat, Hack Squat

**Hamstrings:** Romanian Deadlift, Leg Curl, Good Morning, Nordic Hamstring Curl

**Glutes:** Hip Thrust, Glute Bridge, Cable Kickback

**Calves:** Calf Raise, Seated Calf Raise

**Abs:** Plank, Hanging Leg Raise, Cable Crunch, Ab Wheel Rollout, Russian Twist, Bicycle Crunch, Decline Sit-Up

**Full Body:** Deadlift, Clean and Press, Kettlebell Swing, Farmer's Walk, Turkish Get-Up

## What the import creates

When Nick pastes text and imports:

1. **WorkoutTemplate** per routine (e.g., "Push Day") with **TemplateExercise** entries for each exercise (preserving order, sets, reps, rest, superset groups)
2. **ScheduledWorkout** entries populating the calendar for N weeks
3. **SavedRoutine** record (stores the original text for re-import later)
4. Day assignments are configurable in the import UI (defaults: 3 routines = Mon/Wed/Fri)

## What Nick's current setup looks like

- **Current routine:** PPL Hypertrophy, Week 4 of 6
- **Schedule:** 3-day split across the week with the routine block + 7-day ribbon visible on the home screen
- **Design system:** "Sweat" -- lime (#D4FF3D) accent on near-black, Inter font, JetBrains Mono for numerals in the design (not yet in the app font stack)
- **Watch:** Pixel Watch 3, receives workout state from phone
- **Data:** 187+ historical workout sessions imported from JEFIT, plus sessions logged in this app

## Recommendations for the next routine

When designing Nick's next program, output the routine in the exact text format above so he can paste it directly into the Import screen. Include:

1. `Routine: Name` headers with `---` separators
2. Every exercise with `SetsxReps rest Time` format
3. Superset tags (`A1`/`A2`) where appropriate
4. A progression framework at the end if the program has phases
5. A `N-Week` mention so the parser picks up the duration

The app handles the scheduling, progression tracking, PR detection, and watch sync automatically from there.

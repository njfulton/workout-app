# Workout App UX Improvements - Implementation Plan

## Overview
9 user stories transforming the active workout experience into a focused, single-exercise view with smart defaults.

## Key Architectural Changes

### 1. ActiveExercise gets template metadata
Add `targetSets` and `targetReps` fields (populated from TemplateExercise when starting from template, null for ad-hoc).

### 2. Focused exercise view with navigation
Replace the scrollable LazyColumn of all exercises with a single-exercise/superset view. Add:
- `currentExerciseGroupIndex` to ActiveWorkoutState
- Next/Back buttons at bottom of screen
- "Exercise List" button in top bar → opens a bottom sheet showing all exercises with completion status, allowing jump-to

### 3. No schema changes needed
All features work with the existing DB schema. The `updateSetLog` DAO method already exists for editing sets.

---

## Implementation Steps

### Step 1: Fix destructive migration (Story 1)
**File:** `WorkoutDatabase.kt`
- Remove `.fallbackToDestructiveMigration()`
- Add empty migration stubs for v1→v2, v2→v3, v3→v4 (existing versions)
- Future schema changes will require explicit migrations

### Step 2: Add targetSets/targetReps to ActiveExercise (Stories 4, 6, 8)
**File:** `WorkoutViewModel.kt`
- Add `targetSets: Int?` and `targetReps: Int?` to `ActiveExercise` data class
- Populate from `TemplateExercise` during `startWorkout()` when template-based
- Leave null for ad-hoc exercises

### Step 3: Add currentExerciseGroupIndex + focused navigation (Stories 5, 6)
**File:** `WorkoutViewModel.kt`
- Add `currentExerciseGroupIndex: Int` to `ActiveWorkoutState`
- Add `nextExercise()`, `previousExercise()`, `jumpToExercise(index)` functions
- Add `markExerciseDone(exerciseLogId)` for ad-hoc workouts
- Add auto-advance logic: after logging a set, if sets.size >= targetSets, auto-advance

### Step 4: Add updateSet function (Story 7)
**File:** `WorkoutViewModel.kt`
- Add `updateSet(setLog: SetLog)` that calls `repository.updateSetLog()` and updates local state

### Step 5: Rest timer enhancements (Stories 2, 9)
**File:** `WorkoutViewModel.kt`
- Add sound playback when timer reaches 0 (use Android MediaPlayer with system notification sound)
- Timer already auto-stops; we just need to make the UI changes

### Step 6: Rewrite ActiveWorkoutScreen UI (Stories 2, 3, 4, 5, 6, 7, 8, 9)
**File:** `ActiveWorkoutScreen.kt` — major rewrite

**Top bar:** Workout name, elapsed time, "Exercise List" button (opens bottom sheet)

**Main content — single exercise/superset view:**
- Exercise name + "Set X of Y" (or just set count for ad-hoc)
- Last workout summary (already added)
- Overload suggestion
- Logged sets list — each set tappable for inline editing (shows editable reps/weight fields)
- Input area for next set:
  - Reps pre-filled from targetReps (template) or blank (ad-hoc)
  - Weight pre-filled from last completed set (this session first, then history)
  - Warmup checkbox
  - "Log Set" button
- For ad-hoc: "Mark Done" button when user is finished with exercise

**Rest timer overlay:**
- When active: prominent card/overlay with countdown, "Skip" button
- When done: plays sound, card disappears
- Keep screen on: add `KeepScreenOn()` side effect

**Bottom navigation:**
- Back button (disabled on first exercise)
- Progress indicator: "Exercise 2 of 5"
- Next button (disabled on last exercise, or auto-advances)

**Exercise list bottom sheet:**
- Shows all exercises with checkmarks for completed ones
- Tap to jump to any exercise
- Shows set progress (e.g., "3/4 sets") for each

### Step 7: Keep screen on (Story 3)
**File:** `ActiveWorkoutScreen.kt`
- Use Compose `DisposableEffect` with `WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON`
- Only active while on ActiveWorkoutScreen

# Workout Tracker

Android app for tracking strength training workouts with template-based programming, schedule management, and routine import.

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material3
- **Database**: Room (SQLite)
- **Architecture**: MVVM (ViewModel + Repository + DAO)
- **Navigation**: Compose Navigation
- **Min SDK**: 26 (Android 8.0) / **Target SDK**: 34
- **Compose BOM**: 2024.01.00
- **Kotlin Compiler Extension**: 1.5.7

## Features

### Workout Tracking
- Start workouts from templates or ad-hoc
- Log sets with reps, weight, duration, distance
- Warmup set tagging
- Rest timer with configurable durations
- Superset support (grouped exercises)
- Progressive overload suggestions based on recent performance

### Exercise Library
- Pre-seeded exercise database with categories (Strength, Cardio, Flexibility, Bodyweight)
- 14 muscle groups (Chest, Back, Shoulders, Biceps, Triceps, etc.)
- Custom exercise creation
- Search and filter by category/muscle group

### Templates
- Create workout templates with target sets, reps, and rest times
- Superset grouping via tags
- Edit and reorder exercises
- Start workouts directly from templates

### Routine Import
- Paste routine text and auto-parse exercises, sets/reps, rest times, supersets
- Two-phase import: parse first, then configure schedule
- Start date picker
- Per-routine day-of-week assignment with smart defaults
- Auto-detects week count from text (e.g. "10-Week Program")
- Phase/progression text extraction

### Schedule (Calendar View)
- Monthly calendar grid with navigation
- Green checkmark for completed workouts
- Red X for missed/skipped workouts
- Template name preview on upcoming days
- Tap any day for details and actions (Start, Done, Skip)
- Clear future schedule (preserves completed history)
- Manual scheduling via FAB (templates, rest days, cardio, custom labels)

### Saved Routines
- Routines auto-saved on import with raw text, day assignments, and week count
- Expandable cards showing routine details and day-of-week mappings
- Usage history with date ranges (when you ran each routine)
- Editable notes field for recording thoughts on a routine
- One-tap re-import with option to clear future schedule first

### Workout History
- Chronological workout log with exercise counts and durations
- Detailed workout view with all sets/reps/weights
- Exercise history tracking
- CSV export

### Backup & Restore
- Full JSON export of all database tables to Downloads folder
- Share via Android share sheet (Google Drive, email, etc.)
- Import from JSON file with smart merge (exercises matched by name, no duplicates)
- Preserves all foreign key relationships via ID remapping

## Database Schema

8 entities across 5 DAOs:

| Entity | Purpose |
|---|---|
| `Exercise` | Exercise library (name, category, muscle group, equipment) |
| `WorkoutTemplate` | Named workout templates |
| `TemplateExercise` | Exercises within a template (sets, reps, rest, superset group) |
| `WorkoutLog` | Completed workout sessions |
| `ExerciseLog` | Exercises performed in a workout |
| `SetLog` | Individual sets (reps, weight, duration, distance, warmup flag) |
| `ScheduledWorkout` | Calendar schedule entries (completed, skipped, labels) |
| `SavedRoutine` | Imported routine text with config for re-import |
| `RoutineUsageHistory` | Date ranges of when each saved routine was used |

Database uses `fallbackToDestructiveMigration()` — schema changes will wipe local data. Use Backup & Restore to preserve data across updates.

## Project Structure

```
app/src/main/java/com/workout/tracker/
├── MainActivity.kt                 # Compose entry point
├── WorkoutApp.kt                   # Application class, DB + repository init
├── data/
│   ├── BackupManager.kt            # JSON export/import for all tables
│   ├── Converters.kt               # Room type converters (enums)
│   ├── ExerciseSeedData.kt          # Default exercise library
│   ├── JefitImporter.kt            # Legacy JEFIT data import
│   ├── WorkoutDatabase.kt          # Room database (version 5)
│   ├── dao/                         # 5 DAO interfaces
│   ├── entity/                      # 9 Room entities
│   └── repository/
│       └── WorkoutRepository.kt     # Single repository wrapping all DAOs
├── ui/
│   ├── navigation/
│   │   └── Navigation.kt           # Screen routes + NavHost (15 routes)
│   ├── screens/                     # 13 Compose screens
│   ├── theme/
│   │   └── Theme.kt
│   └── viewmodel/                   # 4 ViewModels
```

## Screens

| Screen | Route | Description |
|---|---|---|
| Home | `home` | Dashboard with quick actions, upcoming schedule, recent workouts |
| Exercises | `exercises` | Browse/search exercise library |
| Templates | `templates` | View/manage workout templates |
| Create Template | `create_template` | Build a new template |
| Edit Template | `edit_template/{id}` | Edit existing template |
| Start Workout | `start_workout` | Choose template to start |
| Active Workout | `active_workout` | Live workout logging |
| Quick Log | `quick_log` | Fast workout entry |
| History | `history` | Past workout logs |
| Workout Detail | `workout_detail/{id}` | Individual workout details |
| Schedule | `schedule` | Monthly calendar view |
| Import Routine | `import_routine` | Paste and configure routine text |
| Saved Routines | `saved_routines` | Previously imported routines |
| Backup & Restore | `backup_restore` | Export/import JSON backups |

## Setup

1. Open in Android Studio (Hedgehog or later)
2. Sync Gradle
3. Run on device/emulator (API 26+)

No API keys or external services required. All data is stored locally in Room.

## Permissions

- `VIBRATE` — rest timer haptic feedback

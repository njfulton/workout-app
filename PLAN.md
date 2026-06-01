# Implementation Plan: Import Options + Calendar View

## Feature 1: Import Screen — Start Date & Day-of-Week Mapping

### Current behavior
- `importRoutineFromText()` parses routines, creates templates, and auto-schedules starting from next Monday
- Day mapping is hardcoded based on routine count (2→Mon/Wed, 3→Mon/Wed/Fri, etc.)

### Changes

**A. Two-phase import flow on ImportRoutineScreen.kt**
1. Phase 1 (current): User pastes text, clicks "Import & Parse"
2. Phase 2 (new): After parsing, show a **configuration step** before generating the schedule:
   - **Start date picker** — defaults to today
   - **Per-routine day picker** — for each parsed routine name, show a row of day-of-week toggles (Mon–Sun). Pre-select based on the current heuristic so it's not blank.
   - **Week count** — auto-detected from text, shown as editable field
   - "Generate Schedule" button to confirm

**B. Split TemplateViewModel import logic**
- New method: `parseRoutineText(text): List<ParsedRoutineInfo>` — returns routine names + detected week count without creating anything yet. This feeds Phase 2 UI.
- Modify `importRoutineFromText()` to accept new params:
  ```kotlin
  fun importRoutineFromText(
      text: String,
      startDate: LocalDate,
      dayAssignments: Map<Int, List<DayOfWeek>>  // routineIndex → days
  )
  ```
- Update `generateScheduleWithProgression()` to use the user-specified start date and day assignments instead of hardcoded patterns.

### Files to modify
- `ImportRoutineScreen.kt` — add Phase 2 configuration UI
- `TemplateViewModel.kt` — split parse/import, accept day assignments + start date

---

## Feature 2: Calendar View (replaces Schedule list)

### Design
Replace the current `LazyColumn` list in `ScheduleScreen.kt` with a **monthly calendar grid**.

**Calendar grid layout:**
- Header: `< March 2026 >` with month navigation arrows
- Day-of-week headers: Mon Tue Wed Thu Fri Sat Sun
- 5–6 rows of day cells in a grid

**Day cell contents:**
- **Past completed workout**: Green checkmark
- **Past missed workout** (scheduled but not completed, date has passed): Red X
- **Past skipped workout**: Red X (same as missed)
- **Today**: Highlighted border/background
- **Future scheduled workout**: Template name (truncated) as small text, e.g. "Push Day"
- **Rest day**: Subtle indicator or empty
- **No workout**: Empty cell

**Interactions:**
- Tap a day → show bottom sheet or dialog with full details (workout name, status, action buttons: Start/Done/Skip/Move — reuse existing action logic from ScheduleItemCard)

### Implementation

**A. ScheduleViewModel changes**
- Add `getScheduleForMonth(year, month)` — queries `getScheduleBetween()` for the full month range
- Add state for `currentMonth: YearMonth` and `monthSchedule: List<ScheduledWorkoutWithTemplate>`
- Determine "missed" status: scheduled date < today AND !isCompleted AND !isSkipped

**B. ScheduleScreen.kt rewrite**
- Replace LazyColumn with a custom calendar Composable
- `CalendarGrid` composable:
  - Takes `YearMonth`, `List<ScheduledWorkoutWithTemplate>`, callbacks
  - Renders 7-column grid using `Column` + `Row` (no LazyGrid needed for 42 cells)
  - Each cell shows status indicator
- Keep FAB for manual scheduling
- Day tap opens a detail dialog/bottom sheet with existing action buttons

**C. ScheduleDao** — already has `getScheduleBetween()`, no changes needed

### Files to modify
- `ScheduleScreen.kt` — full rewrite to calendar view
- `ScheduleViewModel.kt` — add month-based query + state

---

## Implementation Order
1. TemplateViewModel — split parse from import, add new params
2. ImportRoutineScreen — add Phase 2 config UI
3. ScheduleViewModel — add month query
4. ScheduleScreen — calendar view rewrite

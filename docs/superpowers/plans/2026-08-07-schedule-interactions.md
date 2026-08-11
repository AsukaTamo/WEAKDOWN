# Schedule Interactions Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the schedule page respond to horizontal week swipes, wire the guide toggle to the visible grid, and let week pickers plus FAB menus dismiss from outside taps.

**Architecture:** Keep the current state in `ScheduleViewModel` as the single source of truth for the active week and guide visibility. Update the schedule screen to consume that state for both rendering and swipe gestures, and wrap transient menus in a full-screen dismiss layer so blank-space taps close them without disturbing the existing content flow.

**Tech Stack:** Kotlin, Jetpack Compose, Material3, coroutines, existing ViewModel/StateFlow setup.

---

### Task 1: Wire schedule guide visibility into the grid

**Files:**
- Modify: `app/src/main/java/com/example/courseapp/ui/schedule/ScheduleScreen.kt`

- [ ] **Step 1: Update the guide-rendering branch**

```kotlin
if (showGuides) {
    for (i in 1 until periodCount) {
        Divider(
            modifier = Modifier.offset(y = SLOT_HEIGHT * i),
            color = palette.gridLine,
            thickness = 0.5.dp
        )
    }
}
```

- [ ] **Step 2: Verify the schedule still composes**

Run: `.\gradlew.bat assembleDebug`

### Task 2: Make week swipes change the active week without snackbar noise

**Files:**
- Modify: `app/src/main/java/com/example/courseapp/viewmodel/ScheduleViewModel.kt`
- Modify: `app/src/main/java/com/example/courseapp/ui/schedule/ScheduleScreen.kt`

- [ ] **Step 1: Remove the feedback emit from `selectWeek`**

```kotlin
fun selectWeek(week: Int) {
    _currentWeek.value = week
}
```

- [ ] **Step 2: Add a bounded relative-week helper**

```kotlin
fun shiftWeek(delta: Int) {
    val maxWeek = _activeSemester.value?.totalWeeks ?: Int.MAX_VALUE
    val nextWeek = (_currentWeek.value + delta).coerceIn(1, maxWeek)
    if (nextWeek != _currentWeek.value) {
        _currentWeek.value = nextWeek
    }
}
```

- [ ] **Step 3: Attach a horizontal drag detector to the schedule surface**

```kotlin
Modifier.pointerInput(currentWeek) {
    detectHorizontalDragGestures(
        onDragEnd = {
            if (accumulatedDrag <= -swipeThreshold) viewModel.shiftWeek(1)
            if (accumulatedDrag >= swipeThreshold) viewModel.shiftWeek(-1)
        }
    ) { change, dragAmount ->
        accumulatedDrag += dragAmount
        change.consume()
    }
}
```

- [ ] **Step 4: Verify the week label updates in place**

Run: `.\gradlew.bat assembleDebug`

### Task 3: Add blank-space dismissal for week pickers and FAB actions

**Files:**
- Modify: `app/src/main/java/com/example/courseapp/MainActivity.kt`
- Modify: `app/src/main/java/com/example/courseapp/ui/navigation/CourseAppScaffold.kt`
- Modify: `app/src/main/java/com/example/courseapp/ui/schedule/ScheduleScreen.kt`

- [ ] **Step 1: Wrap the week selector panel in a full-screen clickable overlay**

```kotlin
Box(Modifier.fillMaxSize().clickable(...) { showWeekDialog = false }) {
    Box(Modifier.align(Alignment.TopCenter)) {
        WeekSelectorDialog(...)
    }
}
```

- [ ] **Step 2: Wrap the FAB menu in a backdrop layer that closes on outside taps**

```kotlin
if (isOpen) {
    Box(Modifier.matchParentSize().clickable(...) { onToggle() })
}
```

- [ ] **Step 3: Verify blank-space taps dismiss both layers**

Run: `.\gradlew.bat assembleDebug`


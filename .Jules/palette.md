## 2023-10-25 - [Accessibility] Added contentDescription to IconButton in DiagnosticReportDialog
**Learning:** Found an `IconButton` (Close) with a `null` `contentDescription` inside `DiagnosticReportDialog.kt`. For proper accessibility in Android Jetpack Compose, icon-only buttons must have a descriptive `contentDescription` for screen readers like TalkBack, similar to adding an `aria-label` in web dev.
**Action:** Next time, scan for `contentDescription = null` in Jetpack Compose files to easily locate elements needing accessibility enhancements.

## 2024-05-19 - Added ARIA equivalents to interactive elements in Jetpack Compose
**Learning:** Found an accessibility issue where an icon-only `IconButton` in `OpenManusTelemetryDashboard.kt` for pausing the telemetry stream had a static, unhelpful contentDescription ("Freeze Stream" even when already paused).
**Action:** Replaced static contentDescription with dynamic state-based string: `if (isStreamPaused) "Resume Stream" else "Freeze Stream"` to accurately describe the button's action based on current state. Always ensure dynamic toggle icon buttons reflect their actionable state to screen readers, not just a static label.

## 2024-06-18 - [Accessibility] Unique contentDescription for list items
**Learning:** When adding `contentDescription` to interactive elements (like `IconButton`s) inside lists in Jetpack Compose, the generic descriptions (e.g., "Delete Task") should be replaced with contextual ones (e.g., "Delete task: ${task.title}") to provide better navigation and understanding for screen reader users.
**Action:** When working on lists or repeated elements in Jetpack Compose, ensure the `contentDescription` includes context specific to the item.

## 2026-09-08 - Context-aware ARIA equivalents in lists
**Learning:** In Jetpack Compose, when rendering a list of items, interactive elements (like IconButtons for adjusting quantity) often use generic labels like 'Deduct' or 'Receive'. This creates a poor experience for screen reader users who hear the same label repeated without knowing which item it applies to.
**Action:** Always append the item's unique title or name to the `contentDescription` (e.g., 'Deduct ${item.name}') for actions inside lists to ensure contextual clarity.

## 2026-09-08 - [Empty States] Actionable Empty States
**Learning:** Found an empty state in `ProjectDashboard.kt` that displayed a static message ("No tasks found...") without offering a way forward. This leaves users at a dead end when starting a new project.
**Action:** Always include a relevant Call-To-Action (CTA) button and contextual guidance within empty states to encourage interaction and reduce user friction.

## 2026-09-12 - [Empty States] Actionable Empty States in Garage
**Learning:** Found an empty state in `GarageScreen.kt` that displayed a static message ("Tap ADD VEHICLE to store a profile.") without offering a way forward directly within the empty state container. This leaves users at a dead end when starting out in the garage.
**Action:** Always include a relevant Call-To-Action (CTA) button and contextual guidance within empty states to encourage interaction and reduce user friction.

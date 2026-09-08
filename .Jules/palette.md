## 2023-10-25 - [Accessibility] Added contentDescription to IconButton in DiagnosticReportDialog
**Learning:** Found an `IconButton` (Close) with a `null` `contentDescription` inside `DiagnosticReportDialog.kt`. For proper accessibility in Android Jetpack Compose, icon-only buttons must have a descriptive `contentDescription` for screen readers like TalkBack, similar to adding an `aria-label` in web dev.
**Action:** Next time, scan for `contentDescription = null` in Jetpack Compose files to easily locate elements needing accessibility enhancements.
## 2024-05-19 - Added ARIA equivalents to interactive elements in Jetpack Compose
**Learning:** Found an accessibility issue where an icon-only `IconButton` in `OpenManusTelemetryDashboard.kt` for pausing the telemetry stream had a static, unhelpful contentDescription ("Freeze Stream" even when already paused).
**Action:** Replaced static contentDescription with dynamic state-based string: `if (isStreamPaused) "Resume Stream" else "Freeze Stream"` to accurately describe the button's action based on current state. Always ensure dynamic toggle icon buttons reflect their actionable state to screen readers, not just a static label.
## 2026-09-08 - Context-aware ARIA equivalents in lists
**Learning:** In Jetpack Compose, when rendering a list of items, interactive elements (like IconButtons for adjusting quantity) often use generic labels like 'Deduct' or 'Receive'. This creates a poor experience for screen reader users who hear the same label repeated without knowing which item it applies to.
**Action:** Always append the item's unique title or name to the `contentDescription` (e.g., 'Deduct ${item.name}') for actions inside lists to ensure contextual clarity.

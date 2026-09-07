## 2023-10-25 - [Accessibility] Added contentDescription to IconButton in DiagnosticReportDialog
**Learning:** Found an `IconButton` (Close) with a `null` `contentDescription` inside `DiagnosticReportDialog.kt`. For proper accessibility in Android Jetpack Compose, icon-only buttons must have a descriptive `contentDescription` for screen readers like TalkBack, similar to adding an `aria-label` in web dev.
**Action:** Next time, scan for `contentDescription = null` in Jetpack Compose files to easily locate elements needing accessibility enhancements.

## 2024-06-18 - [Accessibility] Unique contentDescription for list items
**Learning:** When adding `contentDescription` to interactive elements (like `IconButton`s) inside lists in Jetpack Compose, the generic descriptions (e.g., "Delete Task") should be replaced with contextual ones (e.g., "Delete task: ${task.title}") to provide better navigation and understanding for screen reader users.
**Action:** When working on lists or repeated elements in Jetpack Compose, ensure the `contentDescription` includes context specific to the item.

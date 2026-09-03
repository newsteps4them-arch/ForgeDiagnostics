## 2023-10-25 - [Accessibility] Added contentDescription to IconButton in DiagnosticReportDialog
**Learning:** Found an `IconButton` (Close) with a `null` `contentDescription` inside `DiagnosticReportDialog.kt`. For proper accessibility in Android Jetpack Compose, icon-only buttons must have a descriptive `contentDescription` for screen readers like TalkBack, similar to adding an `aria-label` in web dev.
**Action:** Next time, scan for `contentDescription = null` in Jetpack Compose files to easily locate elements needing accessibility enhancements.

## 2024-05-18 - [Accessibility] Contextual Content Descriptions for List Items
**Learning:** Found that generic content descriptions like "Delete Task" in list items are ambiguous for screen reader users. In Jetpack Compose, when rendering a list of items, interactive elements must have unique `contentDescription`s.
**Action:** Next time, always append the item title/name to the contentDescription (e.g., "Delete Task: Oil Change") for interactive list elements.

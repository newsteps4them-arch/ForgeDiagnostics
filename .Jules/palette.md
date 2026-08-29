## 2023-10-25 - [Accessibility] Added contentDescription to IconButton in DiagnosticReportDialog
**Learning:** Found an `IconButton` (Close) with a `null` `contentDescription` inside `DiagnosticReportDialog.kt`. For proper accessibility in Android Jetpack Compose, icon-only buttons must have a descriptive `contentDescription` for screen readers like TalkBack, similar to adding an `aria-label` in web dev.
**Action:** Next time, scan for `contentDescription = null` in Jetpack Compose files to easily locate elements needing accessibility enhancements.

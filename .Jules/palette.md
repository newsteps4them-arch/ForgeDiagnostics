## 2025-02-12 - Fix missing contentDescription on close IconButton in DiagnosticReportDialog
**Learning:** Found an `IconButton` where the inner `Icon` had `contentDescription = null`. In Jetpack Compose, icon-only interactive buttons should have a meaningful `contentDescription` for screen readers to accurately read the button's action.
**Action:** Always provide a descriptive string for `contentDescription` in interactive, icon-only buttons instead of `null`.

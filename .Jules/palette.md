## 2024-05-18 - Missing ARIA label in IconButtons
**Learning:** Found several `IconButton` components in Jetpack Compose UI that had their `Icon`'s `contentDescription` set to `null`, making them unlabelled for screen readers.
**Action:** When creating or modifying interactive components like `IconButton` in Jetpack Compose, always ensure a meaningful `contentDescription` is provided for accessibility unless explicitly decorative and accompanied by text.

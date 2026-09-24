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

## 2026-09-10 - Empty States and Interaction
**Learning:** Empty states in Jetpack Compose should not be plain text. Replacing a static "No tasks found" text with a structured component comprising an icon, descriptive text, and a direct Call-To-Action (CTA) button significantly enhances user interaction by reducing friction and providing a clear path forward.
**Action:** When encountering or designing empty states (e.g., empty lists or missing data scenarios), ensure they include a relevant icon, helpful context, and an actionable button aligned with the application's aesthetic.

## 2026-09-08 - [Empty States] Actionable Empty States
**Learning:** Found an empty state in `GarageScreen.kt` that displayed a static message ("No vehicles saved in garage yet.") without offering a way forward. This leaves users at a dead end when starting a new project or entering an empty page.
**Action:** Always include a relevant Call-To-Action (CTA) button and contextual guidance within empty states to encourage interaction and reduce user friction.

## 2024-05-19 - [Empty States] Actionable Empty States in Garage
**Learning:** Found an empty state in `GarageScreen.kt` that displayed a static message ("Tap ADD VEHICLE to store a profile.") without offering a way forward directly from the text. This leaves users at a dead end when starting to add a new vehicle profile.
**Action:** Replaced passive instructional text with a relevant Call-To-Action (CTA) button and contextual guidance within empty states to encourage interaction and reduce user friction.

## 2026-09-12 - [Empty States] Actionable Empty States in Garage
**Learning:** Found an empty state in `GarageScreen.kt` that displayed a static message ("Tap ADD VEHICLE to store a profile.") without offering a way forward directly within the empty state container. This leaves users at a dead end when starting out in the garage.

## 2026-10-25 - Jetpack Compose Empty States UX & Accessibility
**Learning:** Empty states with only text and a decorative icon cause high user friction, and screen readers fail to convey context if the primary visual element has a null contentDescription.
**Action:** Always provide a semantic `contentDescription` for primary empty state icons, and ensure a clear, actionable Call-To-Action (CTA) button is included directly inside the empty state component to encourage immediate user interaction.

## 2025-02-12 - Empty State Semantic Images
**Learning:** In Jetpack Compose empty states, a large central icon or image should not use `contentDescription = null`. It serves as the primary contextual anchor for the empty state block.
**Action:** Always provide a semantic content description (e.g., 'No tasks found') for the primary visual element in empty states to assist screen reader users navigating the block structure.

## 2024-10-24 - Jetpack Compose Icon Accessibility in Buttons
**Learning:** In Jetpack Compose, when an `Icon` component is placed inside a `Button` (or `OutlinedButton`, etc.) immediately alongside descriptive `Text` (e.g., Icon with `contentDescription="Print"` next to `Text("Print PDF")`), screen readers will read both the icon description and the text, resulting in redundant, annoying announcements for visually impaired users.
**Action:** Always set `contentDescription = null` for `Icon` components that are purely decorative or immediately accompanied by functionally identical descriptive text within the same focusable container. Only icon-only buttons (`IconButton`) require a descriptive `contentDescription`.

## 2024-05-14 - Empty State Accessibility Context
**Learning:** Empty states in Jetpack Compose that rely heavily on a primary central visual element (like an illustration or large Icon) need a semantic `contentDescription` (e.g., 'No tasks found' or 'No items found') rather than `null`. Using `null` deprives screen reader users of the primary context of the empty block before they encounter the secondary text details.
**Action:** Always verify that empty state primary icons/illustrations have descriptive accessibility labels instead of being marked as decorative.

## 2026-09-24 - Actionable Empty States
**Learning:** Found empty states in `GarageScreen.kt` and `PartsCatalogScreen.kt` that displayed a static message ('Tap ADD VEHICLE...') without offering a direct way forward. This leaves users at a dead end when starting out.
**Action:** Added a relevant Call-To-Action (CTA) button directly within empty states to encourage interaction and reduce user friction, rather than relying on global app bar buttons.

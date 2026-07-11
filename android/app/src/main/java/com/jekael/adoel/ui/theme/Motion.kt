package com.jekael.adoel.ui.theme

/** Named duration tokens for animations whose values are shared (or must stay hand-in-hand)
 * across files. The critical ones are the dismiss `delay(...)` constants that MUST equal the
 * matching exit-tween duration — before this file, each pair was two separate literals that
 * could silently drift apart when one side got retuned. Durations that appear exactly once,
 * inside a single component, stay inline there; a token should reflect real sharing. */
object Motion {
    /** Slide-in panels (Pengaturan, Statistik): enter tween. */
    const val PANEL_ENTER_MS = 260

    /** Slide-in panels: exit tween AND the dismiss delay that waits for it — one constant so
     * the panel can never be torn down mid-animation because someone retuned only one side. */
    const val PANEL_EXIT_MS = 220

    /** Floating dialogs (FloatingEditDialog, ConfirmDialog): dismiss delay that waits for the
     * exit animation to finish before invoking onDismiss. */
    const val DIALOG_DISMISS_MS = 180

    /** How long a toast stays on screen before auto-dismissing. */
    const val TOAST_VISIBLE_MS = 3500L

    /** Per-item delay step for radar-card list entrance stagger, and its cap so long lists
     * don't keep trickling in forever. */
    const val LIST_STAGGER_STEP_MS = 35L
    const val LIST_STAGGER_MAX_MS = 400L

    /** Per-bar delay step for the Statistik chart's entrance stagger. */
    const val CHART_STAGGER_STEP_MS = 50L
}

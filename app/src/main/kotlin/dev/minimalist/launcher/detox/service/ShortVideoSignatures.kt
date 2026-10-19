package dev.minimalist.launcher.detox.service

/**
 * ShortVideoSignatures — Registry of known short-video container view resource IDs.
 *
 * Used by DetoxAccessibilityService Engine 4 to detect and eject short-video feeds
 * via performGlobalAction(GLOBAL_ACTION_BACK) without blocking legitimate app usage.
 *
 * Stored as a constant map so IDs can be updated per-release without architectural changes.
 * Plan Section 6.4.1 specification.
 */
object ShortVideoSignatures {

    /**
     * Map of packageName → list of view resource IDs that indicate a short-video
     * container is visible in the foreground.
     *
     * Checked via AccessibilityNodeInfo.findAccessibilityNodeInfosByViewId().
     * Detection: any node with matching viewIdResourceName that isVisibleToUser.
     */
    val VIEW_ID_MAP: Map<String, List<String>> = mapOf(
        // Instagram — Reels / Clips viewer (active full-screen player only)
        "com.instagram.android" to listOf(
            "com.instagram.android:id/clips_viewer_view_pager",
            "com.instagram.android:id/reel_viewer_root",
            "com.instagram.android:id/clips_video_container"
        ),
        // YouTube — Shorts (active player containers only, not bottom navigation tab)
        "com.google.android.youtube" to listOf(
            "com.google.android.youtube:id/reel_recycler",
            "com.google.android.youtube:id/reel_player_page_container",
            "com.google.android.youtube:id/shorts_container"
        ),
        // Facebook — Reels (active player container only)
        "com.facebook.katana" to listOf(
            "com.facebook.katana:id/fb_shorts_viewer",
            "com.facebook.katana:id/reels_fullscreen_container"
        ),
        // Snapchat — Spotlight (active viewer only)
        "com.snapchat.android" to listOf(
            "com.snapchat.android:id/spotlight_view_container"
        ),
        // TikTok
        "com.zhiliaoapp.musically" to listOf(
            "com.zhiliaoapp.musically:id/video_feed_layout"
        ),
        "com.ss.android.ugc.trill" to listOf(
            "com.ss.android.ugc.trill:id/video_feed_layout"
        )
    )

    /** Debounce window in ms — prevent repeated back-press on single fling. */
    const val BACK_ACTION_DEBOUNCE_MS = 800L

    /** Inspection debounce in ms on TYPE_WINDOW_CONTENT_CHANGED. */
    const val INSPECTION_DEBOUNCE_MS = 250L
}

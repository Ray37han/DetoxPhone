package dev.minimalist.launcher.core.launcher

import android.app.Activity
import android.os.Bundle

/**
 * Invisible activity used to trigger the system's default launcher chooser dialog.
 *
 * How it works:
 * 1. Enable this activity's component via PackageManager.
 * 2. The system detects two HOME-capable activities → shows the launcher chooser.
 * 3. After the user picks a default, disable this component again.
 *
 * This activity is declared `android:enabled="false"` in the manifest
 * and never actually rendered.
 */
class FakeLauncherActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        finish()
    }
}

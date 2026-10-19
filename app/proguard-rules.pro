# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in the Android SDK's default proguard-android-optimize.txt.

# ── Room ─────────────────────────────────────────────────────────────────────
# Room generates code at compile time; no runtime reflection needed.
# Keep entity field names for schema export verification.
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }

# ── Hilt ──────────────────────────────────────────────────────────────────────
# Hilt-generated components and modules
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# ── Kotlin ────────────────────────────────────────────────────────────────────
-keep class kotlin.Metadata { *; }
-keepclassmembers class ** {
    @kotlin.Metadata *;
}

# ── DataStore ─────────────────────────────────────────────────────────────────
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite* { *; }

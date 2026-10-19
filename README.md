# Detox Phone 📱

A distraction-free Android launcher combined with a **6-Engine Behavioral Digital Detox Suite**. Replaces addictive feeds, infinite scrolling, and notification spam with a text-first, intentional operating environment.

## 🌟 Key Features

1. **Text-Only Launcher**: Fast, alphabetical list of apps with zero colorful icons or gamified badges.
2. **Pre-Launch Friction Gate**: 5-second mindful breathing pause + intentional check-in before addictive apps open.
3. **Micro-Timeboxing**: 1m, 5m, 10m, or 15m sessions enforced by monotonic system timers and hard ejection.
4. **Surgical Short Video Blocker**: Automatically exits YouTube Shorts, Instagram Reels, Facebook Reels, Snapchat Spotlight, and TikTok.
5. **Kinetic Scroll Distance Governor**: Measures scrolling distance in meters and introduces progressive dimming to break doomscrolling trance.
6. **Per-App Hardware Grayscale**: Automatically switches display to monochrome when opening distracting apps via Android Daltonizer.
7. **Quiet Digest Notification Vault**: Silences noisy alerts and bundles them into scheduled digests at 13:00 and 20:00.
8. **100% On-Device Privacy**: Zero trackers, zero cloud dependencies. All data remains in local Room SQLite tables.

---

## 🏗️ Architecture & Philosophy

Detox Phone uses a layered Clean Architecture built with Modern Android components:
- **UI**: Jetpack Compose, Material 3, Dark Monochrome palette, Zero icons.
- **Detox Behavioral Engines**:
  - **Friction Gate**: Breathing pause + mindful intentional check-in before app launch.
  - **Micro-Timeboxing**: Enforced 1m–15m session timers with hard ejection.
  - **Short Video Blocker**: Real-time accessibility filter for YouTube Shorts, Reels, TikTok.
  - **Scroll Governor**: Real-time kinetic meter tracking with progressive visual dimming.
  - **Grayscale Daltonizer**: Automated monochrome hardware screen tuning for selected apps.
  - **Notification Vault**: Scheduled quiet batch digests at 13:00 and 20:00.
- **Data & Storage**: 100% on-device Room SQLite database with automated migrations.
---

## 🛠️ Build & Install

```bash
# Set Java 17
export JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home"

# Build debug APK
./gradlew assembleDebug
```

Built APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

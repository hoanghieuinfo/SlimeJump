<div align="center">

# 🟢 SlimeJump

### A physics-based Android platformer where your slime bounces to the top!

[![Android](https://img.shields.io/badge/Platform-Android%207.0%2B-brightgreen?logo=android)](https://developer.android.com)
[![Java](https://img.shields.io/badge/Language-Java%2011-orange?logo=java)](https://www.java.com)
[![Firebase](https://img.shields.io/badge/Backend-Firebase-yellow?logo=firebase)](https://firebase.google.com)
[![License](https://img.shields.io/badge/License-MIT-blue)](LICENSE)
[![API](https://img.shields.io/badge/Min%20SDK-API%2024-red)](https://developer.android.com/about/versions/nougat)

[Features](#-features) • [Installation](#-installation) • [Gameplay](#-gameplay-guide) • [Firebase Setup](#-firebase-setup) • [Contributing](#-contributing)

</div>

---

## 📖 About

**SlimeJump** is a vertical platformer mobile game for Android where you guide a bouncy slime upward through procedurally generated platforms. Tilt your device to move left and right, land on platforms to bounce higher, collect power-ups, and compete on a global leaderboard — all powered by Firebase.

> **New to Android games?** Don't worry — this guide walks you through everything from installing tools to playing your first game.

---

## ✨ Features

| Feature | Description |
|---|---|
| 🎮 **Physics-based gameplay** | Real gravity, bounce mechanics, and collision detection |
| 📱 **Tilt controls** | Use your device's accelerometer to move the slime |
| 🏆 **Global leaderboard** | Compete with players worldwide via Firebase Firestore |
| 👤 **User accounts** | Sign up, log in, and track your personal stats |
| ⚡ **3 Power-ups** | Jetpack, Shield, and Score Multiplier |
| 🌙 **Dynamic themes** | Automatic Day/Night mode via light sensor |
| 👟 **Pedometer bonus** | Walk 50 steps to unlock a shield power-up |
| 🗺️ **9 Platform types** | Standard, Bouncy, Disappearing, Moving, Spike, and more |

---

## 📸 Screenshots

<div align="center">

| Main Menu | Gameplay | Leaderboard |
|-----------|----------|-------------|
| *(coming soon)* | *(coming soon)* | *(coming soon)* |

</div>

---

## 🛠️ Tech Stack

- **Language:** Java 11
- **Platform:** Android SDK (API 24 → 36)
- **Build tool:** Gradle 8.11 with Kotlin DSL
- **Backend:** Firebase Authentication, Firestore, Realtime Database
- **UI:** AndroidX, Material Design 3
- **Sensors:** Accelerometer, Light sensor, Step counter

---

## 📋 Prerequisites

Before you begin, make sure you have the following installed:

| Tool | Version | Download |
|------|---------|----------|
| Android Studio | Ladybug (2024.2+) | [Download](https://developer.android.com/studio) |
| JDK | 11 or higher | Bundled with Android Studio |
| Android Device / Emulator | API 24+ (Android 7.0+) | — |
| Firebase Account | Free tier is enough | [console.firebase.google.com](https://console.firebase.google.com) |

> **Tip:** Android Studio includes the JDK and Android SDK — you don't need to install them separately.

---

## 🚀 Installation

### Step 1 — Clone the repository

```bash
git clone https://github.com/hoanghieuinfo/slimejump.git
cd slimejump
```

### Step 2 — Set up Firebase

SlimeJump uses Firebase for authentication and the leaderboard. You must create a Firebase project and add the configuration file before building.

See the [Firebase Setup](#-firebase-setup) section below for detailed instructions.

### Step 3 — Open in Android Studio

1. Launch **Android Studio**
2. Click **File → Open**
3. Navigate to the cloned `slimejump` folder and click **OK**
4. Wait for Gradle to sync (this may take a few minutes on first open)

### Step 4 — Run the app

**On a physical device:**
1. Enable **Developer Options** on your Android phone:
   - Go to *Settings → About Phone* and tap **Build Number** 7 times
   - Go back to *Settings → Developer Options* and enable **USB Debugging**
2. Plug your phone into your computer via USB
3. In Android Studio, select your device from the dropdown and press **Run ▶**

**On an emulator:**
1. In Android Studio, go to **Device Manager** (right sidebar)
2. Click **Create Device**, choose a phone with API 24+, and follow the wizard
3. Select the emulator from the dropdown and press **Run ▶**

> **Note:** Tilt controls require a real device with an accelerometer. The emulator may not support sensor input.

---

## 🔥 Firebase Setup

> This step is **required**. The app will not compile without `google-services.json`.

### 1. Create a Firebase project

1. Go to [console.firebase.google.com](https://console.firebase.google.com)
2. Click **Add project** and follow the prompts (you can disable Google Analytics)

### 2. Register your Android app

1. In the Firebase console, click **Add app → Android**
2. Enter the package name: `com.example.slime`
3. Enter a nickname (e.g., *SlimeJump*) — optional
4. Click **Register app**

### 3. Download `google-services.json`

1. Click **Download google-services.json**
2. Place the file here in the project:

```
slimejump/
└── app/
    └── google-services.json   ← put it here
```

### 4. Enable Firebase services

In the Firebase console, enable the following:

| Service | Location | Notes |
|---------|----------|-------|
| **Authentication** | *Build → Authentication → Get started* | Enable **Email/Password** provider |
| **Firestore Database** | *Build → Firestore Database → Create database* | Start in **test mode** for development |
| **Realtime Database** | *Build → Realtime Database → Create database* | Choose any region |

### 5. Create Firestore indexes

The leaderboard query requires a Firestore index. Add a composite index:

- **Collection:** `leaderboard`
- **Fields:** `score` (Descending), `timestamp` (Descending)

> Firebase will prompt you automatically with a link to create the index when you first open the leaderboard screen.

---

## 🕹️ Gameplay Guide

### Objective

Bounce your slime as high as possible by landing on platforms. The higher you go, the more points you earn. Don't fall off the bottom of the screen!

### Controls

| Action | How |
|--------|-----|
| Move left / right | Tilt your device |
| Bounce | Land on any non-fake platform automatically |

### Platform Types

| Platform | Color | Behavior |
|----------|-------|----------|
| Standard | Green | Safe bounce |
| Bouncy | Blue | Extra-high bounce |
| Disappearing | Orange | Vanishes after one bounce |
| Moving | Purple | Slides left and right |
| Falling | Brown | Drops after landing |
| Fake | Translucent | You fall through it — it's a trap! |
| Spike | Red | Instant death — avoid! |
| Spring Trap | Yellow | Launches you sideways |
| Moving Enemy | Red mob | Kills on contact |

### Power-Ups

| Icon | Name | Effect |
|------|------|--------|
| 🚀 | **Jetpack** | Massive upward boost |
| 🛡️ | **Shield** | Absorbs one fatal fall, teleports you back up |
| ✖️2 | **Multiplier** | Doubles your score for a limited time |

> **Pedometer bonus:** Walk 50 steps in real life and a Shield power-up will appear in the game! (Requires device with step counter sensor)

### Scoring

- Score increases with altitude gained
- Collecting a **Score Multiplier** doubles all points earned during its duration
- High scores are saved to your profile and synced to the global leaderboard

---

## 📁 Project Structure

```
slimejump/
├── app/
│   └── src/main/
│       ├── java/com/example/slime/
│       │   ├── GameActivity.java          # Game screen host
│       │   ├── GameView.java              # Core game loop & rendering
│       │   ├── GameOverActivity.java      # Game over & score submission
│       │   ├── MainMenuActivity.java      # Main menu + sensor setup
│       │   ├── LoginActivity.java         # Firebase login
│       │   ├── RegisterActivity.java      # New user registration
│       │   ├── LeaderboardActivity.java   # Global top scores
│       │   ├── ProfileActivity.java       # User stats
│       │   ├── Slime.java                 # Player character & animation
│       │   ├── SpriteSheet.java           # Sprite frame management
│       │   ├── entities/                  # Enums (theme, state, power-up)
│       │   └── platform/                  # All platform & enemy classes
│       ├── res/
│       │   ├── drawable/                  # Sprites, backgrounds, buttons
│       │   ├── layout/                    # Activity XML layouts
│       │   ├── font/                      # Dogica pixel font
│       │   └── values/                    # Strings, colors, themes
│       └── AndroidManifest.xml
├── gradle/
│   └── libs.versions.toml                 # Dependency version catalog
├── build.gradle.kts                       # Root build config
└── settings.gradle.kts                    # Module settings
```

---

## 🔧 Build from Command Line

If you prefer the terminal over Android Studio:

```bash
# Build a debug APK
./gradlew assembleDebug

# Install to a connected device
./gradlew installDebug

# Run all tests
./gradlew test

# Clean build artifacts
./gradlew clean
```

The built APK is located at:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## 🤝 Contributing

Contributions are welcome! Here's how to get started:

1. **Fork** this repository
2. **Create a branch** for your feature:
   ```bash
   git checkout -b feature/your-feature-name
   ```
3. **Commit** your changes with a clear message:
   ```bash
   git commit -m "feat: add new platform type"
   ```
4. **Push** to your fork:
   ```bash
   git push origin feature/your-feature-name
   ```
5. **Open a Pull Request** and describe what you changed

### Commit message convention

| Prefix | Use for |
|--------|---------|
| `feat:` | New features |
| `fix:` | Bug fixes |
| `refactor:` | Code restructuring |
| `docs:` | Documentation changes |
| `chore:` | Build / tooling changes |

---

## ❓ Troubleshooting

**Build fails with "google-services.json not found"**
→ Follow the [Firebase Setup](#-firebase-setup) section to download and place the file.

**Leaderboard doesn't load**
→ Make sure Firestore is enabled in your Firebase console and you've created the composite index.

**Tilt controls don't work**
→ The accelerometer is only available on real devices. Use a physical Android phone for the best experience.

**App crashes on launch**
→ Check that your Firebase Authentication and Firestore are configured and that the package name matches `com.example.slime`.

**Gradle sync fails**
→ Go to **File → Invalidate Caches / Restart** in Android Studio, then sync again.

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).

---

<div align="center">

Made with ❤️ by [hoanghieuinfo](https://github.com/hoanghieuinfo)

⭐ Star this repo if you find it fun or useful!

</div>

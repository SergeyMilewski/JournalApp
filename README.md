# Mini Video Journal App

A modern, immersive Android application built with Jetpack Compose that allows users to capture, describe, and relive their memories through short video clips.

## 📱 Features

*   **Immersive Video Feed**: A full-screen, vertical snapping feed (similar to modern social apps) for seamless browsing of recorded clips.
*   **Video Capture**: Quick access to the device camera to record spontaneous moments.
*   **Post-Capture Journaling**: Add optional text descriptions to your videos via a focused dialog immediately after recording.
*   **Optimized Playback**: Smart, lifecycle-aware video playback using Media3 ExoPlayer, optimized for vertical scrolling performance.
*   **Edge-to-Edge Design**: Fully immersive UI that utilizes the entire screen real estate.
*   **Local Storage**: All videos and metadata are stored securely on your device.

## 🛠 Tech Stack

*   **Language**: [Kotlin](https://kotlinlang.org/)
*   **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose)
*   **Media**: [Media3 ExoPlayer](https://developer.android.com/guide/topics/media/exoplayer) for high-performance video rendering.
*   **Database**: [SQLDelight](https://cashapp.github.io/sqldelight/) for type-safe local metadata storage.
*   **Dependency Injection**: [Koin](https://insert-koin.io/) for lightweight and idiomatic DI.
*   **Image Loading**: [Coil](https://coil-kt.github.io/coil/) with Video Frame support for thumbnails.
*   **Architecture**: Clean Architecture (Presentation, Domain, Data layers) following SOLID principles.

## 🏗 Architecture

The project follows a strict **Clean Architecture** pattern to ensure testability, scalability, and maintainability:

*   **Presentation**: Compose UI, ViewModels, and UI State management (MVI/MVVM hybrid).
*   **Domain**: Pure Kotlin business logic containing Use Cases and Repository interfaces.
*   **Data**: Implementation of repositories, SQLDelight database management, and File System storage for video assets.

## 🚀 Getting Started

1.  **Clone the repository**:
    ```bash
    git clone https://github.com/yourusername/JornalApp.git
    ```
2.  **Open in Android Studio**: Import the project as a Gradle project (Ladybug or newer recommended).
3.  **Build and Run**: Deploy to a physical device or emulator (API 28+).
    *   *Note: A physical device is recommended for the best camera and video playback experience.*

## 🧪 Testing

The project includes unit tests for core business logic and ViewModels:
*   **MockK** for dependency mocking.
*   **Turbine** for testing Kotlin Flows.
*   **JUnit 5** for the test runner.

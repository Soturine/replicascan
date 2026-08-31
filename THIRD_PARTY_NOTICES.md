# Third-party notices

ReplicaScan includes or depends on third-party software. The proprietary
license covers only the original ReplicaScan portions and does not replace the
licenses or terms below.

## Runtime and Android build dependencies

- Android Open Source Project and AndroidX/Jetpack components, including
  Activity, AppCompat, CameraX, Compose, Core, DataStore, Lifecycle,
  Navigation, Room, Test, and WorkManager: primarily Apache License 2.0.
  Source and license information: <https://cs.android.com/androidx/platform/frameworks/support>
- Kotlin and Kotlin standard tooling: Apache License 2.0.
  License information: <https://kotlinlang.org/docs/faq.html#is-kotlin-free>
- kotlinx.coroutines: Apache License 2.0.
  Source and license information: <https://github.com/Kotlin/kotlinx.coroutines>
- Material Components for Android: Apache License 2.0.
  Source and license information: <https://github.com/material-components/material-components-android>
- Gradle and the Gradle Wrapper: Apache License 2.0.
  Source and license information: <https://github.com/gradle/gradle>

## Google ML Kit and Google Play services

ReplicaScan uses Google Play services ML Kit Document Scanner and ML Kit Text
Recognition artifacts. Their use is governed by the applicable Google APIs
and ML Kit terms, not by the ReplicaScan proprietary license. ML Kit documents
are processed on-device; ML Kit components may contact Google for model or
component updates, compatibility information, and API performance/utilization
metrics as described by Google.

- ML Kit terms and privacy: <https://developers.google.com/ml-kit/terms>
- Google APIs Terms of Service: <https://developers.google.com/terms>

## Test dependencies

- JUnit 4: Eclipse Public License 1.0.
  Source and license information: <https://github.com/junit-team/junit4>
- Truth: Apache License 2.0.
  Source and license information: <https://github.com/google/truth>

## Product assets

The current fox illustrations and product-specific visual assets are maintained
as ReplicaScan project assets. No claim is made here over Android, Kotlin,
Google, or other third-party names and marks.

This notice lists the direct dependency families used by the repository. The
resolved dependency graph and packaged metadata remain the authoritative source
for exact versions in a given build. Maintainers must review new dependencies
for license, security, APK-size, and maintenance impact before adoption.

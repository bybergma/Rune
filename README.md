# Runes Läsäventyr

## Version 0.9.1

Build-fix:
- Tog bort gamla `drawable/ic_launcher.png`.
- Behåller den nya barnvänliga bokikonen som `drawable/ic_launcher.xml`.
- Löser Gradle-felet `Duplicate resources`.

Övriga funktioner från v0.9 är oförändrade.

## Bygg APK

Push till `main` startar GitHub Actions-workflowet **Build Android APK**.
När bygget är klart, ladda ner artifact **Runes-Lasaventyr-APK** och installera `app-debug.apk`.

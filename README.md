# Runes Läsäventyr

## Version 0.2

- Appen pratar instruktionerna på svenska.
- Korta ord visas med stora bokstäver.
- Barnet trycker på mikrofonen och läser ordet.
- Det som taligenkänningen hör visas mycket stort i VERSALER.
- Rätt svar ger spelljud, animation och uppläst "Hurra! Rätt!".
- Figuren flyttar ett steg på en bana med 10 steg.
- Fel svar ger uppläst "Försök igen".
- Appen lagrar inte ljudinspelningen.

## Bygg APK

Varje push till `main` startar GitHub Actions-workflowet **Build Android APK**.

När bygget är klart:
1. Öppna **Actions**.
2. Öppna senaste **Build Android APK**.
3. Ladda ner **Runes-Lasaventyr-APK** under **Artifacts**.
4. Packa upp ZIP-filen.
5. Installera `app-debug.apk`.

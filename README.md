# Runes Läsäventyr

En enkel Android-app för tidig lästräning.

## Funktioner
- 10 steg till målet.
- Korta svenska ord.
- Android SpeechRecognizer på svenska (`sv-SE`).
- Rätt läst ord flyttar figuren ett steg framåt.
- Fel eller osäkert resultat ger ett nytt försök.
- Appen lagrar inte ljudinspelningen.

## Bygg APK med GitHub Actions
Varje push till `main` startar automatiskt workflowet **Build Android APK**.

När bygget är klart:
1. Öppna fliken **Actions** i GitHub.
2. Öppna den senaste **Build Android APK**-körningen.
3. Scrolla till **Artifacts**.
4. Ladda ner **Runes-Lasaventyr-APK**.
5. Packa upp ZIP-filen och installera `app-debug.apk` på Android.

Telefonen kan behöva tillåta installation från webbläsaren/Filer första gången.

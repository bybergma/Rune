RUNES LÄSÄVENTYR – ANDROID-PROJEKT

Det här är källkoden till den native Android-versionen.

Funktioner:
- 10-stegs spelplan.
- Ett kort svenskt ord åt gången.
- Androids SpeechRecognizer lyssnar på svenska (sv-SE).
- Appen jämför flera tolkningar av det Rune säger med mål-ordet.
- Rätt ord => figuren flyttar ett steg.
- Fel/osäkert => samma ord ligger kvar och appen ber honom försöka igen.
- Inspelningen lagras inte av appen.
- Mikrofonbehörighet efterfrågas när funktionen används.

Bygg APK:
1. Öppna mappen RunesLasaventyrAndroid i Android Studio.
2. Låt Gradle Sync bli klar.
3. Välj Build > Build App Bundle(s) / APK(s) > Build APK(s).
4. APK:n skapas normalt under:
   app/build/outputs/apk/debug/app-debug.apk

Projektet använder:
- Java 17
- Android Gradle Plugin 8.7.3
- compileSdk 35
- minSdk 26
- targetSdk 35

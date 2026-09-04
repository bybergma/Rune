# Runes Läsäventyr

## Version 0.7

Uppdateringar i denna version:

- Ordet visas nu på **samma rad**, till exempel `BOLL` i stället för bokstäver under varandra.
- Färgprofilen är ändrad till **vit, röd och svart**.
- Figuren på spelplanen är ersatt med **Pikachu**.
- Målet/priset visas som en **Pokémon-boll**.
- Appen har fått en **ny appikon** baserad på Pokémon-bollen.
- Mikrofonresultatet visas fortfarande stort i **VERSALER**.
- När barnet når mål säger appen:
  **"Hurra hurra! Du kan läsa!"**
- Vid målgång kommer en tydligare slutanimation där figuren snurrar/dansar och flyger iväg.

Tre nivåer finns kvar:
1. IS, GÅ, ÅT, DU, VI, OM, BY, ÄR, AJ, HA
2. SOL, KOR, ROR, BIL, MUS, HUS, BOK, MAT, FIS
3. BOLL, KATT, HUND, BORD, STOL, GLAS, FISK, LÄSA, MÅNE, BAJS

## Bygg APK

Push till `main` startar GitHub Actions-workflowet **Build Android APK**.
När bygget är klart, ladda ner artifact **Runes-Lasaventyr-APK** och installera `app-debug.apk`.

> Obs: den här versionen använder Pokémon-bilderna som du skickade in. Om appen någon gång ska publiceras offentligt bör dessa bytas ut mot egna originalfigurer och egen ikon.

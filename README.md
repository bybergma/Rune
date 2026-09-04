# Runes Läsäventyr

## Version 0.9

Den här versionen gör skillnaden tydligare mellan rätt/fel och mellan knappar/information:

- **grönt = rätt**
- **rött = fel**
- icke-klickbara paneler ser inte längre ut som knappar
- riktiga knappar har en tydligare knappstil
- mjuk Pokémon-inspirerad färgkänsla är kvar
- Pikachu och Pokémon-bollen är kvar
- bokikonen är kvar
- ordet visas fortfarande på samma rad

Tre nivåer finns kvar:
1. IS, GÅ, ÅT, DU, VI, OM, BY, ÄR, AJ, HA
2. SOL, KOR, ROR, BIL, MUS, HUS, BOK, MAT, FIS
3. BOLL, KATT, HUND, BORD, STOL, GLAS, FISK, LÄSA, MÅNE, BAJS

Taligenkänning och slutanimationen med:
"Hurra hurra! Du kan läsa!"
finns kvar.

## Bygg APK

Push till `main` startar GitHub Actions-workflowet **Build Android APK**.
När bygget är klart, ladda ner artifact **Runes-Lasaventyr-APK** och installera `app-debug.apk`.

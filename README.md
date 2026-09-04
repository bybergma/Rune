# Runes Läsäventyr

## Version 0.6 – tre nivåer

### Nivå 1
IS, GÅ, ÅT, DU, VI, OM, BY, ÄR, AJ, HA

### Nivå 2
SOL, KOR, ROR, BIL, MUS, HUS, BOK, MAT, FIS

KATT är flyttat till nivå 3 för att hålla nivåerna mer konsekventa.
Nivå 2 har därför nio unika ord; på en 10-stegsbana kan ett ord återkomma.

### Nivå 3
BOLL, KATT, HUND, BORD, STOL, GLAS, FISK, LÄSA, MÅNE, BAJS

## Taligenkänning
- använder ett språkmodellsläge mer lämpat för korta sökord,
- hämtar upp till 10 alternativa tolkningar,
- behåller delresultat,
- ett exakt korrekt delresultat får räknas även om Android tappar det i slutresultatet,
- Android 13+ biasas mot aktuellt ord,
- VI kan accepteras om Android transkriberar det som `V`,
- ÄR kan accepteras om Android transkriberar det som `R`.

## Belöning
Efter varje rätt ord flyttar figuren ett steg med ett kort ljud.

När barnet når mål:
- appen säger exakt **“Hurra hurra! Du kan läsa!”**
- gubben snurrar/dansar och flyger iväg
- appen går därefter tillbaka till nivåvalet.

## Bygg APK
Push till `main` startar GitHub Actions-workflowet **Build Android APK**.
Ladda ner artifact **Runes-Lasaventyr-APK** och installera `app-debug.apk`.

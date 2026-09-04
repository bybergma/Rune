# Runes Läsäventyr

## Version 0.3 – bättre fångst av korta ord

- Visar delresultat från mikrofonen direkt medan barnet pratar.
- Om Android avslutar med `NO_MATCH` men hann höra något visas det ändå i stora VERSALER.
- Lite längre lyssningsfönster för korta ord som MIL och SIL.
- Upp till åtta alternativa igenkänningsresultat används.
- På Android 13+ biasas taligenkänningen mot aktuellt målord.
- Rätt svar ger ljud, "Hurra! Rätt!" och flyttar figuren.
- Fel svar läser upp vad appen uppfattade och ber barnet försöka igen.

## Bygg APK

Push till `main` startar GitHub Actions-workflowet **Build Android APK**. Ladda därefter ner artifact `Runes-Lasaventyr-APK`.

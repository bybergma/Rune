# Runes Läsäventyr

## Version 0.5 – nivåer 2, 3, 4 och 5 bokstäver

När appen startar frågar den högt:
**”Hur många bokstäver vill du läsa? Tryck på två, tre, fyra eller fem.”**

Nivåer:
- **2 bokstäver:** IS, VI, SE, NU, ÅL, AV samt enkla sammanljudningar som SA, SO, MI, MO, RA och RO.
- **3 bokstäver:** SOL, MUS, RAM, MIL m.fl.
- **4 bokstäver:** MÅNE, SOVA, LEKA, MÅLA m.fl.
- **5 bokstäver:** BANAN, KANIN, ROBOT, SOLEN m.fl.

Alla nivåer använder samma 10-stegsbana och mikrofonfunktion:
- det appen hör visas stort i VERSALER,
- rätt svar ger ljud, animation och ”Hurra! Rätt!”,
- figuren flyttar ett steg,
- fel/osäkert svar får ett nytt försök.

## Bygg APK

Push till `main` startar GitHub Actions-workflowet **Build Android APK**.
När bygget är klart, ladda ner artifact **Runes-Lasaventyr-APK** och installera `app-debug.apk`.

VersionCode är höjt till 5 och samma debug-keystore som v4 används, så v5 ska kunna installeras som en uppdatering ovanpå v4.

# Commander-Uhr für Android – Version 0.2

Native Android-App in Kotlin und Jetpack Compose.

## Funktionen
- 2 bis 6 Spieler und individuelle Spielernamen
- Zeit pro Zug von 30 Sekunden bis 15 Minuten in 30-Sekunden-Schritten
- Countdown, Start/Pause, nächster Spieler, zurück
- Zugzeit wird bei jedem Spielerwechsel zurückgesetzt
- Lebenspunkte je Spieler, standardmäßig 40
- Giftmarken je Spieler
- Commander-Schaden pro Commander-Quelle und Zielspieler
- W6-Würfel und Münzwurf
- Bildschirm bleibt während der Partie eingeschaltet
- Keine Internetberechtigung in der App erforderlich

## APK erstellen – automatisch über GitHub
Eine GitHub Actions Workflow-Datei ist enthalten: `.github/workflows/android.yml`.
1. Ein neues Repository auf GitHub erstellen.
2. Den Inhalt dieses Projektordners ins Repository hochladen und auf den Branch `main` pushen.
3. Im Repository den Bereich **Actions** öffnen.
4. Den Workflow **Android APK bauen** starten oder einen Push auf `main` auslösen.
5. Nach erfolgreichem Lauf das Artefakt **Commander-Uhr-APK** herunterladen. Darin liegt `app-debug.apk`.
6. Die APK aufs Android-Handy übertragen und öffnen. Falls Android fragt, die Installation aus dieser Quelle erlauben.

## Lokal bauen
Mit Android Studio öffnen, Gradle-Synchronisierung abwarten und `assembleDebug` ausführen.

## Status / Hinweis
Das Projekt wurde hier nicht kompiliert, da in dieser Arbeitsumgebung Android SDK und Gradle fehlen und kein Netzwerkzugriff verfügbar ist. Der GitHub-Workflow ist dafür vorbereitet, muss aber beim ersten Lauf erfolgreich durchlaufen. Bitte die App vor dem echten Spieleinsatz testen.

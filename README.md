# FINCTRL – finanse osobiste (Android)
Kotlin · Jetpack Compose (Material 3) · MVVM · Room · Coroutines/Flow · Navigation Compose · Firebase (Auth + Firestore).
**Model danych:** Room jako źródło prawdy. Firebase służy do logowania oraz kopii zapasowej/synchronizacji.

Rozszerzona dokumentacja (już kompletna) została zawarta w gpliku pdf w głównym folderze projektu. Plik readme stanowi skróconą wersję dokumentacji (poglądową)


## Uruchomienie

### Wymagania
- Android Studio Narwhal 3 Feature Drop (2025.1.3) lub nowsze – wymagane przez AGP 8.13.
- JDK 17 lub nowszy – Android Studio używa wbudowanego JBR. Przy budowaniu z terminala ustaw `JAVA_HOME` na JDK 17+ (najprościej na katalog `jbr` w folderze instalacji Android Studio).
- Android SDK Platform 35 oraz urządzenie lub emulator z Androidem 7.0+ (API 24).
- Gradle 8.13 nie trzeba instalować – pobierze go wrapper (`gradlew` / `gradlew.bat`).

### Konfiguracja
1. Sklonuj repozytorium i otwórz katalog w Android Studio. Plik `local.properties` ze ścieżką do SDK utworzy się sam; przy budowaniu tylko z terminala ustaw `ANDROID_HOME` albo dodaj do `local.properties` linię `sdk.dir=…`.
2. Umieść plik `google-services.json` w katalogu `app/` (plik jest w `.gitignore`). **Bez niego projekt się nie zbuduje** – wymaga go wtyczka `com.google.gms.google-services`.

### Build
W Android Studio: Gradle Sync, a następnie uruchom konfigurację **app** na urządzeniu lub emulatorze.
Z terminala (na Windowsie `gradlew.bat` zamiast `./gradlew`):

| Polecenie | Efekt |
|---|---|
| `./gradlew assembleDebug` | buduje APK: `app/build/outputs/apk/debug/app-debug.apk` |
| `./gradlew installDebug` | buduje i instaluje aplikację na podłączonym urządzeniu |
| `./gradlew clean` | usuwa wyniki poprzednich buildów |

W projekcie znjduje się gotowy plik apk do instalacji na urządzeniu android (emulatorze lub fizycznym)


### Testy
| Polecenie | Zakres | Wymaga urządzenia |
|---|---|---|
| `./gradlew testDebugUnitTest` | 28 testów jednostkowych (JUnit, MockK, Turbine) na JVM | nie |
| `./gradlew connectedDebugAndroidTest` | 11 testów instrumentowanych: Room i UI Compose | tak |

Testy jednostkowe:
- `ValidatorsTest`, `ErrorHandlerTest`, `CsvExporterTest` – walidacja, mapowanie błędów, eksport CSV
- `TransactionEditViewModelTest`, `TransactionsViewModelTest`, `BudgetsViewModelTest` – logika ViewModeli (zapis, walidacja, filtrowanie, wyszukiwanie, przekroczenie budżetu, przełączanie miesięcy)
- `AutoLockPolicyTest` – automatyczna blokada po powrocie z tła

Testy instrumentowane:
- `FinanceDaoTest` – Room na bazie w pamięci: CRUD, sumy miesięczne, klucze obce (SET NULL / CASCADE), unikalny budżet
- `TransactionEditScreenTest` – Compose UI: komunikaty walidacji, zapis transakcji, filtrowanie kategorii po typie
- `EmptyStateTest` – komponent pustego stanu

Przed testami instrumentowanymi:
- sprawdź, czy urządzenie jest widoczne jako `device` w `adb devices` (przy `unauthorized` zaakceptuj na telefonie monit „Zezwolić na debugowanie USB?”);
- odblokuj ekran telefonu – testy UI Compose nie znajdą interfejsu, gdy ekran jest zablokowany („No compose hierarchies found”).

Pojedynczą klasę uruchomisz parametrem `--tests` (testy jednostkowe), np. `./gradlew testDebugUnitTest --tests "com.finctrl.app.ValidatorsTest"`, a testy instrumentowane jednej klasy – parametrem `-Pandroid.testInstrumentationRunnerArguments.class=com.finctrl.app.FinanceDaoTest`. Pojedyncze testy można też uruchamiać zieloną strzałką przy klasie w Android Studio.

Raporty HTML:
- jednostkowe: `app/build/reports/tests/testDebugUnitTest/index.html`
- instrumentowane: `app/build/reports/androidTests/connected/debug/index.html`

## Struktura
`core/` walidacja i ErrorHandler · `data/local` Room · `data/repo` repozytoria, CSV, Firestore, kursy walut · `security/` PIN (sól + SHA-256, EncryptedSharedPreferences) i BiometricPrompt · `ui/screens/` 9 ekranów z ViewModelami · `ui/AppNav.kt` nawigacja.

Diagramy (architektura, przypadki użycia, baza danych): [docs/diagrams.md](docs/diagrams.md).

## Ekrany
- Logowanie/odblokowanie 
- Pulpit
- Transakcje (filtr + wyszukiwanie)
- Dodawanie/edycja transakcji
- Kategorie
- Budżety 
- Raporty (wykres kołowy)
- Waluty (kursy Frankfurter/ECB)
- Ustawienia (PIN, biometria, CSV, kopia zapasowa).

## Bezpieczeństwo
- PIN przechowywany wyłącznie jako SHA-256(sól + PIN) w EncryptedSharedPreferences (klucz w Android Keystore).
- Strażnik nawigacji (`LockGuard` w `AppNav.kt`): gdy PIN jest ustawiony, a sesja nie jest odblokowana, ekran odblokowania jest kładziony na wierzch stosu – także po odtworzeniu procesu przez system. Po odblokowaniu użytkownik wraca do miejsca, w którym był; przycisk „Wstecz” na ekranie blokady chowa aplikację.
- Automatyczna blokada po powrocie z tła, jeśli aplikacja była w tle dłużej niż 30 s (`security/AutoLock.kt`, `ProcessLifecycleOwner`).
- Wyłączone kopie systemowe (`allowBackup=false`, `data_extraction_rules.xml`), ochrona CSV przed wstrzykiwaniem formuł.
- Hasło i PIN wpisywane w formularzach są trzymane w ViewModelu (przetrwają obrót ekranu), a nie w zapisywanym stanie `Bundle`.

## Ograniczenia
Teksty UI są wpisane na stałe po polsku (do przeniesienia do `strings.xml`). Waluta transakcji jest stała (PLN).
Możliwość ewentruzlnego rozbudowania zmiennych walutowych i językowych w przyszłości. 

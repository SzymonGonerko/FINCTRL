# FINCTRL – finanse osobiste (Android)

Kotlin · Jetpack Compose (Material 3) · MVVM · Room · Coroutines/Flow · Navigation Compose · Firebase (Auth + Firestore).

**Model danych:** Room to źródło prawdy i działa offline. Firebase służy do logowania oraz kopii zapasowej/synchronizacji.

## Uruchomienie
1. Otwórz folder w Android Studio (Narwhal 3 Feature Drop 2025.1.3 lub nowsze – wymagane przez AGP 8.13). Projekt zawiera Gradle Wrapper (`gradlew`, `gradlew.bat`) – Gradle 8.13 pobierze się automatycznie. Kompilacja: JDK 17+ (Android Studio ma wbudowane JBR). Plik `local.properties` ze ścieżką do SDK Android Studio tworzy samo.
2. Umieść plik `google-services.json` w katalogu `app/` (plik jest w `.gitignore`; bez niego projekt się nie zbuduje).
3. Uruchom na urządzeniu lub emulatorze (minSdk 24). Plik APK: `./gradlew assembleDebug` → `app/build/outputs/apk/debug/`.

## Testy
- Jednostkowe (JUnit + MockK + Turbine, JVM): `./gradlew testDebugUnitTest` (Windows: `gradlew.bat testDebugUnitTest`)
  - `ValidatorsTest`, `ErrorHandlerTest`, `CsvExporterTest` – walidacja, mapowanie błędów, eksport CSV
  - `TransactionEditViewModelTest`, `TransactionsViewModelTest`, `BudgetsViewModelTest` – logika ViewModeli (zapis, walidacja, filtrowanie, wyszukiwanie, przekroczenie budżetu, przełączanie miesięcy)
  - `AutoLockPolicyTest` – automatyczna blokada po powrocie z tła
- Instrumentowane (urządzenie/emulator): `./gradlew connectedDebugAndroidTest`
  - `FinanceDaoTest` – Room na bazie w pamięci: CRUD, sumy miesięczne, klucze obce (SET NULL / CASCADE), unikalny budżet
  - `TransactionEditScreenTest` – Compose UI: komunikaty walidacji, zapis transakcji, filtrowanie kategorii po typie
  - `EmptyStateTest` – komponent pustego stanu
- Raporty HTML: `app/build/reports/tests/testDebugUnitTest/index.html` oraz `app/build/reports/androidTests/connected/debug/index.html`.

## Struktura
`core/` walidacja i ErrorHandler · `data/local` Room · `data/repo` repozytoria, CSV, Firestore, kursy walut · `security/` PIN (sól + SHA-256, EncryptedSharedPreferences) i BiometricPrompt · `ui/screens/` 9 ekranów z ViewModelami · `ui/AppNav.kt` nawigacja.

Diagramy (architektura, przypadki użycia, baza danych): [docs/diagrams.md](docs/diagrams.md).

## Ekrany
Logowanie/odblokowanie · Pulpit · Transakcje (filtr + wyszukiwanie) · Dodawanie/edycja transakcji · Kategorie · Budżety · Raporty (wykres kołowy) · Waluty (kursy Frankfurter/ECB) · Ustawienia (PIN, biometria, CSV, kopia zapasowa).

## Bezpieczeństwo
- PIN przechowywany wyłącznie jako SHA-256(sól + PIN) w EncryptedSharedPreferences (klucz w Android Keystore).
- Strażnik nawigacji (`LockGuard` w `AppNav.kt`): gdy PIN jest ustawiony, a sesja nie jest odblokowana, ekran odblokowania jest kładziony na wierzch stosu – także po odtworzeniu procesu przez system. Po odblokowaniu użytkownik wraca do miejsca, w którym był; przycisk „Wstecz” na ekranie blokady chowa aplikację.
- Automatyczna blokada po powrocie z tła, jeśli aplikacja była w tle dłużej niż 30 s (`security/AutoLock.kt`, `ProcessLifecycleOwner`).
- Wyłączone kopie systemowe (`allowBackup=false`, `data_extraction_rules.xml`), ochrona CSV przed wstrzykiwaniem formuł.
- Hasło i PIN wpisywane w formularzach są trzymane w ViewModelu (przetrwają obrót ekranu), a nie w zapisywanym stanie `Bundle`.

## Ograniczenia
Teksty UI są wpisane na stałe po polsku (do przeniesienia do `strings.xml`). Waluta transakcji jest stała (PLN).

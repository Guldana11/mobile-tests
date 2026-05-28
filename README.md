# mobile-tests

Кросс-платформенные UI-автотесты мобильного приложения BNK (Android + iOS) на
**Appium + TestNG**. Один и тот же тест-класс гоняется на обеих платформах —
различия инкапсулированы в Page Object'ах через `switch (Platform.current())`.

---

## Стек

| Компонент | Версия |
|-----------|--------|
| Java | 21 |
| Gradle | 8.13 (wrapper) |
| Appium java-client | 9.3.0 |
| Selenium | 4.25.0 |
| TestNG | 7.10.2 |
| Allure | 2.29.0 |

Драйверы Appium: **UiAutomator2** (Android) и **XCUITest** (iOS).

---

## Требования к окружению

- **macOS** (iOS-тесты требуют симулятор + Xcode).
- **Xcode** с установленным симулятором iPhone 17 (iOS 26.5).
- **Android SDK** с AVD `Android_14` (API 14) и `adb` в `PATH`.
- **Appium 2** + драйверы:
  ```bash
  appium driver install uiautomator2
  appium driver install xcuitest
  ```
- Сборки приложения (в git **не** коммитятся, см. `.gitignore`):
  - Android: `apps/android/app-dev-debug.apk`
  - iOS: `apps/ios/BNK.app`

---

## Структура проекта

```
src/test/java/
├── core/         фабрика драйверов, BaseTest, retry-механизм, enum Platform
├── pages/        Page Object'ы (кросс-платформенные локаторы)
└── tests/        тест-классы (TestNG)
src/test/resources/
├── android.properties / ios.properties   capabilities на платформу
└── suites/       TestNG-сьюты (android.xml, ios.xml, android-iosflow.xml)
inspector-dumps/  сохранённые XML/PNG экранов (для построения локаторов)
```

---

## 1. Запуск окружения

### iOS-симулятор

```bash
# поднять симулятор и открыть окно
xcrun simctl boot "iPhone 17" && open -a Simulator

# проверить, что загрузился
xcrun simctl list devices booted
```

> Девайс и версия берутся из `src/test/resources/ios.properties`
> (`device.name=iPhone 17`, `platform.version=26.5`). Если меняешь симулятор —
> поправь там же.

### Android-эмулятор

```bash
# список доступных AVD
emulator -list-avds

# запустить нужный
emulator -avd Android_14

# дождаться готовности устройства
adb wait-for-device && adb devices
```

### Appium-сервер

Запускается отдельным долгоживущим процессом (держит порт **4723**):

```bash
appium --base-path / --relaxed-security
```

Проверка, что сервер поднялся:

```bash
curl -s http://127.0.0.1:4723/status
```

---

## 2. Запуск тестов

Платформа выбирается флагом `-Dplatform` (по умолчанию `android`). Он определяет
и драйвер, и файл `*.properties`, и TestNG-сьют `suites/<platform>.xml`.

```bash
# Android (весь сьют)
./gradlew test -Dplatform=android

# iOS (весь сьют)
./gradlew test -Dplatform=ios
```

### Кастомный сьют

`-DsuiteFile` переопределяет файл сьюта, не меняя выбор драйвера/проперти:

```bash
# регрессия кросс-платформенного флоу на Android
./gradlew test -Dplatform=android -DsuiteFile=android-iosflow.xml
```

### Снятие дампов экранов

Опция `-DdumpPageSource=true` сохраняет XML + скриншот каждого экрана в
`inspector-dumps/<platform>/` (используется для построения локаторов под новую
платформу):

```bash
./gradlew test -Dplatform=ios -DdumpPageSource=true
```

### Один класс / один метод

```bash
./gradlew test -Dplatform=ios -DsuiteFile=ios.xml --tests "tests.PhoneLoginTest"
```

---

## 3. Отчёты

При падении теста `BaseTest` автоматически сохраняет скриншот и page source в
`build/test-artifacts/`.

Allure-отчёт:

```bash
allure serve allure-results
```

Стандартный отчёт Gradle/TestNG: `build/reports/tests/test/index.html`.

---

## Как это работает (важные детали)

- **Сброс состояния перед каждым тестом.** `BaseTest.setUp()` принудительно
  удаляет приложение (`adb uninstall` / `xcrun simctl uninstall booted`), затем
  Appium ставит его заново. Это единственный надёжный способ очистить состояние
  этого приложения (`noReset`/`clearApp` не срабатывали).
- **iOS-разрешения.** Capability `appium:autoAcceptAlerts=true` автоматически
  принимает системные алерты (геолокация запрашивается на старте). Поэтому на iOS
  `PermissionDialog` — no-op, а проверка разрешения геолокации сделана через
  чтение `Info.plist`.
- **Android ANR.** При частых переустановках System UI может показать диалог
  «isn't responding» — `BaseTest` гасит его нажатием «Wait».
- **Retry.** Упавшие тесты перезапускаются через `RetryAnalyzer` /
  `RetryListener` (подключается в каждом сьюте как `<listener>`).

---

## Матрица покрытия

| Тест | Android | iOS |
|------|:---:|:---:|
| SmokeTest | ✅ | ✅ |
| LanguageSelectionTest | ✅ | ✅ |
| WelcomeTest | ✅ | ✅ |
| BranchesTest | ✅ | ✅ |
| MoreTest | ✅ | ✅ |
| PhoneLoginTest | ✅ | ✅ |
| AgreementLinkTest | ✅ | ✅ |
| TermsDocumentTest | ✅ | ✅ |

Платформенные отличия iOS (учтены в Page Object'ах / тестах):
- **PhoneLogin** — у iOS-экрана нет кнопки «назад» и возврат на Welcome не работает
  (известный баг приложения), поэтому кейс `backArrowReturnsToWelcome` на iOS пропускается
  (`SkipException`).
- **AgreementBottomSheet** — заголовок «Условия банка» (строчная), закрытие тапом по
  затемнённому фону (нет элемента `touch_outside`); сравнение заголовка регистронезависимое.
- **PDF viewer** — документ в `WebView`; кнопки «Принять» нет, поэтому её проверка
  выполняется только на Android.

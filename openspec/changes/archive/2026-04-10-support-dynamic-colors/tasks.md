## 1. Тема и Базовые настройки UI
- [x] 1.1 Изменить `Theme.kt`, добавив поддержку `dynamicColor` для Android 12+.

## 2. Рефакторинг Компонентов (Цвета)
- [x] 2.1 В `HistoryScreen.kt` заменить `Color.Black` и `Color.White` (вокруг иконки) на токены `surface` и `onSurface`.
- [x] 2.2 В `SettingsScreen.kt` заменить дефолтный `Color.White` для текста виджетов на `colorScheme.onSurface`.
- [x] 2.3 В `DashboardScreen.kt` очистить жестко заданные `tint` (используя `onSurface`), не затрагивая светодиоды батареи.

## 3. Локализация и Strings
- [x] 3.1 Создать базовый файл словаря `res/values/strings.xml` и добавить в него все залогированные тексты (Настройки, Приборная панель, История).
- [x] 3.2 Дополнить приложение папками языков `values-en`, `values-es`, `values-fr`, `values-de` с переведенными терминами (Автоперевод/Программный скрипт).
- [x] 3.3 Извлечь все пользовательские тексты из Compose-экранов на перекрестные вызовы `stringResource(id = R.string.key)`.

## 4. Настройка выбора языка (Экран Settings)
- [x] 4.1 Добавить ключ настройки `app_language_code` (`String`) в DataStore и `SettingsUiState`.
- [x] 4.2 Сверстать новый компонент переключения языка на экране настроек со списком доступных локализаций (включая вариант "Системный").
- [x] 4.3 Привязать выбор настройки к вызову `AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(langCode))` для мгновенного применения языка без перезапуска приложения.

## 1. UI на экране настроек

- [x] 1.1 Добавить визуальное разделение в `SettingsScreen.kt`: обернуть логические группы настроек (Батарея, Голосовые команды, GPS и т.д.) в `ElevatedCard` или добавить между ними `HorizontalDivider` с крупными заголовками `Text(style = MaterialTheme.typography.titleMedium)`.
- [x] 1.2 Добавить текстовую кнопку (TextButton/OutlinedButton) с иконкой Google Assistant или просто иконку перехода под разделом "Голосовые команды" в `SettingsScreen.kt`. Назвать её `Настроить команды (Routines)`.

## 2. Логика перехода (Intent)

- [x] 2.1 Добавить передачу лямбды `onOpenAssistantClick: () -> Unit` в компонент интерфейса настроек.
- [x] 2.2 В MainActivity или на месте получения контекста в Compose реализовать функцию запуска `Intent(Intent.ACTION_VIEW, Uri.parse("https://assistant.google.com/settings/routines"))` с блоком `runCatching` или `try/catch` на предмет `ActivityNotFoundException` (с показом Тоста).

## 3. Верификация

- [x] 3.1 Собрать проект, открыть настройки, нажать на кнопку и проверить редирект в системные настройки Google Assistant.

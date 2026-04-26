## 1. Database and Repository Updates

- [x] 1.1 Добавить `reassignSessionsProfile` метод (mass-update query `UPDATE DaySessionEntity`) в `SessionDao` и `SessionRepository` для переноса поездок с одного профиля на другой перед удалением.
- [x] 1.2 Написать/проверить метод для удаления профиля `deleteProfile(profileId)` в `ProfileDao`.

## 2. ViewModel Refactoring

- [x] 2.1 Обновить `MainViewModel.createAndSwitchProfile()` для проверки существующих имен колясок в БД; если дефолтное ("Новая коляска") уже занято, генерировать инкрементные (например, "Новая коляска 2").
- [x] 2.2 Добавить `deleteProfile(deletedId: Long, reassignToId: Long?, onDeleted: () -> Unit)` в `SettingsViewModel`.
- [x] 2.3 Удалить целиком `ProfilePassportViewModel.kt`.

## 3. NavHost and Screen Cleanup

- [x] 3.1 Удалить файл `ProfilePassportScreen.kt`.
- [x] 3.2 В `NavHost.kt` удалить маршрут `Routes.PROFILE_PASSPORT`. Обновить лямбду `onAddProfileClick` в карусели: вызывать `MainViewModel.createAndSwitchProfile()` и после переходить по маршруту `SETTINGS`.

## 4. Settings UI Updates

- [x] 4.1 Добавить кнопку `Удалить коляску` в блоке "Паспорт коляски" на `SettingsScreen.kt`. Кнопка должна быть скрыта или заблокирована, если `profiles.size <= 1`.
- [x] 4.2 Реализовать AlertDialog для удаления коляски с 2 режимами: 단순 (треков = 0) и сложный (выбор: удалить/переназначить треки). Для переназначения использовать выпадающий список доступных колясок.
- [x] 4.3 Настроить автоматическое закрытие/редирект `SettingsScreen` после упешного удаления (или `popBackStack`, если он был открыт).

## ADDED Requirements

### Requirement: Immediate Setting Application
Система SHALL сохранять настройки немедленно при изменении (instant-save).

#### Scenario: Change TTS interval
- **WHEN** пользователь меняет интервал TTS оповещений через барабан
- **THEN** значение немедленно сохраняется в DataStore для текущего профиля
- **AND** навигации назад НЕ происходит

### Requirement: Complex Wheel Pickers
Система SHALL использовать комбинацию барабанов для сложных данных, таких как часы и минуты.

#### Scenario: Selecting hours and minutes
- **WHEN** пользователь редактирует автозакрытие дня
- **THEN** UI отображает два независимых барабана часов и минут

### Requirement: Per-Profile Settings Storage
Все настройки SHALL храниться с привязкой к профилю коляски.
Подробная спецификация: [per-profile-settings/spec.md](../per-profile-settings/spec.md)

### Requirement: Settings Screen Includes Profile Passport
Экран настроек SHALL включать паспорт текущей коляски первой секцией.
Подробная спецификация: [unified-settings-dashboard/spec.md](../unified-settings-dashboard/spec.md)
### Requirement: Reset Lamp Distances Button
Система SHALL предоставлять кнопку для быстрого пересчета дистанций для каждой лампочки на основе профиля весов и общего пробега.

#### Scenario: Recalculating 10 lamps distance
- **WHEN** пользователь изменяет общий пробег на 40 км и нажимает кнопку пересчета
- **THEN** дистанции 10 лампочек автоматически пересчитываются по формулам профиля весов и сохраняются

### Requirement: Localize Developer Contact
Пункт меню "Обратиться к разработчику" SHALL быть локализован на все поддерживаемые языки.

#### Scenario: User opens settings
- **WHEN** пользователь просматривает меню настроек
- **THEN** пункт обращения к разработчику отображается на выбранном языке

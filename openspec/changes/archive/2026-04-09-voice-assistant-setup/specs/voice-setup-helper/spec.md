## ADDED Requirements

### Requirement: Assistant Settings Shortcut Button
Приложение SHALL предоставлять кнопку на экране настроек в разделе "Голосовые команды", которая перебрасывает пользователя в настройки Процедур Google Assistant.

#### Scenario: Navigate to Assistant Settings
- **WHEN** пользователь нажимает кнопку "Настроить Процедуры Ассистента"
- **THEN** система открывает внешний интерфейс настроек Google Assistant.

#### Scenario: Handle Assistant Absence
- **WHEN** пользователь нажимает кнопку, но Google Assistant не установлен или не поддерживает Intent
- **THEN** приложение не падает, а показывает всплывающее уведомление (Toast) "Google Assistant не найден".

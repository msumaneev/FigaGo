## ADDED Requirements

### Requirement: Immediate Setting Application
Система SHALL сохранять настройки по кнопке без закрытия экрана настроек.

#### Scenario: Saving without popBackStack
- **WHEN** пользователь нажимает кнопку сохранить настройку
- **THEN** измененное значение фиксируется в DataStore и отображается на экране (барабане), а навигации назад не происходит

### Requirement: Complex Wheel Pickers
Система SHALL использовать комбинацию барабанов для сложных данных, таких как часы и минуты.

#### Scenario: Selecting hours and minutes
- **WHEN** пользователь редактирует автозакрытие дня
- **THEN** UI отображает два независимых барабана часов и минут

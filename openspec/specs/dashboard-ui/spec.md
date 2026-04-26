# dashboard-ui Specification

## Purpose
TBD - created by archiving change tracker-optimization. Update Purpose after archive.
## Requirements
### Requirement: Distance Instead of Instant Speed
Главный экран (DashboardScreen) SHALL отображать пройденную дистанцию как основной (центральный) показатель, а Время и Среднюю скорость размещать на дополнительных виджетах. Отображение мгновенной скорости полностью скрывается.

#### Scenario: Running dashboard
- **WHEN** пользователь находится на главном экране во время трекинга
- **THEN** по центру большими цифрами выводится "Пробег", а сверху по бокам "Время" и "Средняя скорость"

### Requirement: Precise Metric Formatting for UI
Форматировщик UI SHALL отдавать строку с точно рассчитанными единицами измерения.

#### Scenario: Distance under 1 kilometer (metric)
- **WHEN** пройденная дистанция составляет 450 метров (настройки: метрическая)
- **THEN** UI отображает "450 м"

#### Scenario: Distance over 1 kilometer (metric)
- **WHEN** пройденная дистанция составляет 1450 метров (настройки: метрическая)
- **THEN** UI отображает "1 км 450 м"

#### Scenario: Imperial unit formatting
- **WHEN** пользователь использует мили в настройках
- **THEN** до 1 мили расстояние отображается в футах (или дробных милях), после 1 мили — в милях и футах/десятых (в зависимости от финальной доменной логики)

### Requirement: Precise Voice Announcements (TTS)
Сервис озвучивания (TtsAnnouncerService) SHALL произносить точную дистанцию без грубых округлений до целого километра.

#### Scenario: TTS under 1 kilometer
- **WHEN** срабатывает озвучивание, а пробег составляет 250 метров
- **THEN** синтезатор произносит фразу "Пробег 250 метров"

#### Scenario: TTS over 1 kilometer
- **WHEN** срабатывает озвучивание, а пробег составляет 1200 метров
- **THEN** синтезатор произносит фразу "Пробег один километр двести метров"


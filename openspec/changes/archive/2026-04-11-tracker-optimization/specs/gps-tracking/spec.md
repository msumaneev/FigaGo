## ADDED Requirements

### Requirement: Dynamic Location Batching
Сервис трекинга (TrackingService) SHALL изменять политику задержки получения точек GPS в зависимости от состояния жизненного цикла (Lifecycle) экрана карты.

#### Scenario: UI active (Foreground)
- **WHEN** пользователь открывает приложение и карта видна (onResume)
- **THEN** сервис настраивает параметры `LocationRequest` с `MaxUpdateDelayMillis` = 0 для моментальной доставки точек

#### Scenario: UI inactive (Background)
- **WHEN** пользователь сворачивает приложение или блокирует экран (onPause)
- **THEN** сервис ставит `MaxUpdateDelayMillis` в 30000 мс (30 секунд) для экономии пробуждений процессора

### Requirement: Batch Database Save
Репозиторий локаций SHALL сохранять входящий пакет (массив) gps-координат в постоянное хранилище атомарно.

#### Scenario: Location callback with multiple points
- **WHEN** `LocationCallback` получает результат геолокации, который содержит более 1 точки (из-за пакетирования в фоне)
- **THEN** все точки вставляются в БД Room в рамках единой SQL-транзакции

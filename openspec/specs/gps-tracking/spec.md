## ADDED Requirements

### Requirement: Live Polyline Drawing
Приложение SHALL непрерывно отрисовывать путь движения (Polyline) на карте в активной вкладке записи.

#### Scenario: Real-time map update
- **WHEN** поступает новая координата с GPS
- **THEN** система автоматически перерисовывает линию маршрута, включая новую точку

### Requirement: Historic Track Drawing
Приложение SHALL отображать маршрут сохраненного трека при просмотре истории.

#### Scenario: Map loading in History
- **WHEN** пользователь открывает экран детализации конкретного трека
- **THEN** система загружает связанные координаты и отрисовывает полилинию на карте

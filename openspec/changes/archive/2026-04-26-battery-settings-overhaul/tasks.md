## 1. Фундамент: Весовые профили и Room-миграция

- [x] 1.1 Создать `BatteryWeightProfiles.kt` в `domain/model/` — Kotlin object с массивами весов (Type_3..Type_10) и функцией `getWeights(ledCount)`
- [x] 1.2 Создать `LampStatisticsEntity.kt` в `data/entity/` — Room Entity с полями: id, profile_id, lamp_index, actual_distance, timestamp
- [x] 1.3 Создать `LampStatisticsDao.kt` в `data/dao/` — DAO с методами: insert, getByLampIndex, getAverageByLampIndex, deleteOldest, getCountByLampIndex
- [x] 1.4 Добавить `LampStatisticsDao` в `AppDatabase`, написать Room-миграцию (addMigrations) для новой таблицы `lamp_statistics`
- [x] 1.5 Создать `LampStatisticsRepository` (interface в domain, impl в data) — обёртка над DAO со скользящим окном (HISTORY_LIMIT = 5)

## 2. Бизнес-логика: Прогнозирование и запись фактов

- [x] 2.1 Рефакторинг `ForecastRemainingDistanceUseCase` — заменить линейное распределение на `BatteryWeightProfiles.getWeights()`, интегрировать средние из `LampStatisticsRepository` (горячий/холодный старт)
- [x] 2.2 Рефакторинг `RecordLedEventUseCase` — при записи LED-события вычислять дельту и сохранять в `LampStatisticsRepository`. Реализовать обработку пропусков (распределение по весам)
- [x] 2.3 Исправить расчёт PoNR в `TrackingService.kt` — заменить `maxDistanceKm = currentDistanceKm + forecast` на `maxMileageKm = profile.maxMileage`, ponrKm = maxMileageKm / 2.0

## 3. UI настроек: Батарея

- [x] 3.1 Переупорядочить раздел «Батарея» в `SettingsScreen.kt` — dropdown лампочек первым, затем общий пробег
- [x] 3.2 Заменить WheelPicker количества лампочек на `ExposedDropdownMenuBox` с фиксированными значениями [3, 4, 5, 6, 8, 10]
- [x] 3.3 Добавить индивидуальные `WheelNumberPickerSetting` для каждой лампочки (зависит от 1.1). Показывать статистику рядом с каждым барабаном: «Факт: ~X км» или «нет данных»
- [x] 3.4 Реализовать двустороннюю синхронизацию барабанов в `SettingsViewModel` — изменение общего пробега пересчитывает лампочки, изменение лампочки пересчитывает сумму
- [x] 3.5 Сохранять `ledDistances` в `ProfileEntity` при любом изменении барабанов

## 4. UI Dashboard: Адаптивные LED-индикаторы

- [x] 4.1 Динамический размер LED в `InteractiveLedIndicators()` — 40dp для ≤6, 28dp для 8, 22dp для 10, адаптивный spacing
- [x] 4.2 Генерация динамической цветовой шкалы (красный→оранжевый→зелёный) вместо фиксированного `LED_ACTIVE_COLORS`
- [x] 4.3 Добавить предупреждение-ссылку на экране Dashboard — если статистика хуже установленного на >20%, показать сообщение внизу со ссылкой в настройки батареи. Не показывать если статистики нет

## 5. Настройки: Чистка и email

- [x] 5.1 Удалить `OutlinedButton` «Настроить голосовые команды» из раздела голосовых команд в `SettingsScreen.kt` (строки 306-315). Удалить неиспользуемый параметр `onOpenAssistantClick`
- [x] 5.2 Добавить локализованную ссылку «Обратиться к разработчику» рядом с версией приложения. Добавить строковый ресурс во все 13 файлов strings.xml
- [x] 5.3 По нажатию ссылки — показать диалог согласия, затем запустить экспорт телеметрии (зависит от 6.*)

## 6. Телеметрия и экспорт

- [x] 6.1 Создать `ErrorLogCollector.kt` в `service/` — перехват необработанных исключений через `Thread.setDefaultUncaughtExceptionHandler`, запись в `filesDir/telemetry/errors.jsonl` (JSON Lines: timestamp, errorType, message, stackTrace)
- [x] 6.2 Создать `TelemetryExportUseCase.kt` в `domain/usecase/` — формирование ZIP-архива по выбранным категориям (6 категорий). Включает: settings.json (полный снапшот настроек), device_info.json, led_events.json, lamp_statistics.json, sessions.json, tracks.json, errors.jsonl, locations.json (опционально). Данные за 30 дней.
- [x] 6.3 Создать UI диалога экспорта с чекбоксами: `TelemetryExportDialog.kt` в `ui/settings/`. 6 категорий, 2 заблокированы (устройство + настройки), GPS выключен по умолчанию. Показывать оценку размера архива.
- [x] 6.4 Интегрировать `ErrorLogCollector` в `FigaGoApplication.kt` (init при старте приложения). Подключить диалог экспорта к кнопке «Обратиться к разработчику» через `Intent.ACTION_SEND` (email: sumaneev@gmail.com, тема: «FigaGo Telemetry v{versionName}»)

## 7. Локализация

- [x] 7.1 Добавить новые строковые ресурсы во все 13 файлов strings.xml: ссылка «Обратиться к разработчику», диалог согласия телеметрии, «нет данных», предупреждение о расхождении статистики, названия лампочек в настройках

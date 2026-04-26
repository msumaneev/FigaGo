## Context

FigaGo — Android-приложение для GPS-трекинга перемещений на инвалидной коляске. Ключевая фича — прогнозирование остаточного пробега по LED-индикаторам батареи пульта коляски. Текущая реализация:

- **Настройки батареи** (`SettingsScreen.kt`): один `WheelNumberPicker` для общего пробега и один для количества лампочек (range 1..10). Ранее были индивидуальные барабаны для каждой лампочки, но были удалены при упрощении.
- **Прогноз** (`ForecastRemainingDistanceUseCase.kt`): линейное распределение пробега по лампочкам (`maxMileage / ledCount`). Поддержка `ledDistances` в профиле уже есть, но не активирована.
- **PoNR** (`TrackingService.kt:669-682`): рассчитывается как `(currentDistanceKm + forecast) / 2`, где forecast плавает — приводит к ложным срабатываниям.
- **Телеметрия**: отсутствует.
- **Dashboard** (`DashboardScreen.kt:476-519`): LED-индикаторы фиксированного размера (40dp/32dp) с жёстким массивом цветов на 5 элементов.

## Goals / Non-Goals

**Goals:**
- Реализовать нелинейные весовые профили батареи (Type_3..Type_10) для точного прогнозирования
- Вернуть индивидуальные барабаны по каждой лампочке с двусторонней синхронизацией (общий ↔ каждая)
- Исправить баг точки невозврата (PoNR)
- Добавить телеметрию и email-экспорт с подтверждением пользователя
- Очистить настройки (удалить «Настроить голосовые команды», добавить email-ссылку)
- Корректно отображать 8 и 10 лампочек в UI

**Non-Goals:**
- Синхронизация данных между устройствами
- Серверная телеметрия (только локальная + email)
- Изменение алгоритма GPS-трекинга
- Изменение голосовых команд (только удаление кнопки настройки)

## Decisions

### 1. Профили весов — Enum-объект вместо БД

**Решение:** Хранить весовые массивы в `object BatteryWeightProfiles` (Kotlin object), а не в Room.

**Альтернатива:** Таблица конфигурации в Room.

**Обоснование:** Веса фиксированы по ТЗ, не редактируются пользователем. Enum проще, не требует миграции, удобно для тестирования. Если в будущем нужны пользовательские веса — можно добавить поверх.

```kotlin
object BatteryWeightProfiles {
    val TYPE_3  = floatArrayOf(0.45f, 0.35f, 0.20f)
    val TYPE_4  = floatArrayOf(0.35f, 0.30f, 0.20f, 0.15f)
    val TYPE_5  = floatArrayOf(0.30f, 0.20f, 0.20f, 0.15f, 0.15f)
    val TYPE_6  = floatArrayOf(0.25f, 0.20f, 0.15f, 0.15f, 0.15f, 0.10f)
    val TYPE_8  = floatArrayOf(0.20f, 0.15f, 0.15f, 0.12f, 0.12f, 0.12f, 0.09f, 0.05f)
    val TYPE_10 = floatArrayOf(0.13f, 0.13f, 0.13f, 0.10f, 0.10f, 0.10f, 0.10f, 0.07f, 0.07f, 0.07f)
    
    fun getWeights(ledCount: Int): FloatArray = when(ledCount) {
        3 -> TYPE_3; 4 -> TYPE_4; 5 -> TYPE_5
        6 -> TYPE_6; 8 -> TYPE_8; 10 -> TYPE_10
        else -> FloatArray(ledCount) { 1f / ledCount } // fallback linear
    }
}
```

### 2. Исправление PoNR — использование maxMileage из профиля

**Решение:** Вместо `maxDistanceKm = currentDistanceKm + forecast` использовать фиксированное значение `maxMileage` из профиля.

**Текущий (некорректный) расчёт:**
```
ponrKm = (currentDistanceKm + forecast) / 2   // forecast ≈ maxMileage - currentDistanceKm
// → ponrKm ≈ maxMileage / 2  (но с шумом из-за пересчёта forecast)
```

**Новый расчёт:**
```kotlin
val maxMileageKm = currentProfile.maxMileage.toDouble()  // Фиксированное из паспорта
val ponrKm = maxMileageKm / 2.0
// Предупреждение, когда текущий пробег ≥ ponrKm - ponrWarningKm
```

**Обоснование:** PoNR — это "половина общего ресурса". Общий ресурс — паспортное значение, а не динамический прогноз. Это устраняет ложные срабатывания.

### 3. Таблица LampStatistics — скользящее окно через DAO

**Решение:** Room Entity + DAO с автоматическим удалением старых записей при вставке.

```kotlin
@Entity(tableName = "lamp_statistics")
data class LampStatisticsEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "profile_id") val profileId: Long,
    @ColumnInfo(name = "lamp_index") val lampIndex: Int,
    @ColumnInfo(name = "actual_distance") val actualDistance: Float,
    @ColumnInfo(name = "timestamp") val timestamp: Long
)
```

**HISTORY_LIMIT = 5** — достаточно для сглаживания без потери актуальности.

### 4. Dropdown для выбора количества лампочек

**Решение:** Заменить `WheelNumberPicker` (range 1..10) на `ExposedDropdownMenuBox` с фиксированными значениями [3, 4, 5, 6, 8, 10].

**Обоснование:** WheelPicker с range 1..10 позволяет выбрать невалидные значения (1, 2, 7, 9). Dropdown — стандартный Material3 паттерн для дискретных наборов.

### 5. Адаптивное отображение LED на Dashboard

**Решение:** Динамический размер LED: для ≤6 — текущие 40dp/32dp, для 8 — 28dp/22dp, для 10 — 22dp/18dp. Spacing тоже адаптивный (12dp → 6dp → 4dp).

**Альтернатива:** Wrap на две строки.

**Обоснование:** Одна строка лучше для быстрого визуального считывания в движении. Уменьшение на 2-3 значения кликабельности при 10 лампочках допустимо.

### 6. Телеметрия — файловый лог + Intent.ACTION_SEND

**Решение:** 
- `TelemetryCollector` — собирает события в файл (JSON Lines) в `filesDir/telemetry/`
- При нажатии email-ссылки: собирает экспорт из Room (sessions, led events, locations) + телеметрию + метаданные устройства → пакует в ZIP → создает `Intent.ACTION_SEND` с attachment.
- **Перед отправкой** обязательный `AlertDialog`: «Данные содержат GPS-координаты и информацию о вашем устройстве. Отправить разработчику?»

**Альтернатива:** Firebase Crashlytics / Analytics.

**Обоснование:** Приложение работает офлайн, целевая аудитория малочисленна. Email-экспорт проще, не требует серверной инфраструктуры, даёт полный контроль пользователю.

### 7. Двусторонняя синхронизация барабанов лампочек

**Решение:** В `SettingsViewModel` при изменении общего пробега пересчитывать `ledDistances` через веса:
```
ledDistances[i] = maxMileage * weights[i]
```
При изменении любой лампочки:
```
maxMileage = sum(ledDistances)
```

Рядом с каждым барабаном — серый текст с средним пробегом из LampStatistics (или "нет данных").

## Risks / Trade-offs

| Риск | Митигация |
|------|-----------|
| Room-миграция может потерять данные при обновлении | Автоматическая миграция `addMigrations()` — только ADD TABLE, нет ALTER |
| Пользователь может ввести нереалистичные значения пробега через барабаны | Валидация: сумма лампочек = общий пробег, warning при > 150 км |
| Email-экспорт ZIP может быть большим (много LocationPoint) | Ограничить экспорт последними 30 днями |
| TTS PoNR не сработает если профиль без maxMileage | Проверка `maxMileage != null`, fallback на текущую логику |
| Цвета массива LED_ACTIVE_COLORS жёстко на 5 | Генерировать цветовую шкалу динамически (красный → оранжевый → зелёный) через интерполяцию |

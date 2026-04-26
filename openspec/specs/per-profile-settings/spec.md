## Per-Profile Settings

Каждый профиль коляски имеет полностью независимый набор настроек.
При переключении активного профиля UI автоматически перезагружает настройки.

### Requirement: Per-Profile DataStore Keys
Система SHALL хранить все per-profile настройки с префиксом `p{profileId}_` в DataStore,
чтобы каждая коляска имела свои значения.

#### Scenario: Independent TTS intervals
- **GIVEN** профиль «Vermeiren» (id=1) с TTS интервалом 5 мин
- **AND** профиль «Ortonika» (id=2) с TTS интервалом 1 мин
- **WHEN** пользователь переключается с «Ortonika» на «Vermeiren»
- **THEN** экран настроек отображает TTS интервал = 5 мин

#### Scenario: Independent unit systems
- **GIVEN** профиль «Vermeiren» с единицами «Километры»
- **AND** профиль «Ortonika» с единицами «Мили»
- **WHEN** пользователь переключается на «Ortonika»
- **THEN** дашборд показывает скорость/дистанцию в милях

### Requirement: Auto-Reload on Profile Switch
Система SHALL автоматически перезагружать все настройки при смене `activeProfileId`.

#### Scenario: Settings reload
- **WHEN** пользователь нажимает плитку другого профиля в карусели
- **THEN** `SettingsViewModel` получает событие через `observeActiveProfileId()`
- **AND** вызывает `reloadSettingsForProfile()` для загрузки настроек нового профиля

### Requirement: Global Active Profile ID
Ключ `active_profile_id` SHALL оставаться глобальным (без префикса профиля).

## Per-Profile Keys (DataStore)

| Ключ (шаблон) | Тип | Дефолт | Описание |
|---|---|---|---|
| `p{id}_unit_system` | String | `"km"` | Единицы измерения |
| `p{id}_use_statistics` | Boolean | `true` | Использовать статистику батареи |
| `p{id}_vibrate_on_command` | Boolean | `true` | Вибрация на голосовую команду |
| `p{id}_sound_on_command` | Boolean | `false` | Звук на голосовую команду |
| `p{id}_tts_announce_mode` | String | `"off"` | Режим TTS: off/distance/time |
| `p{id}_tts_distance_interval_km` | Double | `1.0` | Интервал TTS по расстоянию |
| `p{id}_tts_time_interval_min` | Int | `15` | Интервал TTS по времени |
| `p{id}_gps_interval_sec` | Int | `10` | Интервал GPS записи |
| `p{id}_auto_close_time` | String | `"23:59"` | Время автозакрытия |
| `p{id}_auto_pause_speed_limit_kmh` | Int | `15` | Лимит авто-паузы |
| `p{id}_point_of_no_return_warning_km` | Int | `2` | Предупреждение невозврата |
| `p{id}_default_range_level_{L}` | Double | varies | Пробег на уровне заряда L |

## Implementation

- **File**: `SettingsRepositoryImpl.kt` — все get/set/observe методы используют `pid()` для получения текущего профиля
- **File**: `SettingsViewModel.kt` — подписка на `observeActiveProfileId()` с автоматическим `reloadSettingsForProfile()`
- **Flow**: `flatMapLatest` используется для reactive-наблюдения с автопереключением при смене профиля

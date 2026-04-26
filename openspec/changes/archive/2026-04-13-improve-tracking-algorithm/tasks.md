## 1. Базовые фиксы Tracking Service и БД

- [x] 1.1 Добавить фильтр погрешности GPS в `TrackingService.kt` (отбрасывать `accuracy > 30f` в первые секунды старта сегмента).
- [x] 1.2 Исправить баг в `StartTrackUseCase.kt`: пробрасывать реальный `profileId` в момент вызова `trackSegmentDao.insert()`.
- [x] 1.3 Реализовать создание нового `TrackSegment` при получении GPS-координаты, если `timeDiffMs > 120000` (2 минуты с предыдущей точки), для разрыва линии на карте при убийстве процесса системой.

## 2. Activity Recognition Integration

- [x] 2.1 Добавить `play-services-location` в `build.gradle.kts` и разрешение `ACTIVITY_RECOGNITION` в AndroidManifest.
- [x] 2.2 Модифицировать запрос разрешений перед стартом трека в UI (`CheckPermissionsDialog` или сходный) для запроса `ACTIVITY_RECOGNITION` на Android 10+.
- [x] 2.3 Создать `ActivityTransitionReceiver` для подписки на события перехода IN_VEHICLE через `ActivityRecognitionClient`.
- [x] 2.4 Внедрить State Machine на базе Activity Recognition в `TrackingService.kt`, объединив его с Fallback-гистерезисом по скорости.

## 3. History Aggregation & Toggle UI

- [x] 3.1 Расширить `SessionRepository` и `DaySessionDao`: добавить метод для получения сгруппированных сессий по дню `getAllSessionsGroupedByDate()`.
- [x] 3.2 Добавить в `HistoryViewModel.kt` стейт `showAllProfiles: Boolean` и логику переключения потока сессий.
- [x] 3.3 Обновить UI `HistoryScreen.kt`: добавить Switch "Все коляски".
- [x] 3.4 Настроить UI карточек: для `showAllProfiles=true` выводить сводную дистанцию и иконки всех использованных профилей в этот день.

## 4. History Map Visuals

- [x] 4.1 Исправить отрисовку в `HistoryMapScreen.kt`. Вместо одной `Polyline` с частичным PathEffect, группировать смежные точки `location.is_transport == true` в отдельные Polylines красного цвета.
- [x] 4.2 Убедиться, что при переходе `showAllProfiles=true` карта стягивает все точки со всех нужных `day_session` и рисует их корректно.
- [x] 4.3 Проверить, что разрывы сегментов (`track_segment.id` различаются) обрабатываются корректно, и между ними не рисуется прямая синяя линия.

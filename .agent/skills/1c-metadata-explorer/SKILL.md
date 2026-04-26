---
name: 1c-metadata-explorer
description: Исследование структуры конфигурации 1С и метаданных.
---

# Исследователь метаданных 1С

Навык для получения информации о структуре конфигурации: списке объектов, их реквизитах и табличных частях.

## Инструменты

### Информация о конфигурации

Получение общей информации о текущей базе (имя, версия, режим работы):

```bash
python .agent/skills/1c-metadata-explorer/scripts/explorer.py --action info
```

### Список всех объектов (Summary)

Получение полного списка всех справочников, документов и регистров:

```bash
python .agent/skills/1c-metadata-explorer/scripts/explorer.py --action summary
```

### Детали объекта (Detail)

Получение реквизитов, табличных частей и свойств конкретного объекта:

```bash
python .agent/skills/1c-metadata-explorer/scripts/explorer.py --action detail --type "Справочник" --name "Номенклатура"
```

## Технические детали

- **Конфигурация** — `.agent/skills/1c-metadata-explorer/config.json`
- Скрипт импортирует `plugin_transport.py` из корня проекта.

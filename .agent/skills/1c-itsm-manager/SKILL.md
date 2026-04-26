---
name: 1c-itsm-manager
description: Работа с задачами разработчика в ITSM (Канбан).
---

# Менеджер задач ITSM (1C)

Навык для получения списка задач и деталей нарядов из ITSM.

## Инструменты

### Список задач
Возвращает список активных задач (нарядов) текущего разработчика.

```bash
python .agent/skills/1c-itsm-manager/scripts/itsm_client.py --action list
```

### Детали задачи
Получает полное описание конкретной задачи.

```bash
python .agent/skills/1c-itsm-manager/scripts/itsm_client.py --action details --task-id "123456"
```

## Технические детали
- **Конфигурация**: `.agent/skills/1c-itsm-manager/config.json`
- **Зависимости**: `plugin_transport.py`
- Использует SOAP интеграцию с ITSM-системой.
- `QUERY_API_KEY` в `.env` должен соответствовать UUID физлица разработчика в ITSM.

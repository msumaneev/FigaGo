---
name: 1c-processing-manager
description: Захват (Pull) и Сборка (Build) внешних обработок 1С (EPF/ERF). Управление задачами ITSM.
---

# Менеджер внешних обработок 1С

Навык для работы с внешними обработками (EPF/ERF) в 1С:Предприятие.
Все инструменты находятся **внутри папки навыка** — v8unpack, конфиг, скрипты.

## Быстрый старт

### Получение обработки (Pull)

Если пользователь просит «взять наряд», «скачать обработку», «pull обработку», либо **дает прямой локальный путь к файлу `.epf`**:

```bash
# Для скачивания из 1С по имени:
python .agent/skills/1c-processing-manager/scripts/manager.py --action pull --name "ИмяОбработки"

# Для распаковки существующего локального файла:
python .agent/skills/1c-processing-manager/scripts/manager.py --action pull --name "Путь\к\локальному\файлу.epf"
```

Скрипт в зависимости от переданного параметра:
1. Либо найдёт обработку в 1С по имени, поставит блокировку и скачает бинарный файл (EPF/ERF)
2. Либо, если это локальный файл, возьмёт его напрямую (без скачивания и изменения данных в 1С)
3. Распакует через встроенный `v8unpack.exe -P` в текущую папку `workspace/` или в ту же самую папку
4. Выведет путь к распакованным исходникам

### Сборка обработки (Build)

Если пользователь просит «собрать», «сохранить», «build»:

```bash
python .agent/skills/1c-processing-manager/scripts/manager.py --action build --source-dir "C:/path/to/unpacked/src"
```

Скрипт:
1. Соберёт EPF/ERF из папки исходников через `v8unpack.exe -B`
2. Создаст версионированный файл (Имя_v1.epf, Имя_v2.epf, …)
3. Оригинальный файл **не перезатирается**

### Список задач ITSM

```bash
python .agent/skills/1c-processing-manager/scripts/manager.py --action tasks
```

### Детали задачи

```bash
python .agent/skills/1c-processing-manager/scripts/manager.py --action task-details --id "UUID-задачи"
```

## Технические детали

- **Конфигурация** подключения (URL 1С, ITSM, API-ключ) — `.agent/skills/1c-processing-manager/config.json`
- **v8unpack.exe** — `.agent/skills/1c-processing-manager/bin/v8unpack.exe`
- **plugin_transport.py** — `.agent/skills/1c-processing-manager/scripts/plugin_transport.py` (Авторы скопировали этот модуль в папку со скиллом, чтобы скилл можно было переносить как единое целое)
- Скрипт автоматически находит утилиты, конфиги и транспорт **относительно себя** — он не нуждается в PATH и не зависит от корневого проекта.
- В `config.json` параметр `workspace_dir` можно задавать как `.` (текущая директория), чтобы распаковка и скачивание происходило прямо из места запуска скрипта (текущей директории).

## Портативность
Папку `1c-processing-manager` можно скопировать в любой другой проект или окружение. Главное, чтобы внутри были:
- `bin/v8unpack.exe`
- `config.json` с вашими параметрами подключения
- `scripts/manager.py` и `scripts/plugin_transport.py`

## Рабочий процесс (Workflow)

1. `--action pull` → получаем исходники в `workspace/`
2. Редактируем `.bsl`/`text` файлы (модуль объекта)
3. `--action build` → собираем обратно в EPF
4. Пользователь привязывает файл в 1С

> **ВАЖНО:** Редактировать можно ТОЛЬКО текстовые файлы модулей (`.bsl`, `text`).
> XML-файлы визуального представления форм изменять ЗАПРЕЩЕНО.

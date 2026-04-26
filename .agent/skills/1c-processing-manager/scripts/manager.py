#КОНТИНЕНТ Суманеев М и AI наряд #000, 12.03.2026
"""
Менеджер внешних обработок 1С (EPF/ERF).
CLI-обёртка над plugin_transport.py — Pull, Build, Tasks.

Использование:
  python manager.py --action pull --name "ИмяОбработки"
  python manager.py --action build --source-dir "C:/path/to/src"
  python manager.py --action tasks
  python manager.py --action task-details --id "UUID"
"""

import os
import sys
import json
import argparse

# ─── Пути относительно файла скрипта ────────────────────────────────────────
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
SKILL_DIR = os.path.dirname(SCRIPT_DIR)  # .skills/1c-processing-manager/

V8UNPACK_PATH = os.path.join(SKILL_DIR, "bin", "v8unpack.exe")
CONFIG_PATH = os.path.join(SKILL_DIR, "config.json")

# ─── Загрузка конфигурации ───────────────────────────────────────────────────
def load_config() -> dict:
    """Загружает config.json навыка."""
    with open(CONFIG_PATH, "r", encoding="utf-8") as f:
        return json.load(f)


def apply_config_to_env(config: dict):
    """
    Проецирует значения из config.json навыка в переменные окружения,
    которые ожидает plugin_transport.py.
    Если переменная уже установлена (из .env или системы) — НЕ перезатирает.
    """
    mapping = {
        "1c_prod_url":    "1C_PROD_URL",
        "1c_auth_token":  "1C_AUTH_TOKEN",
        "query_api_key":  "QUERY_API_KEY",
        "workspace_dir":  "LOCAL_WORKSPACE_DIR",
        "itsm_wsdl_url":  "ITSM_WSDL_URL",
        "itsm_user":      "ITSM_USER",
        "itsm_password":  "ITSM_PASSWORD",
    }
    for json_key, env_key in mapping.items():
        value = config.get(json_key, "")
        if value and not os.environ.get(env_key):
            os.environ[env_key] = str(value)


# ─── Импорт локального plugin_transport ──────────────────────────────────────
def get_transport():
    """
    Импортирует plugin_transport.py из папки скрипта.
    Добавляет SCRIPT_DIR в sys.path, если ещё не добавлен.
    """
    if SCRIPT_DIR not in sys.path:
        sys.path.insert(0, SCRIPT_DIR)

    # Загружаем конфиг навыка → проецируем в env ДО импорта transport
    try:
        config = load_config()
        apply_config_to_env(config)
    except FileNotFoundError:
        print(f"⚠️ Конфиг не найден: {CONFIG_PATH}", file=sys.stderr)

    import plugin_transport
    return plugin_transport


# ─── CLI команды ─────────────────────────────────────────────────────────────
def cmd_pull(args):
    """Получение (Pull) обработки из 1С."""
    transport = get_transport()
    work_dir = args.work_dir or os.environ.get("LOCAL_WORKSPACE_DIR", os.getcwd())
    result = transport.pull_and_lock(args.name, work_dir=work_dir)
    print(json.dumps(result, ensure_ascii=False, indent=2))


def cmd_build(args):
    """Сборка (Build) обработки из исходников."""
    transport = get_transport()
    output_dir = args.output_dir or os.environ.get("LOCAL_WORKSPACE_DIR", None)
    result = transport.build_plugin(args.source_dir, output_dir=output_dir)
    print(json.dumps(result, ensure_ascii=False, indent=2))


def cmd_tasks(args):
    """Список задач ITSM."""
    transport = get_transport()
    result = transport.get_tasks()
    print(json.dumps(result, ensure_ascii=False, indent=2))


def cmd_task_details(args):
    """Детали задачи ITSM."""
    transport = get_transport()
    result = transport.get_task_details(args.id)
    print(json.dumps(result, ensure_ascii=False, indent=2))


# ─── Точка входа ─────────────────────────────────────────────────────────────
def main():
    parser = argparse.ArgumentParser(
        description="Менеджер внешних обработок 1С (EPF/ERF)",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Примеры:
  %(prog)s --action pull --name "ЗагрузкаДокументов"
  %(prog)s --action build --source-dir "C:/workspace/ЗагрузкаДокументов"
  %(prog)s --action tasks
  %(prog)s --action task-details --id "9c115783-db3a-11db-8458-000e7ff21b7c"
"""
    )

    parser.add_argument(
        "--action",
        choices=["pull", "build", "tasks", "task-details"],
        required=True,
        help="Действие: pull (скачать), build (собрать), tasks (список задач), task-details (детали задачи)"
    )
    parser.add_argument("--name", help="Имя обработки для pull")
    parser.add_argument("--source-dir", help="Путь к папке исходников для build")
    parser.add_argument("--output-dir", help="Путь к папке для сохранения собранного EPF (по умолчанию workspace)")
    parser.add_argument("--work-dir", help="Рабочая папка IDE для pull (по умолчанию workspace)")
    parser.add_argument("--id", help="UUID задачи для task-details")

    args = parser.parse_args()

    # Маршрутизация
    if args.action == "pull":
        if not args.name:
            parser.error("--name обязателен для действия pull (имя обработки или путь к локальному файлу .epf)")
        cmd_pull(args)

    elif args.action == "build":
        if not args.source_dir:
            parser.error("--source-dir обязателен для действия build")
        cmd_build(args)

    elif args.action == "tasks":
        cmd_tasks(args)

    elif args.action == "task-details":
        if not args.id:
            parser.error("--id обязателен для действия task-details")
        cmd_task_details(args)


if __name__ == "__main__":
    main()
#КОНТИНЕНТ конец исправления Суманеев М и AI

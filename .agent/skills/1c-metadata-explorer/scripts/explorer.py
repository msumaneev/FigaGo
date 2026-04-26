#КОНТИНЕНТ Суманеев М и AI наряд #000, 12.03.2026
"""
Исследователь метаданных 1С.
CLI-обёртка над plugin_transport.py — Info, Summary, Detail.
"""

import os
import sys
import json
import argparse

# Force UTF-8 for Windows terminal
if sys.platform == "win32":
    import io
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", line_buffering=True)

# ─── Пути относительно файла скрипта ────────────────────────────────────────
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
SKILL_DIR = os.path.dirname(SCRIPT_DIR)
PROJECT_ROOT = os.path.dirname(os.path.dirname(SKILL_DIR))

CONFIG_PATH = os.path.join(SKILL_DIR, "config.json")

def load_config() -> dict:
    with open(CONFIG_PATH, "r", encoding="utf-8") as f:
        return json.load(f)

def apply_config_to_env(config: dict):
    mapping = {
        "1c_prod_url":    "1C_PROD_URL",
        "1c_auth_token":  "1C_AUTH_TOKEN",
    }
    for json_key, env_key in mapping.items():
        value = config.get(json_key, "")
        if value and not os.environ.get(env_key):
            os.environ[env_key] = str(value)

def get_transport():
    if PROJECT_ROOT not in sys.path:
        sys.path.insert(0, PROJECT_ROOT)
    try:
        config = load_config()
        apply_config_to_env(config)
    except FileNotFoundError:
        pass
    import plugin_transport
    return plugin_transport

def main():
    parser = argparse.ArgumentParser(description="Исследователь метаданных 1С")
    parser.add_argument("--action", choices=["info", "summary", "detail"], required=True)
    parser.add_argument("--type", help="Тип объекта (для detail)")
    parser.add_argument("--name", help="Имя объекта (для detail)")
    
    args = parser.parse_args()
    transport = get_transport()
    
    if args.action == "info":
        result = transport.get_configuration_info()
        print(json.dumps(result, ensure_ascii=False, indent=2))
    elif args.action == "summary":
        result = transport.get_metadata_summary()
        print(json.dumps(result, ensure_ascii=False, indent=2))
    elif args.action == "detail":
        if not args.type or not args.name:
            print("Ошибка: --type и --name обязательны для действия detail", file=sys.stderr)
            sys.exit(1)
        result = transport.get_metadata_detail(args.type, args.name)
        print(json.dumps(result, ensure_ascii=False, indent=2))

if __name__ == "__main__":
    main()
#КОНТИНЕНТ конец исправления Суманеев М и AI

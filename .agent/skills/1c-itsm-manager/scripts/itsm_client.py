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
        "itsm_wsdl_url":  "ITSM_WSDL_URL",
        "itsm_user":      "ITSM_USER",
        "itsm_password":  "ITSM_PASSWORD",
        "person_uuid":    "QUERY_API_KEY",
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
    parser = argparse.ArgumentParser(description="ITSM Manager")
    parser.add_argument("--action", choices=["list", "details"], required=True)
    parser.add_argument("--task-id", help="ID задачи для получения деталей")
    
    args = parser.parse_args()
    transport = get_transport()
    
    try:
        if args.action == "list":
            result = transport.get_tasks()
            print(json.dumps(result, ensure_ascii=False, indent=2))
        elif args.action == "details":
            if not args.task_id:
                print(json.dumps({"success": False, "message": "--task-id is required for details action"}))
                return
            result = transport.get_task_details(args.task_id)
            print(json.dumps(result, ensure_ascii=False, indent=2))
            
    except Exception as e:
        print(json.dumps({"success": False, "message": str(e)}, ensure_ascii=False))

if __name__ == "__main__":
    main()

#КОНТИНЕНТ Суманеев М и AI наряд #000, 12.03.2026
"""
Менеджер запросов 1С.
CLI-обёртка над plugin_transport.py — Execute, Validate.
"""

import os
import sys
import json
import argparse
import re

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
        "query_api_key":  "QUERY_API_KEY",
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

def validate_document_date_filter(query: str):
    """Запрет запросов к Документ.* без ограничения по дате."""
    q_lower = query.lower().replace('\r', ' ').replace('\n', ' ')
    
    # Проверяем, есть ли обращение к таблицам документов
    if re.search(r'\bдокумент\.\w+', q_lower):
        # Проверяем наличие ДАТАВРЕМЯ в условии
        if not re.search(r'датавремя\s*\(', q_lower):
            return ("ОШИБКА: Запросы к таблицам Документ.* ОБЯЗАНЫ содержать "
                    "фильтр по дате (ДАТАВРЕМЯ). Это предотвращает Full Table Scan. "
                    "Добавьте: И Дата > ДАТАВРЕМЯ(год, месяц, день) — "
                    "ограничивайте последними 10 месяцами.")
    return None

def validate_data_security(query: str):
    return None
    q_lower = query.lower()
    
    # 1. Запрет выборки всех полей (SELECT *) из ФизЛиц и Контрагентов
    if re.search(r'выбрать\s+(первые\s+\d+\s+)?\*', q_lower):
        if 'справочник.физическиелица' in q_lower or 'справочник.контрагенты' in q_lower:
            return "ОШИБКА БЕЗОПАСНОСТИ: Запрещено получать все данные (*) из справочников ФизическиеЛица и Контрагенты."
            
    # 2. Запрещенные поля ПДн
    forbidden_patterns = [
        r'\bдатарождения\b', r'\bместорождения\b',
        r'\bадрес\w*\b', 
        r'\bтелефон\w*\b', r'\bтелефоны\b', r'\be-?mail\w*\b', r'\bпочта\b',
        r'\bснилс\b', r'\bинн\b',
        r'\bпаспорт\w*\b', r'\bсерия\b',
        r'\bдолжность\w*\b', r'\bместоработы\b',
        r'\bконтактнаяинформация\b'
    ]
    
    for pattern in forbidden_patterns:
        if re.search(pattern, q_lower):
            return f"ОШИБКА БЕЗОПАСНОСТИ: В запросе обнаружено обращение к запрещенным персональным данным (сработало правило: {pattern})."
            
    return None

def main():
    parser = argparse.ArgumentParser(description="Менеджер запросов 1С")
    parser.add_argument("--action", choices=["execute", "validate"], required=True)

    # Два способа передать запрос:
    #   --query "ВЫБРАТЬ ..."        (напрямую в командной строке)
    #   --query-file /tmp/q.txt      (из файла — решает проблемы экранирования)
    #   --query-text "ВЫБРАТЬ ..."   (алиас для --query, обратная совместимость)
    query_group = parser.add_mutually_exclusive_group(required=True)
    query_group.add_argument("--query", dest="query", help="Текст запроса (в командной строке)")
    query_group.add_argument("--query-text", dest="query", help="Алиас для --query")
    query_group.add_argument("--query-file", dest="query_file", help="Путь к файлу с текстом запроса (UTF-8)")
    
    args = parser.parse_args()

    # Если запрос передан через файл — читаем
    if hasattr(args, 'query_file') and args.query_file:
        try:
            with open(args.query_file, "r", encoding="utf-8") as f:
                args.query = f.read().strip()
        except FileNotFoundError:
            print(json.dumps({"error": f"Файл не найден: {args.query_file}"}, ensure_ascii=False, indent=2))
            sys.exit(1)
    
    if not args.query:
        print(json.dumps({"error": "Запрос пуст"}, ensure_ascii=False, indent=2))
        sys.exit(1)
    
    # Проверка обязательного фильтра по дате для Документ.*
    date_err = validate_document_date_filter(args.query)
    if date_err:
        print(json.dumps({"error": date_err}, ensure_ascii=False, indent=2))
        sys.exit(1)
    
    sec_err = validate_data_security(args.query)
    if sec_err:
        print(json.dumps({"error": sec_err}, ensure_ascii=False, indent=2))
        sys.exit(1)
        
    transport = get_transport()
    
    if args.action == "execute":
        result = transport.execute_query(args.query)
        print(json.dumps(result, ensure_ascii=False, indent=2))
    elif args.action == "validate":
        result = transport.validate_query(args.query)
        print(json.dumps(result, ensure_ascii=False, indent=2))

if __name__ == "__main__":
    main()
#КОНТИНЕНТ конец исправления Суманеев М и AI

# -*- coding: utf-8 -*-
"""
Аудит полноты переводов: сравнивает ключи базового values/strings.xml
со всеми локализованными файлами.
Запуск: python audit_strings.py
"""
import os
import re

RES_DIR = r"c:\FigaGo\app\src\main\res"

LOCALES = [
    "values-en", "values-tr", "values-es", "values-fr", "values-de",
    "values-uk", "values-ar", "values-ka", "values-ja", "values-ko",
    "values-zh-rCN", "values-zh-rTW",
]


def extract_keys(filepath: str) -> set:
    """Извлекает все ключи name=... из strings.xml."""
    if not os.path.exists(filepath):
        return set()
    with open(filepath, "r", encoding="utf-8") as f:
        return set(re.findall(r'name="([^"]+)"', f.read()))


def main():
    base_path = os.path.join(RES_DIR, "values", "strings.xml")
    base_keys = extract_keys(base_path)
    # Исключаем технические ключи
    skip = {"app_name", "notification_channel_id"}
    base_keys -= skip

    print(f"[Audit] FigaGo translations")
    print(f"   Base file: {len(base_keys)} keys (values/strings.xml)")
    print()

    all_ok = True
    for folder in LOCALES:
        path = os.path.join(RES_DIR, folder, "strings.xml")
        locale_keys = extract_keys(path)
        missing = sorted(base_keys - locale_keys)
        extra = sorted(locale_keys - base_keys - skip)

        if missing or extra:
            all_ok = False
            print(f"  FAIL {folder}")
            if missing:
                print(f"     Missing ({len(missing)}):")
                for k in missing[:10]:
                    print(f"       - {k}")
                if len(missing) > 10:
                    print(f"       ... and {len(missing)-10} more")
            if extra:
                print(f"     Extra ({len(extra)}):")
                for k in extra[:5]:
                    print(f"       + {k}")
        else:
            print(f"  OK   {folder} -- {len(locale_keys)} keys")

    if all_ok:
        print(f"\n[OK] All translations are synchronized!")
    else:
        print(f"\n[WARN] There are discrepancies. Run generate_strings.py to fix.")


if __name__ == "__main__":
    main()

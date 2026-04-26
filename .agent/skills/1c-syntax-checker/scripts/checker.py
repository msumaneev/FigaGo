import os
import sys
import argparse
import urllib.request
import zipfile
import subprocess
import json
from pathlib import Path

# Конфигурация
GITHUB_API_URL = "https://api.github.com/repos/1c-syntax/bsl-language-server/releases/latest"
SKILL_DIR = Path(__file__).parent.parent
BIN_DIR = SKILL_DIR / "bin"
EXE_PATH = BIN_DIR / "bsl-language-server.exe"

def download_latest_release():
    print("Получение информации о последнем релизе bsl-language-server...")
    try:
        req = urllib.request.Request(GITHUB_API_URL, headers={'User-Agent': 'Mozilla/5.0'})
        with urllib.request.urlopen(req) as response:
            data = json.loads(response.read().decode())
            
        win_asset = next((asset for asset in data['assets'] if asset['name'] == 'bsl-language-server_win.zip'), None)
        if not win_asset:
            print("Ошибка: релиз для Windows не найден.")
            sys.exit(1)
            
        download_url = win_asset['browser_download_url']
        zip_path = BIN_DIR / "bsl-language-server_win.zip"
        
        print(f"Скачивание {download_url}...")
        BIN_DIR.mkdir(parents=True, exist_ok=True)
        urllib.request.urlretrieve(download_url, zip_path)
        
        print("Распаковка архива...")
        with zipfile.ZipFile(zip_path, 'r') as zip_ref:
            zip_ref.extractall(BIN_DIR)
            
        # Удаляем архив после распаковки
        os.remove(zip_path)
        print("bsl-language-server успешно установлен.")
    except Exception as e:
        print(f"Ошибка при загрузке: {e}")
        sys.exit(1)

def run_analysis(src_dir):
    if not EXE_PATH.exists():
        download_latest_release()
        
    cmd = [
        str(EXE_PATH),
        "-a",
        "-s", str(src_dir),
        "-r", "console"
    ]
    
    print(f"Запуск анализа: {' '.join(cmd)}")
    
    # Запускаем процесс, перехватываем вывод
    # cp866 используется часто в консоли Windows, но bsl ls может выдавать utf-8
    result = subprocess.run(cmd, capture_output=True, text=True, encoding='utf-8', errors='replace')
    
    if result.stdout:
        print(result.stdout)
    if result.stderr:
        print("Сообщения STDERR:")
        print(result.stderr)
        
    # В случае отсутствия вывода (если репортер console почему-то пуст)
    if not result.stdout and not result.stderr:
        print("Проверка завершена. Вывод пуст (возможно, нет ошибок или нужно использовать другой reporter).")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Анализатор кода 1С с использованием BSL Language Server")
    parser.add_argument("--src-dir", required=True, help="Путь к директории с исходным кодом 1С")
    args = parser.parse_args()
    
    src_path = Path(args.src_dir).resolve()
    if not src_path.exists():
        print(f"Ошибка: директория {src_path} не существует.")
        sys.exit(1)
        
    run_analysis(src_path)

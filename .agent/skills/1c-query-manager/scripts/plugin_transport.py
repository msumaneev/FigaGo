import os
import requests
import base64
import json
from dotenv import load_dotenv
import zeep
from requests import Session
from requests.auth import HTTPBasicAuth
from zeep.transports import Transport
from typing import Dict, Any, List, Optional
import asyncio

# Load environment variables
load_dotenv()

def _find_v8unpack() -> str:
    """Поиск v8unpack.exe: рядом с exe (шара) → рядом со скриптом (dev) → PATH"""
    import sys
    # 1. Рядом с exe-бинарником (для режима шары)
    exe_dir = os.path.dirname(os.path.abspath(sys.executable))
    candidate = os.path.join(exe_dir, "v8unpack.exe")
    if os.path.exists(candidate):
        return candidate
    # 2. Рядом со скриптом (dev-режим: python mcp_server.py)
    script_dir = os.path.dirname(os.path.abspath(__file__))
    candidate = os.path.join(script_dir, "v8unpack.exe")
    if os.path.exists(candidate):
        return candidate
    # 3. В папке bin/ на уровень выше скрипта (для автономного скилла 1c-processing-manager)
    candidate = os.path.join(os.path.dirname(script_dir), "bin", "v8unpack.exe")
    if os.path.exists(candidate):
        return candidate
    # 4. Fallback на PATH
    return "v8unpack.exe"

# Settings
PROD_URL = os.environ.get("1C_PROD_URL", "http://krsWsUt.contlog.local/web/hs/web/data")
AUTH_TOKEN = os.environ.get("1C_AUTH_TOKEN", "Basic YXV0bzo4ODg=")
QUERY_API_KEY = os.environ.get("QUERY_API_KEY", "")
ITSM_WSDL_URL = os.environ.get("ITSM_WSDL_URL", "http://wsitsm.contlog.local:8082/wsitsm/ws/wsitsm.1cws?wsdl")
ITSM_USER = os.environ.get("ITSM_USER", "wsITSM")
ITSM_PASSWORD = os.environ.get("ITSM_PASSWORD", "kNBO1WZc019uvp86")
DEV_UUID = os.environ.get("QUERY_API_KEY") 

def _get_headers() -> Dict[str, str]:
    headers = {
        "Content-Type": "application/json",
        "Accept": "application/json"
    }
    if AUTH_TOKEN:
        if "Basic " in AUTH_TOKEN or "Bearer " in AUTH_TOKEN:
            headers["Authorization"] = AUTH_TOKEN
        else:
            headers["Authorization"] = f"Basic {AUTH_TOKEN}"
    return headers

def _get_itsm_auth_header() -> str:
    if not ITSM_USER or not ITSM_PASSWORD:
        return ""
    auth_str = f"{ITSM_USER}:{ITSM_PASSWORD}"
    encoded_auth = base64.b64encode(auth_str.encode('utf-8')).decode('utf-8')
    return f"Basic {encoded_auth}"

def _call_itsm_soap(operation: str, params: Dict[str, Any]) -> Any:
    if not ITSM_WSDL_URL:
        raise ValueError("ITSM_WSDL_URL is not set in .env")
    
    import urllib.parse
    
    soap_url = ITSM_WSDL_URL.split('?')[0]
    
    params_xml = ""
    for k, v in params.items():
        params_xml += f"<ns1:{k}>{v}</ns1:{k}>\n"
        
    soap_ns = "http://krs_1c.contlog.local/wsitsm"
    
    envelope = f"""<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ns1="{soap_ns}">
   <soapenv:Header/>
   <soapenv:Body>
      <ns1:{operation}>
         {params_xml}
      </ns1:{operation}>
   </soapenv:Body>
</soapenv:Envelope>"""

    quoted_operation = urllib.parse.quote(operation)
    headers = {
        "Content-Type": "text/xml; charset=utf-8",
        "SOAPAction": f"{soap_ns}#{quoted_operation}"
    }
    
    auth_header = _get_itsm_auth_header()
    if auth_header:
        headers["Authorization"] = auth_header
        
    response = requests.post(soap_url, data=envelope.encode('utf-8'), headers=headers, timeout=300)
    
    if response.status_code != 200:
        response.raise_for_status()
        
    return response.text

def _call_1c_method(method_id: str, params: Dict[str, Any] = None) -> Any:
    if params is None:
        params = {}
        
    payload = {
        "Идентификатор": method_id,
        "Параметры": params
    }
    
    response = requests.post(PROD_URL, json=payload, headers=_get_headers(), timeout=300)
    response.raise_for_status()
    
    content = response.text
    try:
        data = json.loads(content)
        if isinstance(data, str):
            try:
                data = json.loads(data)
            except:
                pass
                
        if isinstance(data, dict):
            if data.get("Ошибка", False) or data.get("Отказ", False):
                err_msg = data.get("ОписаниеОшибки") or data.get("СообщениеОтказа") or "1C Backend Error"
                return {"error": err_msg}
                
            res = None
            if "Результат" in data and data["Результат"] is not None:
                res = data["Результат"]
            elif "Выполнено" in data and data["Выполнено"] not in (True, False, None):
                res = data["Выполнено"]
                
            if res is not None:
                if isinstance(res, str):
                    try: res = json.loads(res)
                    except: pass
                return res
        return data
    except json.JSONDecodeError:
        return {"error": "Failed to parse JSON", "raw": content}

def get_tasks() -> List[Dict[str, Any]]:
    try:
        xml_res = _call_itsm_soap("ВернутьСписокЗадачMCP", {"УидФизЛица": DEV_UUID})
        import re
        import html
        match = re.search(r'<(?:ns1|m|default):return[^>]*>(.*?)</(?:ns1|m|default):return>', xml_res, re.DOTALL)
        if not match: return []
            
        json_str = html.unescape(match.group(1).strip())
        data = json.loads(json_str)
        
        tasks = []
        if data and isinstance(data, list):
            for item in data:
                t_id = item.get("УидИнцидента") or item.get("УидНаряда")
                n_order = item.get("НомерНаряда", "")
                n_incident = item.get("НомерИнцидента", "")
                obj = item.get("ОбъектОбслуживания", "")
                desc = item.get("ОписаниеКратко", "")
                
                summary = ""
                if n_incident: summary += f"И# {n_incident} "
                summary += f"Н# {n_order}"
                if obj: summary += f" [{obj}]"
                if desc: summary += f" {desc}"
                
                tasks.append({
                    "task_id": str(t_id) if t_id else None,
                    "task_number": str(n_order),
                    "summary": summary.strip(),
                    "status": item.get("Состояние"),
                    "created_at": item.get("Дата")
                })
        return tasks
    except Exception as e:
        return [{"error": str(e)}]

def get_task_details(task_id: str) -> Dict[str, Any]:
    try:
        xml_res = _call_itsm_soap("ОписаниеИнцидента", {"УидДокумента": task_id})
        import re
        import html
        def get_tag_content(tag_name, xml):
            m = re.search(fr'<(?:ns1|m|default):{tag_name}[^>]*>(.*?)</(?:ns1|m|default):{tag_name}>', xml, re.DOTALL)
            return html.unescape(m.group(1).strip()) if m else ""

        return {
            "task_id": task_id,
            "description": get_tag_content("return", xml_res),
            "attachments_json": get_tag_content("ТаблицаВложений", xml_res),
            "confirmations_json": get_tag_content("ТаблицаПодтверждений", xml_res),
            "summary": get_tag_content("Представление", xml_res)
        }
    except Exception as e:
        return {"error": str(e)}

def _unwrap_1c_response(data: Any) -> Any:
    if not isinstance(data, dict):
        return data
    if data.get("Ошибка"): return {"error": data.get("Ошибка")}
    if "Данные" in data: return data
    
    result = data.get("Результат")
    if isinstance(result, dict):
        if result.get("Ошибка"): return {"error": result["Ошибка"]}
        if "Данные" in result: return result
    
    inner = data.get("Выполнено")
    if isinstance(inner, dict):
        return _unwrap_1c_response(inner)
    
    return data

def pull_and_lock(plugin_name_or_path: str, work_dir: str = None) -> Dict[str, Any]:
    try:
        workspace_dir = work_dir if work_dir else os.environ.get("LOCAL_WORKSPACE_DIR", os.getcwd())
        if not os.path.exists(workspace_dir):
            os.makedirs(workspace_dir, exist_ok=True)
            
        import pathlib
        is_local = os.path.exists(plugin_name_or_path) and os.path.isfile(plugin_name_or_path)
        
        locked_name = ""
        locked_uuid = None
        code = ""
        original_path = ""
        
        if is_local:
            original_path = os.path.abspath(plugin_name_or_path)
            locked_name = pathlib.Path(plugin_name_or_path).stem
        else:
            search_res = _call_1c_method("НаСервере.ПолучитьМетаданныеMCP", {
                "ТипЗапроса": "ДопФормы_Список",
                "Поиск": plugin_name_or_path
            })
            
            data_list = _unwrap_1c_response(search_res)
            if isinstance(data_list, dict) and "error" in data_list:
                 return {"status": "error", "message": f"Ошибка поиска: {data_list['error']}"}
            
            payload = data_list.get("Данные", []) if isinstance(data_list, dict) else []
            if not payload:
                return {"status": "error", "message": f"Обработка '{plugin_name_or_path}' не найдена в списке."}
            
            target = None
            for item in payload:
                if item.get("name") == plugin_name_or_path:
                    target = item
                    break
            if not target: target = payload[0]
                
            locked_name = target.get("name")
            locked_uuid = target.get("form_ref")
            version_ref = target.get("current_version_ref")
            code = target.get("code")
            
            if not version_ref:
                return {"status": "error", "message": f"У обработки '{locked_name}' не задана основная версия (current_version_ref пуст)."}

            lock_res = _call_1c_method("НаСервере.ПолучитьМетаданныеMCP", {
                "ТипЗапроса": "ДопФормы_Заблокировать",
                "form_ref": locked_uuid,
                "developer_name": "AI Agent " + (os.environ.get("USER", os.environ.get("USERNAME", "unknown")))
            })

            download_res = _call_1c_method("НаСервере.ПолучитьМетаданныеMCP", {
                "ТипЗапроса": "ДопФормы_СкачатьВерсию",
                "version_ref": version_ref
            })
            
            payload = _unwrap_1c_response(download_res)
            if isinstance(payload, dict) and "error" in payload:
                return {"status": "error", "message": f"Ошибка скачивания: {payload['error']}"}
            
            binary_data_struct = payload.get("Данные", payload) if isinstance(payload, dict) else payload
            binary_b64 = binary_data_struct.get("binary_base64", "") if isinstance(binary_data_struct, dict) else ""
            filename = binary_data_struct.get("filename", "") if isinstance(binary_data_struct, dict) else ""
            kind = target.get("kind", "epf")

            if not binary_b64:
                 return {"status": "error", "message": f"Бинарные данные не получены для версии {version_ref}"}

            ext = ".epf"
            if filename:
                ext = pathlib.Path(filename).suffix or ext
            elif kind == "erf":
                ext = ".erf"
            
            safe_name = f"{code}_{locked_name}".replace(" ", "").replace("/", "_").replace("\\", "_") if code else locked_name.replace(" ", "_")
            original_path = os.path.join(workspace_dir, f"{safe_name}{ext}")
            
            binary_data = base64.b64decode(binary_b64)
            with open(original_path, "wb") as f:
                f.write(binary_data)
        
        safe_name = locked_name.replace(" ", "_").replace("/", "_").replace("\\", "_")
        unpack_dir = os.path.join(workspace_dir, safe_name)
        if os.path.exists(unpack_dir):
            import shutil
            shutil.rmtree(unpack_dir)
        os.makedirs(unpack_dir, exist_ok=True)
        
        v8unpack_exe = _find_v8unpack()
        import subprocess
        result = subprocess.run(
            [v8unpack_exe, "-P", original_path, unpack_dir],
            capture_output=True, text=True, timeout=30
        )
        
        if result.returncode == 0:
            return {
                "status": "success",
                "message": f"Обработка '{locked_name}' успешно распакована.\nЛокальный путь: {original_path}\nПапка исходников: {unpack_dir}",
                "local_path": original_path,
                "unpack_dir": unpack_dir,
                "1c_uuid": locked_uuid,
                "1c_name": locked_name
            }
        else:
             return {
                "status": "partial_success",
                "message": f"Бинарник находится в {original_path}, но ошибка распаковки: {result.stderr}",
                "local_path": original_path
            }
    except Exception as e:
        return {"status": "error", "message": str(e)}

def build_plugin(source_dir: str, output_dir: str = None) -> Dict[str, Any]:
    import os
    import re
    import glob
    import subprocess
    
    try:
        if not os.path.isdir(source_dir):
            return {"status": "error", "message": f"Source directory not found: {source_dir}"}
            
        if output_dir and os.path.isdir(output_dir):
            workspace_dir = output_dir
        else:
            env_workspace = os.environ.get("LOCAL_WORKSPACE_DIR", "")
            if env_workspace and os.path.isdir(env_workspace):
                workspace_dir = env_workspace
            else:
                workspace_dir = os.path.dirname(os.path.normpath(source_dir))
        plugin_name = os.path.basename(os.path.normpath(source_dir))
        
        ext = ".epf"
        original_patterns = glob.glob(os.path.join(workspace_dir, f"{plugin_name}.*"))
        for p in original_patterns:
            if p.endswith(".epf"):
                ext = ".epf"
                break
            elif p.endswith(".erf"):
                ext = ".erf"
                break
                
        pattern = re.compile(f"^{re.escape(plugin_name)}_v(\\d+){re.escape(ext)}$", re.IGNORECASE)
        max_v = 0
        
        for f in os.listdir(workspace_dir):
            if os.path.isfile(os.path.join(workspace_dir, f)):
                match = pattern.match(f)
                if match:
                    v = int(match.group(1))
                    if v > max_v: max_v = v
                        
        next_v = max_v + 1
        new_filename = f"{plugin_name}_v{next_v}{ext}"
        new_filepath = os.path.join(workspace_dir, new_filename)
        
        v8unpack_exe = _find_v8unpack()
            
        result = subprocess.run(
            [v8unpack_exe, "-B", source_dir, new_filepath],
            capture_output=True, text=True, timeout=30
        )
        
        if result.returncode == 0:
            return {
                "status": "success",
                "message": f"Сборка успешна: {new_filename}",
                "build_path": new_filepath,
                "version": next_v
            }
        else:
            return {"status": "error", "message": f"Ошибка сборки v8unpack: {result.stderr}"}
    except Exception as e:
        return {"status": "error", "message": str(e)}

def execute_query(query_text: str, api_key: str = None) -> Dict[str, Any]:
    key_to_use = api_key if api_key is not None else QUERY_API_KEY
    params = {"ТипЗапроса": "ВыполнитьЗапрос", "ТекстЗапроса": query_text, "APIКлюч": key_to_use}
    try:
        data = _call_1c_method("НаСервере.ПолучитьМетаданныеMCP", params)
        if isinstance(data, dict) and "error" in data: return {"status": "error", "message": data["error"]}
        return data
    except Exception as e: return {"status": "error", "message": str(e)}

def get_metadata_summary() -> Dict[str, Any]:
    params = {"ТипЗапроса": "Сводка", "APIКлюч": QUERY_API_KEY}
    try:
        data = _call_1c_method("НаСервере.ПолучитьМетаданныеMCP", params)
        if isinstance(data, dict) and "error" in data: return {"status": "error", "message": data["error"]}
        return data
    except Exception as e: return {"status": "error", "message": str(e)}

def get_metadata_detail(object_type: str, object_name: str) -> Dict[str, Any]:
    params = {"ТипЗапроса": "Детально", "ТипОбъекта": object_type, "ИмяОбъекта": object_name, "APIКлюч": QUERY_API_KEY}
    try:
        data = _call_1c_method("НаСервере.ПолучитьМетаданныеMCP", params)
        if isinstance(data, dict) and "error" in data: return {"status": "error", "message": data["error"]}
        return data
    except Exception as e: return {"status": "error", "message": str(e)}

def validate_query(query_text: str, api_key: str = None) -> Dict[str, Any]:
    key_to_use = api_key if api_key is not None else QUERY_API_KEY
    params = {"ТипЗапроса": "ВалидацияЗапроса", "ТекстЗапроса": query_text, "APIКлюч": key_to_use}
    try:
        data = _call_1c_method("НаСервере.ПолучитьМетаданныеMCP", params)
        if isinstance(data, dict) and "error" in data: return {"status": "error", "message": data["error"]}
        return data.get("Данные", data) if isinstance(data, dict) else data
    except Exception as e: return {"status": "error", "message": str(e)}

def get_configuration_info() -> Dict[str, Any]:
    params = {"ТипЗапроса": "ИнформацияОКонфигурации", "APIКлюч": QUERY_API_KEY}
    try:
        data = _call_1c_method("НаСервере.ПолучитьМетаданныеMCP", params)
        if isinstance(data, dict) and "error" in data: return {"status": "error", "message": data["error"]}
        return data.get("Данные", data) if isinstance(data, dict) else data
    except Exception as e: return {"status": "error", "message": str(e)}

def _run_async(coro):
    try: loop = asyncio.get_event_loop()
    except RuntimeError:
        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
    return loop.run_until_complete(coro)

# Removed vectorization/db module references as they are not needed here and cause chopping.
def memory_search(query: str, entity_type: str = "all") -> List[Dict[str, Any]]: return []
def semantic_search(query: str, entity_type: str = "all") -> List[Dict[str, Any]]: return []
def memory_save(title: str, summary: str, category: str, tags: List[str], content_md: str) -> Dict[str, Any]: return {"id": 0, "file": None}
def find_symbol_definition(symbol_name: str, context_type: str = None) -> List[Dict[str, Any]]: return []
def get_method_context(method_id: int) -> Dict[str, Any]: return {}
def get_symbol_references(symbol_name: str) -> List[Dict[str, Any]]: return []
def memory_context() -> List[Dict[str, Any]]: return []

if __name__ == "__main__":
    import sys
    if len(sys.argv) > 1:
        cmd = sys.argv[1]
        if cmd == "tasks": print(json.dumps(get_tasks(), indent=2, ensure_ascii=False))
        elif cmd == "pull" and len(sys.argv) > 2: print(json.dumps(pull_and_lock(sys.argv[2]), indent=2, ensure_ascii=False))
        elif cmd == "query" and len(sys.argv) > 2: print(json.dumps(execute_query(sys.argv[2]), indent=2, ensure_ascii=False))
        elif cmd == "summary": print(json.dumps(get_metadata_summary(), indent=2, ensure_ascii=False))
        elif cmd == "detail" and len(sys.argv) > 3: print(json.dumps(get_metadata_detail(sys.argv[2], sys.argv[3]), indent=2, ensure_ascii=False))
    else: print("Usage: python plugin_transport.py [tasks | pull <name> | query <query_text> | summary | detail <type> <name>]")

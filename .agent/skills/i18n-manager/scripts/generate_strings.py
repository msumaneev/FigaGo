# -*- coding: utf-8 -*-
"""
Генерирует ВСЕ strings.xml для всех языков FigaGo из мастер-словаря.
Запуск: python generate_strings.py
"""
import os
import xml.etree.ElementTree as ET

RES_DIR = r"c:\FigaGo\app\src\main\res"

# Маппинг: код языка → имя папки
LOCALES = {
    "en":    "values-en",
    "tr":    "values-tr",
    "es":    "values-es",
    "fr":    "values-fr",
    "de":    "values-de",
    "uk":    "values-uk",
    "ar":    "values-ar",
    "ka":    "values-ka",
    "ja":    "values-ja",
    "ko":    "values-ko",
    "zh-CN": "values-zh-rCN",
    "zh-TW": "values-zh-rTW",
}

# ═══════════════ МАСТЕР-СЛОВАРЬ ПЕРЕВОДОВ ═══════════════
# Ключ → { "lang_code": "перевод", ... }
# Русский (values/) берётся из основного файла и НЕ генерируется этим скриптом.

TRANSLATIONS = {
    # ── Навигация ──
    "nav_dashboard": {"en":"Dashboard","tr":"Panel","es":"Panel","fr":"Tableau","de":"Dashboard","uk":"Панель","ar":"لوحة","ka":"პანელი","ja":"ダッシュボード","ko":"대시보드","zh-CN":"仪表盘","zh-TW":"儀表板"},
    "nav_history": {"en":"History","tr":"Geçmiş","es":"Historial","fr":"Historique","de":"Verlauf","uk":"Історія","ar":"السجل","ka":"ისტორია","ja":"履歴","ko":"기록","zh-CN":"历史","zh-TW":"歷史"},
    "nav_settings": {"en":"Settings","tr":"Ayarlar","es":"Ajustes","fr":"Paramètres","de":"Einstellungen","uk":"Налаштування","ar":"الإعدادات","ka":"პარამეტრები","ja":"設定","ko":"설정","zh-CN":"设置","zh-TW":"設定"},
    "nav_me": {"en":"Me","tr":"Ben","es":"Yo","fr":"Moi","de":"Ich","uk":"Я","ar":"أنا","ka":"მე","ja":"自分","ko":"나","zh-CN":"我","zh-TW":"我"},
    "nav_start": {"en":"Start","tr":"Başla","es":"Inicio","fr":"Départ","de":"Start","uk":"Старт","ar":"بدء","ka":"დაწყება","ja":"スタート","ko":"시작","zh-CN":"开始","zh-TW":"開始"},
    "content_back": {"en":"Back","tr":"Geri","es":"Atrás","fr":"Retour","de":"Zurück","uk":"Назад","ar":"رجوع","ka":"უკან","ja":"戻る","ko":"뒤로","zh-CN":"返回","zh-TW":"返回"},

    # ── Дашборд — Кнопки ──
    "dashboard_start_trip": {"en":"Start Trip","tr":"Sürüşü Başlat","es":"Iniciar viaje","fr":"Démarrer le trajet","de":"Fahrt starten","uk":"Почати поїздку","ar":"بدء الرحلة","ka":"მგზავრობის დაწყება","ja":"走行開始","ko":"주행 시작","zh-CN":"开始行程","zh-TW":"開始行程"},
    "dashboard_pause": {"en":"Pause","tr":"Duraklat","es":"Pausa","fr":"Pause","de":"Pause","uk":"Пауза","ar":"إيقاف مؤقت","ka":"პაუზა","ja":"一時停止","ko":"일시정지","zh-CN":"暂停","zh-TW":"暫停"},
    "dashboard_finish": {"en":"Stop/Finish","tr":"Bitir","es":"Finalizar","fr":"Terminer","de":"Beenden","uk":"Стоп/Фініш","ar":"إنهاء","ka":"დასრულება","ja":"終了","ko":"종료","zh-CN":"结束","zh-TW":"結束"},
    "dashboard_resume": {"en":"Resume","tr":"Devam Et","es":"Reanudar","fr":"Reprendre","de":"Fortsetzen","uk":"Відновити","ar":"استئناف","ka":"განახლება","ja":"再開","ko":"재개","zh-CN":"继续","zh-TW":"繼續"},
    "carousel_add": {"en":"Add","tr":"Ekle","es":"Añadir","fr":"Ajouter","de":"Hinzufügen","uk":"Додати","ar":"إضافة","ka":"დამატება","ja":"追加","ko":"추가","zh-CN":"添加","zh-TW":"新增"},
    "dashboard_manual_transport_start": {"en":"In Transport","tr":"Araçtayım","es":"En transporte","fr":"En transport","de":"Im Transport","uk":"Їду в транспорті","ar":"في وسيلة النقل","ka":"ტრანსპორტში ვარ","ja":"乗車中","ko":"대중교통 탑승 중","zh-CN":"在交通工具上","zh-TW":"在交通工具上"},
    "dashboard_manual_transport_stop": {"en":"Leave Transport","tr":"Araçtan İn","es":"Dejar transporte","fr":"Quitter transport","de":"Transport verlassen","uk":"Покинути транспорт","ar":"مغادرة النقل","ka":"ტრანსპორტის დატოვება","ja":"降車","ko":"대중교통 하차","zh-CN":"离开交通工具","zh-TW":"離開交通工具"},

    # ── Дашборд — Метрики ──
    "dashboard_mileage": {"en":"Mileage","tr":"Mesafe","es":"Distancia","fr":"Distance","de":"Strecke","uk":"Пробіг","ar":"المسافة","ka":"მანძილი","ja":"走行距離","ko":"주행거리","zh-CN":"里程","zh-TW":"里程"},
    "dashboard_time_elapsed": {"en":"Trip Time","tr":"Sürüş Süresi","es":"Tiempo","fr":"Durée","de":"Fahrzeit","uk":"Час у дорозі","ar":"وقت الرحلة","ka":"მგზავრობის დრო","ja":"走行時間","ko":"주행시간","zh-CN":"行程时间","zh-TW":"行程時間"},
    "dashboard_avg_speed": {"en":"Avg Speed","tr":"Ort. Hız","es":"Vel. media","fr":"Vit. moy.","de":"Ø Tempo","uk":"Сер. швидк.","ar":"متوسط السرعة","ka":"საშ. სიჩქარე","ja":"平均速度","ko":"평균속도","zh-CN":"平均速度","zh-TW":"平均速度"},
    "dashboard_speed": {"en":"Speed","tr":"Hız","es":"Velocidad","fr":"Vitesse","de":"Tempo","uk":"Швидкість","ar":"السرعة","ka":"სიჩქარე","ja":"速度","ko":"속도","zh-CN":"速度","zh-TW":"速度"},
    "dashboard_distance_day": {"en":"Daily Mileage","tr":"Günlük Mesafe","es":"Dist. diaria","fr":"Dist. jour","de":"Tagesstrecke","uk":"Пробіг за день","ar":"مسافة اليوم","ka":"დღის მანძილი","ja":"日間距離","ko":"일일거리","zh-CN":"日行程","zh-TW":"日行程"},
    "dashboard_record": {"en":"Record","tr":"Rekor","es":"Récord","fr":"Record","de":"Rekord","uk":"Рекорд","ar":"رقم قياسي","ka":"რეკორდი","ja":"記録","ko":"기록","zh-CN":"记录","zh-TW":"紀錄"},
    "dashboard_current_track": {"en":"Current Track","tr":"Mevcut Sürüş","es":"Recorrido actual","fr":"Trajet actuel","de":"Aktuelle Fahrt","uk":"Поточний трек","ar":"المسار الحالي","ka":"მიმდინარე","ja":"現在のトラック","ko":"현재 트랙","zh-CN":"当前轨迹","zh-TW":"目前軌跡"},
    "dashboard_forecast_remaining": {"en":"≈ %.1f km left","tr":"≈ %.1f km kaldı","es":"≈ %.1f km restantes","fr":"≈ %.1f km restants","de":"≈ %.1f km verbleibend","uk":"≈ %.1f км залишилось","ar":"≈ %.1f كم متبقي","ka":"≈ %.1f კმ დარჩა","ja":"≈ %.1f km 残り","ko":"≈ %.1f km 남음","zh-CN":"≈ 剩余 %.1f 公里","zh-TW":"≈ 剩餘 %.1f 公里"},

    # ── Единицы ──
    "unit_km": {"en":"km","tr":"km","es":"km","fr":"km","de":"km","uk":"км","ar":"كم","ka":"კმ","ja":"km","ko":"km","zh-CN":"公里","zh-TW":"公里"},
    "unit_m": {"en":"m","tr":"m","es":"m","fr":"m","de":"m","uk":"м","ar":"م","ka":"მ","ja":"m","ko":"m","zh-CN":"米","zh-TW":"米"},
    "unit_kmh": {"en":"km/h","tr":"km/s","es":"km/h","fr":"km/h","de":"km/h","uk":"км/год","ar":"كم/س","ka":"კმ/სთ","ja":"km/h","ko":"km/h","zh-CN":"公里/时","zh-TW":"公里/時"},
    "unit_mph": {"en":"mph","tr":"mph","es":"mph","fr":"mph","de":"mph","uk":"mph","ar":"mph","ka":"mph","ja":"mph","ko":"mph","zh-CN":"mph","zh-TW":"mph"},
    "unit_mi": {"en":"mi","tr":"mi","es":"mi","fr":"mi","de":"mi","uk":"mi","ar":"ميل","ka":"მილი","ja":"mi","ko":"mi","zh-CN":"英里","zh-TW":"英里"},
    "unit_ft": {"en":"ft","tr":"ft","es":"ft","fr":"ft","de":"ft","uk":"фут","ar":"قدم","ka":"ფუტი","ja":"ft","ko":"ft","zh-CN":"英尺","zh-TW":"英尺"},
    "unit_pcs": {"en":"pcs","tr":"adet","es":"uds","fr":"pcs","de":"Stk","uk":"шт","ar":"قطعة","ka":"ცალი","ja":"個","ko":"개","zh-CN":"个","zh-TW":"個"},
    "dashboard_unit_km": {"en":"km","tr":"km","es":"km","fr":"km","de":"km","uk":"км","ar":"كم","ka":"კმ","ja":"km","ko":"km","zh-CN":"公里","zh-TW":"公里"},
    "dashboard_unit_m": {"en":"m","tr":"m","es":"m","fr":"m","de":"m","uk":"м","ar":"م","ka":"მ","ja":"m","ko":"m","zh-CN":"米","zh-TW":"米"},
    "settings_suffix_sec": {"en":"sec","tr":"sn","es":"seg","fr":"sec","de":"Sek","uk":"сек","ar":"ث","ka":"წმ","ja":"秒","ko":"초","zh-CN":"秒","zh-TW":"秒"},
    "settings_suffix_min": {"en":"min","tr":"dk","es":"min","fr":"min","de":"Min","uk":"хв","ar":"د","ka":"წთ","ja":"分","ko":"분","zh-CN":"分","zh-TW":"分"},
    "settings_suffix_km": {"en":"km","tr":"km","es":"km","fr":"km","de":"km","uk":"км","ar":"كم","ka":"კმ","ja":"km","ko":"km","zh-CN":"公里","zh-TW":"公里"},
    "settings_suffix_kmh": {"en":"km/h","tr":"km/s","es":"km/h","fr":"km/h","de":"km/h","uk":"км/год","ar":"كم/س","ka":"კმ/სთ","ja":"km/h","ko":"km/h","zh-CN":"公里/时","zh-TW":"公里/時"},

    # ── История ──
    "history_title": {"en":"Trip History","tr":"Sürüş Geçmişi","es":"Historial de viajes","fr":"Historique des trajets","de":"Fahrtenverlauf","uk":"Історія поїздок","ar":"سجل الرحلات","ka":"მგზავრობების ისტორია","ja":"走行履歴","ko":"주행 기록","zh-CN":"行程历史","zh-TW":"行程歷史"},
    "history_empty": {"en":"No trips recorded","tr":"Kayıt bulunamadı","es":"Sin registros","fr":"Aucun trajet","de":"Keine Fahrten","uk":"Немає записів","ar":"لا توجد رحلات","ka":"ჩანაწერები არ არის","ja":"走行記録なし","ko":"기록 없음","zh-CN":"暂无记录","zh-TW":"暫無紀錄"},
    "history_active_session": {"en":"Active","tr":"Aktif","es":"Activa","fr":"Active","de":"Aktiv","uk":"Активна","ar":"نشطة","ka":"აქტიური","ja":"記録中","ko":"활성","zh-CN":"进行中","zh-TW":"進行中"},
    "history_clear_all": {"en":"Clear History","tr":"Geçmişi Temizle","es":"Borrar historial","fr":"Effacer l'historique","de":"Verlauf löschen","uk":"Очистити історію","ar":"مسح السجل","ka":"ისტორიის გასუფთავება","ja":"履歴を削除","ko":"기록 삭제","zh-CN":"清除历史","zh-TW":"清除歷史"},
    "history_clear_dialog_title": {"en":"Clear history?","tr":"Geçmiş temizlensin mi?","es":"¿Borrar historial?","fr":"Effacer l'historique?","de":"Verlauf löschen?","uk":"Очистити історію?","ar":"مسح السجل؟","ka":"ისტორიის გასუფთავება?","ja":"履歴を削除しますか？","ko":"기록을 삭제하시겠습니까?","zh-CN":"清除历史？","zh-TW":"清除歷史？"},
    "history_clear_dialog_text": {"en":"Delete all trips permanently?","tr":"Tüm kayıtlar kalıcı olarak silinsin mi?","es":"¿Eliminar permanentemente todas las rutas?","fr":"Supprimer définitivement tous les trajets?","de":"Alle Fahrten unwiderruflich löschen?","uk":"Видалити всі записи без можливості відновлення?","ar":"حذف جميع الرحلات نهائياً؟","ka":"ყველა ჩანაწერის სამუდამოდ წაშლა?","ja":"すべての走行記録を完全に削除しますか？","ko":"모든 기록을 영구적으로 삭제하시겠습니까?","zh-CN":"永久删除所有行程？","zh-TW":"永久刪除所有行程？"},
    "history_delete_track_title": {"en":"Delete trip?","tr":"Kayıt silinsin mi?","es":"¿Eliminar viaje?","fr":"Supprimer le trajet?","de":"Fahrt löschen?","uk":"Видалити трек?","ar":"حذف الرحلة؟","ka":"ჩანაწერის წაშლა?","ja":"走行記録を削除しますか？","ko":"기록을 삭제하시겠습니까?","zh-CN":"删除行程？","zh-TW":"刪除行程？"},
    "history_delete_track_text": {"en":"Are you sure you want to delete this trip?","tr":"Bu kaydı silmek istediğinize emin misiniz?","es":"¿Está seguro de que desea eliminar este viaje?","fr":"Êtes-vous sûr de vouloir supprimer ce trajet?","de":"Möchten Sie diese Fahrt wirklich löschen?","uk":"Ви впевнені, що хочете видалити цей запис?","ar":"هل أنت متأكد من حذف هذه الرحلة؟","ka":"დარწმუნებული ხართ, რომ გსურთ ამ ჩანაწერის წაშლა?","ja":"この走行記録を削除してもよろしいですか？","ko":"이 기록을 삭제하시겠습니까?","zh-CN":"确定删除此行程？","zh-TW":"確定刪除此行程？"},
    "dialog_yes": {"en":"Yes","tr":"Evet","es":"Sí","fr":"Oui","de":"Ja","uk":"Так","ar":"نعم","ka":"დიახ","ja":"はい","ko":"예","zh-CN":"是","zh-TW":"是"},
    "dialog_cancel": {"en":"Cancel","tr":"İptal","es":"Cancelar","fr":"Annuler","de":"Abbrechen","uk":"Скасувати","ar":"إلغاء","ka":"გაუქმება","ja":"キャンセル","ko":"취소","zh-CN":"取消","zh-TW":"取消"},
    "history_show_all_profiles": {"en":"Show all profiles","tr":"Tüm profilleri göster","es":"Mostrar todos los perfiles","fr":"Afficher tous les profils","de":"Alle Profile anzeigen","uk":"Показувати всі профілі","ar":"إظهار جميع الملفات الشخصية","ka":"ყველა პროფილის ჩვენება","ja":"すべてのプロファイルを表示","ko":"모든 프로필 표시","zh-CN":"显示所有档案","zh-TW":"顯示所有檔案"},

    # ── Детали дня ──
    "day_detail_title_fallback": {"en":"Day Details","tr":"Gün Detayları","es":"Detalles del día","fr":"Détails du jour","de":"Tagesdetails","uk":"Деталі дня","ar":"تفاصيل اليوم","ka":"დღის დეტალები","ja":"日報","ko":"일일 상세","zh-CN":"日详情","zh-TW":"日詳情"},
    "day_detail_stats_title": {"en":"Daily Stats","tr":"Günlük İstatistikler","es":"Estadísticas diarias","fr":"Statistiques du jour","de":"Tagesstatistik","uk":"Статистика дня","ar":"إحصائيات اليوم","ka":"დღის სტატისტიკა","ja":"日間統計","ko":"일일 통계","zh-CN":"日统计","zh-TW":"日統計"},
    "stat_distance": {"en":"Distance","tr":"Mesafe","es":"Distancia","fr":"Distance","de":"Strecke","uk":"Дистанція","ar":"المسافة","ka":"მანძილი","ja":"距離","ko":"거리","zh-CN":"距离","zh-TW":"距離"},
    "stat_segments": {"en":"Segments","tr":"Bölümler","es":"Segmentos","fr":"Segments","de":"Abschnitte","uk":"Відрізків","ar":"المقاطع","ka":"სეგმენტები","ja":"セグメント","ko":"구간","zh-CN":"段数","zh-TW":"段數"},
    "stat_moving_time": {"en":"Moving Time","tr":"Hareket Süresi","es":"En movimiento","fr":"En mouvement","de":"Fahrzeit","uk":"У русі","ar":"وقت التحرك","ka":"მოძრაობის დრო","ja":"走行時間","ko":"이동시간","zh-CN":"移动时间","zh-TW":"移動時間"},
    "map_snippet_led": {"en":"LEDs left: %1$d","tr":"Kalan gösterge: %1$d","es":"Indicadores restantes: %1$d","fr":"Indicateurs restants: %1$d","de":"Verbleibende LEDs: %1$d","uk":"Залишилось індикаторів: %1$d","ar":"المؤشرات المتبقية: %1$d","ka":"დარჩენილი ინდიკატორები: %1$d","ja":"残りLED: %1$d","ko":"남은 표시등: %1$d","zh-CN":"剩余指示灯: %1$d","zh-TW":"剩餘指示燈: %1$d"},
    "history_segment_name": {"en":"Segment %d","tr":"Bölüm %d","es":"Segmento %d","fr":"Segment %d","de":"Abschnitt %d","uk":"Сегмент %d","ar":"المقطع %d","ka":"სეგმენტი %d","ja":"セグメント %d","ko":"구간 %d","zh-CN":"分段 %d","zh-TW":"分段 %d"},

    # ── Профиль ──
    "profile_title": {"en":"Wheelchair Passport","tr":"Tekerlekli Sandalye Kimliği","es":"Ficha de la silla","fr":"Fiche du fauteuil","de":"Rollstuhl-Pass","uk":"Паспорт візка","ar":"بطاقة الكرسي","ka":"ეტლის პასპორტი","ja":"車いすパスポート","ko":"휠체어 프로필","zh-CN":"轮椅档案","zh-TW":"輪椅檔案"},
    "profile_name_label": {"en":"Wheelchair name","tr":"Sandalye adı","es":"Nombre de la silla","fr":"Nom du fauteuil","de":"Rollstuhlname","uk":"Назва візка","ar":"اسم الكرسي","ka":"ეტლის სახელი","ja":"車いすの名前","ko":"휠체어 이름","zh-CN":"轮椅名称","zh-TW":"輪椅名稱"},
    "new_wheelchair_default_name": {"en":"New","tr":"Yeni","es":"Nuevo","fr":"Nouveau","de":"Neu","uk":"Новий","ar":"جديد","ka":"ახალი","ja":"新規","ko":"새로운","zh-CN":"新","zh-TW":"新"},
    "profile_icon_label": {"en":"Icon","tr":"Simge","es":"Icono","fr":"Icône","de":"Symbol","uk":"Іконка","ar":"الأيقونة","ka":"ხატი","ja":"アイコン","ko":"아이콘","zh-CN":"图标","zh-TW":"圖示"},
    "profile_type_electric": {"en":"Battery Tracking","tr":"Batarya İzleme","es":"Rastreo de batería","fr":"Suivi de batterie","de":"Batterie-Tracking","uk":"Трекінг батареї","ar":"تتبع البطارية","ka":"ბატარეის თვალყურის დევნება","ja":"バッテリー追跡","ko":"배터리 추적","zh-CN":"电池追踪","zh-TW":"電池追蹤"},
    "profile_type_manual": {"en":"Route Only","tr":"Sadece Rota","es":"Solo ruta","fr":"Itinéraire seul","de":"Nur Route","uk":"Тільки маршрут","ar":"طريق فقط","ka":"მხოლოდ მარშრუტი","ja":"ルートのみ","ko":"경로만","zh-CN":"仅路线","zh-TW":"僅路線"},
    "profile_section_driving": {"en":"Driving Settings","tr":"Sürüş Ayarları","es":"Ajustes de conducción","fr":"Paramètres de conduite","de":"Fahreinstellungen","uk":"Ходові характеристики","ar":"إعدادات القيادة","ka":"სავალი პარამეტრები","ja":"走行設定","ko":"주행 설정","zh-CN":"行驶设置","zh-TW":"行駛設定"},
    "profile_max_speed": {"en":"Max speed","tr":"Maks. hız","es":"Vel. máx.","fr":"Vit. max.","de":"Max. Tempo","uk":"Макс. швидк.","ar":"السرعة القصوى","ka":"მაქს. სიჩქარე","ja":"最高速度","ko":"최대속도","zh-CN":"最高速度","zh-TW":"最高速度"},
    "profile_max_speed_desc": {"en":"Threshold for Transport mode","tr":"Ulaşım moduna geçiş eşiği","es":"Umbral para activar modo Transporte","fr":"Seuil du mode Transport","de":"Schwelle für Transportmodus","uk":"Поріг для режиму Транспорт","ar":"عتبة وضع النقل","ka":"ტრანსპორტის რეჟიმის ზღვარი","ja":"乗車モード切替のしきい値","ko":"교통수단 모드 전환 기준","zh-CN":"交通模式切换阈值","zh-TW":"交通模式切換閾值"},
    "profile_section_battery": {"en":"Battery","tr":"Batarya","es":"Batería","fr":"Batterie","de":"Akku","uk":"Батарея","ar":"البطارية","ka":"ბატარეა","ja":"バッテリー","ko":"배터리","zh-CN":"电池","zh-TW":"電池"},
    "profile_range_title": {"en":"Range per charge","tr":"Tek şarjla menzil","es":"Autonomía por carga","fr":"Autonomie par charge","de":"Reichweite pro Ladung","uk":"Пробіг на одному заряді","ar":"المدى لكل شحنة","ka":"მანძილი ერთ დამუხტვაზე","ja":"1回の充電での航続距離","ko":"1회 충전 주행거리","zh-CN":"单次充电里程","zh-TW":"單次充電里程"},
    "profile_range_desc_km": {"en":"Expected max in kilometers.","tr":"Tahmini maksimum kilometre.","es":"Máximo esperado en kilómetros.","fr":"Maximum prévu en kilomètres.","de":"Erwartete Maximalreichweite in Km.","uk":"Очікуваний максимум у кілометрах.","ar":"الحد الأقصى المتوقع بالكيلومترات.","ka":"მოსალოდნელი მაქსიმუმი კილომეტრებში.","ja":"予想最大距離（km）","ko":"예상 최대 거리 (km)","zh-CN":"预期最大续航（公里）","zh-TW":"預期最大續航（公里）"},
    "profile_range_desc_mi": {"en":"Expected max in miles.","tr":"Tahmini maksimum mil.","es":"Máximo esperado en millas.","fr":"Maximum prévu en miles.","de":"Erwartete Maximalreichweite in Meilen.","uk":"Очікуваний максимум у милях.","ar":"الحد الأقصى المتوقع بالأميال.","ka":"მოსალოდნელი მაქსიმუმი მილებში.","ja":"予想最大距離（マイル）","ko":"예상 최대 거리 (mi)","zh-CN":"预期最大续航（英里）","zh-TW":"預期最大續航（英里）"},
    "profile_led_title": {"en":"Dashboard LEDs","tr":"Gösterge paneli LEDleri","es":"LEDs del panel","fr":"LEDs du tableau","de":"LEDs am Display","uk":"Індикатори на пульті","ar":"مصابيح اللوحة","ka":"პულტის ინდიკატორები","ja":"ダッシュボードLED","ko":"패널 표시등","zh-CN":"面板指示灯","zh-TW":"面板指示燈"},
    "profile_led_desc": {"en":"Battery charge indicators.","tr":"Batarya şarj göstergeleri.","es":"Indicadores de carga de batería.","fr":"Indicateurs de charge batterie.","de":"Akku-Ladezustandsanzeige.","uk":"Індикатори заряду батареї.","ar":"مؤشرات شحن البطارية.","ka":"ბატარეის დამუხტვის ინდიკატორები.","ja":"バッテリー残量インジケーター","ko":"배터리 충전 표시등","zh-CN":"电池电量指示灯","zh-TW":"電池電量指示燈"},
    "profile_save_and_exit": {"en":"Save & Exit","tr":"Kaydet ve Çık","es":"Guardar y salir","fr":"Enregistrer et quitter","de":"Speichern & Zurück","uk":"Зберегти і вийти","ar":"حفظ والخروج","ka":"შენახვა და გასვლა","ja":"保存して戻る","ko":"저장 후 닫기","zh-CN":"保存并退出","zh-TW":"儲存並退出"},
    "profile_icon_default": {"en":"Standard Electric","tr":"Standart Elektrikli","es":"Estándar eléctrica","fr":"Électrique standard","de":"Standard Elektro","uk":"Стандартний електр.","ar":"كهربائي قياسي","ka":"სტანდარტული ელექტრო","ja":"標準電動","ko":"표준 전동","zh-CN":"标准电动","zh-TW":"標準電動"},
    "profile_icon_custom_1": {"en":"Custom 1","tr":"Özel 1","es":"Personalizado 1","fr":"Personnalisé 1","de":"Benutzerdefiniert 1","uk":"Кастомна 1","ar":"مخصص 1","ka":"მორგებული 1","ja":"カスタム1","ko":"커스텀 1","zh-CN":"自定义 1","zh-TW":"自訂 1"},
    "profile_icon_exotic": {"en":"Exotic","tr":"Exotic","es":"Exotic","fr":"Exotic","de":"Exotic","uk":"Exotic","ar":"Exotic","ka":"Exotic","ja":"Exotic","ko":"Exotic","zh-CN":"Exotic","zh-TW":"Exotic"},

    # ── Настройки ──
    "settings_figago_title": {"en":"FigaGo Settings","tr":"FigaGo Ayarları","es":"Ajustes de FigaGo","fr":"Paramètres FigaGo","de":"FigaGo Einstellungen","uk":"Налаштування FigaGo","ar":"إعدادات FigaGo","ka":"FigaGo-ს პარამეტრები","ja":"FigaGo 設定","ko":"FigaGo 설정","zh-CN":"FigaGo 设置","zh-TW":"FigaGo 設定"},
    "settings_section_passport": {"en":"Wheelchair Passport","tr":"Tekerlekli Sandalye Kimliği","es":"Ficha de la silla","fr":"Fiche du fauteuil","de":"Rollstuhl-Pass","uk":"Паспорт візка","ar":"بطاقة الكرسي","ka":"ეტლის პასპორტი","ja":"車いすパスポート","ko":"휠체어 프로필","zh-CN":"轮椅档案","zh-TW":"輪椅檔案"},
    "settings_section_battery": {"en":"Battery","tr":"Batarya","es":"Batería","fr":"Batterie","de":"Akku","uk":"Батарея","ar":"البطارية","ka":"ბატარეა","ja":"バッテリー","ko":"배터리","zh-CN":"电池","zh-TW":"電池"},
    "settings_section_display": {"en":"Metrics Display","tr":"Metrik Gösterimi","es":"Visualización de métricas","fr":"Affichage des métriques","de":"Metriken-Anzeige","uk":"Відображення метрик","ar":"عرض المقاييس","ka":"მეტრიკების ჩვენება","ja":"メトリクス表示","ko":"지표 표시","zh-CN":"指标显示","zh-TW":"指標顯示"},
    "settings_unit_title": {"en":"Measurement Units","tr":"Ölçü Birimi","es":"Unidades de medida","fr":"Unités de mesure","de":"Maßeinheiten","uk":"Одиниці виміру","ar":"وحدات القياس","ka":"საზომი ერთეულები","ja":"単位","ko":"측정 단위","zh-CN":"计量单位","zh-TW":"計量單位"},
    "settings_unit_desc": {"en":"For speed and distance display","tr":"Hız ve mesafe gösterimi için","es":"Para mostrar velocidad y distancia","fr":"Pour l'affichage vitesse et distance","de":"Für Geschwindigkeit und Strecke","uk":"Для відображення швидкості і дистанції","ar":"لعرض السرعة والمسافة","ka":"სიჩქარის და მანძილის ჩვენებისთვის","ja":"速度と距離の表示用","ko":"속도 및 거리 표시용","zh-CN":"用于速度和距离显示","zh-TW":"用於速度和距離顯示"},
    "settings_unit_km": {"en":"Kilometers","tr":"Kilometre","es":"Kilómetros","fr":"Kilomètres","de":"Kilometer","uk":"Кілометри","ar":"كيلومترات","ka":"კილომეტრები","ja":"キロメートル","ko":"킬로미터","zh-CN":"公里","zh-TW":"公里"},
    "settings_unit_mi": {"en":"Miles","tr":"Mil","es":"Millas","fr":"Miles","de":"Meilen","uk":"Милі","ar":"أميال","ka":"მილები","ja":"マイル","ko":"마일","zh-CN":"英里","zh-TW":"英里"},
    "settings_gps_bg_section": {"en":"GPS Tracker & Background","tr":"GPS İzleyici ve Arka Plan","es":"GPS y trabajo en segundo plano","fr":"GPS et arrière-plan","de":"GPS-Tracker & Hintergrund","uk":"GPS-трекер і фонова робота","ar":"تتبع GPS والخلفية","ka":"GPS ტრეკერი და ფონი","ja":"GPSトラッカーとバックグラウンド","ko":"GPS 추적기 & 백그라운드","zh-CN":"GPS 追踪和后台","zh-TW":"GPS 追蹤和背景"},
    "settings_gps_interval_title": {"en":"Recording interval (sec)","tr":"Kayıt Aralığı (sn)","es":"Intervalo de registro (seg)","fr":"Intervalle d'enregistrement (sec)","de":"Aufnahmeintervall (Sek)","uk":"Інтервал запису (сек)","ar":"فترة التسجيل (ث)","ka":"ჩაწერის ინტერვალი (წმ)","ja":"記録間隔（秒）","ko":"기록 간격 (초)","zh-CN":"记录间隔（秒）","zh-TW":"記錄間隔（秒）"},
    "settings_gps_interval_desc": {"en":"Affects track accuracy and battery","tr":"Doğruluğu ve pili etkiler","es":"Afecta precisión del recorrido y batería","fr":"Affecte la précision du trajet et la batterie","de":"Beeinflusst Genauigkeit und Akkulaufzeit","uk":"Впливає на точність треку і батарею","ar":"يؤثر على دقة المسار والبطارية","ka":"გავლენას ახდენს ტრეკის სიზუსტეზე და ბატარეაზე","ja":"トラック精度とバッテリーに影響","ko":"트랙 정확도와 배터리에 영향","zh-CN":"影响轨迹精度和电池","zh-TW":"影響軌跡精度和電池"},
    "settings_auto_close_title": {"en":"Auto-close day","tr":"Günü Otomatik Kapat","es":"Cierre automático del día","fr":"Fermeture auto du jour","de":"Tag autom. beenden","uk":"Автозакриття дня","ar":"إغلاق اليوم تلقائياً","ka":"დღის ავტომატური დახურვა","ja":"自動終了","ko":"자동 종료","zh-CN":"自动结束","zh-TW":"自動結束"},
    "settings_auto_close_desc": {"en":"Ends trip after prolonged idle","tr":"Uzun süre boşta kalınca seferi bitirir","es":"Finaliza el viaje tras inactividad prolongada","fr":"Termine le trajet après inactivité prolongée","de":"Beendet die Fahrt bei längerer Inaktivität","uk":"Завершує поїздку при тривалому простої","ar":"ينهي الرحلة بعد فترة خمول طويلة","ka":"ასრულებს მგზავრობას ხანგრძლივი უმოქმედობის შემდეგ","ja":"長時間のアイドル後にトリップを終了","ko":"장시간 미사용 시 주행 종료","zh-CN":"长时间闲置后结束行程","zh-TW":"長時間閒置後結束行程"},
    "settings_tts_section": {"en":"Voice Announcements (TTS)","tr":"Sesli Duyurular (TTS)","es":"Avisos de voz (TTS)","fr":"Annonces vocales (TTS)","de":"Sprachansagen (TTS)","uk":"Голосові оповіщення (TTS)","ar":"الإعلانات الصوتية (TTS)","ka":"ხმოვანი შეტყობინებები (TTS)","ja":"音声案内（TTS）","ko":"음성 안내 (TTS)","zh-CN":"语音播报 (TTS)","zh-TW":"語音播報 (TTS)"},
    "settings_tts_mode_label": {"en":"Mode","tr":"Mod","es":"Modo","fr":"Mode","de":"Modus","uk":"Режим","ar":"الوضع","ka":"რეჟიმი","ja":"モード","ko":"모드","zh-CN":"模式","zh-TW":"模式"},
    "settings_tts_off_short": {"en":"Off","tr":"Kapalı","es":"Apag.","fr":"Arrêt","de":"Aus","uk":"Вимк","ar":"إيقاف","ka":"გამორთ.","ja":"オフ","ko":"끔","zh-CN":"关","zh-TW":"關"},
    "settings_tts_by_km": {"en":"By km","tr":"km'ye göre","es":"Por km","fr":"Par km","de":"Pro km","uk":"По км","ar":"لكل كم","ka":"კმ-ით","ja":"km毎","ko":"km마다","zh-CN":"按公里","zh-TW":"按公里"},
    "settings_tts_by_min": {"en":"By min","tr":"dk'ya göre","es":"Por min","fr":"Par min","de":"Pro Min","uk":"По хв","ar":"لكل دقيقة","ka":"წუთ-ით","ja":"分毎","ko":"분마다","zh-CN":"按分钟","zh-TW":"按分鐘"},
    "settings_tts_desc": {"en":"\"What's my distance\" works anytime","tr":"\"Menzil ne kadar\" her zaman çalışır","es":"El comando \"¿Cuánta distancia?\" funciona siempre","fr":"La commande \"Distance ?\" fonctionne toujours","de":"Der Befehl \"Wie weit?\" funktioniert immer","uk":"Команда \"Який пробіг\" працює завжди","ar":"أمر \"كم المسافة\" يعمل دائماً","ka":"ბრძანება \"რა მანძილია\" ყოველთვის მუშაობს","ja":"「距離は？」コマンドは常に利用可能","ko":"\"주행거리는?\" 명령은 항상 작동","zh-CN":"\"当前里程\"命令始终可用","zh-TW":"「當前里程」命令始終可用"},
    "settings_tts_interval_km_title": {"en":"Interval (km)","tr":"Aralık (km)","es":"Intervalo (km)","fr":"Intervalle (km)","de":"Intervall (km)","uk":"Інтервал (км)","ar":"الفترة (كم)","ka":"ინტერვალი (კმ)","ja":"間隔（km）","ko":"간격 (km)","zh-CN":"间隔（公里）","zh-TW":"間隔（公里）"},
    "settings_tts_interval_min_title": {"en":"Interval (min)","tr":"Aralık (dk)","es":"Intervalo (min)","fr":"Intervalle (min)","de":"Intervall (Min)","uk":"Інтервал (хв)","ar":"الفترة (د)","ka":"ინტერვალი (წთ)","ja":"間隔（分）","ko":"간격 (분)","zh-CN":"间隔（分钟）","zh-TW":"間隔（分鐘）"},
    "settings_voice_assistant_section": {"en":"Voice Assistant","tr":"Sesli Asistan","es":"Asistente de voz","fr":"Assistant vocal","de":"Sprachassistent","uk":"Голосовий помічник","ar":"المساعد الصوتي","ka":"ხმოვანი ასისტენტი","ja":"音声アシスタント","ko":"음성 도우미","zh-CN":"语音助手","zh-TW":"語音助手"},
    "settings_voice_setup": {"en":"Set up voice commands","tr":"Sesli Komutları Kur","es":"Configurar comandos de voz","fr":"Configurer les commandes vocales","de":"Sprachbefehle einrichten","uk":"Налаштувати голосові команди","ar":"إعداد الأوامر الصوتية","ka":"ხმოვანი ბრძანებების დაყენება","ja":"音声コマンドを設定","ko":"음성 명령 설정","zh-CN":"设置语音命令","zh-TW":"設定語音指令"},
    "settings_voice_home_prompt": {"en":"Opening Google Home...","tr":"Google Home açılıyor...","es":"Abriendo Google Home...","fr":"Ouverture de Google Home...","de":"Google Home wird geöffnet...","uk":"Відкриваємо Google Home...","ar":"فتح Google Home...","ka":"Google Home იხსნება...","ja":"Google Homeを開いています...","ko":"Google Home 열기...","zh-CN":"正在打开 Google Home...","zh-TW":"正在開啟 Google Home..."},
    "settings_language_title": {"en":"App Language","tr":"Uygulama Dili","es":"Idioma de la app","fr":"Langue de l'app","de":"App-Sprache","uk":"Мова інтерфейсу","ar":"لغة التطبيق","ka":"აპის ენა","ja":"アプリの言語","ko":"앱 언어","zh-CN":"应用语言","zh-TW":"應用語言"},
    "settings_language_desc": {"en":"Default language in the app","tr":"Uygulamanın varsayılan dili","es":"Idioma predeterminado en la aplicación","fr":"Langue par défaut de l'application","de":"Standardsprache in der App","uk":"Мова за замовчуванням у додатку","ar":"اللغة الافتراضية في التطبيق","ka":"აპის ნაგულისხმევი ენა","ja":"アプリのデフォルト言語","ko":"앱 기본 언어","zh-CN":"应用中的默认语言","zh-TW":"應用中的預設語言"},
    "settings_time_hour": {"en":"hr","tr":"Saat","es":"Hora","fr":"Heure","de":"Std","uk":"Год","ar":"ساعة","ka":"საათი","ja":"時間","ko":"시간","zh-CN":"小时","zh-TW":"小時"},
    "settings_time_min": {"en":"min","tr":"Dk","es":"Min","fr":"Min","de":"Min","uk":"Хв","ar":"دقيقة","ka":"წუთი","ja":"分","ko":"분","zh-CN":"分钟","zh-TW":"分鐘"},
    "settings_time_off": {"en":"Off","tr":"Kapalı","es":"Apag.","fr":"Arrêt","de":"Aus","uk":"Вимк","ar":"إيقاف","ka":"გამორთ.","ja":"オフ","ko":"끔","zh-CN":"关","zh-TW":"關"},

    # ── Диагностика ──
    "diagnostics_title": {"en":"Tracker Diagnostics","tr":"İzleyici Tanılama","es":"Diagnóstico del rastreador","fr":"Diagnostic du traqueur","de":"Tracker-Diagnose","uk":"Діагностика трекера","ar":"تشخيص المتعقب","ka":"ტრეკერის დიაგნოსტიკა","ja":"トラッカー診断","ko":"추적기 진단","zh-CN":"追踪器诊断","zh-TW":"追蹤器診斷"},
    "diagnostics_description": {"en":"Background tracker requires additional permissions.","tr":"Arka plan izleyici ek izinler gerektirir.","es":"El rastreador de fondo requiere permisos adicionales.","fr":"Le traqueur en arrière-plan nécessite des autorisations supplémentaires.","de":"Der Hintergrund-Tracker benötigt zusätzliche Berechtigungen.","uk":"Для роботи фонового трекера потрібні додаткові дозволи.","ar":"يتطلب المتعقب في الخلفية أذونات إضافية.","ka":"ფონურ ტრეკერს სჭირდება დამატებითი ნებართვები.","ja":"バックグラウンドトラッカーには追加の権限が必要です。","ko":"백그라운드 추적기에는 추가 권한이 필요합니다.","zh-CN":"后台追踪器需要额外权限。","zh-TW":"背景追蹤器需要額外權限。"},
    "diagnostics_gps_enabled": {"en":"GPS Enabled","tr":"GPS Açık","es":"GPS Activado","fr":"GPS Activé","de":"GPS Aktiviert","uk":"GPS Увімкнено","ar":"GPS مُفعّل","ka":"GPS ჩართულია","ja":"GPS 有効","ko":"GPS 활성","zh-CN":"GPS 已启用","zh-TW":"GPS 已啟用"},
    "diagnostics_bg_location": {"en":"Background Location","tr":"Arka Plan Konum","es":"Ubicación en segundo plano","fr":"Localisation arrière-plan","de":"Standort im Hintergrund","uk":"Фонова геолокація","ar":"الموقع في الخلفية","ka":"ფონური გეოლოკაცია","ja":"バックグラウンド位置情報","ko":"백그라운드 위치","zh-CN":"后台定位","zh-TW":"背景定位"},
    "diagnostics_battery_exempt": {"en":"Battery Optimization Exempt","tr":"Pil Tasarrufundan Muaf","es":"Excluido de ahorro de batería","fr":"Exempt de l'optimisation batterie","de":"Von Akkuoptimierung ausgenommen","uk":"Виключення з енергозбереження","ar":"معفى من تحسين البطارية","ka":"ენერგიის დაზოგვის გამონაკლისი","ja":"バッテリー最適化の除外","ko":"배터리 최적화 제외","zh-CN":"电池优化豁免","zh-TW":"電池優化豁免"},
    "diagnostics_activity_recognition": {"en":"Activity Recognition","tr":"Hareket Algılama","es":"Reconocimiento de actividad","fr":"Reconnaissance d'activité","de":"Aktivitätserkennung","uk":"Розпізнавання активності","ar":"التعرف على النشاط","ka":"აქტივობის ამოცნობა","ja":"アクティビティ認識","ko":"활동 인식","zh-CN":"活动识别","zh-TW":"活動識別"},
    "diagnostics_action_required": {"en":"Action Required","tr":"İşlem Gerekli","es":"Acción requerida","fr":"Action requise","de":"Aktion erforderlich","uk":"Потрібна дія","ar":"إجراء مطلوب","ka":"საჭიროა მოქმედება","ja":"操作が必要","ko":"조치 필요","zh-CN":"需要操作","zh-TW":"需要操作"},
    "diagnostics_fix": {"en":"Fix","tr":"Düzelt","es":"Reparar","fr":"Corriger","de":"Beheben","uk":"Виправити","ar":"إصلاح","ka":"გამოსწორება","ja":"修正","ko":"수정","zh-CN":"修复","zh-TW":"修復"},
    "diagnostics_all_clear": {"en":"All clear, go back","tr":"Tamam, geri dön","es":"Todo correcto, volver","fr":"Tout est bon, retour","de":"Alles OK, zurück","uk":"Все гаразд, повернутися","ar":"كل شيء جيد، العودة","ka":"ყველაფერი კარგადაა, უკან","ja":"すべてOK、戻る","ko":"모두 정상, 돌아가기","zh-CN":"一切正常，返回","zh-TW":"一切正常，返回"},

    # ── Диалоги и разрешения ──
    "dialog_switch_profile_title": {"en":"Switch wheelchair?","tr":"Sandalye değiştirilsin mi?","es":"¿Cambiar de silla?","fr":"Changer de fauteuil?","de":"Rollstuhl wechseln?","uk":"Змінити візок?","ar":"تغيير الكرسي؟","ka":"ეტლის შეცვლა?","ja":"車いすを変更しますか？","ko":"휠체어를 변경하시겠습니까?","zh-CN":"切换轮椅？","zh-TW":"切換輪椅？"},
    "dialog_switch_profile_text": {"en":"Current trip will end and recording will start for the new profile.","tr":"Mevcut sürüş sonlandırılacak ve yeni profil için kayıt başlayacaktır.","es":"El viaje actual terminará y se iniciará la grabación para el nuevo perfil.","fr":"Le trajet actuel sera terminé et l'enregistrement démarrera pour le nouveau profil.","de":"Die aktuelle Fahrt wird beendet und die Aufzeichnung für das neue Profil beginnt.","uk":"Поточний трек буде завершено і розпочнеться запис для нового профілю.","ar":"سيتم إنهاء الرحلة الحالية وبدء التسجيل للملف الشخصي الجديد.","ka":"მიმდინარე მარშრუტი დასრულდება და ახალი პროფილის ჩაწერა დაიწყება.","ja":"現在のトリップが終了し、新しいプロファイルの記録が開始されます。","ko":"현재 주행이 종료되고 새 프로필로 기록이 시작됩니다.","zh-CN":"当前行程将结束并开始为新档案记录。","zh-TW":"目前行程將結束並開始為新檔案記錄。"},
    "dialog_switch_confirm": {"en":"Yes, switch","tr":"Evet, değiştir","es":"Sí, cambiar","fr":"Oui, changer","de":"Ja, wechseln","uk":"Так, змінити","ar":"نعم، غيّر","ka":"დიახ, შეცვალე","ja":"はい、変更","ko":"네, 변경","zh-CN":"是，切换","zh-TW":"是，切換"},
    "dialog_assistant_error": {"en":"Could not open Google Assistant settings","tr":"Google Asistan ayarları açılamadı","es":"No se pudieron abrir los ajustes del Asistente de Google","fr":"Impossible d'ouvrir les paramètres de l'Assistant Google","de":"Google Assistant-Einstellungen konnten nicht geöffnet werden","uk":"Не вдалося відкрити налаштування Google Асистента","ar":"تعذر فتح إعدادات مساعد Google","ka":"Google ასისტენტის პარამეტრების გახსნა ვერ მოხერხდა","ja":"Googleアシスタントの設定を開けませんでした","ko":"Google 어시스턴트 설정을 열 수 없습니다","zh-CN":"无法打开 Google 助理设置","zh-TW":"無法開啟 Google 助理設定"},
    "permission_dialog_title": {"en":"Permissions Needed","tr":"İzinler Gerekli","es":"Se necesitan permisos","fr":"Autorisations requises","de":"Berechtigungen erforderlich","uk":"Потрібні дозволи","ar":"الأذونات مطلوبة","ka":"საჭიროა ნებართვები","ja":"権限が必要","ko":"권한 필요","zh-CN":"需要权限","zh-TW":"需要權限"},
    "permission_go_settings": {"en":"Open Settings","tr":"Ayarlara Git","es":"Ir a Ajustes","fr":"Ouvrir les paramètres","de":"Zu Einstellungen","uk":"До налаштувань","ar":"فتح الإعدادات","ka":"პარამეტრებში","ja":"設定を開く","ko":"설정으로","zh-CN":"打开设置","zh-TW":"開啟設定"},
    "permission_cancel": {"en":"No","tr":"Hayır","es":"No","fr":"Non","de":"Nein","uk":"Ні","ar":"لا","ka":"არა","ja":"いいえ","ko":"아니요","zh-CN":"否","zh-TW":"否"},
    "permission_required_title": {"en":"Permissions Required","tr":"İzinler Gerekli","es":"Permisos necesarios","fr":"Autorisations nécessaires","de":"Berechtigungen nötig","uk":"Необхідні дозволи","ar":"الأذونات ضرورية","ka":"საჭირო ნებართვები","ja":"権限が必要です","ko":"권한이 필요합니다","zh-CN":"需要授予权限","zh-TW":"需要授予權限"},
    "permission_required_text": {"en":"FigaGo records GPS tracks in the background and displays a notification for stable operation. Without these permissions the app cannot track your route correctly.","tr":"FigaGo arka planda GPS izi kaydeder ve kararlı çalışma için bildirim görüntüler. Bu izinler olmadan uygulama rotanızı doğru izleyemez.","es":"FigaGo graba trazas GPS en segundo plano y muestra una notificación para un funcionamiento estable. Sin estos permisos la app no podrá rastrear tu ruta correctamente.","fr":"FigaGo enregistre les traces GPS en arrière-plan et affiche une notification pour un fonctionnement stable. Sans ces autorisations, l'app ne pourra pas suivre votre trajet correctement.","de":"FigaGo zeichnet GPS-Tracks im Hintergrund auf und zeigt eine Benachrichtigung für stabilen Betrieb an. Ohne diese Berechtigungen kann die App Ihre Route nicht korrekt verfolgen.","uk":"FigaGo записує GPS-трек у фоні та показує сповіщення для стабільної роботи. Без цих дозволів додаток не зможе коректно відстежувати ваш маршрут.","ar":"يسجل FigaGo مسارات GPS في الخلفية ويعرض إشعاراً للعمل المستقر. بدون هذه الأذونات لن يتمكن التطبيق من تتبع مسارك بشكل صحيح.","ka":"FigaGo ფონურად იწერს GPS ტრეკს და აჩვენებს შეტყობინებას სტაბილური მუშაობისთვის. ამ ნებართვების გარეშე აპი ვერ შეძლებს თქვენი მარშრუტის სწორად თვალყურის დევნებას.","ja":"FigaGoはバックグラウンドでGPSトラックを記録し、安定動作のために通知を表示します。これらの権限がないと、ルートを正しく追跡できません。","ko":"FigaGo는 백그라운드에서 GPS 트랙을 기록하고 안정적인 작동을 위해 알림을 표시합니다. 이 권한이 없으면 앱이 경로를 올바르게 추적할 수 없습니다.","zh-CN":"FigaGo 在后台记录 GPS 轨迹并显示通知以保证稳定运行。没有这些权限，应用程序将无法正确追踪您的路线。","zh-TW":"FigaGo 在背景記錄 GPS 軌跡並顯示通知以確保穩定運行。沒有這些權限，應用程式將無法正確追蹤您的路線。"},
    "permission_retry": {"en":"Retry","tr":"Tekrar Dene","es":"Reintentar","fr":"Réessayer","de":"Wiederholen","uk":"Повторити запит","ar":"إعادة المحاولة","ka":"ხელახლა ცდა","ja":"再試行","ko":"다시 시도","zh-CN":"重试","zh-TW":"重試"},
    "permission_close": {"en":"Close","tr":"Kapat","es":"Cerrar","fr":"Fermer","de":"Schließen","uk":"Закрити","ar":"إغلاق","ka":"დახურვა","ja":"閉じる","ko":"닫기","zh-CN":"关闭","zh-TW":"關閉"},

    # ── Уведомления ──
    "notification_channel_name": {"en":"Track Recording","tr":"Sürüş Kaydı","es":"Grabación de ruta","fr":"Enregistrement de trajet","de":"Streckenaufzeichnung","uk":"Запис треку","ar":"تسجيل المسار","ka":"ტრეკის ჩაწერა","ja":"トラック記録","ko":"트랙 기록","zh-CN":"轨迹记录","zh-TW":"軌跡記錄"},
    "notification_tracking_title": {"en":"FigaGo — recording","tr":"FigaGo — kayıt","es":"FigaGo — grabando","fr":"FigaGo — enregistrement","de":"FigaGo — Aufzeichnung","uk":"FigaGo — запис треку","ar":"FigaGo — تسجيل","ka":"FigaGo — ჩაწერა","ja":"FigaGo — 記録中","ko":"FigaGo — 기록 중","zh-CN":"FigaGo — 记录中","zh-TW":"FigaGo — 記錄中"},
    "notification_tracking_text": {"en":"Distance: %1$.1f km","tr":"Mesafe: %1$.1f km","es":"Distancia: %1$.1f km","fr":"Distance: %1$.1f km","de":"Strecke: %1$.1f km","uk":"Дистанція: %1$.1f км","ar":"المسافة: %1$.1f كم","ka":"მანძილი: %1$.1f კმ","ja":"距離: %1$.1f km","ko":"거리: %1$.1f km","zh-CN":"距离: %1$.1f 公里","zh-TW":"距離: %1$.1f 公里"},
    "notification_paused": {"en":"FigaGo — paused","tr":"FigaGo — duraklatıldı","es":"FigaGo — en pausa","fr":"FigaGo — en pause","de":"FigaGo — pausiert","uk":"FigaGo — пауза","ar":"FigaGo — مؤقتاً","ka":"FigaGo — პაუზა","ja":"FigaGo — 一時停止","ko":"FigaGo — 일시정지","zh-CN":"FigaGo — 已暂停","zh-TW":"FigaGo — 已暫停"},
    "notif_title": {"en":"GPS track recording active","tr":"GPS izleme aktif","es":"Rastreo GPS activo","fr":"Suivi GPS actif","de":"GPS-Aufzeichnung aktiv","uk":"Запис GPS-треку активний","ar":"تتبع GPS نشط","ka":"GPS ტრეკის ჩაწერა აქტიურია","ja":"GPS記録が有効です","ko":"GPS 추적 활성","zh-CN":"GPS 追踪进行中","zh-TW":"GPS 追蹤進行中"},
    "notif_btn_pause": {"en":"Pause","tr":"Duraklat","es":"Pausa","fr":"Pause","de":"Pause","uk":"Пауза","ar":"إيقاف","ka":"პაუზა","ja":"一時停止","ko":"일시정지","zh-CN":"暂停","zh-TW":"暫停"},
    "notif_btn_start": {"en":"Start","tr":"Başlat","es":"Iniciar","fr":"Démarrer","de":"Start","uk":"Старт","ar":"بدء","ka":"გაშვება","ja":"開始","ko":"시작","zh-CN":"开始","zh-TW":"開始"},
    "notif_btn_stop": {"en":"Stop","tr":"Dur","es":"Detener","fr":"Arrêter","de":"Stopp","uk":"Стоп","ar":"إيقاف","ka":"გაჩერება","ja":"停止","ko":"정지","zh-CN":"停止","zh-TW":"停止"},
    "notif_btn_led": {"en":"LED","tr":"Lamba","es":"LED","fr":"LED","de":"LED","uk":"Індикатор","ar":"مصباح","ka":"ინდიკატორი","ja":"LED","ko":"LED","zh-CN":"指示灯","zh-TW":"指示燈"},

    # ── Форматные строки ──
    "fmt_meters": {"en":"%d m","tr":"%d m","es":"%d m","fr":"%d m","de":"%d m","uk":"%d м","ar":"%d م","ka":"%d მ","ja":"%d m","ko":"%d m","zh-CN":"%d 米","zh-TW":"%d 米"},
    "fmt_km_m": {"en":"%1$d km %2$d m","tr":"%1$d km %2$d m","es":"%1$d km %2$d m","fr":"%1$d km %2$d m","de":"%1$d km %2$d m","uk":"%1$d км %2$d м","ar":"%1$d كم %2$d م","ka":"%1$d კმ %2$d მ","ja":"%1$d km %2$d m","ko":"%1$d km %2$d m","zh-CN":"%1$d 公里 %2$d 米","zh-TW":"%1$d 公里 %2$d 米"},
    "fmt_km_only": {"en":"%d km","tr":"%d km","es":"%d km","fr":"%d km","de":"%d km","uk":"%d км","ar":"%d كم","ka":"%d კმ","ja":"%d km","ko":"%d km","zh-CN":"%d 公里","zh-TW":"%d 公里"},
    "fmt_feet": {"en":"%d ft","tr":"%d ft","es":"%d ft","fr":"%d ft","de":"%d ft","uk":"%d футів","ar":"%d قدم","ka":"%d ფუტი","ja":"%d ft","ko":"%d ft","zh-CN":"%d 英尺","zh-TW":"%d 英尺"},
    "fmt_mi_ft": {"en":"%1$d mi %2$d ft","tr":"%1$d mil %2$d ft","es":"%1$d mi %2$d ft","fr":"%1$d mi %2$d ft","de":"%1$d mi %2$d ft","uk":"%1$d милі %2$d футів","ar":"%1$d ميل %2$d قدم","ka":"%1$d მილი %2$d ფუტი","ja":"%1$d マイル %2$d ft","ko":"%1$d mi %2$d ft","zh-CN":"%1$d 英里 %2$d 英尺","zh-TW":"%1$d 英里 %2$d 英尺"},
    "fmt_mi_only": {"en":"%d mi","tr":"%d mil","es":"%d mi","fr":"%d mi","de":"%d mi","uk":"%d милі","ar":"%d ميل","ka":"%d მილი","ja":"%d マイル","ko":"%d mi","zh-CN":"%d 英里","zh-TW":"%d 英里"},

    # ── Голосовые команды ──
    "shortcut_start_track": {"en":"Let's go","tr":"Yola çıktım","es":"Vámonos","fr":"C'est parti","de":"Los geht's","uk":"Я поїхав","ar":"لننطلق","ka":"წავედი","ja":"出発","ko":"출발","zh-CN":"出发了","zh-TW":"出發了"},
    "shortcut_stop_track": {"en":"I stopped","tr":"Durdum","es":"Me detuve","fr":"Je m'arrête","de":"Ich bin angekommen","uk":"Я зупинився","ar":"توقفت","ka":"გავჩერდი","ja":"到着","ko":"멈춤","zh-CN":"我到了","zh-TW":"我到了"},
    "shortcut_announce_status": {"en":"What's my distance","tr":"Mesafem ne kadar","es":"¿Cuánta distancia?","fr":"Quelle distance?","de":"Wie weit?","uk":"Який пробіг","ar":"كم المسافة","ka":"რა მანძილია","ja":"距離は？","ko":"주행거리는?","zh-CN":"行了多远","zh-TW":"行了多遠"},

    # ── Языковые эндонимы ──
    "lang_system": {"en":"System","tr":"Sistem","es":"Sistema","fr":"Système","de":"System","uk":"Системний","ar":"النظام","ka":"სისტემური","ja":"システム","ko":"시스템","zh-CN":"系统","zh-TW":"系統"},
    "lang_ru": {"en":"Русский","tr":"Русский","es":"Русский","fr":"Русский","de":"Русский","uk":"Русский","ar":"Русский","ka":"Русский","ja":"Русский","ko":"Русский","zh-CN":"Русский","zh-TW":"Русский"},
    "lang_en": {"en":"English","tr":"English","es":"English","fr":"English","de":"English","uk":"English","ar":"English","ka":"English","ja":"English","ko":"English","zh-CN":"English","zh-TW":"English"},
    "lang_es": {"en":"Español","tr":"Español","es":"Español","fr":"Español","de":"Español","uk":"Español","ar":"Español","ka":"Español","ja":"Español","ko":"Español","zh-CN":"Español","zh-TW":"Español"},
    "lang_fr": {"en":"Français","tr":"Français","es":"Français","fr":"Français","de":"Français","uk":"Français","ar":"Français","ka":"Français","ja":"Français","ko":"Français","zh-CN":"Français","zh-TW":"Français"},
    "lang_de": {"en":"Deutsch","tr":"Deutsch","es":"Deutsch","fr":"Deutsch","de":"Deutsch","uk":"Deutsch","ar":"Deutsch","ka":"Deutsch","ja":"Deutsch","ko":"Deutsch","zh-CN":"Deutsch","zh-TW":"Deutsch"},
    "lang_zh_cn": {"en":"简体中文","tr":"简体中文","es":"简体中文","fr":"简体中文","de":"简体中文","uk":"简体中文","ar":"简体中文","ka":"简体中文","ja":"简体中文","ko":"简体中文","zh-CN":"简体中文","zh-TW":"简体中文"},
    "lang_zh_tw": {"en":"繁體中文","tr":"繁體中文","es":"繁體中文","fr":"繁體中文","de":"繁體中文","uk":"繁體中文","ar":"繁體中文","ka":"繁體中文","ja":"繁體中文","ko":"繁體中文","zh-CN":"繁體中文","zh-TW":"繁體中文"},
    "lang_ko": {"en":"한국어","tr":"한국어","es":"한국어","fr":"한국어","de":"한국어","uk":"한국어","ar":"한국어","ka":"한국어","ja":"한국어","ko":"한국어","zh-CN":"한국어","zh-TW":"한국어"},
    "lang_ja": {"en":"日本語","tr":"日本語","es":"日本語","fr":"日本語","de":"日本語","uk":"日本語","ar":"日本語","ka":"日本語","ja":"日本語","ko":"日本語","zh-CN":"日本語","zh-TW":"日本語"},
    "lang_tr": {"en":"Türkçe","tr":"Türkçe","es":"Türkçe","fr":"Türkçe","de":"Türkçe","uk":"Türkçe","ar":"Türkçe","ka":"Türkçe","ja":"Türkçe","ko":"Türkçe","zh-CN":"Türkçe","zh-TW":"Türkçe"},
    "lang_ar": {"en":"العربية","tr":"العربية","es":"العربية","fr":"العربية","de":"العربية","uk":"العربية","ar":"العربية","ka":"العربية","ja":"العربية","ko":"العربية","zh-CN":"العربية","zh-TW":"العربية"},
    "lang_ka": {"en":"ქართული","tr":"ქართული","es":"ქართული","fr":"ქართული","de":"ქართული","uk":"ქართული","ar":"ქართული","ka":"ქართული","ja":"ქართული","ko":"ქართული","zh-CN":"ქართული","zh-TW":"ქართული"},
    "lang_uk": {"en":"Українська","tr":"Українська","es":"Українська","fr":"Українська","de":"Українська","uk":"Українська","ar":"Українська","ka":"Українська","ja":"Українська","ko":"Українська","zh-CN":"Українська","zh-TW":"Українська"},

    # ── Новые Настройки Телеметрии ──
    "settings_delete_profile": {"en":"Delete Profile","tr":"Profili Sil","es":"Eliminar Perfil","fr":"Supprimer le profil","de":"Profil löschen","uk":"Видалити профіль","ar":"حذف الملف الشخصي","ka":"პროფილის წაშლა","ja":"プロファイル削除","ko":"프로필 삭제","zh-CN":"删除档案","zh-TW":"刪除檔案"},
    "settings_auto_transport_title": {"en":"Detect Transport Ride","tr":"Araç Yolculuğunu Algıla","es":"Detectar viaje en transporte","fr":"Détecter le trajet en transport","de":"Fahrt im Transportmittel erkennen","uk":"Визначати поїздку в транспорті","ar":"اكتشاف رحلة النقل","ka":"ტრანსპორტით მგზავრობის ამოცნობა","ja":"乗り物移動を検出","ko":"교통수단 이용 감지","zh-CN":"检测乘车行程","zh-TW":"偵測乘車行程"},
    "settings_auto_transport_desc": {"en":"Automatically pause battery tracking when speed exceeds the max limit.","tr":"Hız, maksimum limiti aştığında batarya hesaplamasını otomatik olarak duraklat.","es":"Pausar automáticamente el cálculo de la batería cuando la velocidad supere el límite máximo.","fr":"Mettre automatiquement en pause le calcul de la batterie lorsque la vitesse dépasse la limite maximale.","de":"Akkuberechnung automatisch pausieren, wenn die Geschwindigkeit das Limit überschreitet.","uk":"Автоматично призупиняти розрахунок заряду батареї при перевищенні максимальної швидкості.","ar":"إيقاف حساب البطارية مؤقتاً تلقائياً عندما تتجاوز السرعة الحد الأقصى.","ka":"ავტომატურად შეაჩერე ბატარეის გაანგარიშება, როცა სიჩქარე მაქსიმუმს გადააჭარბებს.","ja":"速度が上限を超えた場合、バッテリー計算を自動的に一時停止します。","ko":"속도가 최대 속도를 초과하면 배터리 계산을 자동으로 일시 중지합니다.","zh-CN":"当速度超过最高限速时，自动暂停电池计算。","zh-TW":"當速度超過最高速限時，自動暫停電池計算。"},
    "settings_voice_vibrate": {"en":"Vibrate on Command","tr":"Komutta Titreşim","es":"Vibrar al comando","fr":"Vibrer sur commande","de":"Vibration bei Befehl","uk":"Вібрація при команді","ar":"اهتزاز عند الأمر","ka":"ვიბრაცია ბრძანებისას","ja":"コマンド実行時に振動","ko":"명령 시 진동","zh-CN":"命令时振动","zh-TW":"命令時震動"},
    "settings_voice_sound": {"en":"Sound Alert","tr":"Sesli Uyarı","es":"Señal de sonido","fr":"Alerte sonore","de":"Tonsignal","uk":"Звуковий сигнал","ar":"تنبيه صوتي","ka":"ხმოვანი სიგნალი","ja":"サウンドアラート","ko":"소리 알림","zh-CN":"声音提示","zh-TW":"聲音提示"},
    "settings_contact_developer": {"en":"Contact Developer","tr":"Geliştirici ile İletişime Geç","es":"Contactar al desarrollador","fr":"Contacter le développeur","de":"Entwickler kontaktieren","uk":"Звернутися до розробника","ar":"اتصل بالمطور","ka":"დეველოპერთან დაკავშირება","ja":"開発者に連絡","ko":"개발자에게 문의","zh-CN":"联系开发者","zh-TW":"聯繫開發者"},

    # ── Лампочки и Статистика ──
    "lamp_number_title": {"en":"LED %d","tr":"LED %d","es":"LED %d","fr":"LED %d","de":"LED %d","uk":"Індикатор %d","ar":"مصباح %d","ka":"ინდიკატორი %d","ja":"LED %d","ko":"LED %d","zh-CN":"指示灯 %d","zh-TW":"指示燈 %d"},
    "lamp_stat_fact": {"en":"Actual: ≈ %.1f","tr":"Gerçek: ≈ %.1f","es":"Real: ≈ %.1f","fr":"Réel: ≈ %.1f","de":"Ist: ≈ %.1f","uk":"Факт: ≈ %.1f","ar":"الفعلي: ≈ %.1f","ka":"ფაქტობრივი: ≈ %.1f","ja":"実測: ≈ %.1f","ko":"실제: ≈ %.1f","zh-CN":"实际: ≈ %.1f","zh-TW":"實際: ≈ %.1f"},
    "lamp_stat_no_data": {"en":"No data","tr":"Veri yok","es":"Sin datos","fr":"Pas de données","de":"Keine Daten","uk":"Немає даних","ar":"لا توجد بيانات","ka":"მონაცემები არ არის","ja":"データなし","ko":"데이터 없음","zh-CN":"暂无数据","zh-TW":"暫無數據"},
    "dashboard_stat_warning": {"en":"⚠ Statistics differ significantly from the settings. Tap to adjust.","tr":"⚠ İstatistikler ayarlardan önemli ölçüde farklı. Ayarlamak için dokunun.","es":"⚠ Las estadísticas difieren significativamente de los ajustes. Toca para configurar.","fr":"⚠ Les statistiques diffèrent considérablement des paramètres. Appuyez pour ajuster.","de":"⚠ Statistiken weichen stark von den Einstellungen ab. Zum Anpassen tippen.","uk":"⚠ Статистика сильно відрізняється від встановлених значень. Натисніть для налаштування.","ar":"⚠ تختلف الإحصائيات بشكل كبير عن الإعدادات. اضغط للتعديل.","ka":"⚠ სტატისტიკა მნიშვნელოვნად განსხვავდება პარამეტრებისგან. შეეხეთ გასასწორებლად.","ja":"⚠ 統計が設定と大きく異なります。タップして調整してください。","ko":"⚠ 통계가 설정과 크게 다릅니다. 조정하려면 탭하세요.","zh-CN":"⚠ 统计数据与设置存在显著差异。点击进行调整。","zh-TW":"⚠ 統計數據與設定存在顯著差異。點擊進行調整。"},
}


def escape_xml(s: str) -> str:
    """Экранирование для Android string resources."""
    s = s.replace("&", "&amp;")
    s = s.replace("<", "&lt;")
    s = s.replace(">", "&gt;")
    s = s.replace("'", "\\'")
    # Не трогаем % (format args)
    return s


def generate_locale_file(locale_code: str, folder_name: str):
    """Генерирует strings.xml для одной локали."""
    path = os.path.join(RES_DIR, folder_name)
    os.makedirs(path, exist_ok=True)
    filepath = os.path.join(path, "strings.xml")

    lines = ["<?xml version='1.0' encoding='utf-8'?>", "<resources>"]

    for key, translations in TRANSLATIONS.items():
        value = translations.get(locale_code)
        if value:
            lines.append(f'  <string name="{key}">{escape_xml(value)}</string>')

    lines.append("</resources>")
    lines.append("")

    with open(filepath, "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(lines))

    return filepath


def main():
    print("[i18n] Generacia strings.xml dlia vsekh yazykov FigaGo...")
    print(f"   Master: {len(TRANSLATIONS)} keys")
    print()

    for locale_code, folder_name in LOCALES.items():
        filepath = generate_locale_file(locale_code, folder_name)
        count = sum(1 for t in TRANSLATIONS.values() if locale_code in t)
        print(f"  OK {locale_code:6s} -> {folder_name:16s} ({count} keys)")

    print(f"\nDone! Generated {len(LOCALES)} files.")


if __name__ == "__main__":
    main()

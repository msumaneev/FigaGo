package com.figago.service

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Коллектор ошибок приложения.
 *
 * Перехватывает необработанные исключения через Thread.setDefaultUncaughtExceptionHandler
 * и записывает в файл errors.jsonl (JSON Lines) в filesDir/telemetry/.
 *
 * Инициализируется в FigaGoApplication.onCreate().
 */
class ErrorLogCollector(private val context: Context) {

    companion object {
        private const val TELEMETRY_DIR = "telemetry"
        private const val ERRORS_FILE = "errors.jsonl"
        private const val MAX_FILE_SIZE = 1024 * 1024 // 1 MB max
    }

    private val telemetryDir: File by lazy {
        File(context.filesDir, TELEMETRY_DIR).also { it.mkdirs() }
    }

    private val _errorsFile: File by lazy {
        File(telemetryDir, ERRORS_FILE)
    }

    /**
     * Устанавливает перехватчик необработанных исключений.
     * Должен вызываться из Application.onCreate().
     */
    fun install() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                writeError(throwable)
                
                // Запускаем CrashActivity
                val intent = android.content.Intent(context, com.figago.ui.CrashActivity::class.java).apply {
                    putExtra("EXTRA_STACK_TRACE", throwable.stackTraceToString())
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                context.startActivity(intent)
                
                // Убиваем процесс, чтобы система не показала стандартный диалог
                android.os.Process.killProcess(android.os.Process.myPid())
                kotlin.system.exitProcess(10)
            } catch (_: Exception) {
                // Не допускаем рекурсию ошибок
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    /**
     * Записывает ошибку в файл errors.jsonl.
     */
    fun writeError(throwable: Throwable) {
        try {
            // Ротация файла при превышении размера
            if (_errorsFile.exists() && _errorsFile.length() > MAX_FILE_SIZE) {
                _errorsFile.delete()
            }

            val json = JSONObject().apply {
                put("timestamp", System.currentTimeMillis())
                put("date", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))
                put("errorType", throwable.javaClass.simpleName)
                put("message", throwable.message ?: "")
                put("stackTrace", throwable.stackTraceToString().take(2000))
            }

            FileWriter(_errorsFile, true).use { writer ->
                writer.appendLine(json.toString())
            }
        } catch (_: Exception) {
            // Молчим — не создаём рекурсивных ошибок
        }
    }

    /**
     * Возвращает файл errors.jsonl для экспорта (может не существовать).
     */
    fun getErrorsFile(): File = _errorsFile
}

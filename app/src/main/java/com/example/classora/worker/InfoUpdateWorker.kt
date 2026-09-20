package com.example.classora.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.classora.data.InfoNotification
import com.example.classora.data.InfoPreferences
import com.example.classora.utils.CSVParser
import com.example.classora.utils.NotificationScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class InfoUpdateWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val url = "https://docs.google.com/spreadsheets/d/1KRl35wo7H-obeWxOq7z5S-SaVnxSN8jQ31usPw4-UjI/export?format=csv"
            val content = URL(url).readText()
            val csvData = CSVParser.parseCsv(content)
            val parsedInfo = mutableListOf<InfoNotification>()
            val infoPreferences = InfoPreferences(context)

            csvData.forEachIndexed { index, parts ->
                if (index > 0 && parts.size >= 3) {
                    val timestampStr = parts[0]
                    var title = ""
                    var message = ""
                    var linkStr = ""

                    val linkIndex = parts.indexOfFirst { it.contains("http", ignoreCase = true) }
                    if (linkIndex != -1) {
                        linkStr = parts[linkIndex]
                    }

                    val possibleTextParts = parts.filterIndexed { i, s -> 
                        i != 0 && i != linkIndex && !s.contains("@") && s.isNotBlank() 
                    }
                    
                    if (possibleTextParts.isNotEmpty()) {
                        message = possibleTextParts.maxByOrNull { it.length } ?: ""
                        title = possibleTextParts.firstOrNull { it != message } ?: message
                    }

                    if (linkStr.contains("about:blank", ignoreCase = true)) linkStr = ""

                    val timestamp: Long = try {
                        val sdf = SimpleDateFormat("M/d/yyyy H:mm:ss", Locale.getDefault())
                        sdf.parse(timestampStr)?.time ?: System.currentTimeMillis()
                    } catch (e: Exception) {
                        System.currentTimeMillis()
                    }

                    if (title.isNotBlank() || message.isNotBlank()) {
                        parsedInfo.add(
                            InfoNotification(
                                id = timestampStr + title.hashCode(),
                                title = title,
                                message = message,
                                link = linkStr,
                                timestamp = timestamp
                            )
                        )
                    }
                }
            }

            if (parsedInfo.isNotEmpty()) {
                val newItems = infoPreferences.saveInfoNotifications(parsedInfo)
                if (newItems.isNotEmpty()) {
                    val notifiedIds = mutableListOf<String>()
                    newItems.forEach { item ->
                        NotificationScheduler.showInfoNotification(context, item.title, item.message)
                        notifiedIds.add(item.id)
                    }
                    infoPreferences.markAsNotified(notifiedIds)
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

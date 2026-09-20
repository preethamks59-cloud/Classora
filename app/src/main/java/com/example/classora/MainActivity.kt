package com.example.classora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.classora.ui.screens.MainScreen
import com.example.classora.ui.theme.ClassoraTheme
import com.example.classora.worker.InfoUpdateWorker
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Schedule background info updates
        scheduleInfoUpdates()
        
        setContent {
            // Force Light Mode: darkTheme = false
            ClassoraTheme(darkTheme = false) {
                MainScreen()
            }
        }
    }

    private fun scheduleInfoUpdates() {
        val infoRequest = PeriodicWorkRequestBuilder<InfoUpdateWorker>(15, TimeUnit.MINUTES)
            .setInitialDelay(1, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "InfoUpdateWork",
            ExistingPeriodicWorkPolicy.KEEP,
            infoRequest
        )
    }
}
